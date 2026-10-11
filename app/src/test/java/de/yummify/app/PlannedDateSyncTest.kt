package de.yummify.app

import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.MealType
import de.yummify.app.data.repository.MealPlanRepository
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PlannedDateSyncTest {
    private val today = LocalDate.of(2026, 10, 11)
    private fun meal(recipe: String, date: String) = MealPlanItem("m-$recipe-$date", "So", 1, MealType.DINNER, recipe, "T", "", null, null, null, false, date)

    @Test fun newPlansArePending() {
        val pending = MealPlanRepository.pendingPlannedUpdates(listOf(meal("a", "2026-10-13"), meal("b", "2026-10-12")), emptyMap(), today)
        assertEquals(mapOf("a" to LocalDate.of(2026, 10, 13), "b" to LocalDate.of(2026, 10, 12)), pending)
    }

    @Test fun nothingIsPendingWhenNotionAlreadyHoldsTheDate() {
        assertTrue(MealPlanRepository.pendingPlannedUpdates(listOf(meal("a", "2026-10-13")), mapOf("a" to "2026-10-13"), today).isEmpty())
    }

    @Test fun removedPlanClearsTheDateInNotion() {
        val pending = MealPlanRepository.pendingPlannedUpdates(emptyList(), mapOf("a" to "2026-10-13"), today)
        assertEquals(mapOf<String, LocalDate?>("a" to null), pending)
    }

    @Test fun dateMovesOnWhenTheNextPlanChanges() {
        // The plan for 10 Oct is past; the next one is 14 Oct, so Notion's value is stale.
        val meals = listOf(meal("a", "2026-10-10"), meal("a", "2026-10-14"))
        assertEquals(mapOf<String, LocalDate?>("a" to LocalDate.of(2026, 10, 14)), MealPlanRepository.pendingPlannedUpdates(meals, mapOf("a" to "2026-10-10"), today))
    }

    @Test fun mealsWithoutRecipeAreIgnored() {
        assertTrue(MealPlanRepository.pendingPlannedUpdates(listOf(meal("", "2026-10-13").copy(recipeId = null)), emptyMap(), today).isEmpty())
    }
}
