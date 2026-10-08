package de.yummify.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import de.yummify.app.data.model.AppLanguage
import de.yummify.app.data.model.RecipeLayout
import de.yummify.app.data.model.StartDestination
import de.yummify.app.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.Default,
    val dynamicColorEnabled: Boolean = false,
    val startDestination: StartDestination = StartDestination.Default,
    val recipeLayout: RecipeLayout = RecipeLayout.Default,
    val appLanguage: AppLanguage = AppLanguage.Default,
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
            themeMode = loadThemeMode(),
            dynamicColorEnabled = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
            startDestination = StartDestination.fromRoute(prefs.getString(KEY_START_ROUTE, null)),
            recipeLayout = RecipeLayout.fromKey(prefs.getString(KEY_RECIPE_LAYOUT, null)),
            appLanguage = AppLanguage.fromKey(prefs.getString(KEY_APP_LANGUAGE, null)),
            autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true),
            tokenInput = prefs.getString(KEY_TOKEN, "") ?: "",
            databaseIdInput = prefs.getString(KEY_DATABASE_ID, "") ?: "",
            inventoryDatabaseIdInput = prefs.getString("notion_inventory_db_id", "") ?: "",
            profileName = prefs.getString(KEY_PROFILE_NAME, "") ?: ""
        )
    )
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    /** Übernimmt den alten Dark-Mode-Schalter genau einmal; danach zählt nur noch der gespeicherte Modus. */
    private fun loadThemeMode(): ThemeMode {
        val legacy = if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
        val mode = ThemeMode.resolve(prefs.getString(KEY_THEME_MODE, null), legacy)
        if (ThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, null)) == null) prefs.edit().putString(KEY_THEME_MODE, mode.key).apply()
        return mode
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.key).apply()
        _preferences.value = _preferences.value.copy(themeMode = mode)
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _preferences.value = _preferences.value.copy(dynamicColorEnabled = enabled)
    }

    fun setStartDestination(destination: StartDestination) {
        prefs.edit().putString(KEY_START_ROUTE, destination.route).apply()
        _preferences.value = _preferences.value.copy(startDestination = destination)
    }

    fun setRecipeLayout(layout: RecipeLayout) {
        prefs.edit().putString(KEY_RECIPE_LAYOUT, layout.key).apply()
        _preferences.value = _preferences.value.copy(recipeLayout = layout)
    }

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.key).apply()
        _preferences.value = _preferences.value.copy(appLanguage = language)
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
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_START_ROUTE = "start_route"
        private const val KEY_RECIPE_LAYOUT = "recipe_layout"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_TOKEN = "notion_token"
        private const val KEY_DATABASE_ID = "notion_db_id"

        /** Liest die Sprache direkt, damit sie schon in attachBaseContext (vor allen Singletons) zur Verfügung steht. */
        fun readAppLanguage(context: Context): AppLanguage =
            AppLanguage.fromKey(context.getSharedPreferences("yummify_user_prefs", Context.MODE_PRIVATE).getString(KEY_APP_LANGUAGE, null))

        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
