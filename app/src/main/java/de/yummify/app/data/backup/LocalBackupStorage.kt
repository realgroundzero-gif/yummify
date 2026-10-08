package de.yummify.app.data.backup

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import de.yummify.app.data.model.CoveredItem
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.model.normalized
import de.yummify.app.data.repository.normalized
import java.io.File

/** Zugriff auf die SharedPreferences-Dateien; austauschbar, damit Tests Schreibfehler nachstellen können. */
interface PrefsStore {
    fun readAll(name: String): Map<String, Any?>
    /** Ersetzt den gesamten Inhalt der Datei auf einmal. False, wenn das Schreiben fehlschlug. */
    fun replaceAll(name: String, values: Map<String, Any?>): Boolean
    /** Zeitpunkt der letzten Änderung (ms) oder 0, wenn es die Datei nicht gibt. */
    fun modifiedAt(name: String): Long
    fun sizeOf(name: String): Long
}

class AndroidPrefsStore(private val context: Context) : PrefsStore {
    private fun file(name: String) = File(context.dataDir, "shared_prefs/$name.xml")

    override fun readAll(name: String): Map<String, Any?> =
        context.getSharedPreferences(name, Context.MODE_PRIVATE).all.mapValues { (_, v) -> if (v is Set<*>) v.toSet() else v }

    override fun replaceAll(name: String, values: Map<String, Any?>): Boolean {
        val edit = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> edit.putString(key, value)
                is Boolean -> edit.putBoolean(key, value)
                is Int -> edit.putInt(key, value)
                is Long -> edit.putLong(key, value)
                is Float -> edit.putFloat(key, value)
                is Set<*> -> edit.putStringSet(key, value.filterIsInstance<String>().toSet())
                else -> throw IllegalArgumentException("Nicht unterstützter Einstellungswert für $key")
            }
        }
        return edit.commit()
    }

    override fun modifiedAt(name: String) = file(name).takeIf { it.isFile }?.lastModified() ?: 0L
    override fun sizeOf(name: String) = file(name).takeIf { it.isFile }?.length() ?: 0L
}

/** Der lokale Stand: [data] ohne die Bereiche in [unreadable], dazu die unveränderten Rohwerte je Datei. */
class LocalSnapshot(val data: BackupData, val unreadable: Set<BackupDomain>, val raw: Map<String, Map<String, Any?>>)

/** Übersetzt zwischen den Preferences der Repositories und [BackupData]. Kennt die Schlüssel aus [StorageLayout]. */
class LocalBackupStorage(private val store: PrefsStore, private val imagesDir: File) {
    private val gson = Gson()

    private fun <T> list(raw: Any?, type: Class<T>): List<T> {
        val json = raw as? String ?: return emptyList()
        val parsed: List<T?>? = try {
            gson.fromJson(json, TypeToken.getParameterized(List::class.java, type).type)
        } catch (e: JsonParseException) {
            throw BackupParseException(e)
        }
        return parsed.orEmpty().filterNotNull()
    }

    private class BackupParseException(cause: Throwable) : Exception(cause)

    fun collect(includeToken: Boolean): LocalSnapshot {
        val raw = (StorageLayout.dataPrefs + StorageLayout.PREFS_USER).associateWith { store.readAll(it) }
        val unreadable = mutableSetOf<BackupDomain>()
        var data = BackupData()
        fun <T> domain(domain: BackupDomain, read: () -> T, apply: (T) -> Unit) {
            try { apply(read()) } catch (e: BackupParseException) { unreadable += domain }
        }

        val recipes = raw.getValue(StorageLayout.PREFS_RECIPES)
        domain(BackupDomain.RECIPES, {
            val caches = recipes.filterKeys { it.startsWith(StorageLayout.RECIPES_PREFIX) && !it.endsWith("_time") && !StorageLayout.isCorruptCopy(it) }
                .map { (key, value) -> key.removePrefix(StorageLayout.RECIPES_PREFIX) to list(value, Recipe::class.java).map { it.normalized() } }.toMap()
            val times = recipes.filterKeys { it.startsWith(StorageLayout.RECIPES_PREFIX) && it.endsWith("_time") && !StorageLayout.isCorruptCopy(it) }
                .mapNotNull { (key, value) -> (value as? String)?.let { key.removePrefix(StorageLayout.RECIPES_PREFIX).removeSuffix("_time") to it } }.toMap()
            Triple(caches, times, (recipes[StorageLayout.KEY_FAVORITES] as? Set<*>).orEmpty().filterIsInstance<String>().toSet())
        }) { (caches, times, favorites) -> data = data.copy(recipeCaches = caches, recipeSyncTimes = times, favorites = favorites) }

        domain(BackupDomain.MEAL_PLAN, { list(raw.getValue(StorageLayout.PREFS_MEAL_PLAN)[StorageLayout.KEY_MEALS], MealPlanItem::class.java) }) {
            data = data.copy(meals = it)
        }

        val shopping = raw.getValue(StorageLayout.PREFS_SHOPPING)
        domain(BackupDomain.SHOPPING, {
            Triple(list(shopping[StorageLayout.KEY_SHOPPING_ITEMS], ShoppingItem::class.java),
                list(shopping[StorageLayout.KEY_COVERED], CoveredItem::class.java),
                list(shopping[StorageLayout.KEY_DISMISSED], String::class.java).toSet())
        }) { (items, covered, dismissed) -> data = data.copy(shoppingItems = items, covered = covered, dismissed = dismissed) }

        val user = raw.getValue(StorageLayout.PREFS_USER)
        val db = (user[StorageLayout.KEY_INVENTORY_DB] as? String).orEmpty()
        val inventory = raw.getValue(StorageLayout.PREFS_INVENTORY)
        domain(BackupDomain.INVENTORY, { list(inventory[StorageLayout.inventoryKey(db)], InventoryItem::class.java).map { it.normalized() } }) {
            data = data.copy(inventoryDatabaseId = db, inventory = it,
                purchases = (inventory[StorageLayout.purchasesKey(db)] as? Set<*>).orEmpty().filterIsInstance<String>().toSet())
        }

        data = data.copy(userPrefs = user.filter { (key, value) ->
            value != null && !StorageLayout.isCorruptCopy(key) && (includeToken || key !in StorageLayout.secretUserKeys)
        }.mapValues { it.value!! })
        return LocalSnapshot(data, unreadable, raw)
    }

    /** Alle Produktbilder (nur Dateien mit gültigem Namen), nach Dateiname. */
    fun images(): Map<String, File> =
        imagesDir.listFiles { f -> f.isFile && BackupZip.isValidImageName(f.name) }.orEmpty().associateBy { it.name }

    /**
     * Berechnet den neuen Gesamtinhalt jeder betroffenen Preferences-Datei für den Zielzustand [target].
     * Nicht verwaltete Schlüssel (z. B. gesicherte Kopien beschädigter Daten) bleiben erhalten. Beim Zusammenführen
     * werden unlesbare Bereiche nicht angefasst; beim Ersetzen wird ihr Rohwert vorher als Kopie gesichert.
     */
    fun buildWrites(target: BackupData, local: LocalSnapshot, mode: ImportMode, now: Long = System.currentTimeMillis()): Map<String, Map<String, Any?>> {
        val writes = mutableMapOf<String, MutableMap<String, Any?>>()
        fun skip(domain: BackupDomain) = domain in local.unreadable && mode == ImportMode.MERGE
        fun edit(name: String, domain: BackupDomain, managed: (String) -> Boolean, newValues: Map<String, Any?>) {
            if (skip(domain)) return
            val map = writes.getOrPut(name) { local.raw.getValue(name).toMutableMap() }
            if (domain in local.unreadable) {
                map.keys.filter { managed(it) && !StorageLayout.isCorruptCopy(it) }.forEach { key -> (map[key] as? String)?.let { map["${key}_corrupt_$now"] = it } }
            }
            map.keys.removeAll { managed(it) && !StorageLayout.isCorruptCopy(it) }
            map.putAll(newValues)
        }
        edit(StorageLayout.PREFS_RECIPES, BackupDomain.RECIPES,
            { (it.startsWith(StorageLayout.RECIPES_PREFIX) || it == StorageLayout.KEY_FAVORITES) },
            buildMap {
                target.recipeCaches.forEach { (key, list) -> put(StorageLayout.RECIPES_PREFIX + key, gson.toJson(list)) }
                target.recipeSyncTimes.forEach { (key, time) -> put("${StorageLayout.RECIPES_PREFIX}${key}_time", time) }
                put(StorageLayout.KEY_FAVORITES, target.favorites.toSet())
            })
        edit(StorageLayout.PREFS_MEAL_PLAN, BackupDomain.MEAL_PLAN, { it == StorageLayout.KEY_MEALS },
            mapOf(StorageLayout.KEY_MEALS to gson.toJson(target.meals)))
        edit(StorageLayout.PREFS_SHOPPING, BackupDomain.SHOPPING,
            { it == StorageLayout.KEY_SHOPPING_ITEMS || it == StorageLayout.KEY_COVERED || it == StorageLayout.KEY_DISMISSED },
            mapOf(StorageLayout.KEY_SHOPPING_ITEMS to gson.toJson(target.shoppingItems),
                StorageLayout.KEY_COVERED to gson.toJson(target.covered),
                StorageLayout.KEY_DISMISSED to gson.toJson(target.dismissed.toList())))
        val db = target.inventoryDatabaseId
        val itemsKey = StorageLayout.inventoryKey(db)
        val purchasesKey = StorageLayout.purchasesKey(db)
        edit(StorageLayout.PREFS_INVENTORY, BackupDomain.INVENTORY,
            { key -> if (mode == ImportMode.REPLACE) key.startsWith("items_") || key.startsWith("purchases_") else key == itemsKey || key == purchasesKey },
            mapOf(itemsKey to gson.toJson(target.inventory), purchasesKey to target.purchases.toSet()))
        edit(StorageLayout.PREFS_USER, BackupDomain.USER_PREFS, { false }, target.userPrefs)
        return writes
    }
}
