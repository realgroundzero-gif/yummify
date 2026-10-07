package de.yummify.app.ui.screens.shopping_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.repository.ShoppingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
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
    val message: String? = null
)

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {
    private val shoppingRepo = ShoppingListRepository.getInstance(application)
    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shoppingRepo.items.collect { items ->
                updateState(items, _uiState.value.filter)
            }
        }
    }

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
