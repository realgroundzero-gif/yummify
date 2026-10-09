package de.yummify.app

import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.InventoryMath
import de.yummify.app.data.model.InventoryMath.Coverage
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class IngredientCoverageTest {
    private val today = LocalDate.of(2026, 10, 9)
    private fun stock(name: String, quantity: Double, unit: String, expiry: String? = null) = InventoryItem(name = name, quantity = quantity, unit = unit, expiry = expiry)
    private fun cover(ingredient: Ingredient, vararg items: InventoryItem, multiplier: Double = 1.0) = InventoryMath.cover(ingredient, multiplier, items.toList(), today)

    @Test fun sameNameAndComparableUnitsIsEnough() {
        assertEquals(Coverage.ENOUGH, cover(Ingredient("Kartoffeln", 500.0, "g"), stock("Kartoffeln", 2.0, "kg")).state)
        assertEquals(Coverage.ENOUGH, cover(Ingredient("Rote Linsen", 200.0, "g"), stock("Linsen rot", 500.0, "g")).state)
    }

    @Test fun sameArticleInAnotherUnitIsShownInsteadOfBeingCalledMissing() {
        // "Kartoffeln" and "Rote Linsen" exist in the inventory, but as pieces and packs while the recipe says grams.
        val potatoes = cover(Ingredient("Kartoffeln", 500.0, "g"), stock("Kartoffeln", 3.0, "Stück"))
        assertEquals(Coverage.OTHER_UNIT, potatoes.state)
        assertEquals("3 Stück", potatoes.otherUnit)
        assertEquals(500.0, potatoes.shortfall, 1e-9)           // nothing is counted as available
        assertEquals(Coverage.OTHER_UNIT, cover(Ingredient("Rote Linsen", 200.0, "g"), stock("Rote Linsen", 1.0, "Packung")).state)
    }

    @Test fun spellingVariantsOfUnitsAreComparable() {
        assertEquals(Coverage.ENOUGH, cover(Ingredient("Tomaten", 2.0, "Dosen"), stock("Tomaten", 3.0, "Dose")).state)
        assertEquals(Coverage.ENOUGH, cover(Ingredient("Linsen", 1.0, "Packung"), stock("Linsen", 2.0, "Packungen")).state)
        assertEquals(Coverage.ENOUGH, cover(Ingredient("Eier", 4.0, ""), stock("Eier", 6.0, "Stk.")).state)
        assertEquals(Coverage.ENOUGH, cover(Ingredient("Knoblauch", 2.0, "Zehen"), stock("Knoblauch", 5.0, "Zehe")).state)
    }

    @Test fun partialStockReportsTheShortfall() {
        val c = cover(Ingredient("Kartoffeln", 1.0, "kg"), stock("Kartoffeln", 600.0, "g"))
        assertEquals(Coverage.PARTIAL, c.state)
        assertEquals(400.0, c.shortfall * 1000, 1e-6)
        assertEquals(Coverage.PARTIAL, cover(Ingredient("Kartoffeln", 400.0, "g"), stock("Kartoffeln", 300.0, "g"), multiplier = 2.0).state)
    }

    @Test fun expiredOrEmptyStockDoesNotCount() {
        assertEquals(Coverage.MISSING, cover(Ingredient("Kartoffeln", 500.0, "g"), stock("Kartoffeln", 1.0, "kg", "2026-10-01")).state)
        assertEquals(Coverage.MISSING, cover(Ingredient("Kartoffeln", 500.0, "g"), stock("Kartoffeln", 0.0, "kg")).state)
    }

    @Test fun ingredientWithoutAmountNeedsSomeStock() {
        assertEquals(Coverage.ENOUGH, cover(Ingredient("Salz", 0.0, ""), stock("Salz", 1.0, "Packung")).state)
        assertEquals(Coverage.MISSING, cover(Ingredient("Salz", 0.0, "")).state)   // used to read "Im Vorrat" without any stock
    }

    @Test fun similarArticleIsOnlyAHint() {
        val c = cover(Ingredient("Festkochende Kartoffeln", 500.0, "g"), stock("Kartoffeln", 2.0, "kg"))
        assertEquals(Coverage.SIMILAR, c.state)
        assertEquals("Kartoffeln", c.otherUnit)
        assertEquals(500.0, c.shortfall, 1e-9)
        assertEquals(Coverage.MISSING, cover(Ingredient("Milch", 1.0, "l"), stock("Butter", 1.0, "Stk")).state)
    }
}
