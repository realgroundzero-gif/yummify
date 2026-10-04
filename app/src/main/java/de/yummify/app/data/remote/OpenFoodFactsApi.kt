package de.yummify.app.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Read-only barcode lookup. No Notion credentials are sent to this service. */
class OpenFoodFactsApi(
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build(),
    private val baseUrl: String = "https://world.openfoodfacts.org",
    private val userAgent: String = "Yummify/1.1.0 (Android; https://github.com/realgroundzero-gif/yummify)"
) {
    fun product(barcode: String): FoodProduct? {
        val code = normalizeBarcode(barcode)
        val request = Request.Builder().url("$baseUrl/api/v2/product/$code.json?lc=de&fields=product_name_de,product_name,brands,quantity,product_quantity,product_quantity_unit,categories,categories_tags,categories_hierarchy,image_front_url,image_url,ingredients_text_de,ingredients_text,nutriments")
            .header("User-Agent", userAgent).build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Keine Produktdaten erhalten.")
            if (response.code == 404) return null
            if (!response.isSuccessful) throw IOException(if (response.code == 429) "Zu viele Abfragen. Bitte später erneut versuchen." else "Produktabfrage fehlgeschlagen (${response.code}).")
            decode(JsonParser.parseString(body).asJsonObject, code)
        }
    }
    companion object {
        fun normalizeBarcode(value: String): String {
            val code = value.trim().replace(" ", "")
            require(code.matches(Regex("[0-9]{8,14}"))) { "Bitte einen Lebensmittel-Barcode mit 8 bis 14 Ziffern eingeben." }
            return code
        }
        fun decode(response: JsonObject, barcode: String): FoodProduct? {
            if (response.get("status")?.asInt != 1) return null
            val p = response.get("product")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
            fun text(key: String) = p.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() }
            fun tags(key: String) = p.get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { it.takeIf { it.isJsonPrimitive }?.asString }.orEmpty()
            val brand = text("brands")
            val name = text("product_name_de") ?: text("product_name")
            val title = name?.let { if (brand != null && !it.contains(brand, true)) "$brand $it" else it }
            val amount = parseQuantity(text("quantity")) ?: parseQuantity(listOfNotNull(text("product_quantity"), text("product_quantity_unit")).joinToString(" "))
            val nutriments = p.get("nutriments")?.takeIf { it.isJsonObject }?.asJsonObject
            val nutrition = listOf("energy-kcal_100g" to "kcal", "fat_100g" to "g Fett", "carbohydrates_100g" to "g Kohlenhydrate", "proteins_100g" to "g Protein", "salt_100g" to "g Salz")
                .mapNotNull { (key, label) -> nutriments?.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.let { "$it $label" } }.joinToString(" · ")
            return FoodProduct(barcode, title?.take(200), amount?.first, amount?.second,
                text("categories"), (tags("categories_hierarchy") + tags("categories_tags")).distinct(),
                (text("image_front_url") ?: text("image_url"))?.takeIf { it.startsWith("https://") },
                listOfNotNull(brand?.let { "Marke: $it" }, (text("ingredients_text_de") ?: text("ingredients_text"))?.let { "Zutaten: $it" },
                    nutrition.takeIf { it.isNotBlank() }?.let { "Nährwerte pro 100 g/ml: $it" }).joinToString("\n"))
        }
        fun parseQuantity(raw: String?): Pair<Double, String>? {
            val match = Regex("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*(kg|g|ml|cl|l|liter|litre|gramm|grammes?)\\s*$", RegexOption.IGNORE_CASE).find(raw.orEmpty()) ?: return null
            val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return null
            return when (val unit = match.groupValues[2].lowercase()) {
                "liter", "litre" -> amount to "l"
                "gramm", "gram", "gramme", "grammes" -> amount to "g"
                "cl" -> amount * 10 to "ml"
                else -> amount to unit
            }
        }
    }
}

data class FoodProduct(val barcode: String, val name: String?, val quantity: Double?, val unit: String?,
    val categories: String?, val categoryTags: List<String>, val imageUrl: String?, val details: String) {
    val sourceUrl get() = "https://world.openfoodfacts.org/product/$barcode"
    fun notes() = (listOfNotNull(quantity?.let { "Packungsinhalt: ${de.yummify.app.data.model.InventoryMath.number(it)} ${unit.orEmpty()}" }, details.takeIf { it.isNotBlank() }).joinToString("\n")).take(1500) + "\nQuelle: Open Food Facts · $sourceUrl\nDaten: ODbL · Bilder: CC BY-SA"
    fun suggestedCategory(options: List<String>): String? {
        val localized = categories.orEmpty().split(',').map { it.trim() }.filter { it.isNotBlank() }
        options.firstOrNull { option -> localized.any { it.equals(option, true) } }?.let { return it }
        val tags = categoryTags.toSet()
        val hints = listOf(
            setOf("en:spices", "en:condiments", "en:vegetable-oils") to listOf("Gewürze & Öle", "Gewürze"),
            setOf("en:frozen-foods") to listOf("Tiefkühl"),
            setOf("en:dairies", "en:milks", "en:cheeses", "en:yogurts") to listOf("Kühlregal & Milchprodukte", "Milchprodukte"),
            setOf("en:meats", "en:fishes", "en:seafood") to listOf("Fisch & Fleisch", "Fleisch"),
            setOf("en:fruits", "en:vegetables") to listOf("Obst & Gemüse", "Gemüse"),
            setOf("en:breads", "en:pastries") to listOf("Bäckerei"),
            setOf("en:cereals-and-their-products", "en:cereals", "en:pastas", "en:rices") to listOf("Vorrat & Trockenwaren", "Vorrat")
        )
        hints.forEach { (matches, names) ->
            if (tags.any { it in matches }) {
                options.firstOrNull { option -> names.any { it.equals(option, true) } }?.let { return it }
                if (options.isEmpty()) return names.first()
            }
        }
        // Never add untranslated taxonomy IDs to an existing Notion dropdown.
        return if (options.isEmpty()) localized.lastOrNull { !it.contains(':') }?.take(100) else null
    }
}
