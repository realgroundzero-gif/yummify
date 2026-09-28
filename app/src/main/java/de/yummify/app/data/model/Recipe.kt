package de.yummify.app.data.model

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val cookTimeMinutes: Int,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val category: String, // e.g. "pasta", "veggie", "quick", "protein", "onepot", "baking"
    val tags: List<String>,
    val score: Double = 4.8,
    val isFavorite: Boolean = false,
    val isHeroOfDay: Boolean = false,
    val ingredients: List<Ingredient>,
    val instructions: List<String>,
    val difficulty: String = "Einfach",
    val estimatedCost: String = "€€ (Günstig)",
    val lastCookedDate: String = "Gestern",
    val defaultServings: Int = 2,
    val notionPageId: String? = null,
    val notionUrl: String? = "https://notion.so"
)
