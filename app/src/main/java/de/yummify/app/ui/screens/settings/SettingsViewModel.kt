package de.yummify.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.NotionConfig
import de.yummify.app.data.remote.NotionRecipeReader
import de.yummify.app.data.repository.RecipeRepository
import de.yummify.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val config: NotionConfig = NotionConfig(),
    val tokenInput: String = "",
    val databaseIdInput: String = "",
    val inventoryDatabaseIdInput: String = "",
    val isCreatingInventory: Boolean = false,
    val isTestingConnection: Boolean = false,
    val isSyncing: Boolean = false,
    val connectionResult: String? = null,
    val isSaving: Boolean = false,
    val darkModeEnabled: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val profileName: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = UserPreferencesRepository.getInstance(application)
    private val recipeRepo = RecipeRepository.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var first = true
            var saved = prefsRepo.preferences.value
            prefsRepo.preferences.collect { prefs ->
                // Only replace text fields when the stored values change, so toggling a switch keeps unsaved input.
                val inputsChanged = first || prefs.tokenInput != saved.tokenInput || prefs.databaseIdInput != saved.databaseIdInput ||
                    prefs.inventoryDatabaseIdInput != saved.inventoryDatabaseIdInput
                first = false
                saved = prefs
                val current = _uiState.value
                _uiState.value = current.copy(
                    darkModeEnabled = prefs.darkModeEnabled,
                    autoSyncEnabled = prefs.autoSyncEnabled,
                    profileName = prefs.profileName,
                    tokenInput = if (inputsChanged) prefs.tokenInput else current.tokenInput,
                    databaseIdInput = if (inputsChanged) prefs.databaseIdInput else current.databaseIdInput,
                    inventoryDatabaseIdInput = if (inputsChanged) prefs.inventoryDatabaseIdInput else current.inventoryDatabaseIdInput,
                    config = current.config.copy(
                        integrationToken = prefs.tokenInput,
                        recipeDatabaseId = prefs.databaseIdInput,
                        inventoryDatabaseId = prefs.inventoryDatabaseIdInput,
                        isConfigured = prefs.tokenInput.isNotBlank()
                    )
                )
            }
        }
        viewModelScope.launch {
            recipeRepo.state.collect { state ->
                val time = state.lastSync?.let { "%02d.%02d., %02d:%02d Uhr".format(it.dayOfMonth, it.monthValue, it.hour, it.minute) }
                _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(lastSyncTime = time ?: "Noch nicht synchronisiert"))
            }
        }
    }

    fun onTokenChanged(value: String) {
        _uiState.value = _uiState.value.copy(tokenInput = value, connectionResult = null)
    }

    fun onDatabaseIdChanged(value: String) {
        _uiState.value = _uiState.value.copy(databaseIdInput = value, connectionResult = null)
    }

    fun onInventoryDatabaseIdChanged(value: String) {
        _uiState.value = _uiState.value.copy(inventoryDatabaseIdInput = value, connectionResult = null)
    }

    fun createInventoryDatabase() {
        if (_uiState.value.isCreatingInventory) return
        viewModelScope.launch {
            val draft = _uiState.value
            _uiState.value = draft.copy(isCreatingInventory = true, connectionResult = null)
            try {
                require(draft.tokenInput.isNotBlank() && draft.databaseIdInput.isNotBlank()) { "Zuerst Token und Rezept-Datenbank-ID eingeben." }
                require(draft.inventoryDatabaseIdInput.isBlank()) { "Es ist bereits eine Inventar-Datenbank eingetragen." }
                val id = withContext(Dispatchers.IO) {
                    de.yummify.app.data.remote.NotionInventoryApi(draft.tokenInput).createDatabase(draft.databaseIdInput)
                }
                prefsRepo.saveNotionConfig(draft.tokenInput.trim(), RecipeRepository.formatNotionId(draft.databaseIdInput), id)
                _uiState.value = _uiState.value.copy(inventoryDatabaseIdInput = id, connectionResult = "✅ Inventar-Datenbank mit allen Feldern angelegt und verbunden.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(connectionResult = "❌ ${e.message}")
            } finally { _uiState.value = _uiState.value.copy(isCreatingInventory = false) }
        }
    }

    fun testInventoryConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true)
            try {
                val draft = _uiState.value
                require(draft.tokenInput.isNotBlank() && draft.inventoryDatabaseIdInput.isNotBlank()) { "Token und Inventar-Datenbank-ID fehlen." }
                withContext(Dispatchers.IO) { de.yummify.app.data.remote.NotionInventoryApi(draft.tokenInput).source(draft.inventoryDatabaseIdInput) }
                _uiState.value = _uiState.value.copy(connectionResult = "✅ Inventar-Datenbank erreichbar. Alle Felder vorhanden.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) { _uiState.value = _uiState.value.copy(connectionResult = "❌ ${e.message}")
            } finally { _uiState.value = _uiState.value.copy(isTestingConnection = false) }
        }
    }

    fun syncNotion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, connectionResult = null)
            val error = recipeRepo.refresh()
            val count = recipeRepo.state.value.recipes.size
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                connectionResult = when {
                    error != null -> "❌ Synchronisation fehlgeschlagen: $error"
                    count > 0 -> "✅ $count Rezept(e) aus Notion geladen."
                    else -> "⚠️ Verbindung OK, aber keine Rezepte in der Datenbank gefunden."
                }
            )
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionResult = null)
            val token = _uiState.value.tokenInput.trim()
            val formattedDbId = RecipeRepository.formatNotionId(_uiState.value.databaseIdInput)
            val result = if (token.isBlank() || formattedDbId.isBlank()) "❌ Token oder Datenbank-ID fehlt."
            else try {
                withContext(Dispatchers.IO) { NotionRecipeReader(token, formattedDbId).testConnection() }
                "✅ Verbindung erfolgreich! Notion DB erreichbar."
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) { "❌ ${e.message ?: "Verbindung zu Notion fehlgeschlagen."}" }
            _uiState.value = _uiState.value.copy(
                databaseIdInput = formattedDbId,
                isTestingConnection = false,
                connectionResult = result
            )
        }
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val formattedDbId = RecipeRepository.formatNotionId(_uiState.value.databaseIdInput)
            prefsRepo.saveNotionConfig(_uiState.value.tokenInput.trim(), formattedDbId, RecipeRepository.formatNotionId(_uiState.value.inventoryDatabaseIdInput))
            delay(300)
            _uiState.value = _uiState.value.copy(
                databaseIdInput = formattedDbId,
                isSaving = false,
                connectionResult = "✅ Einstellungen gespeichert."
            )
        }
    }

    fun saveProfileName(name: String) = prefsRepo.setProfileName(name)

    fun toggleDarkMode(enabled: Boolean) {
        prefsRepo.setDarkModeEnabled(enabled)
    }

    fun toggleAutoSync(enabled: Boolean) {
        prefsRepo.setAutoSyncEnabled(enabled)
    }
}
