package de.yummify.app.data.model

import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val pageId: String? = null,
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "Stk",
    val category: String = "Vorrat",
    val categoryOptions: List<String> = emptyList(),
    val location: String = "Vorratsschrank",
    val minimum: Double = 0.0,
    val expiry: String? = null,
    val barcode: String = "",
    val notes: String = "",
    val coverUrl: String? = null,
    val localCoverPath: String? = null,
    val coverPending: Boolean = false,
    val dirty: Boolean = true,
    val deleted: Boolean = false
) {
    val isLow: Boolean get() = quantity <= minimum
    fun isExpired(today: LocalDate = LocalDate.now()) = expiryDate()?.isBefore(today) == true
    fun expiresSoon(today: LocalDate = LocalDate.now()): Boolean = expiryDate()?.let { !it.isBefore(today) && !it.isAfter(today.plusDays(7)) } ?: false
    fun expiryDate(): LocalDate? = expiry?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    private fun validCategoryLength() = (if (categoryOptions.isNotEmpty() && category == categoryOptions.joinToString(", ")) categoryOptions else listOf(category)).all { it.length <= 100 }
    fun validate(): String? = when {
        name.isBlank() -> "Bitte einen Namen eingeben."
        name.length > 200 || notes.length > 2000 || barcode.length > 200 -> "Name, Barcode oder Notiz ist zu lang."
        !quantity.isFinite() || quantity < 0 -> "Die Menge muss eine Zahl ab 0 sein."
        !minimum.isFinite() || minimum < 0 -> "Der Mindestbestand muss eine Zahl ab 0 sein."
        unit.length > 100 || !validCategoryLength() || location.length > 100 -> "Einheit, Kategorie oder Lagerort ist zu lang."
        expiry != null && expiryDate() == null -> "Ablaufdatum als JJJJ-MM-TT eingeben."
        else -> null
    }
}

object InventoryMath {
    fun quantityStep(unit: String): Double = when (unit.trim().lowercase(Locale.GERMAN)) {
        "g", "ml" -> 50.0
        "kg", "l" -> 0.1
        else -> 1.0
    }
    fun stepQuantity(quantity: Double, delta: Double): Double {
        require(quantity.isFinite() && quantity >= 0 && delta.isFinite()) { "Ungültige Menge." }
        val result = java.math.BigDecimal.valueOf(quantity).add(java.math.BigDecimal.valueOf(delta))
            .max(java.math.BigDecimal.ZERO).toDouble()
        require(result.isFinite()) { "Die Menge ist zu groß." }
        return result
    }

    fun number(value: Double): String = java.text.DecimalFormat("0.###").format(value)
    fun normalizedName(value: String) = value.trim().lowercase(Locale.GERMAN).replace(Regex("\\s+"), " ")
    private fun unit(value: String): Pair<String, Double> = when (value.trim().lowercase(Locale.GERMAN).removeSuffix(".")) {
        "g" -> "mass" to 1.0
        "kg" -> "mass" to 1000.0
        "ml" -> "volume" to 1.0
        "l" -> "volume" to 1000.0
        "", "stk", "stück", "stueck" -> "piece" to 1.0
        else -> value.trim().lowercase(Locale.GERMAN) to 1.0
    }
    fun convert(amount: Double, from: String, to: String): Double? {
        val a = unit(from); val b = unit(to)
        return if (a.first == b.first) amount * a.second / b.second else null
    }
    fun consume(ingredients: List<Ingredient>, multiplier: Double, stock: List<InventoryItem>, today: LocalDate = LocalDate.now()): List<InventoryItem> {
        require(multiplier.isFinite() && multiplier > 0) { "Ungültige Portionen." }
        var result = stock
        ingredients.forEach { ingredient ->
            require(ingredient.amount.isFinite() && ingredient.amount >= 0) { "Ungültige Zutatenmenge." }
            var remaining = ingredient.amount * multiplier
            val candidates = result.filter { !it.deleted && !it.isExpired(today) && normalizedName(it.name) == normalizedName(ingredient.name) && convert(it.quantity, it.unit, ingredient.unit) != null }
                .sortedBy { it.expiry ?: "9999-12-31" }
            candidates.forEach { candidate ->
                if (remaining > 0) {
                    val available = convert(candidate.quantity, candidate.unit, ingredient.unit)!!
                    val used = minOf(available, remaining)
                    val delta = convert(used, ingredient.unit, candidate.unit)!!
                    result = result.map { if (it.id == candidate.id) it.copy(quantity = (it.quantity - delta).coerceAtLeast(0.0), dirty = true) else it }
                    remaining -= used
                }
            }
            require(remaining <= 0.000001) { "Nicht genug ${ingredient.name} im Vorrat. Fehlende Zutaten zuerst einkaufen." }
        }
        return result
    }
    fun missing(ingredient: Ingredient, multiplier: Double, stock: List<InventoryItem>, today: LocalDate = LocalDate.now()): Double {
        val available = stock.filter { !it.deleted && !it.isExpired(today) && normalizedName(it.name) == normalizedName(ingredient.name) }
            .sumOf { convert(it.quantity, it.unit, ingredient.unit) ?: 0.0 }
        return (ingredient.amount * multiplier - available).coerceAtLeast(0.0)
    }
}

/** Persisted schema options, including values that are not used by any item yet. */
data class InventoryChoices(
    val categories: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val units: List<String> = emptyList(),
    val categoryMultiSelect: Boolean = false,
    val categoryType: String = "rich_text",
    val locationType: String = "rich_text",
    val unitType: String = "rich_text"
)
