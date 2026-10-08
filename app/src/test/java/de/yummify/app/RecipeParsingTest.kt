package de.yummify.app

import de.yummify.app.data.repository.RecipeRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeParsingTest {
    private val id = "0123456789abcdef0123456789abcdef"
    private val uuid = "01234567-89ab-cdef-0123-456789abcdef"

    @Test fun notionIdsAreRecognizedInEveryCommonForm() {
        assertEquals(uuid, RecipeRepository.formatNotionId(id))
        assertEquals(uuid, RecipeRepository.formatNotionId(" $uuid "))
        assertEquals(uuid, RecipeRepository.formatNotionId("https://www.notion.so/workspace/$id?v=abc"))
        // Regression: a title in front of the ID used to produce an invalid ID.
        assertEquals(uuid, RecipeRepository.formatNotionId("https://www.notion.so/workspace/Rezepte-$id?v=abc"))
        assertEquals(uuid, RecipeRepository.formatNotionId("https://www.notion.so/Meine-Rezepte-${id.uppercase()}"))
        assertEquals(uuid, RecipeRepository.formatNotionId("https://team.notion.site/$id/"))
        assertEquals("", RecipeRepository.formatNotionId("  "))
        assertEquals("keine-id", RecipeRepository.formatNotionId("keine-id"))
    }

    private fun parsed(line: String) = RecipeRepository.parseIngredientLine(line).let { Triple(it.amount, it.unit, it.name) }

    @Test fun quantitiesFractionsAndRangesAreParsed() {
        assertEquals(Triple(200.0, "g", "Mehl"), parsed("200 g Mehl"))
        assertEquals(Triple(200.0, "g", "Mehl"), parsed("200g Mehl"))
        assertEquals(Triple(1.5, "kg", "Tomaten"), parsed("1,5 kg Tomaten"))
        assertEquals(Triple(0.5, "TL", "Salz"), parsed("1/2 TL Salz"))
        assertEquals(Triple(1.5, "EL", "Öl"), parsed("1 1/2 EL Öl"))
        assertEquals(Triple(0.5, "Bund", "Petersilie"), parsed("½ Bund Petersilie"))
        assertEquals(Triple(1.25, "", "Tassen Milch"), parsed("1¼ Tassen Milch"))
        assertEquals(Triple(3.0, "Zehen", "Knoblauch"), parsed("2-3 Zehen Knoblauch"))
        assertEquals(Triple(2.0, "", "Eier"), parsed("- 2 Eier"))
        assertEquals(Triple(0.0, "", "Salz und Pfeffer"), parsed("Salz und Pfeffer"))
        assertEquals(Triple(0.0, "", "1/0 Tasse"), parsed("1/0 Tasse"))
    }

    @Test fun linesWinOverCommasSoDescriptionsStayTogether() {
        val lines = RecipeRepository.parseIngredients("1 Zwiebel, gewürfelt\n200 g Reis\n\n")
        assertEquals(listOf("Zwiebel, gewürfelt", "Reis"), lines.map { it.name })
        val singleLine = RecipeRepository.parseIngredients("200 g Reis, 1,5 l Brühe, Salz")
        assertEquals(listOf("Reis", "Brühe", "Salz"), singleLine.map { it.name })
        assertEquals(1.5, singleLine[1].amount, 0.0)
    }
}
