package de.yummify.app

import com.google.gson.JsonParser
import de.yummify.app.data.remote.DropdownOption
import de.yummify.app.data.remote.NotionOptionsApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class NotionOptionsApiTest {
    private val database = "11111111-1111-1111-1111-111111111111"
    private val schema = """{"properties":{"Name":{"type":"title"},"Kategorie":{"type":"multi_select","multi_select":{"options":[{"id":"a1","name":"Pasta","color":"red"},{"id":"b2","name":"Salat","color":"green"}]}},
        "Einheit":{"type":"rich_text"},"Lagerort":{"type":"select","select":{"options":[]}},"Status":{"type":"status","status":{"options":[{"id":"s","name":"Neu"}]}}}}"""
    private fun withServer(block: (MockWebServer, NotionOptionsApi) -> Unit) = MockWebServer().use { server ->
        server.start(); block(server, NotionOptionsApi("test-token", baseUrl = server.url("v1").toString().removeSuffix("/")))
    }

    @Test fun listsOnlySelectAndMultiSelectColumns() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("""{"data_sources":[{"id":"source"}]}"""))
        server.enqueue(MockResponse().setBody(schema))
        val loaded = api.load(database)
        assertEquals("source", loaded.source)
        assertEquals(listOf("Kategorie", "Lagerort"), loaded.fields.map { it.name })
        assertEquals(listOf("Pasta", "Salat"), loaded.fields.first().options.map { it.name })
        assertEquals("multi_select", loaded.fields.first().type)
    }

    @Test fun addingKeepsExistingOptionsByIdAndWritesBackToNotion() = withServer { server, api ->
        server.enqueue(MockResponse().setBody(schema))                       // current state
        server.enqueue(MockResponse().setBody("{}"))                         // PATCH
        server.enqueue(MockResponse().setBody(schema.replace("""{"id":"b2","name":"Salat","color":"green"}""",
            """{"id":"b2","name":"Salat","color":"green"},{"id":"c3","name":"Dessert","color":"blue"}""")))  // read back
        val options = api.addOptions("source", "Kategorie", listOf(" Dessert ", "salat"))
        assertEquals(listOf("Pasta", "Salat", "Dessert"), options.map { it.name })
        assertEquals("c3", options.last().id)
        server.takeRequest()
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertEquals("/v1/data_sources/source", patch.path)
        val sent = JsonParser.parseString(patch.body.readUtf8()).asJsonObject.getAsJsonObject("properties")
            .getAsJsonObject("Kategorie").getAsJsonObject("multi_select").getAsJsonArray("options")
        assertEquals(3, sent.size())
        assertEquals("a1", sent[0].asJsonObject["id"].asString)
        assertEquals("b2", sent[1].asJsonObject["id"].asString)
        assertEquals("Dessert", sent[2].asJsonObject["name"].asString)
        assertFalse(sent[2].asJsonObject.has("id"))
    }

    @Test fun nothingIsSentWhenEveryEntryExists() = withServer { server, api ->
        server.enqueue(MockResponse().setBody(schema))
        assertEquals(2, api.addOptions("source", "Kategorie", listOf("pasta")).size)
        assertEquals(1, server.requestCount)
    }

    @Test fun rejectsUnusableEntriesAndMissingColumns() = withServer { server, api ->
        assertNotNull(NotionOptionsApi.validate(" "))
        assertNotNull(NotionOptionsApi.validate("Obst, Gemüse"))
        assertNotNull(NotionOptionsApi.validate("x".repeat(101)))
        assertNull(NotionOptionsApi.validate("Kräuter & Gewürze"))
        server.enqueue(MockResponse().setBody(schema))
        assertThrows(java.io.IOException::class.java) { api.addOptions("source", "Gibt es nicht", listOf("x")) }
        assertThrows(IllegalArgumentException::class.java) { NotionOptionsApi.mergedOptions(listOf(DropdownOption("1", "A")), listOf("B,C")) }
    }

    @Test fun notionErrorsAreReported() = withServer { server, api ->
        server.enqueue(MockResponse().setBody(schema))
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Insufficient permissions"}"""))
        val error = assertThrows(java.io.IOException::class.java) { api.addOptions("source", "Lagerort", listOf("Keller")) }
        assertTrue(error.message!!.contains("403"))
    }
}
