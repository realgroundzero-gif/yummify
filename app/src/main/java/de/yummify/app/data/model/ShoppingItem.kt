package de.yummify.app.data.model

data class ShoppingItem(
    val id: String,
    val name: String,
    val amountWithUnit: String,
    val category: String, // e.g., "Obst & Gemüse", "Kühlregal & Milchprodukte", "Vorrat"
    val recipeName: String? = null,
    val note: String? = null,
    val isChecked: Boolean = false,
    val quantity: Double? = null,
    val unit: String? = null
)
