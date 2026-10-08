package de.yummify.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val darkModeEnabled: Boolean = false,

    val autoSyncEnabled: Boolean = true,
    val tokenInput: String = "",
    val databaseIdInput: String = "",
    val inventoryDatabaseIdInput: String = "",
    val profileName: String = ""
)

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("yummify_user_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(
        UserPreferences(
            darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false),
            autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true),
            tokenInput = prefs.getString(KEY_TOKEN, "") ?: "",
            databaseIdInput = prefs.getString(KEY_DATABASE_ID, "") ?: "",
            inventoryDatabaseIdInput = prefs.getString("notion_inventory_db_id", "") ?: "",
            profileName = prefs.getString(KEY_PROFILE_NAME, "") ?: ""
        )
    )
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _preferences.value = _preferences.value.copy(darkModeEnabled = enabled)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
        _preferences.value = _preferences.value.copy(autoSyncEnabled = enabled)
    }

    fun setProfileName(name: String) {
        val trimmed = name.trim().take(60)
        prefs.edit().putString(KEY_PROFILE_NAME, trimmed).apply()
        _preferences.value = _preferences.value.copy(profileName = trimmed)
    }

    fun saveNotionConfig(token: String, databaseId: String, inventoryDatabaseId: String = _preferences.value.inventoryDatabaseIdInput) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_DATABASE_ID, databaseId)
            .putString("notion_inventory_db_id", inventoryDatabaseId)
            .apply()
        _preferences.value = _preferences.value.copy(
            tokenInput = token,
            databaseIdInput = databaseId,
            inventoryDatabaseIdInput = inventoryDatabaseId
        )
    }

    companion object {
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_TOKEN = "notion_token"
        private const val KEY_DATABASE_ID = "notion_db_id"

        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
