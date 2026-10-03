package de.yummify.app.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import de.yummify.app.data.local.SampleData
import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.ShoppingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Repository for all data operations. Uses Notion API when configured,
 * otherwise falls back to built-in offline sample data.
 */
class RecipeRepository(
    private val notionToken: String = "",
    private val recipeDatabaseId: String = ""
) {
    private val gson = Gson()
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private val formattedDbId: String
        get() = formatNotionId(recipeDatabaseId)

    val isNotionConfigured: Boolean
        get() = notionToken.isNotBlank() && formattedDbId.isNotBlank()

    // ─────── Recipes ──────────────────────────────────────────────────────────

    suspend fun getAllRecipes(): List<Recipe> {
        if (!isNotionConfigured) return SampleData.recipes
        return try {
            // Always run blocking OkHttp calls on IO dispatcher
            withContext(Dispatchers.IO) {
                fetchRecipesFromNotion()
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Notion fetch failed", e)
            emptyList()
        }
    }

    suspend fun getRecipeById(id: String): Recipe? {
        if (!isNotionConfigured) return SampleData.recipes.find { it.id == id }
        return try {
            withContext(Dispatchers.IO) {
                fetchSingleRecipeFromNotion(id)
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Failed to fetch single recipe $id", e)
            null
        }
    }

    private fun fetchSingleRecipeFromNotion(pageId: String): Recipe? {
        val request = Request.Builder()
            .url("https://api.notion.com/v1/pages/$pageId")
            .addHeader("Authorization", "Bearer ${notionToken.trim()}")
            .addHeader("Notion-Version", "2022-06-28")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("RecipeRepository", "Single page fetch failed ${response.code}")
                    return null
                }
                val json = response.body?.string() ?: return null
                val page = gson.fromJson(json, NotionPage::class.java)
                val recipe = page.toRecipe() ?: return null

                // Fetch page body blocks (instructions) preserving Markdown
                val pageBlocks = fetchPageBlocksAsMarkdown(pageId)
                if (pageBlocks.isNotEmpty()) {
                    recipe.copy(instructions = pageBlocks)
                } else {
                    recipe
                }
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error fetching single recipe $pageId", e)
            null
        }
    }

    private fun fetchPageBlocksAsMarkdown(pageId: String): List<String> {
        val request = Request.Builder()
            .url("https://api.notion.com/v1/blocks/$pageId/children?page_size=100")
            .addHeader("Authorization", "Bearer ${notionToken.trim()}")
            .addHeader("Notion-Version", "2022-06-28")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("RecipeRepository", "Blocks query failed ${response.code}")
                    return emptyList()
                }
                val json = response.body?.string() ?: return emptyList()
                val blockResponse = gson.fromJson(json, NotionBlockListResponse::class.java)
                blockResponse.results.mapNotNull { block -> block.toMarkdown() }
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Failed to fetch page blocks for $pageId", e)
            emptyList()
        }
    }

    suspend fun searchRecipes(query: String, category: String? = null): List<Recipe> {
        val all = getAllRecipes()
        return all.filter { recipe ->
            val matchesQuery = query.isBlank() ||
                    recipe.title.contains(query, ignoreCase = true) ||
                    recipe.description.contains(query, ignoreCase = true) ||
                    recipe.tags.any { it.contains(query, ignoreCase = true) } ||
                    recipe.ingredients.any { it.name.contains(query, ignoreCase = true) }
            val matchesCategory = category == null || category == "all" ||
                    recipe.category == category
            matchesQuery && matchesCategory
        }
    }

    // ─────── Meal Plan ────────────────────────────────────────────────────────

    suspend fun getMealPlan(): List<MealPlanItem> {
        if (isNotionConfigured) return emptyList()
        return SampleData.mealPlanItems
    }

    // ─────── Shopping List ────────────────────────────────────────────────────

    suspend fun getShoppingItems(): List<ShoppingItem> {
        if (isNotionConfigured) return emptyList()
        return SampleData.shoppingItems
    }

    // ─────── Notion API ───────────────────────────────────────────────────────

    private fun fetchRecipesFromNotion(): List<Recipe> {
        val dbId = formattedDbId
        if (dbId.isBlank()) return emptyList()

        val allPages = mutableListOf<NotionPage>()
        var startCursor: String? = null

        do {
            val bodyMap = mutableMapOf<String, Any>("page_size" to 100)
            startCursor?.let { bodyMap["start_cursor"] = it }
            val body = gson.toJson(bodyMap)

            val request = Request.Builder()
                .url("https://api.notion.com/v1/databases/$dbId/query")
                .addHeader("Authorization", "Bearer ${notionToken.trim()}")
                .addHeader("Notion-Version", "2022-06-28")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            try {
                val notionResponse = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "no body"
                        Log.e("RecipeRepository", "Notion query failed ${response.code}: $errorBody")
                        null
                    } else {
                        val json = response.body?.string()
                        if (json != null) {
                            gson.fromJson(json, NotionQueryResponse::class.java)
                        } else null
                    }
                }

                if (notionResponse == null) {
                    break
                }

                allPages.addAll(notionResponse.results)

                if (notionResponse.hasMore && !notionResponse.nextCursor.isNullOrEmpty()) {
                    startCursor = notionResponse.nextCursor
                } else {
                    startCursor = null
                }
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error fetching Notion recipes", e)
                break
            }
        } while (startCursor != null)

        val recipes = allPages.mapNotNull { page -> page.toRecipe() }
        Log.d("RecipeRepository", "Fetched ${recipes.size} recipes from Notion (${allPages.size} total pages)")
        return recipes
    }

    fun testNotionConnection(token: String, databaseId: String): Boolean {
        val dbId = formatNotionId(databaseId)
        if (token.isBlank() || dbId.isBlank()) return false
        return try {
            val request = Request.Builder()
                .url("https://api.notion.com/v1/databases/$dbId")
                .addHeader("Authorization", "Bearer ${token.trim()}")
                .addHeader("Notion-Version", "2022-06-28")
                .get()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updatePlannedDate(pageId: String, date: java.time.LocalDate?): Boolean {
        if (!isNotionConfigured) return false
        return try {
            withContext(Dispatchers.IO) {
                val dateVal = if (date != null) {
                    """{"start":"$date"}"""
                } else {
                    "null"
                }
                val body = """{"properties":{"Geplant am":{"date":$dateVal}}}"""
                val request = Request.Builder()
                    .url("https://api.notion.com/v1/pages/$pageId")
                    .addHeader("Authorization", "Bearer ${notionToken.trim()}")
                    .addHeader("Notion-Version", "2022-06-28")
                    .addHeader("Content-Type", "application/json")
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.d("RecipeRepository", "updatePlannedDate status ${response.code} for page $pageId date=$date")
                    response.isSuccessful
                }
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Failed to update planned date in Notion for $pageId", e)
            false
        }
    }

    suspend fun updateRating(pageId: String, rating: Int): Boolean {
        if (!isNotionConfigured) return false
        return try {
            withContext(Dispatchers.IO) {
                // Notion Bewertung uses a SELECT with star emoji options: ★, ★★, ★★★, ★★★★, ★★★★★
                val stars = "★".repeat(rating.coerceIn(1, 5))
                val body = """{"properties":{"Bewertung":{"select":{"name":"$stars"}}}}"""
                val request = Request.Builder()
                    .url("https://api.notion.com/v1/pages/$pageId")
                    .addHeader("Authorization", "Bearer ${notionToken.trim()}")
                    .addHeader("Notion-Version", "2022-06-28")
                    .addHeader("Content-Type", "application/json")
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.d("RecipeRepository", "updateRating select status ${response.code} for page $pageId stars=$stars")
                    if (!response.isSuccessful) {
                        val numberBody = """{"properties":{"Bewertung":{"number":$rating}}}"""
                        val req2 = Request.Builder()
                            .url("https://api.notion.com/v1/pages/$pageId")
                            .addHeader("Authorization", "Bearer ${notionToken.trim()}")
                            .addHeader("Notion-Version", "2022-06-28")
                            .addHeader("Content-Type", "application/json")
                            .patch(numberBody.toRequestBody("application/json".toMediaType()))
                            .build()
                        client.newCall(req2).execute().use { res2 ->
                            Log.d("RecipeRepository", "updateRating number fallback status ${res2.code}")
                            res2.isSuccessful
                        }
                    } else {
                        true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "updateRating failed", e)
            false
        }
    }

    // ─────── Notion JSON Models ───────────────────────────────────────────────

    private data class NotionQueryResponse(
        @SerializedName("results") val results: List<NotionPage> = emptyList(),
        @SerializedName("has_more") val hasMore: Boolean = false,
        @SerializedName("next_cursor") val nextCursor: String? = null
    )

    private data class NotionPage(
        @SerializedName("id") val id: String = "",
        @SerializedName("cover") val cover: NotionCover? = null,
        @SerializedName("properties") val properties: Map<String, NotionProperty> = emptyMap()
    ) {
        fun toRecipe(): Recipe? {
            val title = properties["Name"]?.title?.firstOrNull()?.plainText
                ?: properties.values.firstOrNull { it.type == "title" }?.title?.firstOrNull()?.plainText
                ?: properties["Titel"]?.title?.firstOrNull()?.plainText
                ?: properties["Title"]?.title?.firstOrNull()?.plainText
                ?: return null

            // Cover header image from Notion page, fallback to Bild/URL properties
            val imageUrl = cover?.url
                ?: properties["Bild"]?.url
                ?: properties["URL"]?.url
                ?: properties.values.firstOrNull { it.type == "url" }?.url
                ?: "https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=500&q=80"

            val descriptionText = properties["Beschreibung"]?.richTextText
                ?: properties["Rezepterstellung"]?.richTextText
                ?: ""

            // Portionen
            val servings = properties["Portionen"]?.number?.toInt()
                ?: properties.values.firstOrNull { it.type == "number" }?.number?.toInt()
                ?: 2

            // Categories & Cuisine
            val categoryTags = mutableListOf<String>()
            properties["Kategorie"]?.let { prop ->
                prop.select?.name?.let { if (it.isNotBlank()) categoryTags.add(it) }
                prop.multiSelect?.forEach { if (it.name.isNotBlank()) categoryTags.add(it.name) }
            }
            properties["Küche"]?.let { prop ->
                prop.select?.name?.let { if (it.isNotBlank()) categoryTags.add(it) }
                prop.multiSelect?.forEach { if (it.name.isNotBlank()) categoryTags.add(it.name) }
            }
            properties["Tags"]?.multiSelect?.forEach { if (it.name.isNotBlank()) categoryTags.add(it.name) }

            if (categoryTags.isEmpty()) categoryTags.add("Hauptgericht")

            // Parse Ingredients from "Zutaten" or "Lebensmittel"
            val zutatenText = properties["Zutaten"]?.richTextText
                ?.ifBlank { properties["Lebensmittel"]?.richTextText }
                ?: ""

            val ingredientList = if (zutatenText.isNotBlank()) {
                zutatenText.split("\n", ",").mapNotNull { line ->
                    val trimmed = line.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim()
                    if (trimmed.isNotBlank()) {
                        parseIngredientLine(trimmed)
                    } else null
                }
            } else {
                emptyList()
            }

            // Parse Instructions from "Rezepterstellung"
            val rezepterstellungText = properties["Rezepterstellung"]?.richTextText ?: ""
            val instructionList = if (rezepterstellungText.isNotBlank()) {
                rezepterstellungText.split("\n").mapNotNull { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank()) trimmed else null
                }
            } else {
                emptyList()
            }

            // Score / rating from "Bewertung" SELECT (star emoji options: ★=1, ★★=2, ..., ★★★★★=5)
            val parsedScore = run {
                val selectName = properties["Bewertung"]?.select?.name ?: ""
                if (selectName.contains("★")) {
                    // Count the star characters to get the numeric rating
                    selectName.count { it == '★' }.toDouble()
                } else {
                    // Fallback: try number property or plain text
                    properties["Bewertung"]?.number
                        ?: selectName.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                        ?: properties["Bewertung"]?.richTextText
                            ?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull()
                        ?: 0.0
                }
            }

            val url = properties["URL"]?.url ?: "https://notion.so"

            return Recipe(
                id = id,
                title = title,
                description = descriptionText.ifBlank { "Köstliches Rezept aus deiner Notion-Datenbank." },
                imageUrl = imageUrl,
                cookTimeMinutes = 25,
                calories = 450,
                proteinGrams = 25,
                carbsGrams = 40,
                fatGrams = 15,
                category = categoryTags.firstOrNull()?.lowercase() ?: "all",
                tags = categoryTags,
                score = parsedScore,
                defaultServings = servings,
                ingredients = ingredientList,
                instructions = instructionList,
                notionPageId = id,
                notionUrl = url
            )
        }
    }

    private data class NotionCover(
        @SerializedName("type") val type: String = "",
        @SerializedName("external") val external: NotionFileUrl? = null,
        @SerializedName("file") val file: NotionFileUrl? = null
    ) {
        val url: String?
            get() = external?.url ?: file?.url
    }

    private data class NotionFileUrl(
        @SerializedName("url") val url: String = ""
    )

    private data class NotionProperty(
        @SerializedName("type") val type: String = "",
        @SerializedName("title") val title: List<NotionRichText>? = null,
        @SerializedName("rich_text") val richText: List<NotionRichText>? = null,
        @SerializedName("number") val number: Double? = null,
        @SerializedName("url") val url: String? = null,
        @SerializedName("select") val select: NotionSelectOption? = null,
        @SerializedName("multi_select") val multiSelect: List<NotionSelectOption>? = null,
        @SerializedName("date") val date: NotionDate? = null
    ) {
        val richTextText: String
            get() = richText?.joinToString("") { it.plainText } ?: ""
    }

    private data class NotionSelectOption(
        @SerializedName("name") val name: String = ""
    )

    private data class NotionDate(
        @SerializedName("start") val start: String = ""
    )

    private data class NotionRichText(
        @SerializedName("plain_text") val plainText: String = "",
        @SerializedName("annotations") val annotations: NotionAnnotations? = null
    )

    private data class NotionAnnotations(
        @SerializedName("bold") val bold: Boolean = false,
        @SerializedName("italic") val italic: Boolean = false,
        @SerializedName("strikethrough") val strikethrough: Boolean = false,
        @SerializedName("code") val code: Boolean = false
    )

    private data class NotionBlockContent(
        @SerializedName("rich_text") val richText: List<NotionRichText>? = null
    ) {
        fun toMarkdown(): String {
            if (richText == null) return ""
            return richText.joinToString("") { item ->
                var text = item.plainText
                val ann = item.annotations
                if (ann != null) {
                    if (ann.bold) text = "**$text**"
                    if (ann.italic) text = "*$text*"
                    if (ann.code) text = "`$text`"
                    if (ann.strikethrough) text = "~~$text~~"
                }
                text
            }
        }
    }

    private data class NotionBlockListResponse(
        @SerializedName("results") val results: List<NotionBlock> = emptyList()
    )

    private data class NotionBlock(
        @SerializedName("id") val id: String = "",
        @SerializedName("type") val type: String = "",
        @SerializedName("paragraph") val paragraph: NotionBlockContent? = null,
        @SerializedName("heading_1") val heading1: NotionBlockContent? = null,
        @SerializedName("heading_2") val heading2: NotionBlockContent? = null,
        @SerializedName("heading_3") val heading3: NotionBlockContent? = null,
        @SerializedName("bulleted_list_item") val bulletedListItem: NotionBlockContent? = null,
        @SerializedName("numbered_list_item") val numberedListItem: NotionBlockContent? = null,
        @SerializedName("to_do") val toDo: NotionBlockContent? = null,
        @SerializedName("quote") val quote: NotionBlockContent? = null,
        @SerializedName("callout") val callout: NotionBlockContent? = null
    ) {
        fun toMarkdown(): String? {
            val content = when (type) {
                "paragraph" -> paragraph
                "heading_1" -> heading1
                "heading_2" -> heading2
                "heading_3" -> heading3
                "bulleted_list_item" -> bulletedListItem
                "numbered_list_item" -> numberedListItem
                "to_do" -> toDo
                "quote" -> quote
                "callout" -> callout
                else -> null
            } ?: return null

            val text = content.toMarkdown()
            if (text.isBlank()) return null

            return when (type) {
                "heading_1" -> "# $text"
                "heading_2" -> "## $text"
                "heading_3" -> "### $text"
                "bulleted_list_item" -> "• $text"
                "numbered_list_item" -> text
                "to_do" -> "[ ] $text"
                "quote" -> "> $text"
                "callout" -> "💡 $text"
                else -> text
            }
        }
    }

    companion object {
        fun formatNotionId(rawInput: String): String {
            var cleaned = rawInput.trim()
            if (cleaned.contains("notion.so")) {
                cleaned = cleaned.substringBefore("?").substringAfterLast("/")
            }
            cleaned = cleaned.replace("-", "")
            return if (cleaned.length == 32) {
                "${cleaned.substring(0, 8)}-${cleaned.substring(8, 12)}-${cleaned.substring(12, 16)}-${cleaned.substring(16, 20)}-${cleaned.substring(20)}"
            } else {
                cleaned
            }
        }

        fun parseIngredientLine(line: String): Ingredient {
            val trimmed = line.trim()
                .removePrefix("-")
                .removePrefix("•")
                .removePrefix("*")
                .trim()
            if (trimmed.isBlank()) return Ingredient(name = "", amount = 0.0, unit = "")

            // Fraction e.g. 1/2 or 1/4
            val fractionRegex = Regex("""^(\d+)/(\d+)\s*(.*)$""")
            val fractionMatch = fractionRegex.find(trimmed)
            if (fractionMatch != null) {
                val num = fractionMatch.groupValues[1].toDoubleOrNull() ?: 1.0
                val den = fractionMatch.groupValues[2].toDoubleOrNull() ?: 2.0
                val amount = num / den
                val rest = fractionMatch.groupValues[3].trim()
                val (unit, name) = extractUnitAndName(rest)
                return Ingredient(name = name, amount = amount, unit = unit)
            }

            // Number e.g. 200, 1.5, 1,5
            val numberRegex = Regex("""^(\d+(?:[.,]\d+)?)\s*(.*)$""")
            val numberMatch = numberRegex.find(trimmed)
            if (numberMatch != null) {
                val amountStr = numberMatch.groupValues[1].replace(",", ".")
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val rest = numberMatch.groupValues[2].trim()
                val (unit, name) = extractUnitAndName(rest)
                return Ingredient(name = name, amount = amount, unit = unit)
            }

            return Ingredient(name = trimmed, amount = 0.0, unit = "")
        }

        private fun extractUnitAndName(rest: String): Pair<String, String> {
            if (rest.isBlank()) return "" to ""

            val commonUnits = setOf(
                "g", "kg", "ml", "l", "dl", "cl",
                "el", "tl", "esslöffel", "teelöffel",
                "prise", "prisen", "pck.", "pck", "packung", "packungen", "päckchen",
                "dosen", "dose", "becher", "glas", "gläser", "zehe", "zehen",
                "stk", "stk.", "stück", "scheibe", "scheiben", "bund", "kästchen",
                "tbsp", "tsp", "cup", "cups", "oz", "lb"
            )

            val parts = rest.split(Regex("""\s+"""), limit = 2)
            val firstWord = parts[0].lowercase().removeSuffix(".")

            if (firstWord in commonUnits) {
                val unit = parts[0]
                val name = parts.getOrNull(1) ?: ""
                return unit to name
            }

            return "" to rest
        }
    }
}
