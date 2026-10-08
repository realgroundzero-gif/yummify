package de.yummify.app.ui.screens.recipe_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.RecipeLayout
import de.yummify.app.data.model.RecipeDraft
import de.yummify.app.data.remote.NotionRecipeApi
import de.yummify.app.data.repository.RecipeRepository
import de.yummify.app.data.repository.RecipeState
import de.yummify.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime

data class RecipeListUiState(
    val recipes: List<Recipe> = emptyList(),
    val filteredRecipes: List<Recipe> = emptyList(),
    val categories: List<String> = listOf("all"),
    val heroRecipe: Recipe? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "all",
    val favoritesOnly: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "",
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val creationError: String? = null,
    val error: String? = null,
    val fromNotion: Boolean = false,
    val layout: RecipeLayout = RecipeLayout.Default
)

private data class Filters(val query: String = "", val category: String = "all", val favoritesOnly: Boolean = false)
private data class Creation(val busy: Boolean = false, val error: String? = null)

class RecipeListViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = UserPreferencesRepository.getInstance(application)
    private val recipes = RecipeRepository.getInstance(application)
    private val filters = MutableStateFlow(Filters())
    private val creation = MutableStateFlow(Creation())

    val uiState: StateFlow<RecipeListUiState> = combine(recipes.state, filters, creation, prefsRepo.preferences) { state, filter, create, prefs ->
        build(state, filter, create, prefs.recipeLayout)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, build(recipes.state.value, Filters(), Creation(), prefsRepo.preferences.value.recipeLayout))

    private fun build(state: RecipeState, filter: Filters, create: Creation, layout: RecipeLayout): RecipeListUiState {
        val all = state.recipes
        val browsing = filter.query.isBlank() && filter.category == "all" && !filter.favoritesOnly
        val hero = if (browsing && all.size > 1) all.firstOrNull { it.isHeroOfDay } ?: all.first() else null
        val filtered = all.filter { recipe ->
            recipe.id != hero?.id && matches(recipe, filter.query) &&
                (filter.category == "all" || recipe.category.equals(filter.category, true) || recipe.tags.any { it.equals(filter.category, true) }) &&
                (!filter.favoritesOnly || recipe.isFavorite)
        }
        return RecipeListUiState(
            recipes = all, filteredRecipes = filtered, categories = extractCategories(all), heroRecipe = hero,
            searchQuery = filter.query, selectedCategory = filter.category, favoritesOnly = filter.favoritesOnly,
            isSyncing = state.loading && all.isNotEmpty(), isLoading = state.loading && all.isEmpty(),
            lastSyncTime = state.lastSync?.let(::relative).orEmpty(), isCreating = create.busy, creationError = create.error,
            error = state.error, fromNotion = state.fromNotion, layout = layout
        )
    }

    private fun matches(recipe: Recipe, query: String) = query.isBlank() ||
        recipe.title.contains(query, true) || recipe.description.contains(query, true) ||
        recipe.tags.any { it.contains(query, true) } || recipe.ingredients.any { it.name.contains(query, true) }

    private fun relative(time: LocalDateTime): String {
        val minutes = Duration.between(time, LocalDateTime.now()).toMinutes()
        return when {
            minutes < 1 -> "Gerade eben"
            minutes < 60 -> "Vor $minutes Min."
            time.toLocalDate() == LocalDateTime.now().toLocalDate() -> "Heute, ${"%02d:%02d".format(time.hour, time.minute)}"
            else -> "%02d.%02d., %02d:%02d".format(time.dayOfMonth, time.monthValue, time.hour, time.minute)
        }
    }

    private fun extractCategories(recipes: List<Recipe>): List<String> {
        val extracted = recipes.flatMap { recipe ->
            val tags = recipe.tags.filter { it.isNotBlank() && !it.equals("all", ignoreCase = true) }
            tags.ifEmpty { listOf(recipe.category) }
        }.map { it.trim() }.filter { it.isNotBlank() && !it.equals("all", ignoreCase = true) }.distinct().sorted()
        return listOf("all") + extracted
    }

    fun beginCreation() { creation.value = creation.value.copy(error = null) }

    fun createRecipe(draft: RecipeDraft, onSuccess: () -> Unit) {
        if (creation.value.busy) return
        creation.value = Creation(busy = true)
        viewModelScope.launch {
            try {
                val prefs = prefsRepo.preferences.value
                val id = withContext(Dispatchers.IO) { NotionRecipeApi(prefs.tokenInput).create(prefs.databaseIdInput, draft) }
                val category = draft.category.trim()
                recipes.addCreated(Recipe(id, draft.title.trim(), draft.description.trim(), draft.imageUrl.trim(), null, null, null, null, null,
                    category.ifBlank { "all" }.lowercase(), listOfNotNull(category.ifBlank { null }),
                    ingredients = RecipeRepository.parseIngredients(draft.ingredients), instructions = draft.steps,
                    defaultServings = draft.servings, notionPageId = id, notionUrl = "https://www.notion.so/${id.replace("-", "")}"))
                filters.value = Filters()
                creation.value = Creation()
                onSuccess()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) { creation.value = Creation(error = e.message ?: "Das Rezept konnte nicht gespeichert werden.") }
        }
    }

    fun onSearchQueryChanged(query: String) { filters.value = filters.value.copy(query = query) }
    fun onCategorySelected(category: String) { filters.value = filters.value.copy(category = category) }
    fun onFavoritesOnlyToggled() { filters.value = filters.value.copy(favoritesOnly = !filters.value.favoritesOnly) }
    fun onSyncClicked() = recipes.refreshAsync()
    fun onFavoriteToggled(recipe: Recipe) = recipes.toggleFavorite(recipe.id)
}
