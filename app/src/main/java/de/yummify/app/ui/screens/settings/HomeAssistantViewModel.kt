package de.yummify.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.remote.HomeAssistantApi
import de.yummify.app.data.remote.HomeAssistantTodoList
import de.yummify.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeAssistantUiState(
    val url: String = "",
    val token: String = "",
    val todo: String = "",
    val lists: List<HomeAssistantTodoList> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class HomeAssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferencesRepository.getInstance(application)
    private val _uiState = MutableStateFlow(prefs.preferences.value.let {
        HomeAssistantUiState(url = it.homeAssistantUrl, token = it.homeAssistantToken, todo = it.homeAssistantTodo)
    })
    val uiState: StateFlow<HomeAssistantUiState> = _uiState.asStateFlow()

    fun onUrlChanged(value: String) { _uiState.value = _uiState.value.copy(url = value, message = null) }
    fun onTokenChanged(value: String) { _uiState.value = _uiState.value.copy(token = value, message = null) }
    fun onTodoChosen(entity: String) { _uiState.value = _uiState.value.copy(todo = entity, message = null) }

    /** Checks address and token, then loads the to-do lists (Bring lists first). */
    fun connect() {
        val draft = _uiState.value
        if (draft.busy) return
        _uiState.value = draft.copy(busy = true, message = null)
        viewModelScope.launch {
            try {
                require(draft.url.isNotBlank() && draft.token.isNotBlank()) { "Bitte Adresse und Token von Home Assistant eintragen." }
                val (url, lists) = withContext(Dispatchers.IO) {
                    val api = HomeAssistantApi(draft.url, draft.token)
                    api.check()
                    HomeAssistantApi.normalize(draft.url) to api.todoLists()
                }
                require(lists.isNotEmpty()) { "Verbunden, aber es gibt keine Aufgabenlisten. Ist die Bring-Integration in Home Assistant eingerichtet?" }
                val chosen = draft.todo.takeIf { id -> lists.any { it.entityId == id } } ?: lists.first().entityId
                _uiState.value = _uiState.value.copy(url = url, lists = lists, todo = chosen, busy = false, isError = false,
                    message = "Verbunden. ${lists.size} ${if (lists.size == 1) "Liste" else "Listen"} gefunden. Bitte Liste wählen und speichern.")
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, isError = true, message = e.message ?: "Verbindung zu Home Assistant fehlgeschlagen.")
            }
        }
    }

    fun save() {
        val draft = _uiState.value
        try {
            val url = if (draft.url.isBlank()) "" else HomeAssistantApi.normalize(draft.url)
            prefs.saveHomeAssistant(url, draft.token, draft.todo)
            _uiState.value = draft.copy(url = url, isError = false, message = if (url.isBlank()) "Home Assistant ist getrennt." else "Gespeichert.")
        } catch (e: IllegalArgumentException) {
            _uiState.value = draft.copy(isError = true, message = e.message)
        }
    }

    fun disconnect() {
        prefs.saveHomeAssistant("", "", "")
        _uiState.value = HomeAssistantUiState(message = "Home Assistant ist getrennt.")
    }
}
