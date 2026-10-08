package de.yummify.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.Recipe
import de.yummify.app.share.RecipeCardRenderer
import de.yummify.app.share.RecipeShareText
import de.yummify.app.share.RecipeSharer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale

private fun recipe(
    title: String = "Tomatensuppe",
    servings: Int = 2,
    minutes: Int? = 30,
    ingredients: List<Ingredient> = listOf(Ingredient("Tomaten", 400.0, "g"), Ingredient("Sahne", 0.25, "l"), Ingredient("Salz", 0.0, "")),
    steps: List<String> = listOf("Tomaten **würfeln**.", "Alles kochen."),
    image: String = ""
) = Recipe("id", title, "", image, minutes, null, null, null, null, "suppe", listOf("Suppe"), ingredients = ingredients,
    instructions = steps, defaultServings = servings, notionUrl = "https://www.notion.so/private")

class RecipeShareTextTest {
    private var locale: Locale = Locale.getDefault()
    @Before fun german() { locale = Locale.getDefault(); Locale.setDefault(Locale.GERMANY) }
    @After fun restore() { Locale.setDefault(locale) }

    @Test fun textFollowsTheAgreedStructureWithTheSelectedServings() {
        val text = RecipeShareText.build(recipe(), servings = 4)
        assertEquals("""🍽️ Tomatensuppe
⏱️ Zeit: 30 Min. | 👥 Portionen: 4

Zutaten:
• 800 g Tomaten
• 0,5 l Sahne
• Salz

Zubereitung:
1. Tomaten würfeln.
2. Alles kochen.

(Geteilt via Yummify)""", text)
    }

    @Test fun missingTimeIngredientsAndStepsLeaveOutTheirLines() {
        val text = RecipeShareText.build(recipe(minutes = null, ingredients = emptyList(), steps = emptyList()), servings = 2)
        assertEquals("🍽️ Tomatensuppe\n👥 Portionen: 2\n\n(Geteilt via Yummify)", text)
    }

    @Test fun privateNotionLinkIsNeverShared() {
        assertFalse(RecipeShareText.build(recipe(), 2).contains("notion"))
    }

    @Test fun stepsAreNumberedHeadingsRestartAndMarkupIsRemoved() {
        val steps = RecipeShareText.steps(listOf("## Vorbereiten", "1. Zwiebel *fein* hacken", "• Messer scharf", "Anbraten", "### Soße", "Köcheln `10` Min.", "> Tipp", "💡 Gut für Reste"))
        assertEquals(listOf("Vorbereiten:", "1. Zwiebel fein hacken", "• Messer scharf", "2. Anbraten", "", "Soße:", "1. Köcheln 10 Min.", "„Tipp“", "💡 Gut für Reste"), steps)
    }

    @Test fun amountsAreScaledWithoutRoundingErrors() {
        val ingredient = Ingredient("Mehl", 0.3333, "kg")
        assertEquals("1 kg Mehl", RecipeShareText.ingredientLine(ingredient, 3.0))
        assertEquals("2 Prise Salz", RecipeShareText.ingredientLine(Ingredient("Salz", 1.0, "Prise"), 2.0))
        assertEquals("Salz", RecipeShareText.ingredientLine(Ingredient("Salz", 0.0, ""), 2.0))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecipeShareFileTest {
    private lateinit var context: Context
    private fun sharesDir() = File(context.cacheDir, "shares")
    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        sharesDir().deleteRecursively()
        // FileProvider caches the root folders per authority for the whole JVM, but Robolectric
        // creates a new cache folder for every test.
        FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }.get(null).let { (it as MutableMap<*, *>).clear() }
    }

    private fun decode(file: File) = BitmapFactory.decodeFile(file.absolutePath)

    @Test fun cardIs1080PixelsWideAndSharedThroughTheFileProvider() = runBlocking {
        val content = RecipeSharer.prepare(context, recipe(), 4) { null }
        val uri = content.imageUri!!
        assertEquals("${context.packageName}.inventory.files", uri.authority)
        val file = sharesDir().listFiles()!!.single()
        assertTrue(uri.path!!.endsWith(file.name))
        val bitmap = decode(file)
        assertEquals(1080, bitmap.width)
        assertTrue(bitmap.height > 400)
        assertTrue(content.text.contains("• 800 g Tomaten"))
    }

    @Test fun olderSharesAreDeletedBeforeANewOneIsCreated() = runBlocking {
        sharesDir().mkdirs()
        val old = File(sharesDir(), "alt.png").apply { writeText("x") }
        RecipeSharer.prepare(context, recipe(), 2) { null }
        assertFalse(old.exists())
        assertEquals(1, sharesDir().listFiles()!!.size)
    }

    @Test fun coverMakesTheCardTallerAndALoadFailureStillShares() = runBlocking {
        val cover = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        RecipeSharer.prepare(context, recipe(image = "https://example.com/a.jpg"), 2) { cover }
        val withCover = decode(sharesDir().listFiles()!!.single()).height
        val content = RecipeSharer.prepare(context, recipe(image = "https://example.com/a.jpg"), 2) { null }
        val without = decode(sharesDir().listFiles()!!.single()).height
        assertNotNull(content.imageUri)
        assertTrue("cover $withCover vs none $without", withCover > without)
    }

    @Test fun longIngredientListsAreCutAndCounted() {
        val many = (1..30).map { Ingredient("Zutat $it", it.toDouble(), "g") }
        val (rows, hidden) = RecipeCardRenderer.ingredientRows(recipe(ingredients = many), 2)
        assertEquals(20, rows.size)
        assertEquals(10, hidden)
        assertTrue(RecipeCardRenderer.render(recipe(ingredients = many), 2, null).height < 4000)
    }
}
