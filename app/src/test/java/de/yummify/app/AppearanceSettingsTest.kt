package de.yummify.app

import android.content.Context
import de.yummify.app.data.model.AppLanguage
import de.yummify.app.data.model.RecipeLayout
import de.yummify.app.data.model.StartDestination
import de.yummify.app.data.model.ThemeMode
import de.yummify.app.data.model.resolveLaunchTarget
import de.yummify.app.data.repository.UserPreferencesRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private val routes = setOf("recipes", "planner", "shopping", "inventory", "settings")

class AppearanceLogicTest {
    @Test fun oldSwitchOnBecomesDarkAndOffBecomesLight() {
        assertEquals(ThemeMode.DARK, ThemeMode.resolve(storedKey = null, legacyDarkMode = true))
        assertEquals(ThemeMode.LIGHT, ThemeMode.resolve(storedKey = null, legacyDarkMode = false))
    }

    @Test fun neverSetDefaultsToLight() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.resolve(null, null))
    }

    @Test fun storedModeWinsOverTheOldSwitch() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.resolve("system", legacyDarkMode = true))
        assertEquals(ThemeMode.LIGHT, ThemeMode.resolve("light", legacyDarkMode = true))
    }

    @Test fun unknownStoredValuesFallBackToDefaults() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.resolve("sepia", null))
        assertEquals(StartDestination.RECIPES, StartDestination.fromRoute(null))
        assertEquals(StartDestination.RECIPES, StartDestination.fromRoute("settings"))
        assertEquals(StartDestination.RECIPES, StartDestination.fromRoute(""))
        assertEquals(RecipeLayout.LIST, RecipeLayout.fromKey(null))
        assertEquals(RecipeLayout.LIST, RecipeLayout.fromKey("carousel"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromKey(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromKey("fr"))
    }

    @Test fun systemLanguageTagsMapToTheOfferedLanguages() {
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromTag("de-AT"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("fr"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
    }

    @Test fun launcherStartOpensTheChosenStartPage() {
        for (destination in StartDestination.values()) {
            val target = resolveLaunchTarget(null, null, destination.route, routes)
            assertEquals(destination.route, target.startRoute)
            assertNull(target.recipeId)
        }
    }

    @Test fun missingOrBrokenChoiceStartsWithRecipes() {
        assertEquals("recipes", resolveLaunchTarget(null, null, null, routes).startRoute)
        assertEquals("recipes", resolveLaunchTarget(null, null, "nowhere", routes).startRoute)
    }

    @Test fun intentRouteWinsOverTheChosenStartPage() {
        val target = resolveLaunchTarget("shopping", null, "planner", routes)
        assertEquals("shopping", target.startRoute)
    }

    @Test fun intentRecipeIdOpensDetailsOverTheChosenStartPage() {
        val target = resolveLaunchTarget(null, "abc", "inventory", routes)
        assertEquals("inventory", target.startRoute)
        assertEquals("abc", target.recipeId)
    }

    @Test fun routeAndRecipeIdTogetherAreKept() {
        val target = resolveLaunchTarget("planner", "abc", "inventory", routes)
        assertEquals(LaunchExpectation("planner", "abc"), LaunchExpectation(target.startRoute, target.recipeId))
    }

    @Test fun unknownIntentRouteAndEmptyRecipeIdAreIgnored() {
        val target = resolveLaunchTarget("bogus", "", "planner", routes)
        assertEquals("planner", target.startRoute)
        assertNull(target.recipeId)
    }

    private data class LaunchExpectation(val route: String, val recipeId: String?)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearancePreferencesTest {
    private lateinit var context: Context
    private val prefsName = "yummify_user_prefs"

    @Before fun clean() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun prefs() = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    @Test fun freshInstallUsesTheDefaults() {
        val p = UserPreferencesRepository(context).preferences.value
        assertEquals(ThemeMode.LIGHT, p.themeMode)
        assertFalse(p.dynamicColorEnabled)
        assertEquals(StartDestination.RECIPES, p.startDestination)
        assertEquals(RecipeLayout.LIST, p.recipeLayout)
        assertEquals(AppLanguage.SYSTEM, p.appLanguage)
    }

    @Test fun oldDataWithoutTheNewFieldsLoadsWithDefaults() {
        // Data of an older app version: only the fields that existed back then.
        prefs().edit().putBoolean("auto_sync", false).putString("notion_token", "t").commit()
        val p = UserPreferencesRepository(context).preferences.value
        assertEquals(ThemeMode.LIGHT, p.themeMode)
        assertEquals(StartDestination.RECIPES, p.startDestination)
        assertFalse(p.autoSyncEnabled)
        assertEquals("t", p.tokenInput)
    }

    @Test fun oldSwitchOnMigratesToDark() {
        prefs().edit().putBoolean("dark_mode", true).commit()
        assertEquals(ThemeMode.DARK, UserPreferencesRepository(context).preferences.value.themeMode)
    }

    @Test fun oldSwitchOffMigratesToLight() {
        prefs().edit().putBoolean("dark_mode", false).commit()
        assertEquals(ThemeMode.LIGHT, UserPreferencesRepository(context).preferences.value.themeMode)
    }

    @Test fun migrationRunsOnlyOnce() {
        prefs().edit().putBoolean("dark_mode", true).commit()
        UserPreferencesRepository(context).setThemeMode(ThemeMode.SYSTEM)
        // The old key is still there, but the stored mode wins from now on.
        assertEquals(ThemeMode.SYSTEM, UserPreferencesRepository(context).preferences.value.themeMode)
        prefs().edit().putBoolean("dark_mode", false).commit()
        assertEquals(ThemeMode.SYSTEM, UserPreferencesRepository(context).preferences.value.themeMode)
    }

    @Test fun migrationResultIsPersistedAtOnce() {
        prefs().edit().putBoolean("dark_mode", true).commit()
        UserPreferencesRepository(context)
        assertEquals("dark", prefs().getString("theme_mode", null))
    }

    @Test fun allChoicesSurviveARestart() {
        UserPreferencesRepository(context).apply {
            setThemeMode(ThemeMode.DARK)
            setDynamicColorEnabled(true)
            setStartDestination(StartDestination.INVENTORY)
            setRecipeLayout(RecipeLayout.GRID)
            setAppLanguage(AppLanguage.ENGLISH)
        }
        val p = UserPreferencesRepository(context).preferences.value
        assertEquals(ThemeMode.DARK, p.themeMode)
        assertTrue(p.dynamicColorEnabled)
        assertEquals(StartDestination.INVENTORY, p.startDestination)
        assertEquals(RecipeLayout.GRID, p.recipeLayout)
        assertEquals(AppLanguage.ENGLISH, p.appLanguage)
        assertEquals(AppLanguage.ENGLISH, UserPreferencesRepository.readAppLanguage(context))
    }

    @Test fun brokenStoredValuesAreNormalized() {
        prefs().edit().putString("theme_mode", "").putString("start_route", "settings").putString("recipe_layout", "x").putString("app_language", "fr").commit()
        val p = UserPreferencesRepository(context).preferences.value
        assertEquals(ThemeMode.LIGHT, p.themeMode)
        assertEquals(StartDestination.RECIPES, p.startDestination)
        assertEquals(RecipeLayout.LIST, p.recipeLayout)
        assertEquals(AppLanguage.SYSTEM, p.appLanguage)
    }
}
