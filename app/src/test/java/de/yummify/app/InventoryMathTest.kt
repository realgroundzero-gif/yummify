package de.yummify.app

import de.yummify.app.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class InventoryMathTest {
    private val today = LocalDate.of(2026, 10, 3)
    @Test fun convertsCompatibleUnitsOnly() {
        assertEquals(1250.0, InventoryMath.convert(1.25, "kg", "g")!!, 0.0001)
        assertEquals(0.75, InventoryMath.convert(750.0, "ml", "l")!!, 0.0001)
        assertEquals(3.0, InventoryMath.convert(3.0, "Stk.", "Stück")!!, 0.0001)
        assertNull(InventoryMath.convert(1.0, "g", "ml"))
        assertNull(InventoryMath.convert(1.0, "EL", "g"))
    }
    @Test fun missingScalesAndIgnoresExpiredAndDeleted() {
        val stock = listOf(InventoryItem(name = " Mehl ", quantity = 0.25, unit = "kg"),
            InventoryItem(name = "Mehl", quantity = 10.0, unit = "kg", expiry = "2026-10-02"),
            InventoryItem(name = "Mehl", quantity = 10.0, unit = "kg", deleted = true))
        assertEquals(150.0, InventoryMath.missing(Ingredient("Mehl", 200.0, "g"), 2.0, stock, today), 0.0001)
    }
    @Test fun expiryIncludesTodayAndNextSevenDays() {
        assertFalse(InventoryItem(expiry = "2026-10-03").isExpired(today))
        assertTrue(InventoryItem(expiry = "2026-10-10").expiresSoon(today))
        assertFalse(InventoryItem(expiry = "2026-10-11").expiresSoon(today))
        assertTrue(InventoryItem(quantity = 2.0, minimum = 2.0).isLow)
    }
    @Test fun rejectsInvalidInput() {
        assertNotNull(InventoryItem(name = "Milch", quantity = Double.NaN).validate())
        assertNotNull(InventoryItem(name = "Milch", quantity = -1.0).validate())
        assertNotNull(InventoryItem(name = "Milch", expiry = "2026-02-30").validate())
        assertNull(InventoryItem(name = "Milch", quantity = 0.0, expiry = "2026-10-03").validate())
    }
    @Test fun consumesEarliestExpiryAndConvertsUnits() {
        val late = InventoryItem(id = "late", name = "Mehl", quantity = 1.0, unit = "kg", expiry = "2026-12-01", dirty = false)
        val early = InventoryItem(id = "early", name = "Mehl", quantity = 100.0, unit = "g", expiry = "2026-10-03", dirty = false)
        val result = InventoryMath.consume(listOf(Ingredient("Mehl", 150.0, "g")), 2.0, listOf(late, early), today)
        assertEquals(0.0, result.first { it.id == "early" }.quantity, 0.0001)
        assertEquals(0.8, result.first { it.id == "late" }.quantity, 0.0001)
        assertTrue(result.all { it.dirty })
    }
    @Test fun repeatedIngredientCannotOverdrawStock() {
        val original = listOf(InventoryItem(name = "Mehl", quantity = 100.0, unit = "g"))
        assertThrows(IllegalArgumentException::class.java) {
            InventoryMath.consume(listOf(Ingredient("Mehl", 80.0, "g"), Ingredient("Mehl", 80.0, "g")), 1.0, original, today)
        }
        assertEquals(100.0, original.single().quantity, 0.0)
    }
    @Test fun exactDateDoesNotLeakAcrossMonthsOrYears() {
        val item = MealPlanItem("x", "Sa", 3, MealType.DINNER, "r", "Rezept", "", 0, 0, 1, plannedDate = "2026-10-03")
        assertTrue(item.matchesDate(today))
        assertFalse(item.matchesDate(LocalDate.of(2026, 11, 3)))
        assertFalse(item.matchesDate(LocalDate.of(2027, 10, 3)))
    }
    @Test fun categoryLengthAppliesToEachSelectedOption() {
        val choices = listOf("a".repeat(70), "b".repeat(70))
        assertNull(InventoryItem(name = "Mais", category = choices.joinToString(", "), categoryOptions = choices).validate())
        assertNotNull(InventoryItem(name = "Mais", category = "a".repeat(101)).validate())
    }

    @Test fun quantityStepperIsPreciseAndNeverGoesNegative() {
        assertEquals(0.3, InventoryMath.stepQuantity(0.2, 0.1), 0.0)
        assertEquals(0.0, InventoryMath.stepQuantity(0.1, -0.25), 0.0)
        assertEquals(0.1, InventoryMath.quantityStep("kg"), 0.0)
        assertEquals(50.0, InventoryMath.quantityStep("g"), 0.0)
        assertEquals(1.0, InventoryMath.quantityStep("Dose(n)"), 0.0)
        assertThrows(IllegalArgumentException::class.java) { InventoryMath.stepQuantity(Double.NaN, 1.0) }
    }

}
