package de.yummify.app

import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.MealType
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.model.WeekShoppingPlanner
import de.yummify.app.data.model.summary
import de.yummify.app.ui.screens.shopping_list.PlanRange
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeekShoppingPlannerTest {
    private val today = LocalDate.of(2026, 10, 7) // Wednesday
    private var ids = 0
    private fun plan(meals: List<MealPlanItem>, recipes: List<Recipe>, stock: List<InventoryItem> = emptyList(), list: List<ShoppingItem> = emptyList(),
                     from: LocalDate = today, to: LocalDate = today.plusDays(6)) =
        WeekShoppingPlanner.plan(meals, recipes, stock, list, from, to, today) { "id${ids++}" }

    private fun recipe(id: String, title: String, vararg ingredients: Ingredient) =
        Recipe(id, title, "", "", null, null, null, null, null, "x", emptyList(), ingredients = ingredients.toList(), instructions = emptyList())

    private fun meal(recipeId: String?, date: LocalDate, cooked: Boolean = false, type: MealType = MealType.DINNER) =
        MealPlanItem("m${ids++}", "Mi", date.dayOfMonth, type, recipeId, "Titel $recipeId", "", null, null, null, cooked, date.toString())

    private fun stock(name: String, quantity: Double, unit: String, expiry: String? = null) = InventoryItem(name = name, quantity = quantity, unit = unit, expiry = expiry)

    private val pasta = recipe("pasta", "Pasta", Ingredient("Rote Paprika", 2.0, "Stk"), Ingredient("Passierte Tomaten (Dose)", 400.0, "g"), Ingredient("Salz", 0.0, ""))
    private val salat = recipe("salat", "Salat", Ingredient("Paprika rot", 1.0, "Stk"), Ingredient("Tomaten passiert", 0.3, "kg"))

    @Test fun sameIngredientAcrossRecipesIsAddedUpEvenInDifferentUnits() {
        val result = plan(listOf(meal("pasta", today), meal("salat", today.plusDays(1))), listOf(pasta, salat))
        val paprika = result.toBuy.single { it.ingredient.name == "Rote Paprika" }
        assertEquals(3.0, paprika.ingredient.amount, 1e-9)
        assertEquals(listOf("Pasta", "Salat"), paprika.recipes)
        val tomatoes = result.toBuy.single { it.ingredient.name.contains("Tomaten") }
        assertEquals(700.0, tomatoes.ingredient.amount, 1e-9)
        assertEquals("g", tomatoes.ingredient.unit)
    }

    @Test fun differentProductsAreNotAddedUp() {
        val plain = recipe("plain", "Plain", Ingredient("Tomaten", 300.0, "g"))
        val result = plan(listOf(meal("pasta", today), meal("plain", today)), listOf(pasta, plain))
        val tomatoes = result.toBuy.filter { it.ingredient.name.contains("Tomaten") }
        assertEquals(mapOf("Passierte Tomaten (Dose)" to 400.0, "Tomaten" to 300.0), tomatoes.associate { it.ingredient.name to it.ingredient.amount })
    }

    @Test fun inventoryCoversWhatItCanAndTheRestIsBought() {
        val result = plan(listOf(meal("pasta", today)), listOf(pasta), stock = listOf(stock("Tomaten passiert", 1.0, "kg"), stock("Paprika", 1.0, "Stk")))
        // 400 g tomatoes are fully covered by 1 kg; one of two red peppers is covered by "Paprika" only if the names match (they must not).
        assertEquals(listOf("Passierte Tomaten (Dose)"), result.covered.map { it.name })
        assertEquals("Vorrat: 1000 g", result.covered.single().reason)
        assertTrue(result.toBuy.any { it.ingredient.name == "Rote Paprika" && it.ingredient.amount == 2.0 })
    }

    @Test fun partialStockReducesTheAmountAndSaysSo() {
        val result = plan(listOf(meal("pasta", today)), listOf(pasta), stock = listOf(stock("Rote Paprika", 1.0, "Stk")))
        val paprika = result.toBuy.single { it.ingredient.name == "Rote Paprika" }
        assertEquals(1.0, paprika.ingredient.amount, 1e-9)
        assertEquals("Vorrat deckt 1 Stk", paprika.note)
        assertTrue(result.covered.none { it.name == "Rote Paprika" })
    }

    @Test fun expiredStockAndIncompatibleUnitsDoNotCount() {
        val result = plan(listOf(meal("pasta", today)), listOf(pasta),
            stock = listOf(stock("Tomaten passiert", 5.0, "kg", expiry = "2026-10-01"), stock("Rote Paprika", 3.0, "g")))
        assertTrue(result.covered.isEmpty())
        assertEquals(3, result.toBuy.size)
    }

    @Test fun runningTheSamePlanTwiceAddsNothingTheSecondTime() {
        val first = plan(listOf(meal("pasta", today)), listOf(pasta))
        val listed = first.toBuy.map { ShoppingItem(it.ingredient.name, it.ingredient.name, "", "", quantity = it.ingredient.amount.takeIf { a -> a > 0 }, unit = it.ingredient.unit.ifBlank { "Stk" }) }
        val second = plan(listOf(meal("pasta", today)), listOf(pasta), list = listed)
        assertTrue(second.toBuy.isEmpty())
        assertEquals(3, second.alreadyListed)
    }

    @Test fun onlyTheMissingRestIsAddedWhenSomethingIsAlreadyListed() {
        val listed = listOf(ShoppingItem("1", "Rote Paprika", "1 Stk", "", quantity = 1.0, unit = "Stk", isChecked = true))
        val result = plan(listOf(meal("pasta", today)), listOf(pasta), list = listed)
        assertEquals(1.0, result.toBuy.single { it.ingredient.name == "Rote Paprika" }.ingredient.amount, 1e-9)
    }

    @Test fun cookedMealsOtherDaysAndMissingRecipesAreSkipped() {
        val meals = listOf(meal("pasta", today, cooked = true), meal("salat", today.plusDays(10)), meal("weg", today.plusDays(1)), meal(null, today))
        val result = plan(meals, listOf(pasta, salat))
        assertEquals(1, result.mealCount)
        assertEquals(listOf("Titel weg"), result.missingRecipes)
        assertTrue(result.toBuy.isEmpty())
    }

    @Test fun amountlessIngredientsAreListedOnceAndUpgradedWhenAnAmountAppears() {
        val withAmount = recipe("suppe", "Suppe", Ingredient("Salz", 1.0, "TL"))
        val result = plan(listOf(meal("pasta", today), meal("suppe", today)), listOf(pasta, withAmount))
        val salt = result.toBuy.filter { it.ingredient.name == "Salz" }
        assertEquals(1, salt.size)
        assertEquals(1.0, salt.single().ingredient.amount, 1e-9)
        assertEquals("TL", salt.single().ingredient.unit)
    }

    @Test fun amountlessIngredientInStockIsCovered() {
        val result = plan(listOf(meal("pasta", today)), listOf(pasta), stock = listOf(stock("Salz", 1.0, "Packung")))
        assertTrue(result.covered.any { it.name == "Salz" })
    }

    @Test fun summaryNamesAddedCoveredAndListedItems() {
        val result = plan(listOf(meal("pasta", today)), listOf(pasta), stock = listOf(stock("Tomaten passiert", 1.0, "kg")))
        assertEquals("2 Zutaten hinzugefügt. 1 schon im Vorrat.", result.summary())
    }

    @Test fun rangesStartTodayAndNeverIncludePastDays() {
        assertEquals(today to today.plusDays(6), PlanRange.NEXT_7_DAYS.dates(today))
        assertEquals(today to LocalDate.of(2026, 10, 11), PlanRange.THIS_WEEK.dates(today))
        assertEquals(LocalDate.of(2026, 10, 12) to LocalDate.of(2026, 10, 18), PlanRange.NEXT_WEEK.dates(today))
    }
}
