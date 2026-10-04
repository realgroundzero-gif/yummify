package de.yummify.app

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.yummify.app.data.model.RecipeDraft
import de.yummify.app.data.remote.NotionRecipeApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class NotionRecipeApiTest {
    private val database = "11111111-1111-1111-1111-111111111111"
    private fun draft() = RecipeDraft("Pasta \"Rustica\"", "Frisch & einfach", 2, "Pasta", "1,5 kg Tomaten\n200 g Nudeln", "Tomaten schneiden.\nNudeln kochen.", "https://example.org/pasta.jpg")
    private fun schema(categoryType: String = "multi_select") = JsonParser.parseString("""{"Titel":{"type":"title"},"Beschreibung":{"type":"rich_text"},"Portionen":{"type":"number"},"Zutaten":{"type":"rich_text"},"Kategorie":{"type":"$categoryType"}}""").asJsonObject
    private fun withServer(block: (MockWebServer, NotionRecipeApi) -> Unit) = MockWebServer().use { server ->
        server.start(); block(server, NotionRecipeApi("test-token", baseUrl = server.url("v1").toString().removeSuffix("/")))
    }
    private fun enqueueSchema(server: MockWebServer, schema: JsonObject = schema()) {
        server.enqueue(MockResponse().setBody("""{"data_sources":[{"id":"source"}]}"""))
        server.enqueue(MockResponse().setBody("""{"properties":$schema}"""))
    }
    @Test fun createsRecipeWithSchemaTitleIngredientsPortionsStepsAndCover() = withServer { server, api ->
        enqueueSchema(server); server.enqueue(MockResponse().setBody("""{"id":"new-recipe"}"""))
        assertEquals("new-recipe", api.create(database, draft()))
        assertEquals("/v1/databases/$database", server.takeRequest().path)
        assertEquals("/v1/data_sources/source", server.takeRequest().path)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/pages", request.path)
        assertEquals("2025-09-03", request.getHeader("Notion-Version"))
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals("source", body.getAsJsonObject("parent")["data_source_id"].asString)
        val properties = body.getAsJsonObject("properties")
        assertEquals(draft().title, properties.getAsJsonObject("Titel").getAsJsonArray("title")[0].asJsonObject.getAsJsonObject("text")["content"].asString)
        assertEquals(2, properties.getAsJsonObject("Portionen")["number"].asInt)
        assertEquals(draft().ingredients, properties.getAsJsonObject("Zutaten").getAsJsonArray("rich_text")[0].asJsonObject.getAsJsonObject("text")["content"].asString)
        assertEquals(2, body.getAsJsonArray("children").size())
        assertEquals(draft().imageUrl, body.getAsJsonObject("cover").getAsJsonObject("external")["url"].asString)
    }
    @Test fun missingRequiredFieldStopsBeforeCreatingAnyPage() = withServer { server, api ->
        val schema = schema().apply { remove("Zutaten") }; enqueueSchema(server, schema)
        assertThrows(IllegalArgumentException::class.java) { api.create(database, draft()) }
        assertEquals(2, server.requestCount)
    }
    @Test fun wrongFieldTypeStopsBeforeWriting() = withServer { server, api ->
        enqueueSchema(server, schema("formula"))
        assertThrows(IllegalArgumentException::class.java) { api.create(database, draft()) }
        assertEquals(2, server.requestCount)
    }
    @Test fun multipleSourcesCannotCreateRecipeInWrongDatabase() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("""{"data_sources":[{"id":"one"},{"id":"two"}]}"""))
        assertThrows(IllegalArgumentException::class.java) { api.create(database, draft()) }
        assertEquals(1, server.requestCount)
    }
    @Test fun failedCreateDoesNotReturnRecipeIdOrRetryWrite() = withServer { server, api ->
        enqueueSchema(server); server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))
        assertThrows(IOException::class.java) { api.create(database, draft()) }
        assertEquals(3, server.requestCount)
    }
    @Test fun validationRejectsEmptyInstructionsInvalidPortionsAndInsecureImage() = withServer { server, api ->
        assertThrows(IllegalArgumentException::class.java) { api.create(database, draft().copy(instructions = "")) }
        assertThrows(IllegalArgumentException::class.java) { api.create(database, draft().copy(servings = 0)) }
        assertThrows(IllegalArgumentException::class.java) { api.create(database, draft().copy(imageUrl = "http://example.org/image.jpg")) }
        assertEquals(0, server.requestCount)
    }
    @Test fun longIngredientTextIsSplitWithinNotionRichTextLimits() {
        val ingredients = "a".repeat(4500)
        val body = NotionRecipeApi.payload("source", schema(), draft().copy(ingredients = ingredients, category = "", description = "", imageUrl = ""))
        val json = com.google.gson.Gson().toJsonTree(body).asJsonObject
        val chunks = json.getAsJsonObject("properties").getAsJsonObject("Zutaten").getAsJsonArray("rich_text")
        assertEquals(3, chunks.size())
        assertEquals(ingredients, chunks.joinToString("") { it.asJsonObject.getAsJsonObject("text")["content"].asString })
        assertFalse(json.has("cover"))
        assertFalse(json.getAsJsonObject("properties").has("Kategorie"))
    }
}
