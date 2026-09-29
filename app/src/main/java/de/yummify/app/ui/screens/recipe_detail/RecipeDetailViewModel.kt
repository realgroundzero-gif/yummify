package de.yummify.app.ui.screens.recipe_detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.repository.RecipeRepository
import de.yummify.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val servings: Int = 2,
    val isLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val checkedIngredients: Set<String> = emptySet(),
    val currentStep: Int = 0,
    val userRating: Int = 0,          // 0 = unrated, 1-5 = star rating
    val isRatingSaved: Boolean = false, // brief feedback after saving
    val isPlannedSaved: Boolean = false,
    val plannedDateText: String = ""
)

class RecipeDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = UserPreferencesRepository.getInstance(application)
    private val mealPlanRepo = de.yummify.app.data.repository.MealPlanRepository.getInstance(application)
    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val prefs = prefsRepo.preferences.value
            val repository = RecipeRepository(prefs.tokenInput, prefs.databaseIdInput)
            val recipe = repository.getRecipeById(recipeId)
            _uiState.value = _uiState.value.copy(
                recipe = recipe,
                servings = recipe?.defaultServings ?: 2,
                isFavorite = recipe?.isFavorite ?: false,
                userRating = recipe?.score?.toInt()?.coerceIn(0, 5) ?: 0,
                isLoading = false
            )
        }
    }

    fun adjustServings(delta: Int) {
        val current = _uiState.value.servings
        val newServings = (current + delta).coerceAtLeast(1).coerceAtMost(20)
        _uiState.value = _uiState.value.copy(servings = newServings)
    }

    fun toggleIngredient(ingredientName: String) {
        val checked = _uiState.value.checkedIngredients.toMutableSet()
        if (ingredientName in checked) checked.remove(ingredientName) else checked.add(ingredientName)
        _uiState.value = _uiState.value.copy(checkedIngredients = checked)
    }

    fun toggleFavorite() {
        _uiState.value = _uiState.value.copy(isFavorite = !_uiState.value.isFavorite)
    }

    fun setCurrentStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    fun portionMultiplier(): Double {
        val recipe = _uiState.value.recipe ?: return 1.0
        return _uiState.value.servings.toDouble() / recipe.defaultServings.toDouble()
    }

    fun updateRating(rating: Int) {
        val pageId = _uiState.value.recipe?.id ?: return
        // Update UI immediately for responsiveness
        _uiState.value = _uiState.value.copy(userRating = rating, isRatingSaved = false)
        viewModelScope.launch {
            val prefs = prefsRepo.preferences.value
            val repository = RecipeRepository(prefs.tokenInput, prefs.databaseIdInput)
            val success = repository.updateRating(pageId, rating)
            if (success) {
                _uiState.value = _uiState.value.copy(isRatingSaved = true)
                // Clear the "saved" indicator after 2 seconds
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(isRatingSaved = false)
            }
        }
    }

    fun planRecipeForDate(date: java.time.LocalDate, mealType: de.yummify.app.data.model.MealType) {
        val recipe = _uiState.value.recipe ?: return
        mealPlanRepo.addMealPlan(recipe, date, mealType)
        val formattedDate = date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        _uiState.value = _uiState.value.copy(
            isPlannedSaved = true,
            plannedDateText = "$formattedDate (${mealType.displayName})"
        )
        viewModelScope.launch {
            kotlinx.coroutines.delay(3500)
            _uiState.value = _uiState.value.copy(isPlannedSaved = false)
        }
    }
}
