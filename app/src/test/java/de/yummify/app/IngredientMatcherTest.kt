package de.yummify.app

import de.yummify.app.data.model.IngredientMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientMatcherTest {
    private fun same(a: String, b: String) = IngredientMatcher.same(a, b)

    @Test fun examplesFromTheIssueMatch() {
        assertTrue(same("Rote Paprika", "Paprika rot"))
        assertTrue(same("Passierte Tomaten (Dose)", "Tomaten passiert"))
        assertTrue(same("Frühlingszwiebeln, gehackt", "Lauchzwiebeln"))
    }

    @Test fun quantitiesUnitsAndKitchenWordsAreIgnored() {
        assertTrue(same("500 g Mehl", "Mehl"))
        assertTrue(same("2 Dosen Kidneybohnen", "Kidneybohnen"))
        assertTrue(same("frische Petersilie, fein gehackt", "Petersilie"))
        assertTrue(same("1 Bund Basilikum", "Basilikum (frisch)"))
    }

    @Test fun pluralAndUmlautsDoNotMatter() {
        assertTrue(same("Tomate", "Tomaten"))
        assertTrue(same("Zwiebel", "Zwiebeln"))
        assertTrue(same("Äpfel", "Apfel"))
        assertTrue(same("Möhre", "Karotten"))
        assertTrue(same("Eier", "Ei"))
        assertTrue(same("Kartoffeln", "Erdapfel"))
        assertTrue(same("  MILCH ", "milch"))
    }

    @Test fun differentProductsStayDifferent() {
        // Wrongly treating these as stock would make a needed ingredient vanish from the list.
        assertFalse(same("Milch", "Kokosmilch"))
        assertFalse(same("Butter", "Erdnussbutter"))
        assertFalse(same("Paprika", "Paprikapulver"))
        assertFalse(same("Paprika", "Rote Paprika"))
        assertFalse(same("Salz", "Meersalz"))
        assertFalse(same("Reis", "Reisnudeln"))
        assertFalse(same("Tomaten", "Tomatenmark"))
        assertFalse(same("Mehl", "Zucker"))
        assertFalse(same("Hähnchenbrust", "Hähnchenkeule"))
    }

    @Test fun textWithoutAProductNeverMatches() {
        assertFalse(same("", ""))
        assertFalse(same("2 Dosen", "3 Dosen"))
        assertEquals(emptySet<String>(), IngredientMatcher.tokens("500 g"))
    }
}
