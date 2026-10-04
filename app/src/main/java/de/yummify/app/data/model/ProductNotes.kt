package de.yummify.app.data.model

/** Remove only the previous scanner's generated lines once the separate product fields exist. */
fun InventoryItem.compactProductNotes(): InventoryItem {
    if (deleted || productUrl != "https://world.openfoodfacts.org/product/$barcode" ||
        !notes.contains("Quelle: Open Food Facts · $productUrl")) return this
    val compact = notes.lineSequence().filterNot { line ->
        (line.startsWith("Zutaten: ") && ingredients != null) ||
            (line.startsWith("Nährwerte pro 100 g/ml: ") && listOf(calories, fat, carbohydrates, protein).any { it != null }) ||
            line == "Quelle: Open Food Facts · $productUrl" || line == "Daten: ODbL · Bilder: CC BY-SA"
    }.joinToString("\n").trim()
    return if (compact == notes) this else copy(notes = compact, dirty = true)
}
