package de.yummify.app.ui.screens.recipe_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.repository.RecipeRepository
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
    val currentStep: Int = 0
)

class RecipeDetailViewModel : ViewModel() {
    private val repository = RecipeRepository()
    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val recipe = repository.getRecipeById(recipeId)
            _uiState.value = _uiState.value.copy(
                recipe = recipe,
                servings = recipe?.defaultServings ?: 2,
                isFavorite = recipe?.isFavorite ?: false,
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
}
