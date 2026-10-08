package de.yummify.app.data.backup

import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.MealPlanItem
import java.io.File
import java.time.LocalDateTime

/**
 * Reine Logik für "Zusammenführen" und "Ersetzen" (kein Android, kein Dateizugriff).
 *
 * Zusammenführen: Elemente werden über ihre ID zusammengelegt, nichts geht verloren. Elemente haben
 * keinen eigenen Änderungszeitpunkt; bei Konflikten (gleiche ID, anderer Inhalt) entscheidet deshalb der
 * Zeitpunkt je Bereich: Ist der Export neuer als die letzte lokale Änderung des Bereichs, gewinnt die Datei,
 * sonst (auch bei Gleichstand) der lokale Stand. Gelöschtes wird nicht übertragen.
 */
object BackupMerge {
    /** Ob der Export für den jeweiligen Bereich neuer ist als die letzte lokale Änderung. */
    data class Newer(
        val recipes: Boolean = false, val mealPlan: Boolean = false, val shopping: Boolean = false,
        val inventory: Boolean = false, val userPrefs: Boolean = false
    )

    fun merge(local: BackupData, imported: BackupData, newer: Newer, takeToken: Boolean): BackupData {
        val cacheKeys = (local.recipeCaches.keys + imported.recipeCaches.keys)
        return BackupData(
            recipeCaches = cacheKeys.associateWith { key ->
                mergeById(local.recipeCaches[key].orEmpty(), imported.recipeCaches[key].orEmpty(), { it.id }) { _, _ -> newer.recipes }
            },
            recipeSyncTimes = cacheKeys.mapNotNull { key -> laterTime(local.recipeSyncTimes[key], imported.recipeSyncTimes[key])?.let { key to it } }.toMap(),
            favorites = local.favorites + imported.favorites,
            meals = mergeMeals(local.meals, imported.meals, newer.mealPlan),
            shoppingItems = mergeById(local.shoppingItems, imported.shoppingItems, { it.id }) { _, _ -> newer.shopping },
            covered = mergeById(local.covered, imported.covered, { it.id }) { _, _ -> newer.shopping },
            dismissed = local.dismissed + imported.dismissed,
            inventoryDatabaseId = local.inventoryDatabaseId,
            inventory = mergeInventory(local.inventory, imported.inventory, newer.inventory),
            purchases = local.purchases + imported.purchases,
            userPrefs = mergeUserPrefs(local.userPrefs, imported.userPrefs, newer.userPrefs, takeToken)
        )
    }

    /** Lokale Daten werden durch den Dateiinhalt ersetzt. Verbindungsdaten und Token bleiben (Token nur mit Zustimmung ersetzt). */
    fun replace(local: BackupData, imported: BackupData, takeToken: Boolean): BackupData = imported.copy(
        inventoryDatabaseId = local.inventoryDatabaseId,
        userPrefs = mergeUserPrefs(local.userPrefs, imported.userPrefs, importedNewer = true, takeToken = takeToken)
    )

    internal fun <T> mergeById(local: List<T>, imported: List<T>, id: (T) -> String, preferImported: (T, T) -> Boolean): List<T> {
        val importedById = imported.associateBy(id)
        val merged = local.map { l -> importedById[id(l)]?.takeIf { it != l && preferImported(l, it) } ?: l }
        val localIds = local.map(id).toSet()
        return merged + imported.distinctBy(id).filter { id(it) !in localIds }
    }

    private fun laterTime(a: String?, b: String?): String? {
        fun parse(value: String?) = value?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val ta = parse(a); val tb = parse(b)
        return when {
            tb == null -> a
            ta == null -> b
            tb.isAfter(ta) -> b
            else -> a
        }
    }

    private fun slot(meal: MealPlanItem) = meal.plannedDate?.takeIf { it.isNotBlank() }?.let { "$it/${meal.mealType}" }

    /** Pro Datum und Mahlzeit gibt es genau einen Eintrag; kollidieren zwei verschiedene IDs, gewinnt der neuere Stand. */
    internal fun mergeMeals(local: List<MealPlanItem>, imported: List<MealPlanItem>, importedNewer: Boolean): List<MealPlanItem> {
        val localIds = local.map { it.id }.toSet()
        var base = local
        val accepted = imported.distinctBy { it.id }.filter { incoming ->
            if (incoming.id in localIds) return@filter true
            val key = slot(incoming) ?: return@filter true
            val clash = base.any { it.id != incoming.id && slot(it) == key }
            if (clash && importedNewer) base = base.filterNot { it.id != incoming.id && slot(it) == key }
            !clash || importedNewer
        }
        return mergeById(base, accepted, { it.id }) { _, _ -> importedNewer }
    }

    /**
     * Inventar: lokale, noch nicht abgeglichene Änderungen (dirty) haben vor einem nicht geänderten Dateistand
     * Vorrang; sonst entscheidet [importedNewer]. Übernommene Einträge werden dirty markiert und laufen über den
     * normalen sync(). Ein gelöschter Eintrag, den es lokal nicht gibt, wird nur mit Notion-Seite übernommen
     * (damit das Archivieren dort nachgeholt wird).
     */
    internal fun mergeInventory(local: List<InventoryItem>, imported: List<InventoryItem>, importedNewer: Boolean): List<InventoryItem> {
        val result = local.toMutableList()
        imported.distinctBy { it.id }.forEach { incoming ->
            val index = result.indexOfFirst { it.id == incoming.id || (incoming.pageId != null && it.pageId == incoming.pageId) }
            if (index < 0) {
                if (!incoming.deleted || incoming.pageId != null) result += incoming.copy(dirty = true)
            } else {
                val current = result[index]
                val candidate = incoming.copy(id = current.id)
                val takeIncoming = candidate != current && importedNewer && !(current.dirty && !incoming.dirty)
                if (takeIncoming) result[index] = candidate.copy(dirty = true)
            }
        }
        return result
    }

    /**
     * Einstellungen je Schlüssel. Zugangsdaten nur mit Zustimmung, Verbindungsdaten nur wenn lokal leer,
     * alles andere nach [importedNewer]; unbekannte Schlüssel (neue Einstellungen) werden mitgenommen.
     */
    internal fun mergeUserPrefs(local: Map<String, Any>, imported: Map<String, Any>, importedNewer: Boolean, takeToken: Boolean): Map<String, Any> {
        val result = local.toMutableMap()
        imported.forEach { (key, value) ->
            val text = value as? String
            when {
                key in StorageLayout.secretUserKeys -> if (takeToken && !text.isNullOrBlank()) result[key] = value
                key in StorageLayout.connectionUserKeys -> if ((result[key] as? String).isNullOrBlank() && !text.isNullOrBlank()) result[key] = value
                key !in result || importedNewer -> result[key] = value
            }
        }
        return result
    }

    /**
     * Bezieht importierte Inventar-Einträge auf die lokalen Bilder: Pfade zeigen auf [imagesDir], fehlt die Datei
     * in der Sicherung, wird der Pfad entfernt. Gehört die Sicherung zu einer anderen Notion-Datenbank, werden
     * Seiten-IDs verworfen (sie gelten nur dort).
     */
    fun rebaseInventory(items: List<InventoryItem>, imagesDir: File, availableImages: Set<String>, dropPageIds: Boolean): List<InventoryItem> =
        items.map { item ->
            val name = item.localCoverPath?.let { File(it.replace('\\', '/')).name }
            item.copy(
                localCoverPath = name?.takeIf { it in availableImages }?.let { File(imagesDir, it).absolutePath },
                pageId = if (dropPageIds) null else item.pageId
            )
        }
}
