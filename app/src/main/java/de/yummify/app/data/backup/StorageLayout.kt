package de.yummify.app.data.backup

import java.util.Locale

/**
 * Zentrale Liste aller Orte, an denen Yummify Daten auf dem Gerät ablegt. Neue Speicherorte müssen hier
 * eingetragen werden; ein Test vergleicht die Liste mit allen `getSharedPreferences`-Aufrufen im Quelltext.
 */
object StorageLayout {
    const val PREFS_RECIPES = "yummify_recipes"
    const val PREFS_MEAL_PLAN = "yummify_meal_plan"
    const val PREFS_SHOPPING = "yummify_shopping_list"
    const val PREFS_INVENTORY = "yummify_inventory"
    const val PREFS_USER = "yummify_user_prefs"
    /** Gerätespezifisch (Transparenz je Widget-ID, Schalter für das Android-Backup): nicht Teil von Export und Löschen. */
    const val PREFS_WIDGET = "widget_prefs"
    const val PREFS_BACKUP_SETTINGS = "yummify_backup_settings"

    /** Lokale Fachdaten, die Export, Import und "Alle lokalen Daten löschen" betreffen. */
    val dataPrefs = listOf(PREFS_RECIPES, PREFS_MEAL_PLAN, PREFS_SHOPPING, PREFS_INVENTORY)
    val allPrefs = dataPrefs + PREFS_USER + PREFS_WIDGET + PREFS_BACKUP_SETTINGS

    /** Lokale Produktbilder (Unterordner von filesDir). */
    const val IMAGES_DIR = "inventory_images"

    /** Schlüssel in [PREFS_USER] mit Zugangsdaten: kommen nur auf ausdrücklichen Wunsch in einen Export. */
    val secretUserKeys = setOf("notion_token")
    /** Verbindungsdaten: bleiben beim Import erhalten, solange sie lokal gesetzt sind. */
    val connectionUserKeys = setOf("notion_db_id", "notion_inventory_db_id")
    const val KEY_INVENTORY_DB = "notion_inventory_db_id"

    // Schlüssel in den Fach-Preferences (müssen zu den Repositories passen)
    const val KEY_FAVORITES = "favorites"
    const val RECIPES_PREFIX = "recipes_"
    const val KEY_MEALS = "planned_meals_json"
    const val KEY_SHOPPING_ITEMS = "shopping_items_json"
    const val KEY_COVERED = "covered_items_json"
    const val KEY_DISMISSED = "dismissed_suggestions_json"
    fun inventoryKey(databaseId: String) = "items_${databaseId.ifBlank { "local" }}"
    fun purchasesKey(databaseId: String) = "purchases_$databaseId"

    /** Gesicherte Kopien unlesbarer Daten (`…_corrupt_<Zeit>`) bleiben bei Import unangetastet. */
    fun isCorruptCopy(key: String) = "_corrupt_" in key
}

/** Größen in lesbaren Einheiten (1 KB = 1024 Byte), z. B. "1,5 MB". */
fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024 && unit < units.lastIndex) { value /= 1024; unit++ }
    return String.format(locale, "%.1f %s", value, units[unit])
}

/** Belegter Speicher, getrennt nach Daten (JSON/Preferences), lokalen Produktbildern und Cache. */
data class StorageUsage(val dataBytes: Long, val imageBytes: Long, val cacheBytes: Long)
