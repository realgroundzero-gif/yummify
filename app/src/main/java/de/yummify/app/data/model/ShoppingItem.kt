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

/**
 * An ingredient from the week plan that was not put on the list because the inventory already
 * covers it. The user can move it back to the list with one tap.
 */
data class CoveredItem(
    val id: String,
    val name: String,
    /** What the plan needs, e.g. "400 g". */
    val needed: String,
    /** Why it was left out, e.g. "Vorrat: 500 g". */
    val reason: String,
    val quantity: Double?,
    val unit: String?,
    val recipeName: String?,
    /** Set when the user confirmed a "Vielleicht schon da" suggestion; "Doch kaufen" then remembers the decision. */
    val suggestionKey: String? = null
)
