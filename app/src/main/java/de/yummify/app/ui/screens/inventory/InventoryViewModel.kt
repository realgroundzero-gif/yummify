package de.yummify.app.ui.screens.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.*
import de.yummify.app.data.repository.InventoryRepository
import de.yummify.app.data.repository.ShoppingListRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class StockFilter(val title: String) { ALL("Alle"), LOW("Nachkaufen"), SOON("Bald fällig"), EXPIRED("Abgelaufen") }
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository.getInstance(application)
    private val shopping = ShoppingListRepository.getInstance(application)
    val items = repository.items
    val sync = repository.syncState
    val message = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    init { repository.syncOnResume() }
    fun save(item: InventoryItem, onSuccess: () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            try { repository.save(item); onSuccess() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { message.value = e.message }
            finally { saving.value = false }
        }
    }
    fun adjust(id: String, delta: Double) = action { repository.adjust(id, delta) }
    fun remove(id: String) = action { repository.remove(id) }
    fun sync() = action { repository.sync() }
    private fun action(block: suspend () -> Unit) { viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { message.value = e.message }
    } }
    fun toShopping(items: List<InventoryItem>) {
        val count = shopping.addInventoryNeeds(items)
        message.value = if (count == 0) "Diese Artikel stehen bereits auf der Einkaufsliste." else "$count Artikel zur Einkaufsliste hinzugefügt."
    }
}
