package de.yummify.app.data.model

/** Optional values stay null when the Notion database has no matching column; the UI hides them. */
data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val cookTimeMinutes: Int?,
    val calories: Int?,
    val proteinGrams: Int?,
    val carbsGrams: Int?,
    val fatGrams: Int?,
    val category: String, // e.g. "pasta", "veggie", "quick", "protein", "onepot", "baking"
    val tags: List<String>,
    val score: Double = 0.0,
    val isFavorite: Boolean = false,
    val isHeroOfDay: Boolean = false,
    val ingredients: List<Ingredient>,
    val instructions: List<String>,
    val difficulty: String? = null,
    val estimatedCost: String? = null,
    val lastCookedDate: String? = null,
    val defaultServings: Int = 2,
    val notionPageId: String? = null,
    val notionUrl: String? = null
)
