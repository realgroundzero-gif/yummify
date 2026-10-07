package de.yummify.app.data.remote

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.repository.RecipeRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDate

/**
 * Reads recipes through data sources (Notion-Version 2025-09-03). Failures throw instead of
 * returning partial or empty lists, so callers can tell "no recipes" from "Notion not reachable".
 */
class NotionRecipeReader(
    private val token: String,
    databaseId: String,
    private val client: OkHttpClient = NotionHttp.client,
    private val baseUrl: String = NotionHttp.BASE_URL
) {
    private val databaseId = RecipeRepository.formatNotionId(databaseId)
    // Nulls must be sent: {"date": null} is how Notion clears a date.
    private val gson = GsonBuilder().serializeNulls().create()
    private var sources: List<String>? = null
    private var schema: JsonObject? = null

    private fun request(path: String, method: String = "GET", body: Any? = null): JsonObject {
        val builder = Request.Builder().url("$baseUrl/$path")
            .header("Authorization", "Bearer ${token.trim()}")
            .header("Notion-Version", NotionHttp.VERSION)
        if (method != "GET") builder.method(method, gson.toJson(body ?: emptyMap<String, Any>()).toRequestBody("application/json".toMediaType()))
        return client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching { JsonParser.parseString(raw).asJsonObject["message"]?.asString }.getOrNull().orEmpty()
                throw IOException(when (response.code) {
                    401 -> "Notion lehnt den Token ab (401). Bitte den Integration-Token prüfen."
                    403, 404 -> "Notion findet die Rezept-Datenbank nicht (${response.code}). Ist sie für die Integration freigegeben?"
                    else -> "Notion ${response.code}: ${detail.take(200)}".trimEnd(' ', ':')
                })
            }
            runCatching { JsonParser.parseString(raw).asJsonObject }.getOrElse { throw IOException("Notion liefert eine ungültige Antwort.") }
        }
    }

    private fun sources(): List<String> = sources ?: run {
        require(databaseId.matches(Regex("[0-9a-fA-F-]{36}"))) { "Bitte eine gültige Rezept-Datenbank-ID in den Einstellungen hinterlegen." }
        val list = request("databases/$databaseId").getAsJsonArray("data_sources")
            ?.map { it.asJsonObject["id"].asString }.orEmpty()
        if (list.isEmpty()) throw IOException("Keine Notion-Datenquelle für die Rezept-Datenbank gefunden.")
        list.also { sources = it }
    }

    private fun schema(): JsonObject = schema ?: (request("data_sources/${sources().first()}").getAsJsonObject("properties") ?: JsonObject()).also { schema = it }

    fun fetchAll(): List<Recipe> = sources().flatMap { source ->
        val pages = mutableListOf<JsonObject>()
        var cursor: String? = null
        do {
            val body = mutableMapOf<String, Any>("page_size" to 100)
            cursor?.let { body["start_cursor"] = it }
            val result = request("data_sources/$source/query", "POST", body)
            result.getAsJsonArray("results")?.forEach { pages += it.asJsonObject }
            cursor = if (result["has_more"]?.asBoolean == true) result.string("next_cursor") else null
        } while (cursor != null)
        pages.mapNotNull { toRecipe(it) }
    }

    /** Page properties plus the full page body as Markdown-like lines. */
    fun fetchRecipe(pageId: String): Recipe {
        val recipe = toRecipe(request("pages/$pageId")) ?: throw IOException("Das Rezept hat keinen Titel.")
        val body = fetchBlocks(pageId)
        return if (body.isNotEmpty()) recipe.copy(instructions = body) else recipe
    }

    private fun fetchBlocks(pageId: String): List<String> {
        val lines = mutableListOf<String>()
        var cursor: String? = null
        do {
            val result = request("blocks/$pageId/children?page_size=100" + cursor?.let { "&start_cursor=$it" }.orEmpty())
            result.getAsJsonArray("results")?.forEach { block -> blockToMarkdown(block.asJsonObject)?.let { lines += it } }
            cursor = if (result["has_more"]?.asBoolean == true) result.string("next_cursor") else null
        } while (cursor != null)
        return lines
    }

    /** Writes the date only when the database has a date column "Geplant am". Returns false when it does not. */
    fun updatePlannedDate(pageId: String, date: LocalDate?): Boolean {
        if (schema().getAsJsonObject(PLANNED_DATE)?.string("type") != "date") return false
        request("pages/$pageId", "PATCH", mapOf("properties" to mapOf(PLANNED_DATE to mapOf("date" to date?.let { mapOf("start" to it.toString()) }))))
        return true
    }

    fun updateRating(pageId: String, rating: Int) {
        val value = rating.coerceIn(1, 5)
        val payload: Any = when (schema().getAsJsonObject(RATING)?.string("type")) {
            "select" -> mapOf("select" to mapOf("name" to "★".repeat(value)))
            "number" -> mapOf("number" to value)
            null -> throw IllegalArgumentException("Die Rezept-Datenbank hat kein Feld „$RATING“.")
            else -> throw IllegalArgumentException("Das Feld „$RATING“ muss Auswahl oder Zahl sein.")
        }
        request("pages/$pageId", "PATCH", mapOf("properties" to mapOf(RATING to payload)))
    }

    fun testConnection() { sources(); schema() }

    companion object {
        const val PLANNED_DATE = "Geplant am"
        const val RATING = "Bewertung"

        private fun JsonObject.string(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.takeIf { it.isJsonPrimitive }?.asString
        private fun JsonObject.obj(key: String): JsonObject? = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
        private fun JsonObject.array(key: String): JsonArray? = get(key)?.takeIf { it.isJsonArray }?.asJsonArray

        private fun plain(array: JsonArray?): String = array?.joinToString("") { it.asJsonObject.string("plain_text").orEmpty() }.orEmpty()

        /** Text of a property whatever its type: title, rich_text, select, multi_select, number, url or formula. */
        internal fun text(property: JsonObject?): String? {
            property ?: return null
            val value = when (property.string("type")) {
                "title" -> plain(property.array("title"))
                "rich_text" -> plain(property.array("rich_text"))
                "select", "status" -> property.obj(property.string("type")!!)?.string("name")
                "multi_select" -> property.array("multi_select")?.joinToString(", ") { it.asJsonObject.string("name").orEmpty() }
                "number" -> property.get("number")?.takeUnless { it.isJsonNull }?.asDouble?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
                "url" -> property.string("url")
                "formula" -> property.obj("formula")?.let { it.string("string") ?: it.string("number") }
                else -> null
            }
            return value?.trim()?.takeIf { it.isNotEmpty() }
        }

        private fun names(property: JsonObject?): List<String> = when (property?.string("type")) {
            "select" -> listOfNotNull(property.obj("select")?.string("name"))
            "multi_select" -> property.array("multi_select")?.mapNotNull { it.asJsonObject.string("name") }.orEmpty()
            "rich_text" -> listOfNotNull(text(property))
            else -> emptyList()
        }.map { it.trim() }.filter { it.isNotEmpty() }

        /** First number in a number or text column, e.g. "30 Min." → 30. */
        internal fun number(property: JsonObject?): Double? {
            property ?: return null
            if (property.string("type") == "number") return property.get("number")?.takeUnless { it.isJsonNull }?.asDouble
            return text(property)?.let { Regex("""\d+(?:[.,]\d+)?""").find(it)?.value?.replace(',', '.')?.toDoubleOrNull() }
        }

        internal fun toRecipe(page: JsonObject): Recipe? {
            val properties = page.obj("properties") ?: return null
            fun prop(vararg names: String) = names.firstNotNullOfOrNull { properties.obj(it) }
            val title = properties.entrySet().firstOrNull { it.value.asJsonObject.string("type") == "title" }
                ?.let { text(it.value.asJsonObject) } ?: return null
            val id = page.string("id") ?: return null

            val cover = page.obj("cover")?.let { cover -> cover.string("type")?.let { cover.obj(it)?.string("url") } }
            val tags = (names(prop("Kategorie")) + names(prop("Küche")) + names(prop("Tags"))).distinct()
            val ingredientText = text(prop("Zutaten")) ?: text(prop("Lebensmittel")) ?: ""
            val instructionText = text(prop("Rezepterstellung")) ?: ""
            val ratingProperty = prop("Bewertung")
            val ratingText = text(ratingProperty).orEmpty()
            val score = if (ratingText.contains('★')) ratingText.count { it == '★' }.toDouble() else number(ratingProperty) ?: 0.0

            return Recipe(
                id = id,
                title = title,
                description = text(prop("Beschreibung")) ?: "",
                imageUrl = cover ?: text(prop("Bild")) ?: "",
                cookTimeMinutes = number(prop("Zeit", "Zubereitungszeit", "Kochzeit", "Dauer"))?.toInt(),
                calories = number(prop("Kalorien", "kcal"))?.toInt(),
                proteinGrams = number(prop("Protein"))?.toInt(),
                carbsGrams = number(prop("Kohlenhydrate"))?.toInt(),
                fatGrams = number(prop("Fett"))?.toInt(),
                category = tags.firstOrNull()?.lowercase() ?: "all",
                tags = tags,
                score = score,
                ingredients = RecipeRepository.parseIngredients(ingredientText),
                instructions = instructionText.lines().map { it.trim() }.filter { it.isNotEmpty() },
                difficulty = text(prop("Schwierigkeit", "Aufwand")),
                estimatedCost = text(prop("Kosten", "Preis")),
                defaultServings = number(prop("Portionen", "Portion"))?.toInt()?.takeIf { it > 0 } ?: 2,
                notionPageId = id,
                notionUrl = page.string("url") ?: text(prop("URL"))
            )
        }

        private fun richMarkdown(array: JsonArray?): String = array?.joinToString("") { element ->
            val item = element.asJsonObject
            var text = item.string("plain_text").orEmpty()
            val ann = item.obj("annotations")
            if (ann != null && text.isNotBlank()) {
                if (ann.get("bold")?.asBoolean == true) text = "**$text**"
                if (ann.get("italic")?.asBoolean == true) text = "*$text*"
                if (ann.get("code")?.asBoolean == true) text = "`$text`"
                if (ann.get("strikethrough")?.asBoolean == true) text = "~~$text~~"
            }
            text
        }.orEmpty()

        internal fun blockToMarkdown(block: JsonObject): String? {
            val type = block.string("type") ?: return null
            val content: JsonElement = block.get(type) ?: return null
            if (!content.isJsonObject) return null
            val text = richMarkdown(content.asJsonObject.array("rich_text")).takeIf { it.isNotBlank() } ?: return null
            return when (type) {
                "heading_1" -> "# $text"
                "heading_2" -> "## $text"
                "heading_3" -> "### $text"
                "bulleted_list_item" -> "• $text"
                "numbered_list_item", "paragraph" -> text
                "to_do" -> "${if (content.asJsonObject.get("checked")?.asBoolean == true) "[x]" else "[ ]"} $text"
                "quote" -> "> $text"
                "callout" -> "💡 $text"
                else -> null
            }
        }
    }
}
