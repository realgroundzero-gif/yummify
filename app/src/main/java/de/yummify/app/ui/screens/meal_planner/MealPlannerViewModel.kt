package de.yummify.app.ui.screens.meal_planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.matchesDate
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
    val isToday: Boolean,
    val date: LocalDate
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
                    isToday = day == today,
                    date = day
                )
            }

            val currentDayKey = if (_uiState.value.selectedDayKey.isNotEmpty()) _uiState.value.selectedDayKey else dayNames.getOrElse((today.dayOfWeek.value - 1)) { "Mo" }
            val storedMeals = mealPlanRepo.plannedMeals.value
            val meals = storedMeals
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
        val date = _uiState.value.days.firstOrNull { it.key == dayKey }?.date
        val dayMeals = meals.filter { date != null && it.matchesDate(date) }
        _uiState.value = _uiState.value.copy(
            selectedDayKey = dayKey,
            dayMeals = dayMeals
        )
    }

    fun toggleCooked(mealId: String) {
        mealPlanRepo.toggleCooked(mealId)
    }

    fun addRecipeToMeal(recipe: Recipe, mealType: MealType) {
        val selectedDay = _uiState.value.selectedDayKey
        val dayTab = _uiState.value.days.firstOrNull { it.key == selectedDay }
        val today = LocalDate.now()
        val targetDate = dayTab?.date ?: today
        mealPlanRepo.addMealPlan(recipe, targetDate, mealType)
        viewModelScope.launch {
            val prefs = prefsRepo.preferences.value
            val repository = RecipeRepository(prefs.tokenInput, prefs.databaseIdInput)
            repository.updatePlannedDate(recipe.id, targetDate)
        }
    }

    fun removeMeal(mealId: String) {
        val item = _uiState.value.meals.firstOrNull { it.id == mealId }
        mealPlanRepo.removeMealPlan(mealId)
        val recipeId = item?.recipeId
        if (!recipeId.isNullOrEmpty()) {
            viewModelScope.launch {
                val prefs = prefsRepo.preferences.value
                val repository = RecipeRepository(prefs.tokenInput, prefs.databaseIdInput)
                repository.updatePlannedDate(recipeId, null)
            }
        }
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
