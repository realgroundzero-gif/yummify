package de.yummify.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.remote.DropdownField
import de.yummify.app.data.remote.NotionOptionsApi
import de.yummify.app.data.repository.InventoryRepository
import de.yummify.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One Notion database whose dropdown columns can be extended. */
enum class DataArea(val title: String) { RECIPES("Rezepte"), INVENTORY("Inventar") }

data class DataAreaState(
    val configured: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val source: String? = null,
    val fields: List<DropdownField> = emptyList()
)

data class DataCareUiState(
    val areas: Map<DataArea, DataAreaState> = DataArea.values().associateWith { DataAreaState() },
    /** "area/field" while an entry is being written to Notion. */
    val saving: String? = null,
    val message: String? = null
)

class DataCareViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferencesRepository.getInstance(application)
    private val _uiState = MutableStateFlow(DataCareUiState())
    val uiState: StateFlow<DataCareUiState> = _uiState.asStateFlow()

    private fun update(area: DataArea, change: (DataAreaState) -> DataAreaState) {
        _uiState.value = _uiState.value.copy(areas = _uiState.value.areas + (area to change(_uiState.value.areas.getValue(area))))
    }

    private fun databaseId(area: DataArea): String = prefs.preferences.value.let {
        if (area == DataArea.RECIPES) it.databaseIdInput else it.inventoryDatabaseIdInput
    }

    init { loadAll() }

    fun loadAll() = DataArea.values().forEach { load(it) }

    fun load(area: DataArea) {
        val token = prefs.preferences.value.tokenInput
        val database = databaseId(area)
        if (token.isBlank() || database.isBlank()) { update(area) { DataAreaState(configured = false) }; return }
        update(area) { it.copy(configured = true, loading = true, error = null) }
        viewModelScope.launch {
            try {
                val schema = withContext(Dispatchers.IO) { NotionOptionsApi(token).load(database) }
                update(area) { it.copy(loading = false, source = schema.source, fields = schema.fields) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                update(area) { it.copy(loading = false, error = e.message ?: "Notion ist nicht erreichbar.") }
            }
        }
    }

    /** Writes the new entry to the Notion column first; the list on screen shows what Notion reports back. */
    fun add(area: DataArea, field: String, entry: String, onDone: () -> Unit = {}) {
        if (_uiState.value.saving != null) return
        val problem = NotionOptionsApi.validate(entry)
        if (problem != null) { _uiState.value = _uiState.value.copy(message = problem); return }
        val source = _uiState.value.areas.getValue(area).source ?: return
        val token = prefs.preferences.value.tokenInput
        _uiState.value = _uiState.value.copy(saving = "${area.name}/$field", message = null)
        viewModelScope.launch {
            val message = try {
                val options = withContext(Dispatchers.IO) { NotionOptionsApi(token).addOptions(source, field, listOf(entry)) }
                update(area) { state -> state.copy(fields = state.fields.map { if (it.name == field) it.copy(options = options) else it }) }
                if (area == DataArea.INVENTORY) runCatching { InventoryRepository.getInstance(getApplication()).refreshChoices() }
                onDone()
                "„${entry.trim()}“ ist jetzt in Notion unter „$field“ verfügbar."
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { "Nicht gespeichert: ${e.message ?: "Notion ist nicht erreichbar."}" }
            _uiState.value = _uiState.value.copy(saving = null, message = message)
        }
    }

    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }
}
