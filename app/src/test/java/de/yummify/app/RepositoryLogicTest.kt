package de.yummify.app

import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.MealType
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.remote.RateLimitRetry
import de.yummify.app.data.repository.MealPlanRepository
import de.yummify.app.data.repository.ShoppingListRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RepositoryLogicTest {
    @Test fun shoppingCategoriesMatchWholeWordsAndCompounds() {
        // Regression: "Ei" used to match Rindfleisch, Reis and Weizenmehl.
        assertEquals("Fisch & Fleisch", ShoppingListRepository.categoryFor("Rindfleisch"))
        assertEquals("Vorrat & Trockenwaren", ShoppingListRepository.categoryFor("Reis"))
        assertEquals("Vorrat & Trockenwaren", ShoppingListRepository.categoryFor("Weizenmehl"))
        assertEquals("Kühlregal & Milchprodukte", ShoppingListRepository.categoryFor("Eier"))
        assertEquals("Kühlregal & Milchprodukte", ShoppingListRepository.categoryFor("Ei"))
        assertEquals("Gewürze & Öle", ShoppingListRepository.categoryFor("Olivenöl"))
        assertEquals("Gewürze & Öle", ShoppingListRepository.categoryFor("Paprikapulver"))
        assertEquals("Gewürze & Öle", ShoppingListRepository.categoryFor("Meersalz"))
        assertEquals("Obst & Gemüse", ShoppingListRepository.categoryFor("rote Paprika"))
        assertEquals("Obst & Gemüse", ShoppingListRepository.categoryFor("Kirschtomaten"))
    }

    @Test fun sameIngredientIsMergedAcrossRecipesAndUnits() {
        var next = 0
        val first = ShoppingListRepository.mergeIngredients(emptyList(), "Pasta", listOf(Ingredient("Tomaten", 200.0, "g")), 1.0) { "id${next++}" }
        val merged = ShoppingListRepository.mergeIngredients(first, "Salat", listOf(Ingredient("tomaten ", 0.3, "kg"), Ingredient("Gurke", 1.0, "")), 1.0) { "id${next++}" }
        val tomatoes = merged.single { it.name == "Tomaten" }
        assertEquals(500.0, tomatoes.quantity!!, 1e-9)
        assertEquals("500 g", tomatoes.amountWithUnit)
        assertEquals("Pasta · Salat", tomatoes.recipeName)
        assertEquals(2, merged.size)
    }

    @Test fun ingredientsWithoutAmountAreListedOnce() {
        val first = ShoppingListRepository.mergeIngredients(emptyList(), "Pasta", listOf(Ingredient("Salz", 0.0, "")), 1.0) { "a" }
        val second = ShoppingListRepository.mergeIngredients(first, "Suppe", listOf(Ingredient("Salz", 0.0, "")), 1.0) { "b" }
        assertEquals("Pasta · Suppe", second.single().recipeName)
    }

    @Test fun mergedEntriesStayHighlightedForTodaysRecipes() {
        val item = ShoppingItem("a", "Tomaten", "500 g", "Obst & Gemüse", recipeName = "Pasta · Salat", quantity = 500.0, unit = "g")
        assertTrue(de.yummify.app.widget.ShoppingListWidgetProvider.isToday(item, setOf("salat")))
    }

    @Test fun checkedOrIncompatibleEntriesAreNotMerged() {
        val bought = ShoppingItem("a", "Milch", "1 l", "Kühlregal & Milchprodukte", isChecked = true, quantity = 1.0, unit = "l")
        val result = ShoppingListRepository.mergeIngredients(listOf(bought), "Kuchen", listOf(Ingredient("Milch", 200.0, "g")), 1.0) { "b" }
        assertEquals(2, result.size)
    }

    private fun meal(id: String, recipe: String?, date: String?, calories: Int? = null) =
        MealPlanItem(id, "Mo", 28, MealType.DINNER, recipe, "Titel", "", calories, if (calories == 450) 25 else null, if (calories == 450) 25 else null, plannedDate = date)

    @Test fun demoMealsAndPlaceholderNutritionAreRemoved() {
        val migrated = MealPlanRepository.migrate(listOf(meal("m1", "1", null), meal("m2", "x", "2026-10-07"), meal("own", "x", "2026-10-07", 450)))
        assertEquals(listOf("m2", "own"), migrated.map { it.id })
        assertNull(migrated.last().calories)
    }

    @Test fun notionPlannedDatePrefersNextUpcomingDate() {
        val today = LocalDate.of(2026, 10, 7)
        val meals = listOf(meal("a", "r", "2026-10-01"), meal("b", "r", "2026-10-12"), meal("c", "r", "2026-10-09"), meal("d", "other", "2026-10-08"))
        assertEquals(LocalDate.of(2026, 10, 9), MealPlanRepository.nextPlannedDate(meals, "r", today))
        assertEquals(LocalDate.of(2026, 10, 1), MealPlanRepository.nextPlannedDate(meals.take(1), "r", today))
        assertNull(MealPlanRepository.nextPlannedDate(meals, "missing", today))
    }

    @Test fun rateLimitedRequestsAreRepeatedWithRetryAfter() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "2"))
            server.enqueue(MockResponse().setBody("ok"))
            val waits = mutableListOf<Long>()
            val client = OkHttpClient.Builder().addInterceptor(RateLimitRetry(sleep = { waits += it })).build()
            client.newCall(Request.Builder().url(server.url("/")).build()).execute().use { assertEquals("ok", it.body!!.string()) }
            assertEquals(listOf(2000L), waits)
        }
    }
}
