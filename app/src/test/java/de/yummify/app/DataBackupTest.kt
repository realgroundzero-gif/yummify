package de.yummify.app

import android.content.Context
import com.google.gson.Gson
import de.yummify.app.data.backup.*
import de.yummify.app.data.model.CoveredItem
import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.MealType
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.repository.InventoryRepository
import de.yummify.app.data.repository.MealPlanRepository
import de.yummify.app.data.repository.RecipeRepository
import de.yummify.app.data.repository.ShoppingListRepository
import de.yummify.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val TOKEN = "secret_abc123TOKEN"

private class FakeHub(var pending: Int = 0) : RepositoryHub {
    var dataReloads = 0
    var prefReloads = 0
    override suspend fun reloadData() { dataReloads++ }
    override fun reloadPreferences() { prefReloads++ }
    override val pendingInventoryChanges get() = pending
}

/** Echte Preferences, aber das n-te Schreiben schlägt fehl (Rollback-Test). Danach arbeitet es normal. */
private class FailingStore(private val inner: PrefsStore, private val failOn: Int) : PrefsStore by inner {
    var writes = 0
    override fun replaceAll(name: String, values: Map<String, Any?>): Boolean = if (++writes == failOn) false else inner.replaceAll(name, values)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataBackupTest {
    private lateinit var context: Context
    private lateinit var store: AndroidPrefsStore
    private val gson = Gson()
    private val imagesDir get() = File(context.filesDir, StorageLayout.IMAGES_DIR)

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        store = AndroidPrefsStore(context)
        StorageLayout.allPrefs.forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        imagesDir.deleteRecursively()
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        UserPreferencesRepository.getInstance(context).saveNotionConfig("", "", "")
    }

    private fun manager(hub: RepositoryHub = FakeHub(), prefs: PrefsStore = store, at: String = "2030-01-01T00:00:00Z") =
        BackupManager(context, "1.3.0", prefs, hub) { Instant.parse(at) }

    private fun recipe(id: String, title: String = "Rezept $id") = Recipe(id, title, "", "", 20, 400, 10, 50, 10, "all", emptyList(),
        ingredients = listOf(Ingredient("Mehl", 100.0, "g")), instructions = listOf("Kochen"))

    private fun item(id: String, quantity: Double = 1.0, dirty: Boolean = false, cover: String? = null, pageId: String? = null) =
        InventoryItem(id = id, name = "Artikel $id", quantity = quantity, dirty = dirty, localCoverPath = cover, pageId = pageId)

    private fun meal(id: String, date: String, type: MealType = MealType.DINNER) =
        MealPlanItem(id, "Mo", 1, type, "r1", "Titel", "", null, null, null, plannedDate = date)

    private fun prefs(name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    /** Legt in allen Bereichen Daten ab, inklusive Token und einem Produktbild. */
    private fun seed(dbId: String = "") {
        prefs(StorageLayout.PREFS_RECIPES).edit().putString("recipes_db1", gson.toJson(listOf(recipe("r1"), recipe("r2"))))
            .putString("recipes_db1_time", "2026-01-02T10:00:00").putStringSet("favorites", setOf("r1")).commit()
        prefs(StorageLayout.PREFS_MEAL_PLAN).edit().putString("planned_meals_json", gson.toJson(listOf(meal("m1", "2026-02-01")))).commit()
        prefs(StorageLayout.PREFS_SHOPPING).edit()
            .putString("shopping_items_json", gson.toJson(listOf(ShoppingItem("shop1", "Milch", "1 l", "Kühlregal"))))
            .putString("covered_items_json", gson.toJson(listOf(CoveredItem("c1", "Salz", "1 TL", "Vorrat", null, null, null))))
            .putString("dismissed_suggestions_json", gson.toJson(listOf("a|b"))).commit()
        imagesDir.mkdirs()
        File(imagesDir, "photo1.jpg").writeBytes(byteArrayOf(1, 2, 3, 4))
        prefs(StorageLayout.PREFS_USER).edit().putString("notion_token", TOKEN).putString("notion_db_id", "db1")
            .putString("notion_inventory_db_id", dbId).putBoolean("dark_mode", true).commit()
        prefs(StorageLayout.PREFS_INVENTORY).edit()
            .putString(StorageLayout.inventoryKey(dbId), gson.toJson(listOf(item("i1", cover = File(imagesDir, "photo1.jpg").absolutePath, dirty = true), item("i2"))))
            .putStringSet("purchases_$dbId", setOf("p1")).commit()
    }

    private fun exportBytes(manager: BackupManager, includeToken: Boolean): ByteArray =
        ByteArrayOutputStream().also { manager.export(it, includeToken) }.toByteArray()

    private fun zipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) { val e = zip.nextEntry ?: break; result[e.name] = zip.readBytes() }
        }
        return result
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip -> entries.forEach { (name, text) -> zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() } }
    }.toByteArray()

    private fun manifestJson(version: Int = 1, format: String = "yummify-backup", counts: String = "") =
        """{"format":"$format","formatVersion":$version,"appVersion":"1.3.0","createdAt":"2030-01-01T00:00:00Z","includesToken":false$counts}"""

    private fun snapshot() = StorageLayout.allPrefs.associateWith { store.readAll(it) }

    private fun importError(bytes: ByteArray): String {
        val e = assertThrows(BackupException::class.java) { manager().prepareImport(ByteArrayInputStream(bytes)) }
        return e.message.orEmpty()
    }

    // ---- Export ----

    @Test fun exportContainsAllAreasButNoTokenByDefault() {
        seed()
        val bytes = exportBytes(manager(), includeToken = false)
        val entries = zipEntries(bytes)
        assertEquals(setOf("manifest.json", "data/recipes.json", "data/meal_plan.json", "data/shopping_list.json", "data/inventory.json",
            "data/user_prefs.json", "images/photo1.jpg"), entries.keys)
        entries.values.forEach { assertFalse(String(it, Charsets.ISO_8859_1).contains(TOKEN)) }
        val manifest = gson.fromJson(String(entries.getValue("manifest.json")), BackupManifest::class.java)
        assertEquals("yummify-backup", manifest.format)
        assertEquals(1, manifest.formatVersion)
        assertEquals("1.3.0", manifest.appVersion)
        assertEquals("2030-01-01T00:00:00Z", manifest.createdAt)
        assertEquals(false, manifest.includesToken)
        assertTrue(String(entries.getValue("data/user_prefs.json")).contains("dark_mode"))
        assertTrue(String(entries.getValue("data/shopping_list.json")).contains("c1"))
        assertTrue(String(entries.getValue("data/shopping_list.json")).contains("a|b"))
    }

    @Test fun exportWithTokenOnlyWhenRequested() {
        seed()
        val entries = zipEntries(exportBytes(manager(), includeToken = true))
        assertTrue(String(entries.getValue("data/user_prefs.json")).contains(TOKEN))
        assertEquals(true, gson.fromJson(String(entries.getValue("manifest.json")), BackupManifest::class.java).includesToken)
    }

    @Test fun exportReportsElementCountAndRoundTrips() {
        seed()
        val out = ByteArrayOutputStream()
        val result = manager().export(out, false)
        // 2 Rezepte + 1 Favorit + 1 Plan + 1 Einkauf + 2 Inventar + 1 Bild
        assertEquals(8, result.counts.total)
        val parsed = manager().prepareImport(ByteArrayInputStream(out.toByteArray()))
        assertEquals(result.counts, parsed.counts)
        assertEquals(setOf("photo1.jpg"), parsed.images.keys)
    }

    @Test fun exportSkipsUnreadableAreaWithWarningInsteadOfFailingSilently() {
        seed()
        prefs(StorageLayout.PREFS_MEAL_PLAN).edit().putString("planned_meals_json", "{kaputt").commit()
        val result = manager().export(ByteArrayOutputStream(), false)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings.single().contains("Wochenplan"))
    }

    @Test fun exportToBrokenStreamFailsWithGermanMessage() {
        seed()
        val broken = object : java.io.OutputStream() { override fun write(b: Int) = throw java.io.IOException("Speicher voll") }
        val e = assertThrows(BackupException::class.java) { manager().export(broken, false) }
        assertTrue(e.message!!.contains("geschrieben"))
        assertFalse(File(context.noBackupFilesDir, "backup_work").exists())
    }

    // ---- Prüfung beim Import ----

    @Test fun importRejectsBrokenFilesWithGermanMessageAndLeavesNothingBehind() {
        seed()
        val before = snapshot()
        assertTrue(importError("das ist keine zip".toByteArray()).contains("ZIP"))
        assertTrue(importError(ByteArray(0)).contains("ZIP"))
        assertTrue(importError(zipOf("irgendwas.txt" to "hallo")).contains("Manifest"))
        assertTrue(importError(zipOf("manifest.json" to "{nicht json")).contains("Manifest"))
        assertTrue(importError(zipOf("manifest.json" to manifestJson(format = "andere-app"))).contains("Kein Yummify-Export"))
        assertTrue(importError(zipOf("manifest.json" to manifestJson(version = 2))).contains("neueren Version"))
        assertTrue(importError(zipOf("manifest.json" to manifestJson(version = 0))).contains("Formatversion"))
        assertTrue(importError(zipOf("manifest.json" to manifestJson(), "data/recipes.json" to "[[[")).contains("recipes.json"))
        assertTrue(importError(zipOf("manifest.json" to manifestJson(counts = ""","elementCounts":{"recipes":3,"favorites":0,"meals":0,"shoppingItems":0,"inventoryItems":0,"images":0}"""))).contains("unvollständig"))
        val valid = exportBytes(manager(), false)
        assertTrue(importError(valid.copyOf(valid.size / 2)).isNotBlank())   // abgeschnitten
        assertEquals(before, snapshot())
        assertTrue(File(imagesDir, "photo1.jpg").exists())
        assertFalse(File(context.noBackupFilesDir, "backup_work/import").exists())
    }

    @Test fun importRejectsUnsafeImageNamesByIgnoringThem() {
        val parsed = manager().prepareImport(ByteArrayInputStream(zipOf("manifest.json" to manifestJson(), "images/../../evil.txt" to "x", "images/ok.jpg" to "y")))
        assertEquals(setOf("ok.jpg"), parsed.images.keys)
        assertFalse(File(context.noBackupFilesDir, "evil.txt").exists())
    }

    @Test fun olderExportWithoutNewFieldsIsNormalized() = runBlocking {
        val inventory = """{"items":[{"id":"a","name":"Mehl","quantity":1.0,"unit":"kg","dirty":false}]}"""
        val recipes = """{"caches":{"db1":[{"id":"r","title":"Alt"}]}}"""
        val bytes = zipOf("manifest.json" to manifestJson(), "data/inventory.json" to inventory, "data/recipes.json" to recipes)
        val m = manager()
        m.applyImport(m.prepareImport(ByteArrayInputStream(bytes)), ImportMode.MERGE, false)
        val stored = gson.fromJson(prefs(StorageLayout.PREFS_INVENTORY).getString("items_local", null), Array<InventoryItem>::class.java).single()
        assertEquals("Mehl", stored.name)
        // Neue Felder fehlen in der Datei: nach dem Laden durch das Repository sind sie nie null.
        val loaded = InventoryRepository(context).items.value.single()
        assertEquals(emptyList<String>(), loaded.categoryOptions)
        assertNull(loaded.validate())
        assertEquals("Alt", gson.fromJson(prefs(StorageLayout.PREFS_RECIPES).getString("recipes_db1", null), Array<Recipe>::class.java).single().title)
    }

    // ---- Import: Zusammenführen und Ersetzen ----

    @Test fun mergeKeepsLocalDataAddsNewAndMarksImportedInventoryDirty() = runBlocking {
        seed()
        val exported = exportBytes(manager(at = "2000-01-01T00:00:00Z"), false)   // Datei älter als jede lokale Änderung
        // Lokal ändern: i1 wird anders, ein neuer Artikel kommt dazu, ein Plan-Eintrag wird entfernt.
        prefs(StorageLayout.PREFS_INVENTORY).edit().putString("items_local", gson.toJson(listOf(item("i1", quantity = 9.0, dirty = true), item("i3")))).commit()
        prefs(StorageLayout.PREFS_MEAL_PLAN).edit().putString("planned_meals_json", gson.toJson(listOf(meal("m9", "2026-03-01")))).commit()
        val m = manager()
        val result = m.applyImport(m.prepareImport(ByteArrayInputStream(exported)), ImportMode.MERGE, false)
        assertEquals(ImportMode.MERGE, result.mode)
        val inventory = gson.fromJson(prefs(StorageLayout.PREFS_INVENTORY).getString("items_local", null), Array<InventoryItem>::class.java).associateBy { it.id }
        assertEquals(setOf("i1", "i2", "i3"), inventory.keys)
        assertEquals(9.0, inventory.getValue("i1").quantity, 0.0)     // lokal gewinnt: Datei ist älter
        assertTrue(inventory.getValue("i2").dirty)                     // aus der Datei übernommen -> sync() überträgt es
        val meals = gson.fromJson(prefs(StorageLayout.PREFS_MEAL_PLAN).getString("planned_meals_json", null), Array<MealPlanItem>::class.java)
        assertEquals(setOf("m1", "m9"), meals.map { it.id }.toSet())
        assertEquals(setOf("r1"), prefs(StorageLayout.PREFS_RECIPES).getStringSet("favorites", null))
    }

    @Test fun mergeTakesNewerFileVersionOnConflictAndLocalOnTie() {
        val local = item("a", quantity = 1.0)
        val file = item("a", quantity = 5.0)
        assertEquals(5.0, BackupMerge.mergeInventory(listOf(local), listOf(file), importedNewer = true).single().quantity, 0.0)
        assertTrue(BackupMerge.mergeInventory(listOf(local), listOf(file), importedNewer = true).single().dirty)
        assertEquals(1.0, BackupMerge.mergeInventory(listOf(local), listOf(file), importedNewer = false).single().quantity, 0.0)   // Gleichstand/älter: lokal
        // Nicht abgeglichene lokale Änderung schlägt einen unveränderten Dateistand, auch wenn er neuer ist.
        assertEquals(1.0, BackupMerge.mergeInventory(listOf(local.copy(dirty = true)), listOf(file), importedNewer = true).single().quantity, 0.0)
        // Gelöschte Einträge ohne Notion-Seite werden nicht neu angelegt.
        assertTrue(BackupMerge.mergeInventory(emptyList(), listOf(item("x").copy(deleted = true)), true).isEmpty())
        assertEquals(1, BackupMerge.mergeInventory(emptyList(), listOf(item("x", pageId = "p").copy(deleted = true)), true).size)
    }

    @Test fun mealsInTheSameSlotKeepOnlyTheWinner() {
        val local = listOf(meal("local", "2026-05-01"))
        val file = listOf(meal("file", "2026-05-01"), meal("other", "2026-05-02"))
        assertEquals(listOf("local", "other"), BackupMerge.mergeMeals(local, file, importedNewer = false).map { it.id })
        assertEquals(setOf("file", "other"), BackupMerge.mergeMeals(local, file, importedNewer = true).map { it.id }.toSet())
    }

    @Test fun userPrefsMergeNeverTakesTokenWithoutConsentAndKeepsConnection() {
        val local = mapOf<String, Any>("notion_token" to "lokal", "notion_db_id" to "dbL", "notion_inventory_db_id" to "", "dark_mode" to false)
        val file = mapOf<String, Any>("notion_token" to "datei", "notion_db_id" to "dbF", "notion_inventory_db_id" to "invF", "dark_mode" to true, "neu" to 3)
        val without = BackupMerge.mergeUserPrefs(local, file, importedNewer = true, takeToken = false)
        assertEquals("lokal", without["notion_token"])
        assertEquals("dbL", without["notion_db_id"])
        assertEquals("invF", without["notion_inventory_db_id"])   // lokal leer -> aus der Datei
        assertEquals(true, without["dark_mode"])
        assertEquals(3, without["neu"])
        assertEquals("datei", BackupMerge.mergeUserPrefs(local, file, true, takeToken = true)["notion_token"])
        assertEquals(false, BackupMerge.mergeUserPrefs(local, file, importedNewer = false, takeToken = false)["dark_mode"])
        // Eine Datei ohne Token löscht den lokalen nie.
        assertEquals("lokal", BackupMerge.mergeUserPrefs(local, emptyMap(), true, true)["notion_token"])
    }

    @Test fun replaceSwapsLocalDataForFileContentButKeepsConnectionAndToken() = runBlocking {
        seed(dbId = "")
        val exported = exportBytes(manager(), includeToken = true)
        // Lokaler Stand weicht ab.
        prefs(StorageLayout.PREFS_INVENTORY).edit().putString("items_local", gson.toJson(listOf(item("lokal")))).commit()
        prefs(StorageLayout.PREFS_SHOPPING).edit().putString("shopping_items_json", "[]").commit()
        File(imagesDir, "local-only.jpg").writeBytes(byteArrayOf(9))
        prefs(StorageLayout.PREFS_USER).edit().putString("notion_token", "anderer").putString("notion_db_id", "dbLokal").commit()
        val m = manager()
        m.applyImport(m.prepareImport(ByteArrayInputStream(exported)), ImportMode.REPLACE, takeToken = false)
        val inventory = gson.fromJson(prefs(StorageLayout.PREFS_INVENTORY).getString("items_local", null), Array<InventoryItem>::class.java)
        assertEquals(setOf("i1", "i2"), inventory.map { it.id }.toSet())
        assertEquals(File(imagesDir, "photo1.jpg").absolutePath, inventory.first { it.id == "i1" }.localCoverPath)
        assertEquals(listOf("photo1.jpg"), imagesDir.list()!!.toList())
        assertTrue(prefs(StorageLayout.PREFS_SHOPPING).getString("shopping_items_json", "")!!.contains("Milch"))
        assertEquals("anderer", prefs(StorageLayout.PREFS_USER).getString("notion_token", null))   // keine Zustimmung
        assertEquals("dbLokal", prefs(StorageLayout.PREFS_USER).getString("notion_db_id", null))
    }

    @Test fun tokenFromFileIsAppliedOnlyWithConsent() = runBlocking {
        seed()
        val exported = exportBytes(manager(), includeToken = true)
        prefs(StorageLayout.PREFS_USER).edit().putString("notion_token", "alt").commit()
        val m = manager()
        m.applyImport(m.prepareImport(ByteArrayInputStream(exported)), ImportMode.MERGE, takeToken = true)
        assertEquals(TOKEN, prefs(StorageLayout.PREFS_USER).getString("notion_token", null))
    }

    @Test fun importRebasesImagePathsAndMergeKeepsLocalImages() = runBlocking {
        seed()
        val exported = exportBytes(manager(), false)
        // "Neues Gerät": Bilder und Daten sind weg, nur ein anderes lokales Bild existiert.
        imagesDir.deleteRecursively(); imagesDir.mkdirs(); File(imagesDir, "mine.jpg").writeBytes(byteArrayOf(7))
        prefs(StorageLayout.PREFS_INVENTORY).edit().clear().commit()
        val m = manager()
        m.applyImport(m.prepareImport(ByteArrayInputStream(exported)), ImportMode.MERGE, false)
        assertEquals(setOf("mine.jpg", "photo1.jpg"), imagesDir.list()!!.toSet())
        val i1 = gson.fromJson(prefs(StorageLayout.PREFS_INVENTORY).getString("items_local", null), Array<InventoryItem>::class.java).first { it.id == "i1" }
        assertEquals(File(imagesDir, "photo1.jpg").absolutePath, i1.localCoverPath)
        assertFalse(File(imagesDir.parentFile, "inventory_images.old").exists())
    }

    @Test fun inventoryFromAnotherNotionDatabaseDropsPageIdsAndWarns() = runBlocking {
        seed(dbId = "invA")
        prefs(StorageLayout.PREFS_INVENTORY).edit().putString("items_invA", gson.toJson(listOf(item("i1", pageId = "page1")))).commit()
        val exported = exportBytes(manager(), false)
        prefs(StorageLayout.PREFS_USER).edit().putString("notion_inventory_db_id", "invB").commit()
        val m = manager()
        val result = m.applyImport(m.prepareImport(ByteArrayInputStream(exported)), ImportMode.REPLACE, false)
        assertTrue(result.warnings.single().contains("anderen Notion-Datenbank"))
        val stored = gson.fromJson(prefs(StorageLayout.PREFS_INVENTORY).getString("items_invB", null), Array<InventoryItem>::class.java).single()
        assertNull(stored.pageId)
    }

    @Test fun previewDoesNotChangeAnything() {
        seed()
        val exported = exportBytes(manager(), true)
        val before = snapshot()
        val images = imagesDir.list()!!.toList()
        val hub = FakeHub()
        val parsed = manager(hub).prepareImport(ByteArrayInputStream(exported))
        assertEquals(8, parsed.counts.total)
        assertEquals(before, snapshot())
        assertEquals(images, imagesDir.list()!!.toList())
        assertEquals(0, hub.dataReloads)
    }

    // ---- Import: Rollback ----

    @Test fun failureWhileApplyingRestoresThePreviousState() {
        seed()
        val exported = exportBytes(manager(), false)
        prefs(StorageLayout.PREFS_MEAL_PLAN).edit().putString("planned_meals_json", gson.toJson(listOf(meal("lokal", "2026-09-09")))).commit()
        File(imagesDir, "local-only.jpg").writeBytes(byteArrayOf(5))
        val before = snapshot()
        val imagesBefore = imagesDir.list()!!.toSet()
        for (failOn in 1..4) {
            val hub = FakeHub()
            val failing = FailingStore(store, failOn)
            val m = manager(hub, failing)
            val parsed = m.prepareImport(ByteArrayInputStream(exported))
            val e = assertThrows(BackupException::class.java) { runBlocking { m.applyImport(parsed, ImportMode.REPLACE, false) } }
            assertTrue(e.message!!.contains("wiederhergestellt"))
            assertEquals("Schreibversuch $failOn", before, snapshot())
            assertEquals(imagesBefore, imagesDir.list()!!.toSet())
            assertFalse(File(imagesDir.parentFile, "inventory_images.old").exists())
            assertEquals(0, hub.dataReloads)   // Repositories wurden nicht angefasst
        }
    }

    // ---- Speicher ----

    @Test fun storageUsageIsSplitIntoDataImagesAndCache() {
        seed()
        File(context.cacheDir, "x/y").mkdirs()
        File(context.cacheDir, "x/y/blob").writeBytes(ByteArray(3000))
        val usage = manager().storageUsage()
        assertTrue(usage.dataBytes > 0)
        assertEquals(4, usage.imageBytes)
        assertEquals(3000, usage.cacheBytes)
    }

    @Test fun bytesAreShownInReadableUnits() {
        assertEquals("0 B", formatBytes(0, Locale.GERMAN))
        assertEquals("1023 B", formatBytes(1023, Locale.GERMAN))
        assertEquals("1,0 KB", formatBytes(1024, Locale.GERMAN))
        assertEquals("1,5 MB", formatBytes(1536L * 1024, Locale.GERMAN))
        assertEquals("2,0 GB", formatBytes(2L * 1024 * 1024 * 1024, Locale.GERMAN))
    }

    @Test fun clearingTheCacheKeepsRecipesInventoryAndImages() {
        seed()
        File(context.cacheDir, "shares").mkdirs()
        File(context.cacheDir, "shares/card.png").writeBytes(ByteArray(100))
        val before = snapshot()
        manager().clearCache()
        assertFalse(File(context.cacheDir, "shares").exists())
        assertTrue(manager().storageUsage().cacheBytes < 100)   // höchstens die Verwaltungsdatei des geöffneten Coil-Caches
        assertEquals(before, snapshot())
        assertTrue(File(imagesDir, "photo1.jpg").exists())
    }

    // ---- Alle lokalen Daten löschen ----

    @Test fun deleteAllRemovesDataImagesAndCacheAndResetsRunningRepositories() = runBlocking {
        seed()
        File(context.cacheDir, "c.bin").writeBytes(ByteArray(10))
        val recipes = RecipeRepository(context)
        val meals = MealPlanRepository(context)
        val shopping = ShoppingListRepository(context)
        val inventory = InventoryRepository(context)
        val user = UserPreferencesRepository(context)
        assertEquals(setOf("r1"), recipes.favorites.value)
        assertEquals(1, meals.plannedMeals.value.size)
        assertEquals(1, shopping.items.value.size)
        assertEquals(1, shopping.covered.value.size)
        assertEquals(1, shopping.dismissedSuggestions.value.size)
        assertEquals(2, inventory.items.value.size)
        assertTrue(inventory.syncState.value.pending > 0)
        val hub = AppRepositories(recipes, meals, shopping, inventory, user)
        manager(hub).deleteAllLocalData()

        assertTrue(recipes.favorites.value.isEmpty())
        assertTrue(meals.plannedMeals.value.isEmpty())
        assertTrue(shopping.items.value.isEmpty())
        assertTrue(shopping.covered.value.isEmpty())
        assertTrue(shopping.dismissedSuggestions.value.isEmpty())
        assertTrue(inventory.items.value.isEmpty())
        assertEquals(0, inventory.syncState.value.pending)
        StorageLayout.dataPrefs.forEach { assertTrue("$it nicht leer", prefs(it).all.isEmpty()) }
        assertFalse(imagesDir.exists() && imagesDir.list()!!.isNotEmpty())
        assertFalse(File(context.cacheDir, "c.bin").exists())
        // Verbindungsdaten und Einstellungen bleiben.
        assertEquals(TOKEN, prefs(StorageLayout.PREFS_USER).getString("notion_token", null))
        assertEquals("db1", prefs(StorageLayout.PREFS_USER).getString("notion_db_id", null))
        // Neu gestartete Repositories sehen ebenfalls den leeren Zustand.
        assertTrue(InventoryRepository(context).items.value.isEmpty())
        assertTrue(MealPlanRepository(context).plannedMeals.value.isEmpty())
    }

    @Test fun deleteAllReportsPendingInventoryChangesFromTheRepository() {
        val hub = FakeHub(pending = 3)
        assertEquals(3, manager(hub).pendingInventoryChanges)
    }

    @Test fun failedDeleteRestoresTheData() {
        seed()
        val before = snapshot()
        val failing = FailingStore(store, failOn = 2)
        val e = assertThrows(BackupException::class.java) { runBlocking { manager(prefs = failing).deleteAllLocalData() } }
        assertTrue(e.message!!.contains("wiederhergestellt"))
        assertEquals(before, snapshot())
        assertTrue(File(imagesDir, "photo1.jpg").exists())
    }

    // ---- Android-Backup ----

    @Test fun autoBackupSwitchDefaultsToOnAndControlsTheAgent() {
        assertTrue(BackupSettings.isAutoBackupEnabled(context))
        assertTrue(YummifyBackupAgent.shouldBackUp(context))
        BackupSettings.setAutoBackupEnabled(context, false)
        assertFalse(YummifyBackupAgent.shouldBackUp(context))
        BackupSettings.setAutoBackupEnabled(context, true)
        assertTrue(YummifyBackupAgent.shouldBackUp(context))
    }

    @Test fun tokenKeyMatchesWhatTheRepositoryWrites() {
        UserPreferencesRepository(context).apply {
            saveNotionConfig("tok", "d", "i")
            saveHomeAssistant("http://homeassistant.local:8123", "ha-tok", "todo.bring")
        }
        assertEquals("ha-tok", prefs(StorageLayout.PREFS_USER).getString("home_assistant_token", null))
        assertTrue(StorageLayout.secretUserKeys.all { prefs(StorageLayout.PREFS_USER).contains(it) })
        assertEquals("tok", prefs(StorageLayout.PREFS_USER).getString("notion_token", null))
        assertTrue(StorageLayout.connectionUserKeys.all { prefs(StorageLayout.PREFS_USER).contains(it) })
    }
}
