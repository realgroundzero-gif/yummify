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
    val isTestingConnection: Boolean = false,
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
                    config = NotionConfig(
                        integrationToken = prefs.tokenInput,
                        recipeDatabaseId = prefs.databaseIdInput,
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

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionResult = null)
            val token = _uiState.value.tokenInput
            val dbId = _uiState.value.databaseIdInput
            val repo = RecipeRepository(token, dbId)
            val isSuccess = if (token.isNotBlank() && dbId.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    repo.testNotionConnection(token, dbId)
                }
            } else false

            val result = if (isSuccess) {
                "✅ Verbindung erfolgreich! Notion DB erreichbar."
            } else if (token.isNotBlank() && dbId.isNotBlank()) {
                "❌ Verbindung zu Notion fehlgeschlagen. Token oder DB-ID prüfen."
            } else {
                "❌ Token oder Datenbank-ID fehlt."
            }
            _uiState.value = _uiState.value.copy(isTestingConnection = false, connectionResult = result)
        }
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            prefsRepo.saveNotionConfig(_uiState.value.tokenInput, _uiState.value.databaseIdInput)
            delay(300)
            _uiState.value = _uiState.value.copy(isSaving = false)
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
