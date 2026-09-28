package de.yummify.app.ui.screens.meal_planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.MealType
import de.yummify.app.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DayTab(
    val key: String,         // "Mo", "Di", ...
    val dayOfMonth: Int,
    val isToday: Boolean
)

data class MealPlannerUiState(
    val weekLabel: String = "",
    val weekRange: String = "",
    val days: List<DayTab> = emptyList(),
    val selectedDayKey: String = "",
    val meals: List<MealPlanItem> = emptyList(),
    val dayMeals: List<MealPlanItem> = emptyList(),
    val totalCalories: Int = 0,
    val targetCalories: Int = 2100,
    val totalProtein: Int = 0,
    val targetProtein: Int = 130,
    val totalCarbs: Int = 0,
    val targetCarbs: Int = 220,
    val isSyncing: Boolean = false
)

class MealPlannerViewModel : ViewModel() {
    private val repository = RecipeRepository()
    private val _uiState = MutableStateFlow(MealPlannerUiState())
    val uiState: StateFlow<MealPlannerUiState> = _uiState.asStateFlow()

    private val dayNames = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    init { loadMealPlan() }

    private fun loadMealPlan() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val monday = today.minusDays(((today.dayOfWeek.value - 1).toLong()))
            val weekYear = today.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            val startStr = "${monday.dayOfMonth}. ${monday.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN)}"
            val endDate = monday.plusDays(6)
            val endStr = "${endDate.dayOfMonth}. ${endDate.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN)}"

            val days = (0..6).map { offset ->
                val day = monday.plusDays(offset.toLong())
                DayTab(
                    key = dayNames[offset],
                    dayOfMonth = day.dayOfMonth,
                    isToday = day == today
                )
            }

            val todayKey = dayNames.getOrElse((today.dayOfWeek.value - 1)) { "Mo" }
            val meals = repository.getMealPlan()

            _uiState.value = _uiState.value.copy(
                weekLabel = "KW $weekYear",
                weekRange = "$startStr – $endStr",
                days = days,
                selectedDayKey = todayKey,
                meals = meals
            )
            selectDay(todayKey, meals)
        }
    }

    fun selectDay(dayKey: String, allMeals: List<MealPlanItem>? = null) {
        val meals = allMeals ?: _uiState.value.meals
        val dayMeals = meals.filter { it.dayOfWeek == dayKey }
        val totalCal = dayMeals.sumOf { it.calories }
        val totalProt = dayMeals.sumOf { it.proteinGrams }
        val totalCarbs = dayMeals.sumOf { it.proteinGrams * 2 } // approx for demo
        _uiState.value = _uiState.value.copy(
            selectedDayKey = dayKey,
            dayMeals = dayMeals,
            totalCalories = totalCal,
            totalProtein = totalProt,
            totalCarbs = totalCarbs.coerceAtMost(220)
        )
    }

    fun toggleCooked(mealId: String) {
        val updated = _uiState.value.meals.map {
            if (it.id == mealId) it.copy(isCooked = !it.isCooked) else it
        }
        _uiState.value = _uiState.value.copy(meals = updated)
        selectDay(_uiState.value.selectedDayKey, updated)
    }

    fun syncFromNotion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }
}
