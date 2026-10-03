package de.yummify.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.NotionConfig
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
    val remindersEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = UserPreferencesRepository.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.preferences.collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    darkModeEnabled = prefs.darkModeEnabled,
                    remindersEnabled = prefs.remindersEnabled,
                    autoSyncEnabled = prefs.autoSyncEnabled,
                    tokenInput = prefs.tokenInput,
                    databaseIdInput = prefs.databaseIdInput,
                    inventoryDatabaseIdInput = prefs.inventoryDatabaseIdInput,
                    config = NotionConfig(
                        integrationToken = prefs.tokenInput,
                        recipeDatabaseId = prefs.databaseIdInput,
                        inventoryDatabaseId = prefs.inventoryDatabaseIdInput,
                        isConfigured = prefs.tokenInput.isNotBlank(),
                        lastSyncTime = if (prefs.tokenInput.isNotBlank()) "Aktiv" else "Nicht konfiguriert"
                    )
                )
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
            val token = _uiState.value.tokenInput.trim()
            val formattedDbId = RecipeRepository.formatNotionId(_uiState.value.databaseIdInput)

            if (token.isBlank() || formattedDbId.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    connectionResult = "❌ Notion Token oder DB-ID nicht konfiguriert."
                )
                return@launch
            }

            val repo = RecipeRepository(token, formattedDbId)
            val now = java.text.SimpleDateFormat("HH:mm 'Uhr'", java.util.Locale.GERMANY).format(java.util.Date())

            try {
                val recipes = repo.getAllRecipes()
                val statusMsg = if (recipes.isNotEmpty()) {
                    "✅ ${recipes.size} Rezept(e) erfolgreich aus Notion geladen."
                } else {
                    "⚠️ Verbindung OK, aber keine Rezepte in der Datenbank gefunden.\nPrüfe ob deine Notion-Integration Zugriff auf die Datenbank hat."
                }
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    connectionResult = statusMsg,
                    config = _uiState.value.config.copy(lastSyncTime = "Heute, $now")
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    connectionResult = "❌ Synchronisation fehlgeschlagen: ${e.message}"
                )
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionResult = null)
            val token = _uiState.value.tokenInput.trim()
            val formattedDbId = RecipeRepository.formatNotionId(_uiState.value.databaseIdInput)
            val repo = RecipeRepository(token, formattedDbId)
            val isSuccess = if (token.isNotBlank() && formattedDbId.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    repo.testNotionConnection(token, formattedDbId)
                }
            } else false

            val result = if (isSuccess) {
                "✅ Verbindung erfolgreich! Notion DB erreichbar."
            } else if (token.isNotBlank() && formattedDbId.isNotBlank()) {
                "❌ Verbindung zu Notion fehlgeschlagen. Token oder DB-ID prüfen."
            } else {
                "❌ Token oder Datenbank-ID fehlt."
            }
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

    fun toggleDarkMode(enabled: Boolean) {
        prefsRepo.setDarkModeEnabled(enabled)
    }

    fun toggleReminders(enabled: Boolean) {
        prefsRepo.setRemindersEnabled(enabled)
    }

    fun toggleAutoSync(enabled: Boolean) {
        prefsRepo.setAutoSyncEnabled(enabled)
    }
}
