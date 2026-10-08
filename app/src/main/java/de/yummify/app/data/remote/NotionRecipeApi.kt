package de.yummify.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.yummify.app.data.model.RecipeDraft
import de.yummify.app.data.repository.RecipeRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** Creates a complete recipe in one page request after checking the existing schema. */
class NotionRecipeApi(private val token: String,
    private val client: OkHttpClient = NotionHttp.client,
    private val baseUrl: String = NotionHttp.BASE_URL) {
    private val gson = Gson()
    private fun request(path: String, body: Any? = null): JsonObject {
        val request = Request.Builder().url("$baseUrl/$path")
            .header("Authorization", "Bearer ${token.trim()}").header("Notion-Version", NotionHttp.VERSION)
        if (body != null) request.post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
        return client.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Notion konnte das Rezept nicht speichern (${response.code}). Bitte Verbindung und Integrationsrechte prüfen.")
            JsonParser.parseString(response.body?.string() ?: throw IOException("Notion liefert keine Antwort.")).asJsonObject
        }
    }
    fun create(databaseId: String, draft: RecipeDraft): String {
        require(draft.validate() == null) { draft.validate().orEmpty() }
        require(token.isNotBlank() && databaseId.isNotBlank()) { "Bitte zuerst die Notion-Rezeptverbindung in den Einstellungen einrichten." }
        val id = RecipeRepository.formatNotionId(databaseId)
        require(id.matches(Regex("[0-9a-fA-F-]{36}"))) { "Bitte eine gültige Rezept-Datenbank-ID in den Einstellungen hinterlegen." }
        val sources = request("databases/$id").getAsJsonArray("data_sources")
        require(sources != null && sources.size() == 1) { "Die Rezept-Datenbank muss genau eine Datenquelle enthalten." }
        val source = sources[0].asJsonObject["id"].asString
        val schema = request("data_sources/$source").getAsJsonObject("properties")
            ?: throw IOException("Notion liefert kein Rezept-Schema.")
        val payload = payload(source, schema, draft)
        return request("pages", payload)["id"]?.asString ?: throw IOException("Notion hat keine Rezept-ID bestätigt. Bitte vor einem erneuten Speichern die Datenbank prüfen.")
    }
    companion object {
        private fun rich(text: String) = text.chunked(2000).map { mapOf("type" to "text", "text" to mapOf("content" to it)) }
        fun payload(source: String, schema: JsonObject, draft: RecipeDraft): Map<String, Any> {
            val title = schema.entrySet().firstOrNull { it.value.asJsonObject["type"]?.asString == "title" }?.key
                ?: throw IllegalArgumentException("Die Rezept-Datenbank benötigt ein Titelfeld.")
            val properties = mutableMapOf<String, Any>(title to mapOf("title" to rich(draft.title.trim())))
            fun field(names: List<String>, value: Any, allowed: Set<String>) {
                val name = names.firstOrNull { schema.has(it) }
                    ?: throw IllegalArgumentException("Notion-Feld „${names.first()}“ fehlt in der Rezept-Datenbank.")
                val type = schema.getAsJsonObject(name)["type"]?.asString
                require(type in allowed) { "Notion-Feld „$name“ muss ${allowed.joinToString(" / ")} sein." }
                val data = when (type) {
                    "rich_text" -> rich(value.toString())
                    "number" -> value
                    "select" -> mapOf("name" to value)
                    "multi_select" -> listOf(mapOf("name" to value))
                    else -> error("Nicht unterstütztes Rezeptfeld.")
                }
                properties[name] = mapOf(type to data)
            }
            field(listOf("Portionen"), draft.servings, setOf("number"))
            field(listOf("Zutaten", "Lebensmittel"), draft.ingredients.trim(), setOf("rich_text"))
            if (draft.description.isNotBlank()) field(listOf("Beschreibung"), draft.description.trim(), setOf("rich_text"))
            if (draft.category.isNotBlank()) field(listOf("Kategorie"), draft.category.trim(), setOf("select", "multi_select", "rich_text"))
            val body = mutableMapOf<String, Any>("parent" to mapOf("type" to "data_source_id", "data_source_id" to source), "properties" to properties,
                "children" to draft.steps.map { mapOf("object" to "block", "type" to "numbered_list_item", "numbered_list_item" to mapOf("rich_text" to rich(it))) })
            if (draft.imageUrl.isNotBlank()) body["cover"] = mapOf("type" to "external", "external" to mapOf("url" to draft.imageUrl.trim()))
            return body
        }
    }
}
