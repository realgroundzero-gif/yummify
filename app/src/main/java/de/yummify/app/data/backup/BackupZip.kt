package de.yummify.app.data.backup

import com.google.gson.Gson
import com.google.gson.JsonParseException
import de.yummify.app.data.model.CoveredItem
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.model.normalized
import de.yummify.app.data.repository.normalized
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// Inhalt der JSON-Dateien. Alle Felder sind nullable, weil Gson Kotlin-Defaults ignoriert:
// ältere oder fremde Dateien ohne ein Feld liefern null und werden beim Einlesen normalisiert.
internal data class RecipesFile(val caches: Map<String, List<Recipe?>?>? = null, val syncTimes: Map<String, String>? = null, val favorites: List<String>? = null)
internal data class MealPlanFile(val meals: List<MealPlanItem?>? = null)
internal data class ShoppingFile(val items: List<ShoppingItem?>? = null, val covered: List<CoveredItem?>? = null, val dismissed: List<String>? = null)
internal data class InventoryFile(val databaseId: String? = null, val items: List<InventoryItem?>? = null, val purchases: List<String>? = null)
internal data class UserPrefsFile(val values: Map<String, PrefEntry>? = null)

/** Ergebnis des vollständigen Einlesens: nichts davon hat bisher Daten auf dem Gerät verändert. */
class ParsedBackup internal constructor(
    val manifest: BackupManifest,
    val data: BackupData,
    /** Bilddateien im Staging-Ordner, nach Dateiname. */
    val images: Map<String, File>,
    val stagingDir: File
) {
    val counts get() = BackupCounts.of(data, images.size)
    val createdAt: Instant get() = Instant.parse(manifest.createdAt)
}

/** Schreibt und liest das Export-Format: ZIP mit `manifest.json`, `data/…json` und `images/<Datei>`. */
object BackupZip {
    const val MANIFEST = "manifest.json"
    const val RECIPES = "data/recipes.json"
    const val MEAL_PLAN = "data/meal_plan.json"
    const val SHOPPING = "data/shopping_list.json"
    const val INVENTORY = "data/inventory.json"
    const val USER_PREFS = "data/user_prefs.json"
    const val IMAGES = "images/"

    private const val MAX_JSON_BYTES = 64L * 1024 * 1024
    private const val MAX_IMAGE_BYTES = 64L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 1024L * 1024 * 1024
    private const val MAX_ENTRIES = 20_000
    private val imageName = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,119}")
    private val gson = Gson()
    private val jsonFiles = setOf(MANIFEST, RECIPES, MEAL_PLAN, SHOPPING, INVENTORY, USER_PREFS)

    fun isValidImageName(name: String) = imageName.matches(name)

    fun write(out: OutputStream, manifest: BackupManifest, data: BackupData, images: Map<String, File>) {
        val zip = ZipOutputStream(out)
        fun json(name: String, value: Any) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(gson.toJson(value).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        json(MANIFEST, manifest)
        json(RECIPES, RecipesFile(data.recipeCaches, data.recipeSyncTimes, data.favorites.sorted()))
        json(MEAL_PLAN, MealPlanFile(data.meals))
        json(SHOPPING, ShoppingFile(data.shoppingItems, data.covered, data.dismissed.sorted()))
        json(INVENTORY, InventoryFile(data.inventoryDatabaseId, data.inventory, data.purchases.sorted()))
        json(USER_PREFS, UserPrefsFile(data.userPrefs.mapNotNull { (k, v) -> PrefCodec.encode(v)?.let { k to it } }.toMap()))
        images.toSortedMap().forEach { (name, file) ->
            require(isValidImageName(name)) { "Ungültiger Bilddateiname: $name" }
            zip.putNextEntry(ZipEntry(IMAGES + name))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        zip.finish()
        zip.flush()
    }

    /**
     * Liest und prüft die Datei vollständig. Bilder landen in [stagingDir] (wird neu angelegt).
     * Wirft [BackupException] mit deutscher Meldung; bei einem Fehler bleibt kein Staging-Ordner zurück.
     */
    fun read(input: InputStream, stagingDir: File): ParsedBackup {
        stagingDir.deleteRecursively()
        check(stagingDir.mkdirs() || stagingDir.isDirectory) { "Zwischenordner konnte nicht angelegt werden." }
        try {
            return readInto(input, stagingDir)
        } catch (e: BackupException) {
            stagingDir.deleteRecursively(); throw e
        } catch (e: ZipException) {
            stagingDir.deleteRecursively(); throw BackupException("Die Datei ist keine gültige ZIP-Datei oder beschädigt.", e)
        } catch (e: IOException) {
            stagingDir.deleteRecursively(); throw BackupException("Die Datei konnte nicht gelesen werden: ${e.message ?: "unbekannter Fehler"}", e)
        } catch (e: RuntimeException) {
            stagingDir.deleteRecursively(); throw e
        }
    }

    private fun readInto(input: InputStream, stagingDir: File): ParsedBackup {
        val zip = ZipInputStream(input)
        val texts = mutableMapOf<String, String>()
        val images = mutableMapOf<String, File>()
        var entries = 0
        var total = 0L
        while (true) {
            val entry = zip.nextEntry ?: break
            if (++entries > MAX_ENTRIES) throw BackupException("Die Datei enthält zu viele Einträge.")
            if (entry.isDirectory) continue
            val name = entry.name
            when {
                name in jsonFiles -> {
                    val bytes = readLimited(zip, MAX_JSON_BYTES, name)
                    total += bytes.size
                    texts[name] = String(bytes, Charsets.UTF_8)
                }
                name.startsWith(IMAGES) && isValidImageName(name.removePrefix(IMAGES)) -> {
                    val target = File(stagingDir, name.removePrefix(IMAGES))
                    var size = 0L
                    target.outputStream().use { out ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            size += n
                            if (size > MAX_IMAGE_BYTES) throw BackupException("Ein Bild in der Datei ist zu groß.")
                            out.write(buffer, 0, n)
                        }
                    }
                    total += size
                    images[name.removePrefix(IMAGES)] = target
                }
                else -> total += readLimited(zip, MAX_JSON_BYTES, name).size   // unbekannte Einträge werden ignoriert
            }
            if (total > MAX_TOTAL_BYTES) throw BackupException("Die Datei ist zu groß.")
        }
        if (entries == 0) throw BackupException("Die Datei ist keine gültige ZIP-Datei.")
        val manifest = parseManifest(texts[MANIFEST] ?: throw BackupException("Kein Yummify-Export: Das Manifest fehlt."))
        val data = BackupData(
            userPrefs = emptyMap()
        ).let { base ->
            val recipes = parse<RecipesFile>(texts[RECIPES], RECIPES)
            val meals = parse<MealPlanFile>(texts[MEAL_PLAN], MEAL_PLAN)
            val shopping = parse<ShoppingFile>(texts[SHOPPING], SHOPPING)
            val inventory = parse<InventoryFile>(texts[INVENTORY], INVENTORY)
            val prefs = parse<UserPrefsFile>(texts[USER_PREFS], USER_PREFS)
            base.copy(
                recipeCaches = recipes?.caches.orEmpty().mapValues { (_, list) -> list.orEmpty().filterNotNull().map { it.normalized() } },
                recipeSyncTimes = recipes?.syncTimes.orEmpty(),
                favorites = recipes?.favorites.orEmpty().toSet(),
                meals = meals?.meals.orEmpty().filterNotNull(),
                shoppingItems = shopping?.items.orEmpty().filterNotNull(),
                covered = shopping?.covered.orEmpty().filterNotNull(),
                dismissed = shopping?.dismissed.orEmpty().toSet(),
                inventoryDatabaseId = inventory?.databaseId.orEmpty(),
                inventory = inventory?.items.orEmpty().filterNotNull().map { it.normalized() },
                purchases = inventory?.purchases.orEmpty().toSet(),
                userPrefs = prefs?.values.orEmpty().mapValues { (key, entry) ->
                    PrefCodec.decode(entry) ?: throw BackupException("Die Einstellungen in der Datei sind unlesbar (Eintrag \"$key\").")
                }
            )
        }
        val counts = BackupCounts.of(data, images.size)
        manifest.elementCounts?.let { declared ->
            if (declared != counts.toMap()) throw BackupException("Die Datei ist unvollständig: Die Anzahl der Einträge passt nicht zum Manifest.")
        }
        if (manifest.includesToken == false && data.userPrefs.keys.any { it in StorageLayout.secretUserKeys }) {
            throw BackupException("Die Datei ist inkonsistent: Sie enthält einen Zugangsschlüssel, obwohl das Manifest keinen angibt.")
        }
        return ParsedBackup(manifest, data, images, stagingDir)
    }

    private fun readLimited(zip: ZipInputStream, limit: Long, name: String): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var size = 0L
        while (true) {
            val n = zip.read(buffer)
            if (n < 0) break
            size += n
            if (size > limit) throw BackupException("Der Eintrag \"$name\" in der Datei ist zu groß.")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private fun parseManifest(text: String): BackupManifest {
        val manifest = try {
            gson.fromJson(text, BackupManifest::class.java)
        } catch (e: JsonParseException) {
            throw BackupException("Kein Yummify-Export: Das Manifest ist nicht lesbar.", e)
        } ?: throw BackupException("Kein Yummify-Export: Das Manifest ist leer.")
        if (manifest.format != BackupManifest.FORMAT) throw BackupException("Kein Yummify-Export: Die Datei hat ein anderes Format.")
        val version = manifest.formatVersion ?: throw BackupException("Kein Yummify-Export: Die Formatversion fehlt.")
        if (version < 1) throw BackupException("Kein Yummify-Export: Ungültige Formatversion.")
        if (version > BackupManifest.CURRENT_VERSION) {
            throw BackupException("Die Datei stammt aus einer neueren Version von Yummify (Format $version) und kann nicht gelesen werden. Bitte die App aktualisieren.")
        }
        if (manifest.createdAt == null || runCatching { Instant.parse(manifest.createdAt) }.isFailure) {
            throw BackupException("Kein Yummify-Export: Der Zeitstempel im Manifest fehlt oder ist ungültig.")
        }
        return manifest
    }

    private inline fun <reified T> parse(text: String?, name: String): T? {
        if (text == null) return null   // fehlende Datei = leerer Bereich (ältere Exporte)
        return try {
            gson.fromJson(text, T::class.java)
        } catch (e: JsonParseException) {
            throw BackupException("Die Datei ist beschädigt: \"$name\" ist nicht lesbar.", e)
        }
    }
}
