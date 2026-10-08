package de.yummify.app.share

import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.InventoryMath
import de.yummify.app.data.model.Recipe

/** Plain-text version of a recipe for messengers and note apps. Pure logic, no Android classes. */
object RecipeShareText {
    /** "200 g", "1,5 kg", "" when the recipe gives no amount (e.g. "Salz"). */
    fun amountLabel(ingredient: Ingredient, multiplier: Double): String {
        val scaled = ingredient.amount * multiplier
        if (!scaled.isFinite() || scaled <= 0.0) return ""
        return "${InventoryMath.number(scaled)} ${ingredient.unit}".trim()
    }

    fun ingredientLine(ingredient: Ingredient, multiplier: Double): String =
        listOf(amountLabel(ingredient, multiplier), ingredient.name.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    /** Recipe quantities for the servings the user has selected right now. */
    fun multiplier(recipe: Recipe, servings: Int): Double = servings.toDouble() / recipe.defaultServings.coerceAtLeast(1)

    fun build(recipe: Recipe, servings: Int): String {
        val multiplier = multiplier(recipe, servings)
        val facts = listOfNotNull(
            recipe.cookTimeMinutes?.let { "⏱️ Zeit: $it Min." },
            "👥 Portionen: $servings"
        ).joinToString(" | ")
        val ingredients = recipe.ingredients.map { ingredientLine(it, multiplier) }.filter { it.isNotBlank() }
        val steps = steps(recipe.instructions)
        return buildString {
            append("🍽️ ").append(recipe.title.trim()).append('\n')
            append(facts).append('\n')
            if (ingredients.isNotEmpty()) {
                append("\nZutaten:\n")
                ingredients.forEach { append("• ").append(it).append('\n') }
            }
            if (steps.isNotEmpty()) {
                append("\nZubereitung:\n")
                steps.forEach { append(it).append('\n') }
            }
            append("\n(Geteilt via Yummify)")
        }
    }

    private val orderedPrefix = Regex("""^\d+[.)]\s+""")

    /**
     * Instructions come from Notion blocks as light Markdown ("## Titel", "• Punkt", "> Zitat").
     * Ordinary steps are numbered; headings restart the numbering; markup characters are removed.
     */
    internal fun steps(instructions: List<String>): List<String> {
        val result = mutableListOf<String>()
        var number = 0
        instructions.map { it.trim() }.filter { it.isNotEmpty() }.forEach { raw ->
            when {
                raw.startsWith("#") -> {
                    val heading = plain(raw.trimStart('#'))
                    if (heading.isNotEmpty()) {
                        if (result.isNotEmpty()) result += ""
                        result += "$heading:"
                        number = 0
                    }
                }
                raw.startsWith("• ") -> result += "• " + plain(raw.removePrefix("• "))
                raw.startsWith("[ ] ") -> result += "• " + plain(raw.removePrefix("[ ] "))
                raw.startsWith("[x] ") -> result += "• " + plain(raw.removePrefix("[x] "))
                raw.startsWith("> ") -> result += "„" + plain(raw.removePrefix("> ")) + "“"
                raw.startsWith("💡") -> result += "💡 " + plain(raw.removePrefix("💡"))
                else -> {
                    val text = plain(raw.replace(orderedPrefix, ""))
                    if (text.isNotEmpty()) result += "${++number}. $text"
                }
            }
        }
        return result
    }

    private val markup = listOf(Regex("""\*\*(.+?)\*\*"""), Regex("""~~(.+?)~~"""), Regex("""`(.+?)`"""), Regex("""\*(.+?)\*"""))

    /** Removes the inline Markdown that the recipe reader adds for bold, italic, code and strikethrough. */
    internal fun plain(text: String): String = markup.fold(text) { acc, regex -> acc.replace(regex, "$1") }.trim()
}
