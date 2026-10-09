package de.yummify.app.data.backup

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.yummify.app.data.model.CoveredItem
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.ShoppingItem

/** Erwartbarer Fehler beim Sichern, Einlesen oder Löschen; die Meldung ist für den Nutzer bestimmt. */
class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Bereiche, die getrennt zusammengeführt werden (jeder hat eine eigene SharedPreferences-Datei). */
enum class BackupDomain(val label: String) {
    RECIPES("Rezepte und Favoriten"),
    MEAL_PLAN("Wochenplan"),
    SHOPPING("Einkaufsliste"),
    INVENTORY("Inventar"),
    USER_PREFS("Einstellungen")
}

enum class ImportMode { MERGE, REPLACE }

/**
 * Kopf jeder Export-Datei. [formatVersion] steigt nur bei inkompatiblen Änderungen;
 * neue optionale Felder in den Daten brauchen keine neue Version (ältere Exporte werden beim Laden normalisiert).
 */
data class BackupManifest(
    val format: String? = null,
    val formatVersion: Int? = null,
    val appVersion: String? = null,
    /** ISO-8601-Zeitstempel (UTC) des Exports. */
    val createdAt: String? = null,
    val includesToken: Boolean? = null,
    val elementCounts: Map<String, Int>? = null
) {
    companion object {
        const val FORMAT = "yummify-backup"
        const val CURRENT_VERSION = 1
    }
}

/** Anzahl der Elemente eines Exports, getrennt nach Art; [total] ist die Zahl in der Erfolgsmeldung. */
data class BackupCounts(
    val recipes: Int = 0, val favorites: Int = 0, val meals: Int = 0, val shoppingItems: Int = 0,
    val inventoryItems: Int = 0, val images: Int = 0
) {
    val total get() = recipes + favorites + meals + shoppingItems + inventoryItems + images
    fun toMap() = mapOf("recipes" to recipes, "favorites" to favorites, "meals" to meals, "shoppingItems" to shoppingItems,
        "inventoryItems" to inventoryItems, "images" to images)

    companion object {
        fun of(data: BackupData, images: Int) = BackupCounts(
            recipes = data.recipeCaches.values.sumOf { it.size }, favorites = data.favorites.size, meals = data.meals.size,
            shoppingItems = data.shoppingItems.size, inventoryItems = data.inventory.count { !it.deleted }, images = images
        )
    }
}

/**
 * Der gesamte lokale Datenbestand als typisierte Daten (ohne Bilddateien).
 * [userPrefs] enthält String-, Boolean-, Int-, Long-, Float- und Set&lt;String&gt;-Werte.
 */
data class BackupData(
    /** Rezepte-Cache je Datenbank-ID (Schlüsselsuffix nach `recipes_`). */
    val recipeCaches: Map<String, List<Recipe>> = emptyMap(),
    val recipeSyncTimes: Map<String, String> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val meals: List<MealPlanItem> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val covered: List<CoveredItem> = emptyList(),
    val dismissed: Set<String> = emptySet(),
    /** Notion-Datenbank, zu der [inventory] gehört; leer = nur lokal. */
    val inventoryDatabaseId: String = "",
    val inventory: List<InventoryItem> = emptyList(),
    val purchases: Set<String> = emptySet(),
    val userPrefs: Map<String, Any> = emptyMap()
)

/** Ein SharedPreferences-Wert mit Typ, damit auch Int/Long/Float/Set beim Import ihren Typ behalten. */
data class PrefEntry(val type: String? = null, val value: String? = null)

internal object PrefCodec {
    private val gson = Gson()
    private val types = setOf("string", "boolean", "int", "long", "float", "stringset")

    fun encode(value: Any): PrefEntry? = when (value) {
        is String -> PrefEntry("string", value)
        is Boolean -> PrefEntry("boolean", value.toString())
        is Int -> PrefEntry("int", value.toString())
        is Long -> PrefEntry("long", value.toString())
        is Float -> PrefEntry("float", value.toString())
        is Set<*> -> PrefEntry("stringset", gson.toJson(value.filterIsInstance<String>().sorted()))
        else -> null
    }

    /** Null, wenn der Eintrag unlesbar ist. */
    fun decode(entry: PrefEntry): Any? {
        val raw = entry.value ?: return null
        if (entry.type !in types) return null
        return when (entry.type) {
            "string" -> raw
            "boolean" -> raw.toBooleanStrictOrNull()
            "int" -> raw.toIntOrNull()
            "long" -> raw.toLongOrNull()
            "float" -> raw.toFloatOrNull()
            else -> runCatching { gson.fromJson<List<String>>(raw, object : TypeToken<List<String>>() {}.type).toSet() }.getOrNull()
        }
    }
}
