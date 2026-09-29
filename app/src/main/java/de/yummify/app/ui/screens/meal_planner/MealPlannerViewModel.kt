package de.yummify.app.ui.screens.meal_planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.MealType
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.repository.MealPlanRepository
import de.yummify.app.data.repository.RecipeRepository
import de.yummify.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

data class DayTab(
    val key: String,         // "Mo", "Di", ...
    val dayOfMonth: Int,
    val isToday: Boolean
)

data class MealPlannerUiState(
    val weekOffset: Int = 0,
    val weekLabel: String = "",
    val weekRange: String = "",
    val days: List<DayTab> = emptyList(),
    val selectedDayKey: String = "",
    val meals: List<MealPlanItem> = emptyList(),
    val dayMeals: List<MealPlanItem> = emptyList(),
    val availableRecipes: List<Recipe> = emptyList(),
    val totalCalories: Int = 0,
    val targetCalories: Int = 2100,
    val totalProtein: Int = 0,
    val targetProtein: Int = 130,
    val totalCarbs: Int = 0,
    val targetCarbs: Int = 220,
    val isSyncing: Boolean = false
)

class MealPlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = UserPreferencesRepository.getInstance(application)
    private val mealPlanRepo = MealPlanRepository.getInstance(application)

    private val _uiState = MutableStateFlow(MealPlannerUiState())
    val uiState: StateFlow<MealPlannerUiState> = _uiState.asStateFlow()

    private val dayNames = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    init {
        viewModelScope.launch {
            mealPlanRepo.plannedMeals.collect {
                loadMealPlan()
            }
        }
    }

    fun previousWeek() {
        val newOffset = _uiState.value.weekOffset - 1
        _uiState.value = _uiState.value.copy(weekOffset = newOffset)
        loadMealPlan(newOffset)
    }

    fun nextWeek() {
        val newOffset = _uiState.value.weekOffset + 1
        _uiState.value = _uiState.value.copy(weekOffset = newOffset)
        loadMealPlan(newOffset)
    }

    private fun loadMealPlan(offsetWeeks: Int = _uiState.value.weekOffset) {
        viewModelScope.launch {
            val prefs = prefsRepo.preferences.value
            val repository = RecipeRepository(prefs.tokenInput, prefs.databaseIdInput)

            val baseDate = LocalDate.now().plusWeeks(offsetWeeks.toLong())
            val monday = baseDate.minusDays(((baseDate.dayOfWeek.value - 1).toLong()))
            val weekYear = monday.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            val startStr = "${monday.dayOfMonth}. ${monday.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN)}"
            val endDate = monday.plusDays(6)
            val endStr = "${endDate.dayOfMonth}. ${endDate.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN)}"

            val today = LocalDate.now()
            val days = (0..6).map { offset ->
                val day = monday.plusDays(offset.toLong())
                DayTab(
                    key = dayNames[offset],
                    dayOfMonth = day.dayOfMonth,
                    isToday = day == today
                )
            }

            val currentDayKey = if (_uiState.value.selectedDayKey.isNotEmpty()) _uiState.value.selectedDayKey else dayNames.getOrElse((today.dayOfWeek.value - 1)) { "Mo" }
            val storedMeals = mealPlanRepo.plannedMeals.value
            val meals = if (storedMeals.isNotEmpty()) storedMeals else if (repository.isNotionConfigured) _uiState.value.meals else repository.getMealPlan()
            val availableRecipes = repository.getAllRecipes()

            _uiState.value = _uiState.value.copy(
                weekLabel = "KW $weekYear",
                weekRange = "$startStr – $endStr",
                days = days,
                selectedDayKey = currentDayKey,
                meals = meals,
                availableRecipes = availableRecipes
            )
            selectDay(currentDayKey, meals)
        }
    }

    fun selectDay(dayKey: String, allMeals: List<MealPlanItem>? = null) {
        val meals = allMeals ?: _uiState.value.meals
        val dayMeals = meals.filter { it.dayOfWeek == dayKey }
        val totalCal = dayMeals.sumOf { it.calories }
        val totalProt = dayMeals.sumOf { it.proteinGrams }
        val totalCarbs = dayMeals.sumOf { it.proteinGrams * 2 }
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

    fun addRecipeToMeal(recipe: Recipe, mealType: MealType) {
        val selectedDay = _uiState.value.selectedDayKey
        val dayTab = _uiState.value.days.firstOrNull { it.key == selectedDay }
        val today = LocalDate.now()
        val targetDate = today.withDayOfMonth(dayTab?.dayOfMonth ?: today.dayOfMonth)
        mealPlanRepo.addMealPlan(recipe, targetDate, mealType)
    }

    fun removeMeal(mealId: String) {
        mealPlanRepo.removeMealPlan(mealId)
    }

    fun syncFromNotion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val prefs = prefsRepo.preferences.value
            val repository = RecipeRepository(prefs.tokenInput, prefs.databaseIdInput)
            val availableRecipes = repository.getAllRecipes()
            _uiState.value = _uiState.value.copy(
                availableRecipes = availableRecipes,
                isSyncing = false
            )
        }
    }
}
