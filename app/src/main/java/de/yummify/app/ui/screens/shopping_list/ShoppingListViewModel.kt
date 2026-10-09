package de.yummify.app.ui.screens.shopping_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.CoveredItem
import de.yummify.app.data.model.HomeAssistantExport
import de.yummify.app.data.remote.HomeAssistantApi
import de.yummify.app.data.repository.UserPreferencesRepository
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.model.StockSuggestion
import de.yummify.app.data.model.StockSuggestions
import de.yummify.app.data.model.WeekShoppingPlanner
import de.yummify.app.data.model.summary
import de.yummify.app.data.repository.InventoryRepository
import de.yummify.app.data.repository.MealPlanRepository
import de.yummify.app.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import de.yummify.app.data.repository.ShoppingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShoppingListUiState(
    val allItems: List<ShoppingItem> = emptyList(),
    val visibleItems: List<ShoppingItem> = emptyList(),
    val groupedItems: Map<String, List<ShoppingItem>> = emptyMap(),
    val filter: String = "missing",
    val openItems: Int = 0,
    val transferring: Boolean = false,
    val planning: Boolean = false,
    val covered: List<CoveredItem> = emptyList(),
    val suggestions: List<StockSuggestion> = emptyList(),
    val homeAssistantReady: Boolean = false,
    val homeAssistantList: String = "",
    /** Articles a send would hand over right now, and how many of the open ones were sent before. */
    val homeAssistantNew: Int = 0,
    val homeAssistantOpen: Int = 0,
    val sendingToHomeAssistant: Boolean = false,
    val message: String? = null
)

/** Which planned meals feed the shopping list. Past days are never included. */
enum class PlanRange(val title: String) {
    NEXT_7_DAYS("Nächste 7 Tage"), THIS_WEEK("Rest dieser Woche"), NEXT_WEEK("Nächste Woche");

    fun dates(today: LocalDate): Pair<LocalDate, LocalDate> {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return when (this) {
            NEXT_7_DAYS -> today to today.plusDays(6)
            THIS_WEEK -> today to monday.plusDays(6)
            NEXT_WEEK -> monday.plusDays(7) to monday.plusDays(13)
        }
    }
}

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {
    private val shoppingRepo = ShoppingListRepository.getInstance(application)
    private val prefsRepo = UserPreferencesRepository.getInstance(application)
    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shoppingRepo.items.collect { items ->
                updateState(items, _uiState.value.filter)
            }
        }
        viewModelScope.launch {
            shoppingRepo.covered.collect { covered -> _uiState.value = _uiState.value.copy(covered = covered) }
        }
        viewModelScope.launch {
            combine(shoppingRepo.items, shoppingRepo.sentToHomeAssistant, prefsRepo.preferences) { list, sent, prefs ->
                Triple(HomeAssistantExport.plan(list, sent, includeSent = false).size, HomeAssistantExport.plan(list, sent, includeSent = true).size, prefs)
            }.collect { (fresh, open, prefs) ->
                _uiState.value = _uiState.value.copy(homeAssistantReady = prefs.homeAssistantConfigured, homeAssistantList = prefs.homeAssistantTodo.removePrefix("todo."),
                    homeAssistantNew = fresh, homeAssistantOpen = open)
            }
        }
        viewModelScope.launch {
            combine(shoppingRepo.items, InventoryRepository.getInstance(application).items, shoppingRepo.dismissedSuggestions) { list, stock, dismissed ->
                StockSuggestions.find(list, stock, dismissed)
            }.collect { suggestions -> _uiState.value = _uiState.value.copy(suggestions = suggestions) }
        }
    }

    /** Puts what the planned meals need on the list, minus inventory and what is already listed. */
    fun generateFromPlan(range: PlanRange) {
        if (_uiState.value.planning) return
        _uiState.value = _uiState.value.copy(planning = true, message = null)
        viewModelScope.launch {
            val message = try {
                withContext(Dispatchers.Default) {
                    val (from, to) = range.dates(LocalDate.now())
                    val recipes = RecipeRepository.getInstance(getApplication()).state.value.recipes
                    val result = WeekShoppingPlanner.plan(
                        MealPlanRepository.getInstance(getApplication()).plannedMeals.value, recipes,
                        InventoryRepository.getInstance(getApplication()).items.value, shoppingRepo.items.value, from, to)
                    when {
                        result.mealCount == 0 -> "Für „${range.title}“ ist kein offenes Gericht im Wochenplan."
                        recipes.isEmpty() -> "Die Rezepte sind noch nicht geladen. Bitte kurz warten oder in den Optionen synchronisieren."
                        else -> { shoppingRepo.applyPlan(result); "${range.title}: ${result.mealCount} Gerichte. ${result.summary()}" }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) { e.message ?: "Die Einkaufsliste konnte nicht erstellt werden." }
            _uiState.value = _uiState.value.copy(planning = false, message = message)
        }
    }

    /** Hands the open articles to the Home Assistant to-do list (Bring). Stops at the first error and keeps what was sent. */
    fun sendToHomeAssistant(includeSent: Boolean) {
        if (_uiState.value.sendingToHomeAssistant) return
        val prefs = prefsRepo.preferences.value
        if (!prefs.homeAssistantConfigured) return
        _uiState.value = _uiState.value.copy(sendingToHomeAssistant = true, message = null)
        viewModelScope.launch {
            val entries = HomeAssistantExport.plan(shoppingRepo.items.value, shoppingRepo.sentToHomeAssistant.value, includeSent)
            val sent = mutableMapOf<String, String>()
            val message = try {
                if (entries.isEmpty()) "Alles ist schon gesendet. Mit „Alle erneut senden“ geht es noch einmal an Home Assistant."
                else {
                    withContext(Dispatchers.IO) {
                        val api = HomeAssistantApi(prefs.homeAssistantUrl, prefs.homeAssistantToken)
                        entries.forEach { entry -> api.addItem(prefs.homeAssistantTodo, entry.name, entry.description); sent[entry.itemId] = entry.signature }
                    }
                    "${entries.size} Artikel an Home Assistant gesendet (${prefs.homeAssistantTodo.removePrefix("todo.")})."
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                (if (sent.isEmpty()) "" else "${sent.size} von ${entries.size} gesendet. ") + (e.message ?: "Senden an Home Assistant fehlgeschlagen.")
            } finally { if (sent.isNotEmpty()) shoppingRepo.markSentToHomeAssistant(sent) }
            _uiState.value = _uiState.value.copy(sendingToHomeAssistant = false, message = message)
        }
    }

    fun confirmSuggestion(suggestion: StockSuggestion) = shoppingRepo.acceptSuggestion(suggestion)
    fun rejectSuggestion(suggestion: StockSuggestion) = shoppingRepo.dismissSuggestion(suggestion.key)
    fun restoreCovered(id: String) = shoppingRepo.restoreCovered(id)
    fun dismissCovered() = shoppingRepo.dismissCovered()

    private fun updateState(items: List<ShoppingItem>, filter: String) {
        val visible = when (filter) {
            "missing" -> items.filter { !it.isChecked }
            else -> items
        }
        val grouped = visible.groupBy { it.category }
        val openCount = items.count { !it.isChecked }
        _uiState.value = _uiState.value.copy(
            allItems = items,
            visibleItems = visible,
            groupedItems = grouped,
            filter = filter,
            openItems = openCount
        )
    }

    fun applyFilter(filter: String) {
        updateState(_uiState.value.allItems, filter)
    }

    fun toggleItem(itemId: String) {
        shoppingRepo.toggleItem(itemId)
    }

    fun clearDoneItems() {
        shoppingRepo.clearDoneItems()
    }

    private fun inventoryItemFor(shopping: ShoppingItem): de.yummify.app.data.model.InventoryItem {
        val match = Regex("^([0-9]+(?:[.,][0-9]+)?)\\s*(.*)$").find(shopping.amountWithUnit.trim())
        val quantity = shopping.quantity ?: match?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        require(quantity != null && quantity.isFinite() && quantity > 0) { "Menge für '${shopping.name}' ist nicht numerisch. Bitte den Artikel im Inventar manuell erfassen." }
        val unit = shopping.unit ?: match?.groupValues?.get(2)?.ifBlank { "Stk" } ?: "Stk"
        return de.yummify.app.data.model.InventoryItem(name = shopping.name, quantity = quantity, unit = unit, category = shopping.category)
    }

    /** Swipe action "Ins Inventar": books one article into the inventory and takes it off the list. */
    fun moveToInventory(item: ShoppingItem): Boolean = try {
        val inventory = de.yummify.app.data.repository.InventoryRepository.getInstance(getApplication())
        val entry = inventoryItemFor(item)
        viewModelScope.launch { inventory.addPurchased(entry, item.id); shoppingRepo.removeItems(setOf(item.id)) }
        _uiState.value = _uiState.value.copy(message = "„${item.name}“ ist im Inventar.")
        true
    } catch (e: IllegalArgumentException) { _uiState.value = _uiState.value.copy(message = e.message); false }

    /** Swipe action "Löschen". */
    fun deleteItem(item: ShoppingItem) {
        shoppingRepo.removeItems(setOf(item.id))
        _uiState.value = _uiState.value.copy(message = "„${item.name}“ gelöscht.")
    }

    /** Swipe action "An Home Assistant": sends one article; it only leaves the list when Home Assistant accepted it. */
    suspend fun sendItemToHomeAssistant(item: ShoppingItem): Boolean {
        val prefs = prefsRepo.preferences.value
        if (!prefs.homeAssistantConfigured) { _uiState.value = _uiState.value.copy(message = "Home Assistant ist noch nicht eingerichtet (Einstellungen › Verbindungen)."); return false }
        val entry = HomeAssistantExport.plan(listOf(item.copy(isChecked = false)), emptyMap(), includeSent = true).firstOrNull() ?: return false
        return try {
            withContext(Dispatchers.IO) { HomeAssistantApi(prefs.homeAssistantUrl, prefs.homeAssistantToken).addItem(prefs.homeAssistantTodo, entry.name, entry.description) }
            shoppingRepo.removeItems(setOf(item.id))
            _uiState.value = _uiState.value.copy(message = "„${item.name}“ an Home Assistant gesendet.")
            true
        } catch (e: kotlinx.coroutines.CancellationException) { throw e
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(message = e.message ?: "Senden an Home Assistant fehlgeschlagen.")
            false
        }
    }

    fun transferPurchased() {
        if (_uiState.value.transferring) return
        val bought = _uiState.value.allItems.filter { it.isChecked }
        _uiState.value = _uiState.value.copy(transferring = true, message = null)
        viewModelScope.launch {
            try {
                val inventory = de.yummify.app.data.repository.InventoryRepository.getInstance(getApplication())
                val parsed = bought.map { shopping ->
                    val match = Regex("^([0-9]+(?:[.,][0-9]+)?)\\s*(.*)$").find(shopping.amountWithUnit.trim())
                    val quantity = shopping.quantity ?: match?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
                    require(quantity != null && quantity.isFinite() && quantity > 0) { "Menge für '${shopping.name}' ist nicht numerisch. Bitte den Artikel im Inventar manuell erfassen." }
                    val unit = shopping.unit ?: match?.groupValues?.get(2)?.ifBlank { "Stk" } ?: "Stk"
                    shopping to de.yummify.app.data.model.InventoryItem(name = shopping.name, quantity = quantity, unit = unit, category = shopping.category)
                }
                parsed.forEach { (shopping, item) ->
                    inventory.addPurchased(item, shopping.id)
                    shoppingRepo.removeItems(setOf(shopping.id))
                }
                _uiState.value = _uiState.value.copy(message = "${parsed.size} gekaufte Artikel im Inventar gespeichert.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) { _uiState.value = _uiState.value.copy(message = e.message) }
            finally { _uiState.value = _uiState.value.copy(transferring = false) }
        }
    }
}
