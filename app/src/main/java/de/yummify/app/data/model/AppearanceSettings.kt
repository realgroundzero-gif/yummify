package de.yummify.app.data.model

/**
 * Darstellungs-Einstellungen als reine Logik ohne Android-Abhängigkeit. Gespeichert werden stabile Schlüssel
 * als Text; fehlende oder unbekannte Werte (ältere App-Version, kaputte Daten) fallen auf den Standard zurück.
 */
enum class ThemeMode(val key: String) {
    LIGHT("light"), DARK("dark"), SYSTEM("system");

    companion object {
        val Default = LIGHT

        fun fromKey(key: String?): ThemeMode? = values().firstOrNull { it.key == key }

        /**
         * Gespeicherter Modus hat Vorrang. Fehlt er, wird der alte Dark-Mode-Schalter einmalig übernommen
         * (an = Dunkel, aus = Hell); wurde auch der nie gesetzt, gilt Hell.
         */
        fun resolve(storedKey: String?, legacyDarkMode: Boolean?): ThemeMode =
            fromKey(storedKey) ?: when (legacyDarkMode) {
                true -> DARK
                false -> LIGHT
                null -> Default
            }
    }
}

/** Die Seite, die beim Start aus dem Launcher geöffnet wird. [route] ist die Navigationsroute in MainActivity. */
enum class StartDestination(val route: String) {
    RECIPES("recipes"), PLANNER("planner"), SHOPPING("shopping"), INVENTORY("inventory");

    companion object {
        val Default = RECIPES

        fun fromRoute(route: String?): StartDestination = values().firstOrNull { it.route == route } ?: Default
    }
}

enum class RecipeLayout(val key: String) {
    LIST("list"), GRID("grid");

    companion object {
        val Default = LIST

        fun fromKey(key: String?): RecipeLayout = values().firstOrNull { it.key == key } ?: Default
    }
}

/** [tag] ist ein BCP-47-Sprach-Tag; null folgt der Systemsprache. */
enum class AppLanguage(val key: String, val tag: String?) {
    SYSTEM("system", null), GERMAN("de", "de"), ENGLISH("en", "en");

    companion object {
        val Default = SYSTEM

        fun fromKey(key: String?): AppLanguage = values().firstOrNull { it.key == key } ?: Default

        /** Sprache aus einer vom System gemeldeten Locale-Liste (z. B. LocaleManager); unbekannte Sprachen gelten als System. */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.substringBefore('-')?.lowercase()
            return values().firstOrNull { it.tag != null && it.tag == language } ?: SYSTEM
        }
    }
}

/** Wohin der Start führt: [startRoute] ist die erste Seite, [recipeId] öffnet darüber die Rezeptdetails. */
data class LaunchTarget(val startRoute: String, val recipeId: String?)

/**
 * Wählt die Startseite. Intent-Extras (Widgets, Deep Links) haben Vorrang vor der eingestellten Startseite;
 * eine unbekannte Route aus dem Intent wird ignoriert. Eine Rezept-ID öffnet die Details über der Startseite.
 */
fun resolveLaunchTarget(
    intentRoute: String?,
    intentRecipeId: String?,
    preferredRoute: String?,
    knownRoutes: Set<String>
): LaunchTarget {
    val start = intentRoute?.takeIf { it in knownRoutes } ?: StartDestination.fromRoute(preferredRoute).route
    return LaunchTarget(start, intentRecipeId?.takeIf { it.isNotBlank() })
}
