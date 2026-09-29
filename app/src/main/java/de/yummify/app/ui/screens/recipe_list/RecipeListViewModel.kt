package de.yummify.app.ui.screens.recipe_list

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

data class RecipeListUiState(
    val recipes: List<Recipe> = emptyList(),
    val filteredRecipes: List<Recipe> = emptyList(),
    val categories: List<String> = listOf("all"),
    val heroRecipe: Recipe? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "all",
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "Vor 3 Min.",
    val isLoading: Boolean = false,
    val error: String? = null
)

class RecipeListViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = UserPreferencesRepository.getInstance(application)

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.preferences.collect { prefs ->
                loadRecipes(prefs.tokenInput, prefs.databaseIdInput)
            }
        }
    }

    private fun extractCategories(recipes: List<Recipe>): List<String> {
        val extracted = recipes.flatMap { recipe ->
            val tags = recipe.tags.filter { it.isNotBlank() && !it.equals("all", ignoreCase = true) }
            if (tags.isNotEmpty()) tags else listOf(recipe.category)
        }.map { it.trim() }.filter { it.isNotBlank() && !it.equals("all", ignoreCase = true) }.distinct().sorted()
        return listOf("all") + extracted
    }

    fun loadRecipes(token: String = "", dbId: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val repository = RecipeRepository(token, dbId)
            val recipes = repository.getAllRecipes()
            val hero = recipes.firstOrNull { it.isHeroOfDay } ?: recipes.firstOrNull()
            val filtered = recipes.filter { it != hero }
            val categories = extractCategories(recipes)
            _uiState.value = _uiState.value.copy(
                recipes = recipes,
                filteredRecipes = filtered,
                heroRecipe = hero,
                categories = categories,
                isLoading = false
            )
            applyFilters(_uiState.value.searchQuery, _uiState.value.selectedCategory)
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
        val heroId = _uiState.value.heroRecipe?.id
        val filtered = allRecipes.filter { recipe ->
            val isHero = recipe.id == heroId && allRecipes.size > 1
            val matchesQuery = query.isBlank() ||
                    recipe.title.contains(query, ignoreCase = true) ||
                    recipe.description.contains(query, ignoreCase = true) ||
                    recipe.tags.any { it.contains(query, ignoreCase = true) } ||
                    recipe.ingredients.any { it.name.contains(query, ignoreCase = true) }
            val matchesCategory = category == "all" ||
                    recipe.category.equals(category, ignoreCase = true) ||
                    recipe.tags.any { it.equals(category, ignoreCase = true) }
            !isHero && matchesQuery && matchesCategory
        }
        _uiState.value = _uiState.value.copy(filteredRecipes = filtered)
    }

    fun onSyncClicked() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val prefs = prefsRepo.preferences.value
            val repository = RecipeRepository(prefs.tokenInput, prefs.databaseIdInput)
            val recipes = repository.getAllRecipes()
            val hero = recipes.firstOrNull { it.isHeroOfDay } ?: recipes.firstOrNull()
            val categories = extractCategories(recipes)
            _uiState.value = _uiState.value.copy(
                recipes = recipes,
                heroRecipe = hero,
                categories = categories,
                isSyncing = false,
                lastSyncTime = "Gerade eben"
            )
            applyFilters(_uiState.value.searchQuery, _uiState.value.selectedCategory)
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
