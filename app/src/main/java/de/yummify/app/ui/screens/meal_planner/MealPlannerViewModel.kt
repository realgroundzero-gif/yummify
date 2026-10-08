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
    private val mealPlanRepo = MealPlanRepository.getInstance(application)
    private val recipes = RecipeRepository.getInstance(application)

    private val _uiState = MutableStateFlow(MealPlannerUiState())
    val uiState: StateFlow<MealPlannerUiState> = _uiState.asStateFlow()

    /** Notion write-back problems; the local plan is always saved. */
    val message = MutableStateFlow<String?>(null)

    private val dayNames = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    init {
        // Meal changes only rebuild the week; recipes come from the shared cache instead of a new Notion request.
        viewModelScope.launch { mealPlanRepo.plannedMeals.collect { loadMealPlan() } }
        viewModelScope.launch {
            recipes.state.collect { state ->
                _uiState.value = _uiState.value.copy(availableRecipes = state.recipes, isSyncing = state.loading)
            }
        }
    }

    fun previousWeek() {
        _uiState.value = _uiState.value.copy(weekOffset = _uiState.value.weekOffset - 1)
        loadMealPlan()
    }

    fun nextWeek() {
        _uiState.value = _uiState.value.copy(weekOffset = _uiState.value.weekOffset + 1)
        loadMealPlan()
    }

    private fun loadMealPlan() {
        val today = LocalDate.now()
        val baseDate = today.plusWeeks(_uiState.value.weekOffset.toLong())
        val monday = baseDate.minusDays((baseDate.dayOfWeek.value - 1).toLong())
        val endDate = monday.plusDays(6)
        fun label(date: LocalDate) = "${date.dayOfMonth}. ${date.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN)}"
        val days = (0..6).map { offset ->
            val day = monday.plusDays(offset.toLong())
            DayTab(key = dayNames[offset], dayOfMonth = day.dayOfMonth, isToday = day == today, date = day)
        }
        val currentDayKey = _uiState.value.selectedDayKey.ifEmpty { dayNames[today.dayOfWeek.value - 1] }
        val meals = mealPlanRepo.plannedMeals.value
        _uiState.value = _uiState.value.copy(
            weekLabel = "KW ${monday.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)}",
            weekRange = "${label(monday)} – ${label(endDate)}",
            days = days,
            meals = meals
        )
        selectDay(currentDayKey, meals)
    }

    fun selectDay(dayKey: String, allMeals: List<MealPlanItem>? = null) {
        val meals = allMeals ?: _uiState.value.meals
        val date = _uiState.value.days.firstOrNull { it.key == dayKey }?.date
        _uiState.value = _uiState.value.copy(
            selectedDayKey = dayKey,
            dayMeals = meals.filter { date != null && it.matchesDate(date) }
        )
    }

    fun toggleCooked(mealId: String) {
        mealPlanRepo.toggleCooked(mealId)
    }

    fun addRecipeToMeal(recipe: Recipe, mealType: MealType) {
        val targetDate = _uiState.value.days.firstOrNull { it.key == _uiState.value.selectedDayKey }?.date ?: LocalDate.now()
        mealPlanRepo.addMealPlan(recipe, targetDate, mealType)
        syncPlannedDate(recipe.id)
    }

    fun removeMeal(mealId: String) {
        val recipeId = _uiState.value.meals.firstOrNull { it.id == mealId }?.recipeId
        mealPlanRepo.removeMealPlan(mealId)
        if (!recipeId.isNullOrEmpty()) syncPlannedDate(recipeId)
    }

    /** Notion has one "Geplant am" per recipe: keep it on the next planned date, or clear it. */
    private fun syncPlannedDate(recipeId: String) {
        viewModelScope.launch {
            try {
                recipes.setPlannedDate(recipeId, mealPlanRepo.plannedDateFor(recipeId))
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                message.value = "Im Wochenplan gespeichert. Notion-Datum nicht aktualisiert: ${e.message ?: "nicht erreichbar"}"
            }
        }
    }

    fun syncFromNotion() = recipes.refreshAsync()
}
