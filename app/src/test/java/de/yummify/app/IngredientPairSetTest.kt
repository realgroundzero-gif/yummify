package de.yummify.app

import de.yummify.app.data.model.IngredientMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Messung für #8: der Regel-Matcher gegen den Paarsatz in `ingredient_pairs.tsv` (auch Grundlage für den Embedding-Vergleich). */
class IngredientPairSetTest {
    private data class Pair(val a: String, val b: String, val same: Boolean)

    private val pairs = javaClass.getResource("/ingredient_pairs.tsv")!!.readText().lines()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .map { line -> line.split("\t").let { Pair(it[0], it[1], it[2] == "gleich") } }

    @Test fun setIsLargeEnough() {
        assertTrue(pairs.size >= 100)
        assertTrue(pairs.count { it.same } >= 40 && pairs.count { !it.same } >= 40)
    }

    @Test fun neverMergesDifferentIngredients() {
        val wrong = pairs.filter { !it.same && IngredientMatcher.same(it.a, it.b) }
        assertEquals("Fälschlich zusammengelegt: $wrong", 0, wrong.size)
    }

    @Test fun findsMostOfTheEqualPairs() {
        val missed = pairs.filter { it.same && !IngredientMatcher.same(it.a, it.b) }
        val found = pairs.count { it.same } - missed.size
        println("RULE_MATCHER gleich gefunden: $found/${pairs.count { it.same }}; verpasst: ${missed.joinToString { "${it.a} ~ ${it.b}" }}")
        assertTrue("Zu viele verpasst: $missed", missed.size <= pairs.count { it.same } / 4)
    }
}
