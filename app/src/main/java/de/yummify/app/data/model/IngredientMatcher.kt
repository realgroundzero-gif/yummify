package de.yummify.app.data.model

import java.util.Locale

/**
 * Decides whether two ingredient names mean the same thing, without any model or network.
 *
 * Names are reduced to a set of word stems: lowercase, umlauts folded, quantities, units, packaging
 * and kitchen words removed ("500 g", "Dose", "frisch", "gehackt", text in parentheses), plural and
 * inflection endings cut, known synonyms unified. Two names match when these sets are equal, so
 * "Rote Paprika" = "Paprika rot" and "Passierte Tomaten (Dose)" = "Tomaten passiert".
 *
 * It is deliberately strict: "Paprika" does not match "Rote Paprika" and "Milch" does not match
 * "Kokosmilch". A missed match only leaves one more line on the shopping list; a wrong match
 * would make a missing ingredient disappear from it.
 */
object IngredientMatcher {
    private fun fold(word: String) = word.lowercase(Locale.GERMAN)
        .replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")

    /** Cuts one plural or inflection ending ("Tomaten" → "tomat", "Zwiebeln" → "zwiebel"); stems stay at least 3 letters long. */
    private fun stem(folded: String): String {
        var result = folded
        for (ending in listOf("en", "er", "e", "n", "s")) {
            // "Kürbis", "Erdnuss", "weiß" end in s without being a plural.
            if (ending == "s" && (folded.endsWith("ss") || folded.endsWith("is") || folded.endsWith("us"))) continue
            if (folded.endsWith(ending) && folded.length - ending.length >= 3) { result = folded.removeSuffix(ending); break }
        }
        // "Kürbisse" and "Kürbis", "weiße" and "weiß" must reach the same stem.
        return if (result.endsWith("iss")) result.dropLast(1) else result
    }

    private fun normalize(word: String) = stem(fold(word))

    /** Words that describe quantity, packaging or preparation, not the product itself. */
    private val ignored: Set<String> = listOf(
        "g", "kg", "mg", "ml", "l", "dl", "cl", "el", "tl", "essloffel", "teeloffel", "tasse", "tassen", "prise", "prisen",
        "dose", "dosen", "glas", "glaser", "packung", "packungen", "pck", "packchen", "pack", "becher", "bund", "zehe", "zehen",
        "stuck", "stucke", "stk", "scheibe", "scheiben", "wurfel", "streifen", "handvoll", "flasche", "flaschen", "beutel",
        "frisch", "frische", "frischer", "frisches", "frischen", "gehackt", "gehackte", "gewurfelt", "gewurfelte", "gehobelt",
        "geschalt", "geschalte", "gerieben", "geriebene", "geraspelt", "gemahlen", "gemahlene", "gepresst", "fein", "feine", "grob",
        "grobe", "klein", "kleine", "gross", "grosse", "mittelgross", "reif", "reife", "bio", "ca", "etwa", "evtl", "optional",
        "nach", "geschmack", "zum", "zur", "fur", "in", "aus", "von", "und", "oder", "mit", "ohne", "der", "die", "das", "ein", "eine",
        "einige", "etwas", "getrocknet", "getrocknete", "tiefgekuhlt", "tk", "natur", "flussig", "flussige"
    ).map { normalize(it) }.toSet()

    /** Words that name the same product. The first entry of each group is the canonical word. */
    private val synonymGroups = listOf(
        listOf("Frühlingszwiebel", "Lauchzwiebel"),
        listOf("Paprika", "Paprikaschote"),
        listOf("Tomate", "Paradeiser"),
        listOf("Kartoffel", "Erdapfel"),
        listOf("Karotte", "Möhre", "Mohrrübe"),
        listOf("Hackfleisch", "Hack", "Gehacktes"),
        listOf("Sahne", "Schlagsahne", "Obers"),
        listOf("Joghurt", "Jogurt"),
        listOf("Quark", "Topfen"),
        listOf("Aubergine", "Eierfrucht"),
        listOf("Blumenkohl", "Karfiol"),
        listOf("Rucola", "Rauke"),
        listOf("Ei", "Eier"),
        listOf("Zucchini", "Zucchino"),
        listOf("Pute", "Truthahn"),
        listOf("Garnele", "Shrimp", "Krabbe"),
        listOf("Sojasauce", "Sojasoße"),
        listOf("Lauch", "Porree"),
        listOf("Hähnchen", "Hühnchen", "Huhn"),
        listOf("Kirschtomate", "Cherrytomate")
    )

    private val canonical: Map<String, String> = synonymGroups.flatMap { group ->
        val head = normalize(group.first())
        group.map { normalize(it) to head }
    }.toMap()

    /** Word stems that identify the ingredient; empty when the text contains only quantities and kitchen words. */
    fun tokens(name: String): Set<String> =
        name.replace(Regex("""\([^)]*\)"""), " ")
            .split(Regex("""[^\p{L}]+"""))
            .filter { it.length > 1 }
            .map { normalize(it) }
            .filter { it !in ignored }
            .map { canonical[it] ?: it }
            .toSet()

    fun same(a: String, b: String): Boolean {
        val first = tokens(a)
        return first.isNotEmpty() && first == tokens(b)
    }
}
