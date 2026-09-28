package de.yummify.app.ui.screens.shopping_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShoppingListUiState(
    val allItems: List<ShoppingItem> = emptyList(),
    val visibleItems: List<ShoppingItem> = emptyList(),
    val groupedItems: Map<String, List<ShoppingItem>> = emptyMap(),
    val filter: String = "missing",
    val isSyncing: Boolean = false,
    val openItems: Int = 0
)

class ShoppingListViewModel : ViewModel() {
    private val repository = RecipeRepository()
    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init { loadItems() }

    private fun loadItems() {
        viewModelScope.launch {
            val items = repository.getShoppingItems()
            updateState(items, _uiState.value.filter)
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
        val updated = _uiState.value.allItems.map {
            if (it.id == itemId) it.copy(isChecked = !it.isChecked) else it
        }
        updateState(updated, _uiState.value.filter)
    }

    fun clearDoneItems() {
        val remaining = _uiState.value.allItems.filter { !it.isChecked }
        updateState(remaining, _uiState.value.filter)
    }

    fun triggerSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            kotlinx.coroutines.delay(1500)
            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }
}
