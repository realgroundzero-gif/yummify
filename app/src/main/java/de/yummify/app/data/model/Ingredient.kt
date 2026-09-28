package de.yummify.app.data.model

data class Ingredient(
    val name: String,
    val amount: Double,
    val unit: String,
    val category: String = "Vorrat",
    val isAvailableInPantry: Boolean = false,
    val isBought: Boolean = false
) {
    fun getFormattedAmount(portionMultiplier: Double = 1.0): String {
        val scaled = amount * portionMultiplier
        val formattedAmount = if (scaled % 1.0 == 0.0) {
            scaled.toInt().toString()
        } else {
            String.format("%.1f", scaled)
        }
        return if (unit.isBlank()) formattedAmount else "$formattedAmount $unit"
    }
}
