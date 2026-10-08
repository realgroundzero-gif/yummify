package de.yummify.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import de.yummify.app.data.repository.RecipeRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class DropdownOption(val id: String, val name: String, val color: String? = null)

/** A select or multi-select column of a Notion database with its current options. */
data class DropdownField(val name: String, val type: String, val options: List<DropdownOption>)

data class DropdownSchema(val source: String, val fields: List<DropdownField>)

/**
 * Reads the dropdown columns of a Notion database and adds entries to them. Existing entries are always sent
 * along unchanged (Notion replaces the whole list), so nothing is renamed, recoloured or removed.
 */
class NotionOptionsApi(private val token: String, private val client: OkHttpClient = NotionHttp.client,
    private val baseUrl: String = NotionHttp.BASE_URL) {
    private val gson = Gson()

    private fun request(path: String, method: String = "GET", body: Any? = null): JsonObject {
        val builder = Request.Builder().url("$baseUrl/$path")
            .header("Authorization", "Bearer ${token.trim()}").header("Notion-Version", NotionHttp.VERSION)
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

    fun load(databaseId: String): DropdownSchema {
        val id = RecipeRepository.formatNotionId(databaseId)
        require(id.matches(Regex("[0-9a-fA-F-]{36}"))) { "Ungültige Datenbank-ID." }
        val sources = request("databases/$id").getAsJsonArray("data_sources") ?: throw IOException("Keine Notion-Datenquelle gefunden.")
        require(sources.size() == 1) { "Die Datenbank muss genau eine Datenquelle enthalten." }
        val source = sources[0].asJsonObject["id"].asString
        val schema = request("data_sources/$source").getAsJsonObject("properties") ?: throw IOException("Notion liefert kein Schema.")
        return DropdownSchema(source, fields(schema))
    }

    /** Adds [names] to the column [field]; returns the options after the change. */
    fun addOptions(source: String, field: String, names: List<String>): List<DropdownOption> {
        val current = fields(request("data_sources/$source").getAsJsonObject("properties") ?: throw IOException("Notion liefert kein Schema."))
            .firstOrNull { it.name == field } ?: throw IOException("Das Notion-Feld „$field“ gibt es nicht mehr.")
        val merged = mergedOptions(current.options, names)
        if (merged.size == current.options.size) return current.options
        val payload = mapOf("properties" to mapOf(field to mapOf(current.type to mapOf("options" to merged.map { option ->
            // Existing entries keep their id; new ones are sent by name only and Notion picks the colour.
            if (option.id.isNotBlank()) mapOf("id" to option.id, "name" to option.name) else mapOf("name" to option.name)
        }))))
        request("data_sources/$source", "PATCH", payload)
        // Read back: the ids and what Notion actually stored are the truth.
        return fields(request("data_sources/$source").getAsJsonObject("properties")).first { it.name == field }.options
    }

    companion object {
        fun fields(schema: JsonObject): List<DropdownField> = schema.entrySet().mapNotNull { (name, value) ->
            val column = value.asJsonObject
            val type = column["type"]?.asString
            if (type != "select" && type != "multi_select") return@mapNotNull null
            val options = (column.getAsJsonObject(type)?.getAsJsonArray("options")?.toList() ?: emptyList()).map { element ->
                val option = element.asJsonObject
                DropdownOption(option["id"]?.asString.orEmpty(), option["name"].asString, option["color"]?.takeUnless { c -> c.isJsonNull }?.asString)
            }
            DropdownField(name, type, options)
        }.sortedBy { it.name.lowercase() }

        /** Message for an unusable entry, or null. Notion rejects commas in entries and limits them to 100 characters. */
        fun validate(name: String): String? = when {
            name.isBlank() -> "Bitte einen Eintrag eingeben."
            name.contains(',') -> "Einträge dürfen kein Komma enthalten."
            name.trim().length > 100 -> "Ein Eintrag darf höchstens 100 Zeichen lang sein."
            else -> null
        }

        /** Existing entries first and unchanged, then the new ones; names that exist already (ignoring case) are skipped. */
        fun mergedOptions(existing: List<DropdownOption>, additions: List<String>): List<DropdownOption> {
            val result = existing.toMutableList()
            additions.map { it.trim() }.forEach { name ->
                require(validate(name) == null) { validate(name).orEmpty() }
                if (result.none { it.name.equals(name, ignoreCase = true) }) result += DropdownOption("", name)
            }
            return result
        }
    }
}
