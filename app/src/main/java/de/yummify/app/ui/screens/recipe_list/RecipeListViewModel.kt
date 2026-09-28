package de.yummify.app.ui.screens.recipe_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeListUiState(
    val recipes: List<Recipe> = emptyList(),
    val filteredRecipes: List<Recipe> = emptyList(),
    val heroRecipe: Recipe? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "all",
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "Vor 3 Min.",
    val isLoading: Boolean = false,
    val error: String? = null
)

class RecipeListViewModel : ViewModel() {
    private val repository = RecipeRepository()

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    init {
        loadRecipes()
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val recipes = repository.getAllRecipes()
            val hero = recipes.firstOrNull { it.isHeroOfDay } ?: recipes.firstOrNull()
            _uiState.value = _uiState.value.copy(
                recipes = recipes,
                filteredRecipes = recipes.filter { !it.isHeroOfDay },
                heroRecipe = hero,
                isLoading = false
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters(query, _uiState.value.selectedCategory)
    }

    fun onCategorySelected(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters(_uiState.value.searchQuery, category)
    }

    private fun applyFilters(query: String, category: String) {
        val allRecipes = _uiState.value.recipes
        val filtered = allRecipes.filter { recipe ->
            val isHero = recipe.isHeroOfDay
            val matchesQuery = query.isBlank() ||
                    recipe.title.contains(query, ignoreCase = true) ||
                    recipe.description.contains(query, ignoreCase = true) ||
                    recipe.tags.any { it.contains(query, ignoreCase = true) }
            val matchesCategory = category == "all" || recipe.category == category ||
                    (category == "veggie" && recipe.tags.any { it.lowercase().contains("vegetarisch") || it.lowercase().contains("vegan") }) ||
                    (category == "quick" && recipe.cookTimeMinutes <= 25) ||
                    (category == "protein" && recipe.tags.any { it.lowercase().contains("protein") })
            !isHero && matchesQuery && matchesCategory
        }
        _uiState.value = _uiState.value.copy(filteredRecipes = filtered)
    }

    fun onSyncClicked() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                lastSyncTime = "Gerade eben"
            )
        }
    }

    fun onFavoriteToggled(recipe: Recipe) {
        val updatedRecipes = _uiState.value.recipes.map {
            if (it.id == recipe.id) it.copy(isFavorite = !it.isFavorite) else it
        }
        _uiState.value = _uiState.value.copy(recipes = updatedRecipes)
        applyFilters(_uiState.value.searchQuery, _uiState.value.selectedCategory)
    }
}
