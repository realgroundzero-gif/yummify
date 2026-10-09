package de.yummify.app.ui.screens.recipe_detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.InventoryMath
import de.yummify.app.data.model.MealType
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.repository.InventoryRepository
import de.yummify.app.data.repository.MealPlanRepository
import de.yummify.app.data.repository.RecipeRepository
import de.yummify.app.data.repository.ShoppingListRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val servings: Int = 2,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val isFavorite: Boolean = false,
    val checkedIngredients: Set<String> = emptySet(),
    val currentStep: Int = 0,
    val userRating: Int = 0,          // 0 = unrated, 1-5 = star rating
    val isRatingSaved: Boolean = false, // brief feedback after saving
    val isPlannedSaved: Boolean = false,
    val plannedDateText: String = "",
    /** Short feedback for Notion write-backs, inventory bookings and shopping list additions. */
    val inventoryMessage: String? = null,
    val consuming: Boolean = false,
    val addedShoppingCount: Int? = null // non-null when feedback should be shown
)

class RecipeDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val recipes = RecipeRepository.getInstance(application)
    private val mealPlanRepo = MealPlanRepository.getInstance(application)
    private val shoppingRepo = ShoppingListRepository.getInstance(application)
    private val inventoryRepo = InventoryRepository.getInstance(application)
    val stock = inventoryRepo.items
    /** Articles on the shopping list, so a recipe ingredient can show that it is already there. */
    val shoppingItems = shoppingRepo.items
    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()
    private var loadedId: String? = null
    private var feedbackJob: Job? = null

    init {
        viewModelScope.launch {
            recipes.favorites.collect { favorites ->
                loadedId?.let { id -> _uiState.value = _uiState.value.copy(isFavorite = id in favorites) }
            }
        }
    }

    /** Loads once per recipe; configuration changes keep the loaded state. */
    fun loadRecipe(recipeId: String, force: Boolean = false) {
        if (!force && loadedId == recipeId && (_uiState.value.recipe != null || _uiState.value.isLoading)) return
        loadedId = recipeId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            try {
                val recipe = recipes.recipe(recipeId)
                _uiState.value = _uiState.value.copy(
                    recipe = recipe,
                    servings = if (_uiState.value.recipe?.id == recipe.id) _uiState.value.servings else recipe.defaultServings,
                    isFavorite = recipe.isFavorite,
                    userRating = recipe.score.toInt().coerceIn(0, 5),
                    isLoading = false
                )
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, loadError = e.message ?: "Das Rezept konnte nicht geladen werden.")
            }
        }
    }

    fun adjustServings(delta: Int) {
        _uiState.value = _uiState.value.copy(servings = (_uiState.value.servings + delta).coerceIn(1, 20))
    }

    fun toggleIngredient(ingredientName: String) {
        val checked = _uiState.value.checkedIngredients
        _uiState.value = _uiState.value.copy(checkedIngredients = if (ingredientName in checked) checked - ingredientName else checked + ingredientName)
    }

    fun toggleFavorite() {
        loadedId?.let { recipes.toggleFavorite(it) }
    }

    fun setCurrentStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    fun portionMultiplier(): Double {
        val recipe = _uiState.value.recipe ?: return 1.0
        return _uiState.value.servings.toDouble() / recipe.defaultServings.coerceAtLeast(1).toDouble()
    }

    private fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(inventoryMessage = message)
    }

    fun messageShown() {
        _uiState.value = _uiState.value.copy(inventoryMessage = null)
    }

    fun updateRating(rating: Int) {
        val recipe = _uiState.value.recipe ?: return
        val previous = _uiState.value.userRating
        _uiState.value = _uiState.value.copy(userRating = rating, isRatingSaved = false)
        viewModelScope.launch {
            try {
                recipes.setRating(recipe.id, rating)
                _uiState.value = _uiState.value.copy(isRatingSaved = true)
                delay(2000)
                _uiState.value = _uiState.value.copy(isRatingSaved = false)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userRating = previous)
                showMessage("Bewertung nicht gespeichert: ${e.message ?: "Notion nicht erreichbar"}")
            }
        }
    }

    fun planRecipeForDate(date: LocalDate, mealType: MealType) {
        val recipe = _uiState.value.recipe ?: return
        mealPlanRepo.addMealPlan(recipe, date, mealType)
        _uiState.value = _uiState.value.copy(
            isPlannedSaved = true,
            plannedDateText = "${date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} (${mealType.displayName})"
        )
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            try {
                recipes.setPlannedDate(recipe.id, mealPlanRepo.plannedDateFor(recipe.id))
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                showMessage("Im Wochenplan gespeichert. Notion-Datum nicht aktualisiert: ${e.message ?: "nicht erreichbar"}")
            }
            delay(3500)
            _uiState.value = _uiState.value.copy(isPlannedSaved = false)
        }
    }

    fun consumeIngredients() {
        if (_uiState.value.consuming) return
        val recipe = _uiState.value.recipe ?: return
        val multiplier = portionMultiplier()
        _uiState.value = _uiState.value.copy(consuming = true)
        viewModelScope.launch {
            try {
                inventoryRepo.consume(recipe.ingredients, multiplier)
                showMessage("Zutaten für ${_uiState.value.servings} Portionen vom Vorrat abgebucht.")
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { showMessage(e.message ?: "Abbuchen fehlgeschlagen.") }
            finally { _uiState.value = _uiState.value.copy(consuming = false) }
        }
    }

    /** Puts one ingredient on the shopping list: what is still missing, or the full amount if nothing usable is in stock. */
    fun addIngredientToShoppingList(ingredient: de.yummify.app.data.model.Ingredient) {
        val multiplier = portionMultiplier()
        val amount = InventoryMath.shoppingAmount(ingredient, multiplier, stock.value)
        shoppingRepo.addIngredients(_uiState.value.recipe?.title.orEmpty(), listOf(ingredient.copy(amount = amount)), 1.0)
    }

    fun addIngredientsToShoppingList() {
        val recipe = _uiState.value.recipe ?: return
        val multiplier = portionMultiplier()
        val missing = recipe.ingredients.mapNotNull { ingredient ->
            val amount = InventoryMath.missing(ingredient, multiplier, stock.value)
            if (amount > 0) ingredient.copy(amount = amount) else null
        }
        val count = shoppingRepo.addIngredients(recipe.title, missing, 1.0)
        _uiState.value = _uiState.value.copy(addedShoppingCount = count)
        viewModelScope.launch {
            delay(3000)
            _uiState.value = _uiState.value.copy(addedShoppingCount = null)
        }
    }
}
