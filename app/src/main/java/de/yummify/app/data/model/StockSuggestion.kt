package de.yummify.app.data.model

import java.time.LocalDate

/** "Vielleicht schon da": a shopping entry for which the inventory holds something similar, but not the same. */
data class StockSuggestion(
    val itemId: String,
    val itemName: String,
    val stockName: String,
    /** Inventory amount as text, e.g. "2 Stk". */
    val stockAmount: String,
    /** Identifies the name pair; a decision "Nein, kaufen" is remembered under this key. */
    val key: String
)

object StockSuggestions {
    /**
     * Open list entries without an inventory match by the rules but with a similar, unexpired inventory item.
     * Pairs the user has already decided on ([dismissed]) are left out. This only suggests; it never changes the list.
     */
    fun find(list: List<ShoppingItem>, stock: List<InventoryItem>, dismissed: Set<String>, today: LocalDate = LocalDate.now()): List<StockSuggestion> {
        val available = stock.filter { !it.deleted && !it.isExpired(today) && it.quantity > 0.0 }
        if (available.isEmpty()) return emptyList()
        return list.filter { !it.isChecked }.mapNotNull { entry ->
            if (available.any { IngredientMatcher.same(it.name, entry.name) }) return@mapNotNull null
            available.firstOrNull { IngredientMatcher.similar(it.name, entry.name) && IngredientMatcher.pairKey(it.name, entry.name) !in dismissed }
                ?.let { match ->
                    StockSuggestion(entry.id, entry.name, match.name, "${InventoryMath.number(match.quantity)} ${match.unit}".trim(),
                        IngredientMatcher.pairKey(match.name, entry.name))
                }
        }
    }
}
