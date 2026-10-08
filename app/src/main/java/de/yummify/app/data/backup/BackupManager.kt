package de.yummify.app.data.backup

import android.content.Context
import coil.imageLoader
import de.yummify.app.data.repository.InventoryRepository
import de.yummify.app.data.repository.MealPlanRepository
import de.yummify.app.data.repository.RecipeRepository
import de.yummify.app.data.repository.ShoppingListRepository
import de.yummify.app.data.repository.UserPreferencesRepository
import de.yummify.app.widget.MealPlannerWidgetProvider
import de.yummify.app.widget.ShoppingListWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/** Was Import und Löschen von den laufenden Repositories brauchen (Schnittstelle, damit Tests sie ersetzen können). */
interface RepositoryHub {
    suspend fun reloadData()
    fun reloadPreferences()
    val pendingInventoryChanges: Int
}

/** Die laufenden Singleton-Repositories; nach Import oder Löschen müssen sie ihren Speicherstand neu einlesen. */
class AppRepositories(
    private val recipes: RecipeRepository, private val mealPlan: MealPlanRepository, private val shopping: ShoppingListRepository,
    private val inventory: InventoryRepository, private val user: UserPreferencesRepository
) : RepositoryHub {
    override suspend fun reloadData() {
        recipes.reloadFromStorage(); mealPlan.reloadFromStorage(); shopping.reloadFromStorage(); inventory.reloadFromStorage()
    }

    /** Zuletzt aufrufen: Ändert sich die Inventar-Datenbank, übernimmt das Inventar seine lokalen Einträge selbst. */
    override fun reloadPreferences() = user.reloadFromStorage()

    override val pendingInventoryChanges: Int get() = inventory.syncState.value.pending

    companion object {
        fun of(context: Context) = AppRepositories(
            RecipeRepository.getInstance(context), MealPlanRepository.getInstance(context), ShoppingListRepository.getInstance(context),
            InventoryRepository.getInstance(context), UserPreferencesRepository.getInstance(context)
        )
    }
}

data class ExportResult(val counts: BackupCounts, val includesToken: Boolean, val warnings: List<String>)
data class ImportResult(val counts: BackupCounts, val mode: ImportMode, val warnings: List<String>)

/**
 * Export, Import, Speicheranzeige und Löschen der lokalen Daten. Import und Löschen laufen als Transaktion:
 * Alles wird vorab geprüft und berechnet, dann geschrieben; schlägt ein Schritt fehl, wird der vorherige Stand wiederhergestellt.
 */
class BackupManager internal constructor(
    private val context: Context,
    private val appVersion: String,
    private val store: PrefsStore = AndroidPrefsStore(context),
    private val repositories: RepositoryHub = AppRepositories.of(context),
    private val now: () -> Instant = Instant::now
) {
    private val imagesDir = File(context.filesDir, StorageLayout.IMAGES_DIR)
    private val storage = LocalBackupStorage(store, imagesDir)
    private val workDir get() = File(context.noBackupFilesDir, "backup_work")

    val pendingInventoryChanges: Int get() = repositories.pendingInventoryChanges

    // ---- Export ----

    /** Schreibt den Export. [out] wird nicht geschlossen. Erst wenn die ganze Datei gebaut und geprüft ist, wird sie übertragen. */
    fun export(out: OutputStream, includeToken: Boolean): ExportResult {
        val local = storage.collect(includeToken)
        val data = local.data.copy(userPrefs = local.data.userPrefs.filterKeys { includeToken || it !in StorageLayout.secretUserKeys })
        val images = storage.images()
        val counts = BackupCounts.of(data, images.size)
        val manifest = BackupManifest(BackupManifest.FORMAT, BackupManifest.CURRENT_VERSION, appVersion, now().toString(),
            includeToken, counts.toMap())
        val work = workDir.apply { deleteRecursively(); mkdirs() }
        val zip = File(work, "export.zip")
        try {
            try {
                zip.outputStream().buffered().use { BackupZip.write(it, manifest, data, images) }
                zip.inputStream().buffered().use { BackupZip.read(it, File(work, "verify")) }   // die Datei muss sich wieder einlesen lassen
            } catch (e: IOException) {
                throw BackupException("Die Sicherung konnte nicht erstellt werden: ${e.message ?: "Speicher voll?"}", e)
            }
            try {
                zip.inputStream().use { it.copyTo(out) }
                out.flush()
            } catch (e: IOException) {
                throw BackupException("Die Datei konnte nicht geschrieben werden: ${e.message ?: "unbekannter Fehler"}", e)
            }
        } finally {
            work.deleteRecursively()
        }
        val warnings = local.unreadable.map { "${it.label} war beschädigt und wurde nicht gesichert." }
        return ExportResult(counts, includeToken, warnings)
    }

    // ---- Import ----

    /** Liest und prüft die Datei vollständig, ohne etwas zu verändern. */
    fun prepareImport(input: InputStream): ParsedBackup {
        val staging = File(workDir, "import")
        workDir.deleteRecursively()
        return BackupZip.read(input, staging)
    }

    fun discardImport(parsed: ParsedBackup) { parsed.stagingDir.deleteRecursively() }

    suspend fun applyImport(parsed: ParsedBackup, mode: ImportMode, takeToken: Boolean): ImportResult = withContext(Dispatchers.IO) {
        val local = storage.collect(includeToken = true)
        val importedAt = parsed.createdAt.toEpochMilli()
        fun newer(prefs: String) = importedAt > store.modifiedAt(prefs)
        val newer = BackupMerge.Newer(newer(StorageLayout.PREFS_RECIPES), newer(StorageLayout.PREFS_MEAL_PLAN),
            newer(StorageLayout.PREFS_SHOPPING), newer(StorageLayout.PREFS_INVENTORY), newer(StorageLayout.PREFS_USER))
        val localDb = local.data.inventoryDatabaseId
        val importedDb = parsed.data.inventoryDatabaseId
        val otherDatabase = localDb.isNotBlank() && importedDb.isNotBlank() && localDb != importedDb
        val imported = parsed.data.copy(inventory = BackupMerge.rebaseInventory(parsed.data.inventory, imagesDir, parsed.images.keys, otherDatabase))
        val target = if (mode == ImportMode.MERGE) BackupMerge.merge(local.data, imported, newer, takeToken)
        else BackupMerge.replace(local.data, imported, takeToken)
        val writes = storage.buildWrites(target, local, mode)

        fun copyImages(fresh: File, old: File?) {
            if (mode == ImportMode.MERGE) old?.listFiles()?.filter { it.isFile }?.forEach { it.copyTo(File(fresh, it.name)) }
            parsed.images.forEach { (name, file) -> File(fresh, name).takeIf { !it.exists() }?.let { file.copyTo(it) } }
        }
        val populate: ((File, File?) -> Unit)? = if (parsed.images.isEmpty() && mode == ImportMode.MERGE) null else ::copyImages
        transact(writes, populate)
        afterChange()
        discardImport(parsed)

        val warnings = buildList {
            if (mode == ImportMode.MERGE) local.unreadable.forEach { add("${it.label} ist lokal beschädigt und wurde nicht verändert.") }
            if (otherDatabase) add("Das Inventar stammt aus einer anderen Notion-Datenbank. Die Verknüpfungen zu Notion-Seiten wurden entfernt.")
        }
        ImportResult(parsed.counts, mode, warnings)
    }

    // ---- Speicher ----

    fun storageUsage() = StorageUsage(
        dataBytes = (StorageLayout.dataPrefs + StorageLayout.PREFS_USER).sumOf { store.sizeOf(it) },
        imageBytes = sizeOf(imagesDir),
        cacheBytes = sizeOf(context.cacheDir)
    )

    private fun sizeOf(file: File): Long = if (file.isFile) file.length() else file.listFiles().orEmpty().sumOf { sizeOf(it) }

    /** Leert nur den Cache (Coil-Bildcache und Dateien in cacheDir). Rezepte, Inventar und Produktbilder bleiben. */
    fun clearCache() {
        val loader = context.imageLoader
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
        // Der Coil-Ordner bleibt stehen, damit der geöffnete Cache nicht beschädigt wird.
        val keep = loader.diskCache?.directory?.toFile()?.canonicalFile
        val failed = context.cacheDir.listFiles().orEmpty().filter { it.canonicalFile != keep }.count { !it.deleteRecursively() }
        if (failed > 0) throw BackupException("Nicht alle Cache-Dateien konnten gelöscht werden.")
    }

    // ---- Alle lokalen Daten löschen ----

    /**
     * Entfernt Rezepte-Cache, Favoriten, Wochenplan, Einkaufsliste, Inventar samt Produktbildern und den Cache.
     * Verbindungsdaten (Token, Datenbank-IDs) und Einstellungen bleiben; an Notion wird nichts geschrieben.
     */
    suspend fun deleteAllLocalData() = withContext(Dispatchers.IO) {
        transact(StorageLayout.dataPrefs.associateWith { emptyMap<String, Any?>() }) { _, _ -> }
        workDir.deleteRecursively()
        afterChange()
        clearCache()
    }

    private suspend fun afterChange() {
        repositories.reloadData()
        repositories.reloadPreferences()
        MealPlannerWidgetProvider.updateAllWidgets(context)
        ShoppingListWidgetProvider.updateAllWidgets(context)
    }

    /**
     * Schreibt alle [writes] und tauscht optional den Bilderordner aus (`populate(neuer Ordner, alter Ordner)`).
     * Bei jedem Fehler werden Preferences und Bilder auf den Stand davor zurückgesetzt.
     */
    private fun transact(writes: Map<String, Map<String, Any?>>, populate: ((File, File?) -> Unit)?) {
        val snapshots = writes.keys.associateWith { store.readAll(it) }
        val oldDir = File(imagesDir.parentFile, imagesDir.name + ".old")
        var swapped = false
        try {
            if (populate != null) {
                if (oldDir.exists()) { if (!imagesDir.exists()) oldDir.renameTo(imagesDir) else oldDir.deleteRecursively() }
                val hadImages = imagesDir.exists()
                if (hadImages) check(imagesDir.renameTo(oldDir)) { "Bilderordner konnte nicht gesichert werden." }
                swapped = true
                check(imagesDir.mkdirs()) { "Bilderordner konnte nicht angelegt werden." }
                populate(imagesDir, oldDir.takeIf { hadImages })
            }
            for ((name, values) in writes) check(store.replaceAll(name, values)) { "Datei $name konnte nicht geschrieben werden." }
        } catch (e: Exception) {
            val failedRestores = snapshots.filter { (name, values) -> !store.replaceAll(name, values) }.keys
            if (swapped) {
                imagesDir.deleteRecursively()
                if (oldDir.exists()) oldDir.renameTo(imagesDir)
            }
            throw BackupException(
                if (failedRestores.isEmpty()) "Die Daten konnten nicht übernommen werden. Der vorherige Stand wurde wiederhergestellt. (${e.message ?: "unbekannter Fehler"})"
                else "Die Daten konnten nicht übernommen werden und der vorherige Stand ließ sich nicht vollständig wiederherstellen. Bitte die App neu starten. (${e.message})", e)
        }
        oldDir.deleteRecursively()
    }

    companion object {
        fun create(context: Context): BackupManager {
            val app = context.applicationContext
            val version = app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "unbekannt"
            return BackupManager(app, version)
        }
    }
}
