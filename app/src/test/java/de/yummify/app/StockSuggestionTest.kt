package de.yummify.app

import android.content.Context
import de.yummify.app.data.model.IngredientMatcher
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.model.StockSuggestions
import de.yummify.app.data.repository.ShoppingListRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StockSuggestionTest {
    private val today = LocalDate.of(2026, 10, 8)
    private fun stock(name: String, quantity: Double = 2.0, unit: String = "Stk", expiry: String? = null) = InventoryItem(name = name, quantity = quantity, unit = unit, expiry = expiry)
    private fun entry(id: String, name: String, checked: Boolean = false) = ShoppingItem(id, name, "1 Stk", "Fisch & Fleisch", "Pfanne", null, checked, 1.0, "Stk")

    @Before fun resetSingleton() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("yummify_shopping_list", Context.MODE_PRIVATE).edit().clear().commit()
        ShoppingListRepository::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true; set(null, null) }
    }

    @Test fun similarIsWeakerThanSameAndNeverEqual() {
        assertTrue(IngredientMatcher.similar("Hähnchenbrustfilet", "Hähnchenbrust"))
        assertTrue(IngredientMatcher.similar("Paprika", "Rote Paprika"))
        assertFalse(IngredientMatcher.similar("Rote Paprika", "Paprika rot"))   // already the same, no question needed
        assertFalse(IngredientMatcher.same("Hähnchenbrustfilet", "Hähnchenbrust"))
        assertFalse(IngredientMatcher.similar("Milch", "Butter"))
        assertFalse(IngredientMatcher.similar("Ei", "Eigelb"))                   // parts under five letters never count
    }

    @Test fun suggestsOnlyWithoutRuleMatchAndWithUnexpiredStock() {
        val list = listOf(entry("1", "Hähnchenbrustfilet"), entry("2", "Tomaten"), entry("3", "Zitronensaft", checked = true))
        val found = StockSuggestions.find(list, listOf(stock("Hähnchenbrust"), stock("Tomaten"), stock("Zitrone")), emptySet(), today)
        assertEquals(listOf("1"), found.map { it.itemId })    // 2 is a rule match, 3 is already checked
        assertEquals("Hähnchenbrust", found.single().stockName)
        assertEquals("2 Stk", found.single().stockAmount)

        assertTrue(StockSuggestions.find(list, listOf(stock("Hähnchenbrust", expiry = "2026-10-01")), emptySet(), today).isEmpty())
        assertTrue(StockSuggestions.find(list, listOf(stock("Hähnchenbrust", quantity = 0.0)), emptySet(), today).isEmpty())
        // A rule match elsewhere in the stock wins: nothing to ask.
        assertTrue(StockSuggestions.find(listOf(entry("1", "Tomaten")), listOf(stock("Cherrytomaten"), stock("Tomaten")), emptySet(), today).isEmpty())
    }

    @Test fun rememberedDecisionIgnoresQuantityAndSpelling() {
        val first = StockSuggestions.find(listOf(entry("1", "Hähnchenbrustfilet")), listOf(stock("Hähnchenbrust")), emptySet(), today).single()
        val again = StockSuggestions.find(listOf(entry("9", "hähnchenbrustfilet (500 g)")), listOf(stock("Hähnchenbrust", quantity = 9.0)), setOf(first.key), today)
        assertTrue(again.isEmpty())
    }

    @Test fun acceptingMovesEntryToCoveredAndUndoRemembersTheAnswer() {
        val repo = ShoppingListRepository.getInstance(RuntimeEnvironment.getApplication())
        repo.addIngredients("Pfanne", listOf(de.yummify.app.data.model.Ingredient("Hähnchenbrustfilet", 1.0, "Stk")), 1.0)
        val item = repo.items.value.single()
        val suggestion = StockSuggestions.find(repo.items.value, listOf(stock("Hähnchenbrust")), emptySet(), today).single()

        repo.acceptSuggestion(suggestion, newId = "c1")
        assertTrue(repo.items.value.isEmpty())
        assertEquals(item.name, repo.covered.value.single().name)
        assertTrue(repo.dismissedSuggestions.value.isEmpty())

        repo.restoreCovered("c1")                                   // "Doch kaufen"
        assertEquals(1, repo.items.value.size)
        assertTrue(repo.covered.value.isEmpty())
        assertEquals(setOf(suggestion.key), repo.dismissedSuggestions.value)
        assertTrue(StockSuggestions.find(repo.items.value, listOf(stock("Hähnchenbrust")), repo.dismissedSuggestions.value, today).isEmpty())
    }

    @Test fun rejectingIsStoredAcrossRestarts() {
        val repo = ShoppingListRepository.getInstance(RuntimeEnvironment.getApplication())
        repo.dismissSuggestion("a|b")
        ShoppingListRepository::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true; set(null, null) }
        assertEquals(setOf("a|b"), ShoppingListRepository.getInstance(RuntimeEnvironment.getApplication()).dismissedSuggestions.value)
    }
}
