package de.yummify.app.ui.screens.settings

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.data.model.AppLanguage
import de.yummify.app.data.model.RecipeLayout
import de.yummify.app.data.model.StartDestination
import de.yummify.app.data.model.ThemeMode
import de.yummify.app.data.repository.UserPreferences
import de.yummify.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AppearanceUiState(
    val themeMode: ThemeMode = ThemeMode.Default,
    val dynamicColorEnabled: Boolean = false,
    /** Dynamische Farben gibt es erst ab Android 12 (API 31). */
    val dynamicColorSupported: Boolean = true,
    val startDestination: StartDestination = StartDestination.Default,
    val recipeLayout: RecipeLayout = RecipeLayout.Default,
    val appLanguage: AppLanguage = AppLanguage.Default
)

class AppearanceViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferencesRepository.getInstance(application)
    private val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val uiState: StateFlow<AppearanceUiState> = prefs.preferences.map { build(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, build(prefs.preferences.value))

    private fun build(p: UserPreferences) = AppearanceUiState(
        themeMode = p.themeMode,
        dynamicColorEnabled = p.dynamicColorEnabled && dynamicSupported,
        dynamicColorSupported = dynamicSupported,
        startDestination = p.startDestination,
        recipeLayout = p.recipeLayout,
        appLanguage = p.appLanguage
    )

    fun onThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)
    fun onDynamicColor(enabled: Boolean) { if (dynamicSupported) prefs.setDynamicColorEnabled(enabled) }
    fun onStartDestination(destination: StartDestination) = prefs.setStartDestination(destination)
    fun onRecipeLayout(layout: RecipeLayout) = prefs.setRecipeLayout(layout)
    fun onAppLanguage(language: AppLanguage) = prefs.setAppLanguage(language)
}
