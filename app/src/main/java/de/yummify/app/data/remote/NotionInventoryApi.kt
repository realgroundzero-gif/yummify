package de.yummify.app.data.remote

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.repository.RecipeRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Version-pinned API: database containers are resolved to their data source. */
class NotionInventoryApi(private val token: String, private val client: OkHttpClient = OkHttpClient.Builder()
    .retryOnConnectionFailure(false).connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build(),
    private val baseUrl: String = "https://api.notion.com/v1") {
    private val mappings = mutableMapOf<String, Map<String, Field>>()
    private val gson = GsonBuilder().serializeNulls().create()
    private fun request(path: String, method: String = "GET", body: Any? = null): JsonObject {
        val builder = Request.Builder().url("$baseUrl/$path")
            .header("Authorization", "Bearer ${token.trim()}")
            .header("Notion-Version", "2025-09-03")
        if (method != "GET") builder.method(method, gson.toJson(body ?: emptyMap<String, Any>()).toRequestBody("application/json".toMediaType()))
        return client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string() ?: throw IOException("Notion liefert keine Antwort.")
            if (!response.isSuccessful) {
                val detail = runCatching { gson.fromJson(raw, JsonObject::class.java).get("message")?.asString }.getOrNull().orEmpty()
                throw IOException("Notion ${response.code}: ${detail.take(250)}")
            }
            gson.fromJson(raw, JsonObject::class.java)
        }
    }
    fun source(databaseId: String): String {
        val id = RecipeRepository.formatNotionId(databaseId)
        require(id.matches(Regex("[0-9a-fA-F-]{36}"))) { "Ungültige Inventar-Datenbank-ID." }
        val database = request("databases/$id")
        val sources = database.getAsJsonArray("data_sources") ?: throw IOException("Keine Notion-Datenquelle gefunden.")
        require(sources.size() == 1) { "Die Inventar-Datenbank muss genau eine Datenquelle enthalten." }
        val source = sources[0].asJsonObject["id"].asString
        val schema = request("data_sources/$source").getAsJsonObject("properties")
            ?: throw IOException("Notion liefert kein Inventar-Schema.")
        val mapping = resolveSchema(schema)
        // Add only missing app metadata; never rename or convert the user's columns.
        val missing = schemaTypes.filterKeys { it !in mapping }
        if (missing.isNotEmpty()) {
            request("data_sources/$source", "PATCH", mapOf("properties" to
                missing.mapValues { (_, type) -> mapOf(type to emptyMap<String, Any>()) }))
        }
        mappings[source] = mapping + missing.mapValues { (name, type) -> Field(name, type) }
        return source
    }
    fun all(source: String, filter: Any? = null): List<InventoryItem> {
        val items = mutableListOf<InventoryItem>()
        var cursor: String? = null
        do {
            val body = mutableMapOf<String, Any>("page_size" to 100)
            cursor?.let { body["start_cursor"] = it }; filter?.let { body["filter"] = it }
            val result = request("data_sources/$source/query", "POST", body)
            result.getAsJsonArray("results").forEach { items.add(decode(it.asJsonObject, mappings[source] ?: legacyMapping)) }
            cursor = if (result["has_more"]?.asBoolean == true) result["next_cursor"].asString else null
        } while (cursor != null)
        return items
    }
    fun save(source: String, item: InventoryItem): String {
        // Stable client ID recovers a committed create after a lost HTTP response.
        val id = item.pageId ?: all(source, mapOf("property" to "Artikel-ID", "rich_text" to mapOf("equals" to item.id))).firstOrNull()?.pageId
        val properties = properties(item, mappings[source] ?: legacyMapping)
        val response = if (id != null) request("pages/$id", "PATCH", mapOf("properties" to properties))
        else request("pages", "POST", mapOf("parent" to mapOf("type" to "data_source_id", "data_source_id" to source), "properties" to properties))
        return response["id"].asString
    }
    fun delete(source: String, item: InventoryItem) {
        val pageId = item.pageId ?: all(source, mapOf("property" to "Artikel-ID", "rich_text" to mapOf("equals" to item.id))).firstOrNull()?.pageId
        pageId?.let { request("pages/$it", "PATCH", mapOf("in_trash" to true)) }
    }
    fun createDatabase(recipeDatabase: String): String {
        val db = request("databases/${RecipeRepository.formatNotionId(recipeDatabase)}")
        val parent = db.getAsJsonObject("parent")
        val pageId = parent.get("page_id")?.takeUnless { it.isJsonNull }?.asString
            ?: throw IOException("Die Rezept-Datenbank muss unter einer für die Integration freigegebenen Notion-Seite liegen.")
        // Recover an existing database from a previous attempt before creating a new one.
        var cursor: String? = null
        do {
            val suffix = cursor?.let { "&start_cursor=$it" }.orEmpty()
            val children = request("blocks/$pageId/children?page_size=100$suffix")
            children.getAsJsonArray("results").forEach { block ->
                val obj = block.asJsonObject
                if (obj["type"]?.asString == "child_database" && obj.getAsJsonObject("child_database")["title"]?.asString == "Yummify Inventar") {
                    val existing = obj["id"].asString
                    source(existing)
                    return existing
                }
            }
            cursor = if (children["has_more"]?.asBoolean == true) children["next_cursor"].asString else null
        } while (cursor != null)
        val schema = (screenshotSchemaTypes + schemaTypes.filterKeys { it in setOf("Mindestbestand", "Barcode", "Notizen", "Artikel-ID") }).mapValues { (_, type) -> mapOf(type to emptyMap<String, Any>()) }
        return request("databases", "POST", mapOf(
            "parent" to mapOf("type" to "page_id", "page_id" to pageId),
            "title" to rich("Yummify Inventar"),
            "initial_data_source" to mapOf("properties" to schema)
        ))["id"].asString
    }
    companion object {
        val schemaTypes = linkedMapOf("Name" to "title", "Menge" to "number", "Einheit" to "rich_text", "Kategorie" to "rich_text", "Lagerort" to "rich_text", "Mindestbestand" to "number", "Ablaufdatum" to "date", "Barcode" to "rich_text", "Notizen" to "rich_text", "Artikel-ID" to "rich_text")
        fun rich(text: String) = if (text.isEmpty()) emptyList() else listOf(mapOf("text" to mapOf("content" to text)))
        data class Field(val name: String, val type: String)
        val legacyMapping = schemaTypes.mapValues { (name, type) -> Field(name, type) }
        val screenshotSchemaTypes = linkedMapOf("Artikel" to "title", "Bestand" to "number",
            "nächstes MHD" to "date", "Kategorie" to "multi_select", "Einheit" to "select", "Lagerort" to "select")
        fun resolveSchema(schema: JsonObject): Map<String, Field> {
            val aliases = mapOf("Name" to listOf("Artikel", "Name"), "Menge" to listOf("Bestand", "Menge"),
                "Ablaufdatum" to listOf("nächstes MHD", "Ablaufdatum"))
            val optional = setOf("Mindestbestand", "Barcode", "Notizen", "Artikel-ID")
            return schemaTypes.mapNotNull { (key, expected) ->
                val name = (aliases[key] ?: listOf(key)).firstOrNull { schema.has(it) }
                if (name == null) {
                    require(key in optional) { "Notion-Feld '${aliases[key]?.first() ?: key}' fehlt." }
                    null
                } else {
                    val type = schema.getAsJsonObject(name)["type"]?.asString
                    val allowed = if (key in setOf("Einheit", "Kategorie", "Lagerort")) setOf("rich_text", "select", "multi_select") else setOf(expected)
                    require(type in allowed) { "Notion-Feld '$name' muss ${allowed.joinToString(" / ")} sein." }
                    key to Field(name, type!!)
                }
            }.toMap()
        }
        fun properties(item: InventoryItem, mapping: Map<String, Field> = legacyMapping): Map<String, Any> {
            val texts = mapOf("Name" to item.name, "Einheit" to item.unit, "Kategorie" to item.category,
                "Lagerort" to item.location, "Barcode" to item.barcode, "Notizen" to item.notes, "Artikel-ID" to item.id)
            return mapping.map { (key, field) ->
                val text = texts[key].orEmpty()
                val value: Any? = when (field.type) {
                    "title", "rich_text" -> rich(text)
                    "number" -> if (key == "Menge") item.quantity else item.minimum
                    "date" -> item.expiry?.let { mapOf("start" to it) }
                    "select" -> text.takeIf { it.isNotBlank() }?.let { mapOf("name" to it) }
                    "multi_select" -> {
                        val names = if (key == "Kategorie" && item.categoryOptions.isNotEmpty() &&
                            item.category == item.categoryOptions.joinToString(", ")) item.categoryOptions
                            else listOf(text).filter { it.isNotBlank() }
                        names.map { mapOf("name" to it) }
                    }
                    else -> error("Nicht unterstütztes Notion-Feld: ${field.type}")
                }
                field.name to mapOf(field.type to value)
            }.toMap()
        }
        fun decode(page: JsonObject, mapping: Map<String, Field> = legacyMapping): InventoryItem {
            val p = page.getAsJsonObject("properties")
            fun property(key: String) = mapping[key]?.let { p.getAsJsonObject(it.name) }
            fun options(key: String) = property(key)?.getAsJsonArray("multi_select")?.map { it.asJsonObject["name"].asString }.orEmpty()
            fun text(key: String): String {
                val obj = property(key) ?: return ""
                return when (mapping[key]?.type) {
                    "select" -> obj.get("select")?.takeUnless { it.isJsonNull }?.asJsonObject?.get("name")?.asString.orEmpty()
                    "multi_select" -> options(key).joinToString(", ")
                    else -> obj.getAsJsonArray(mapping[key]?.type ?: "rich_text")?.joinToString("") {
                        val rich = it.asJsonObject
                        rich["plain_text"]?.asString ?: rich.getAsJsonObject("text")?.get("content")?.asString.orEmpty()
                    }.orEmpty()
                }
            }
            fun number(key: String) = property(key)?.get("number")?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0
            val id = page["id"].asString
            return InventoryItem(id = text("Artikel-ID").ifBlank { id }, pageId = id, name = text("Name"), quantity = number("Menge"),
                unit = text("Einheit"), category = text("Kategorie"), location = text("Lagerort"), categoryOptions = options("Kategorie"),
                minimum = number("Mindestbestand"), expiry = property("Ablaufdatum")?.get("date")?.takeUnless { it.isJsonNull }?.asJsonObject?.get("start")?.asString?.take(10),
                barcode = text("Barcode"), notes = text("Notizen"), dirty = false)
        }
    }
}
