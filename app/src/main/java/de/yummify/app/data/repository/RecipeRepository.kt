package de.yummify.app.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import de.yummify.app.data.local.SampleData
import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.ShoppingItem
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

    private val isNotionConfigured: Boolean
        get() = notionToken.isNotBlank() && recipeDatabaseId.isNotBlank()

    // ─────── Recipes ──────────────────────────────────────────────────────────

    suspend fun getAllRecipes(): List<Recipe> {
        if (!isNotionConfigured) return SampleData.recipes
        return try {
            fetchRecipesFromNotion()
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Notion fetch failed, using sample data", e)
            SampleData.recipes
        }
    }

    suspend fun getRecipeById(id: String): Recipe? {
        return getAllRecipes().find { it.id == id }
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

    suspend fun getMealPlan(): List<MealPlanItem> = SampleData.mealPlanItems

    // ─────── Shopping List ────────────────────────────────────────────────────

    suspend fun getShoppingItems(): List<ShoppingItem> = SampleData.shoppingItems

    // ─────── Notion API ───────────────────────────────────────────────────────

    private fun fetchRecipesFromNotion(): List<Recipe> {
        val body = """{"filter":{},"page_size":100}"""
        val request = Request.Builder()
            .url("https://api.notion.com/v1/databases/$recipeDatabaseId/query")
            .addHeader("Authorization", "Bearer $notionToken")
            .addHeader("Notion-Version", "2022-06-28")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return SampleData.recipes
            val json = response.body?.string() ?: return SampleData.recipes
            val notionResponse = gson.fromJson(json, NotionQueryResponse::class.java)
            return notionResponse.results.mapNotNull { page -> page.toRecipe() }
        }
    }

    fun testNotionConnection(token: String, databaseId: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://api.notion.com/v1/databases/$databaseId")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Notion-Version", "2022-06-28")
                .get()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    // ─────── Notion JSON Models ───────────────────────────────────────────────

    private data class NotionQueryResponse(
        @SerializedName("results") val results: List<NotionPage> = emptyList()
    )

    private data class NotionPage(
        @SerializedName("id") val id: String = "",
        @SerializedName("properties") val properties: Map<String, NotionProperty> = emptyMap()
    ) {
        fun toRecipe(): Recipe? {
            val title = properties["Name"]?.title?.firstOrNull()?.plainText ?: return null
            val calories = properties["Kalorien"]?.number?.toInt() ?: 500
            val prepTime = properties["Zubereitungszeit"]?.number?.toInt() ?: 30
            val tags = properties["Tags"]?.multiSelect?.map { it.name } ?: emptyList()
            val imageUrl = properties["Bild"]?.url ?: ""
            val description = properties["Beschreibung"]?.richText?.firstOrNull()?.plainText ?: ""
            return Recipe(
                id = id,
                title = title,
                description = description,
                imageUrl = imageUrl,
                cookTimeMinutes = prepTime,
                calories = calories,
                proteinGrams = 0,
                carbsGrams = 0,
                fatGrams = 0,
                category = tags.firstOrNull()?.lowercase() ?: "other",
                tags = tags,
                ingredients = emptyList(),
                instructions = emptyList()
            )
        }
    }

    private data class NotionProperty(
        @SerializedName("type") val type: String = "",
        @SerializedName("title") val title: List<NotionRichText>? = null,
        @SerializedName("rich_text") val richText: List<NotionRichText>? = null,
        @SerializedName("number") val number: Double? = null,
        @SerializedName("url") val url: String? = null,
        @SerializedName("multi_select") val multiSelect: List<NotionSelectOption>? = null
    )

    private data class NotionRichText(
        @SerializedName("plain_text") val plainText: String = ""
    )

    private data class NotionSelectOption(
        @SerializedName("name") val name: String = ""
    )
}
