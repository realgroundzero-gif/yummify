package de.yummify.app

import com.google.gson.Gson
import de.yummify.app.data.remote.NotionRecipeReader
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class NotionRecipeReaderTest {
    private lateinit var server: MockWebServer
    private val gson = Gson()
    private val databaseId = "11111111-1111-1111-1111-111111111111"

    @Before fun setup() { server = MockWebServer(); server.start() }
    @After fun close() { server.shutdown() }

    private fun reader() = NotionRecipeReader("secret-token", databaseId, OkHttpClient(), server.url("v1").toString().removeSuffix("/"))
    private fun respond(body: Any, code: Int = 200) = server.enqueue(MockResponse().setResponseCode(code).setBody(if (body is String) body else gson.toJson(body)))
    private fun text(value: String) = listOf(mapOf("plain_text" to value))
    private fun page(id: String, title: String, extra: Map<String, Any> = emptyMap()) = mapOf("id" to id, "url" to "https://www.notion.so/$id",
        "properties" to mapOf("Name" to mapOf("type" to "title", "title" to text(title))) + extra)
    private fun database() = respond(mapOf("data_sources" to listOf(mapOf("id" to "source"))))

    @Test fun loadsAllPagesThroughTheDataSource() {
        database()
        respond(mapOf("results" to listOf(page("p1", "Pasta")), "has_more" to true, "next_cursor" to "c2"))
        respond(mapOf("results" to listOf(page("p2", "Curry")), "has_more" to false))
        assertEquals(listOf("Pasta", "Curry"), reader().fetchAll().map { it.title })
        val db = server.takeRequest()
        assertEquals("/v1/databases/$databaseId", db.path)
        assertEquals("2025-09-03", db.getHeader("Notion-Version"))
        assertEquals("/v1/data_sources/source/query", server.takeRequest().path)
        assertTrue(server.takeRequest().body.readUtf8().contains("\"start_cursor\":\"c2\""))
    }

    @Test fun aFailedPageIsAnErrorNotAShorterList() {
        database()
        respond(mapOf("results" to listOf(page("p1", "Pasta")), "has_more" to true, "next_cursor" to "c2"))
        respond("""{"message":"boom"}""", 500)
        assertThrows(IOException::class.java) { reader().fetchAll() }
    }

    @Test fun unauthorizedTokenGivesAClearMessage() {
        respond("""{"message":"API token is invalid."}""", 401)
        val error = assertThrows(IOException::class.java) { reader().fetchAll() }
        assertTrue(error.message!!.contains("Token"))
    }

    @Test fun missingColumnsStayUnknownInsteadOfPlaceholderValues() {
        val recipe = NotionRecipeReader.toRecipe(gson.toJsonTree(page("p1", "Pasta", mapOf(
            "Kalorien" to mapOf("type" to "number", "number" to 512),
            "Bewertung" to mapOf("type" to "number", "number" to 4)
        ))).asJsonObject)!!
        assertEquals(512, recipe.calories)
        assertNull(recipe.cookTimeMinutes)
        assertNull(recipe.proteinGrams)
        assertNull(recipe.difficulty)
        // Regression: the first number column (here Kalorien) used to become the portion count.
        assertEquals(2, recipe.defaultServings)
        assertEquals(4.0, recipe.score, 0.0)
        assertEquals("", recipe.imageUrl)
    }

    @Test fun readsTimePortionsStarsAndCover() {
        val json = gson.toJsonTree(page("p1", "Pasta", mapOf(
            "Zeit" to mapOf("type" to "rich_text", "rich_text" to text("30 Min.")),
            "Portionen" to mapOf("type" to "number", "number" to 4),
            "Bewertung" to mapOf("type" to "select", "select" to mapOf("name" to "★★★")),
            "Zutaten" to mapOf("type" to "rich_text", "rich_text" to text("200 g Nudeln\n1 Zwiebel, gewürfelt"))
        )) + mapOf("cover" to mapOf("type" to "external", "external" to mapOf("url" to "https://img/x.jpg")))).asJsonObject
        val recipe = NotionRecipeReader.toRecipe(json)!!
        assertEquals(30, recipe.cookTimeMinutes)
        assertEquals(4, recipe.defaultServings)
        assertEquals(3.0, recipe.score, 0.0)
        assertEquals("https://img/x.jpg", recipe.imageUrl)
        assertEquals(listOf("Nudeln", "Zwiebel, gewürfelt"), recipe.ingredients.map { it.name })
    }

    @Test fun pageBodyIsReadAcrossAllBlockPages() {
        respond(page("p1", "Pasta"))
        fun block(type: String, value: String) = mapOf("type" to type, type to mapOf("rich_text" to text(value)))
        respond(mapOf("results" to listOf(block("heading_2", "Schritte"), block("numbered_list_item", "Wasser kochen")), "has_more" to true, "next_cursor" to "b2"))
        respond(mapOf("results" to listOf(block("bulleted_list_item", "Servieren")), "has_more" to false))
        assertEquals(listOf("## Schritte", "Wasser kochen", "• Servieren"), reader().fetchRecipe("p1").instructions)
        server.takeRequest(); server.takeRequest()
        assertTrue(server.takeRequest().path!!.endsWith("start_cursor=b2"))
    }

    @Test fun ratingFollowsTheColumnTypeWithASingleWrite() {
        database()
        respond(mapOf("properties" to mapOf("Bewertung" to mapOf("type" to "number"))))
        respond("{}")
        reader().updateRating("p1", 4)
        server.takeRequest(); server.takeRequest()
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertEquals("""{"properties":{"Bewertung":{"number":4}}}""", patch.body.readUtf8())
        assertEquals(3, server.requestCount)
    }

    @Test fun plannedDateIsSkippedWhenTheColumnDoesNotExist() {
        database()
        respond(mapOf("properties" to mapOf("Name" to mapOf("type" to "title"))))
        assertFalse(reader().updatePlannedDate("p1", LocalDate.of(2026, 10, 7)))
        assertEquals(2, server.requestCount)
    }

    @Test fun plannedDateIsWrittenAndCleared() {
        database()
        respond(mapOf("properties" to mapOf("Geplant am" to mapOf("type" to "date"))))
        respond("{}"); respond("{}")
        val reader = reader()
        assertTrue(reader.updatePlannedDate("p1", LocalDate.of(2026, 10, 7)))
        assertTrue(reader.updatePlannedDate("p1", null))
        server.takeRequest(); server.takeRequest()
        assertTrue(server.takeRequest().body.readUtf8().contains("\"start\":\"2026-10-07\""))
        assertTrue(server.takeRequest().body.readUtf8().contains("\"date\":null"))
    }
}
