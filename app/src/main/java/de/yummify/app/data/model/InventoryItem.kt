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
    val deleted: Boolean = false,
    // Open Food Facts values per 100 g / 100 ml; null means unknown, not zero.
    val calories: Double? = null,
    val fat: Double? = null,
    val carbohydrates: Double? = null,
    val protein: Double? = null,
    val ingredients: String? = null,
    val productUrl: String? = null
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
        listOf(calories, fat, carbohydrates, protein).any { it != null && (!it.isFinite() || it < 0) } -> "Nährwerte müssen Zahlen ab 0 sein."
        (ingredients?.length ?: 0) > 20000 -> "Die Zutatenliste ist zu lang."
        productUrl != null && productUrl.isNotBlank() && !runCatching { java.net.URI(productUrl).let { it.scheme in setOf("https", "http") && !it.host.isNullOrBlank() } }.getOrDefault(false) -> "Ungültige Produkt-URL."
        expiry != null && expiryDate() == null -> "Ablaufdatum als JJJJ-MM-TT eingeben."
        else -> null
    }
}

/** Gson ignores Kotlin defaults: entries stored by older versions may hold null in fields added later. */
@Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
fun InventoryItem.normalized(): InventoryItem = copy(
    id = id ?: java.util.UUID.randomUUID().toString(), name = name ?: "", unit = unit ?: "", category = category ?: "",
    categoryOptions = categoryOptions ?: emptyList(), location = location ?: "", barcode = barcode ?: "", notes = notes ?: ""
)

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
    /** Same ingredient despite different spelling, word order or additions like "(Dose)"; see [IngredientMatcher]. */
    fun sameIngredient(a: String, b: String) = IngredientMatcher.same(a, b)
    /** Spelling variants of the same unit ("Dosen", "Packungen", "Stk.") map to one key so they can be compared. */
    private val unitAliases: Map<String, String> = mapOf(
        "stk" to "stück", "stueck" to "stück", "st" to "stück", "x" to "stück", "stücke" to "stück",
        "dosen" to "dose", "pck" to "packung", "packungen" to "packung", "päckchen" to "packung", "päckchen." to "packung", "packchen" to "packung", "pkg" to "packung",
        "beutel" to "beutel", "becher" to "becher", "gläser" to "glas", "glaeser" to "glas",
        "zehen" to "zehe", "scheiben" to "scheibe", "prisen" to "prise", "bunde" to "bund", "flaschen" to "flasche",
        "esslöffel" to "el", "essloeffel" to "el", "teelöffel" to "tl", "teeloeffel" to "tl", "tassen" to "tasse",
        "gramm" to "g", "kilogramm" to "kg", "liter" to "l", "milliliter" to "ml"
    )
    private fun unit(value: String): Pair<String, Double> {
        val raw = value.trim().lowercase(Locale.GERMAN).removeSuffix(".")
        return when (val key = unitAliases[raw] ?: raw) {
            "g" -> "mass" to 1.0
            "kg" -> "mass" to 1000.0
            "ml" -> "volume" to 1.0
            "l" -> "volume" to 1000.0
            "", "stück" -> "piece" to 1.0
            else -> key to 1.0
        }
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
            val candidates = result.filter { !it.deleted && !it.isExpired(today) && sameIngredient(it.name, ingredient.name) && convert(it.quantity, it.unit, ingredient.unit) != null }
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
    /** Stock that can be used for [ingredient]: same product, not expired, summed in the ingredient's unit. */
    fun available(ingredient: Ingredient, stock: List<InventoryItem>, today: LocalDate = LocalDate.now()): Double =
        stock.filter { !it.deleted && !it.isExpired(today) && sameIngredient(it.name, ingredient.name) }
            .sumOf { convert(it.quantity, it.unit, ingredient.unit) ?: 0.0 }
    fun missing(ingredient: Ingredient, multiplier: Double, stock: List<InventoryItem>, today: LocalDate = LocalDate.now()): Double =
        (ingredient.amount * multiplier - available(ingredient, stock, today)).coerceAtLeast(0.0)
    enum class Coverage { ENOUGH, PARTIAL, OTHER_UNIT, SIMILAR, MISSING }

    /**
     * How well the inventory covers [ingredient]: [shortfall] is what is still missing in the recipe's unit.
     * [otherUnit] describes stock of the same product that cannot be compared with the recipe's unit
     * (500 g needed, "3 Stück" in stock); it is a hint for the user, never counted as available.
     */
    data class Cover(val state: Coverage, val needed: Double, val available: Double, val shortfall: Double, val otherUnit: String?)

    /** Amount that goes on the shopping list for one ingredient: what is missing, otherwise the full recipe amount; 0 without an amount. */
    fun shoppingAmount(ingredient: Ingredient, multiplier: Double, stock: List<InventoryItem>, today: LocalDate = LocalDate.now()): Double {
        if (ingredient.amount <= 0.0) return 0.0
        val cover = cover(ingredient, multiplier, stock, today)
        return if (cover.shortfall > 0.0) cover.shortfall else ingredient.amount * multiplier
    }

    fun cover(ingredient: Ingredient, multiplier: Double, stock: List<InventoryItem>, today: LocalDate = LocalDate.now()): Cover {
        val same = stock.filter { !it.deleted && !it.isExpired(today) && it.quantity > 0.0 && sameIngredient(it.name, ingredient.name) }
        val needed = ingredient.amount * multiplier
        val comparable = same.filter { convert(it.quantity, it.unit, ingredient.unit) != null }
        val available = comparable.sumOf { convert(it.quantity, it.unit, ingredient.unit) ?: 0.0 }
        val other = same.filter { it !in comparable }.joinToString(", ") { "${number(it.quantity)} ${it.unit}".trim() }.ifBlank { null }
        // No rule match: a similar article ("Kartoffeln" for "festkochende Kartoffeln") is only a hint, never counted.
        val similar = if (same.isEmpty()) stock.firstOrNull {
            !it.deleted && !it.isExpired(today) && it.quantity > 0.0 && IngredientMatcher.similar(it.name, ingredient.name)
        } else null
        val notFound = if (similar != null) Cover(Coverage.SIMILAR, needed, 0.0, needed, similar.name) else Cover(Coverage.MISSING, needed, 0.0, needed, null)
        // Without an amount in the recipe ("Salz") any stock of the product is enough.
        if (ingredient.amount <= 0.0) return if (same.isNotEmpty()) Cover(Coverage.ENOUGH, 0.0, available, 0.0, null) else notFound.copy(needed = 0.0, shortfall = 0.0)
        if (same.isEmpty()) return notFound
        val shortfall = (needed - available).coerceAtLeast(0.0)
        val state = when {
            shortfall <= 0.000001 -> Coverage.ENOUGH
            available > 0.0 -> Coverage.PARTIAL
            other != null -> Coverage.OTHER_UNIT
            else -> Coverage.MISSING
        }
        return Cover(state, needed, available, shortfall, other)
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
