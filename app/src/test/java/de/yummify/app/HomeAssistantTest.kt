package de.yummify.app

import com.google.gson.JsonParser
import de.yummify.app.data.backup.StorageLayout
import de.yummify.app.data.model.HomeAssistantExport
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.remote.HomeAssistantApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class HomeAssistantTest {
    private fun item(id: String, name: String, amount: String = "", checked: Boolean = false) =
        ShoppingItem(id, name, amount, "Vorrat", null, null, checked, null, null)

    @Test fun exportSendsOpenItemsWithAmountAsDescription() {
        val list = listOf(item("1", "Milch", "2 l"), item("2", "Salz"), item("3", "Brot", "1 Stk", checked = true), item("4", " "))
        val plan = HomeAssistantExport.plan(list, emptyMap(), includeSent = false)
        assertEquals(listOf("Milch", "Salz"), plan.map { it.name })
        assertEquals("2 l", plan[0].description)
        assertNull(plan[1].description)
    }

    @Test fun alreadySentItemsAreSkippedUntilTheyChange() {
        val list = listOf(item("1", "Milch", "2 l"), item("2", "Eier", "6 Stk"))
        val sent = HomeAssistantExport.plan(list, emptyMap(), true).associate { it.itemId to it.signature }
        assertTrue(HomeAssistantExport.plan(list, sent, includeSent = false).isEmpty())
        assertEquals(2, HomeAssistantExport.plan(list, sent, includeSent = true).size)
        val changed = listOf(item("1", "Milch", "3 l"), item("2", "Eier", "6 Stk"))
        assertEquals(listOf("Milch"), HomeAssistantExport.plan(changed, sent, includeSent = false).map { it.name })
    }

    @Test fun plainHttpIsOnlyAcceptedInTheHomeNetwork() {
        assertEquals("http://homeassistant.local:8123", HomeAssistantApi.normalize("homeassistant.local:8123/"))
        assertEquals("http://192.168.1.5:8123", HomeAssistantApi.normalize("http://192.168.1.5:8123"))
        assertEquals("http://homeassistant:8123", HomeAssistantApi.normalize("homeassistant:8123"))
        assertEquals("https://abc.ui.nabu.casa", HomeAssistantApi.normalize("https://abc.ui.nabu.casa/"))
        assertThrows(IllegalArgumentException::class.java) { HomeAssistantApi.normalize("http://example.com") }
        assertThrows(IllegalArgumentException::class.java) { HomeAssistantApi.normalize("http://8.8.8.8:8123") }
        assertThrows(IllegalArgumentException::class.java) { HomeAssistantApi.normalize("http://172.32.0.1") }
        assertTrue(HomeAssistantApi.isLocalHost("172.16.0.1"))
        assertTrue(HomeAssistantApi.isLocalHost("100.101.1.1"))
        assertThrows(IllegalArgumentException::class.java) { HomeAssistantApi.normalize("   ") }
    }

    private fun withServer(block: (MockWebServer, HomeAssistantApi) -> Unit) = MockWebServer().use { server ->
        server.start(); block(server, HomeAssistantApi("http://127.0.0.1:${server.port}", "secret-token"))
    }

    @Test fun listsTodoEntitiesWithBringFirst() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("""[{"entity_id":"light.kitchen","attributes":{}},
            {"entity_id":"todo.shopping_list","attributes":{"friendly_name":"Shopping List"}},
            {"entity_id":"todo.bring_einkauf","attributes":{"friendly_name":"Bring! Einkauf"}}]"""))
        val lists = api.todoLists()
        assertEquals(listOf("todo.bring_einkauf", "todo.shopping_list"), lists.map { it.entityId })
        assertTrue(lists[0].isBring)
        val request = server.takeRequest()
        assertEquals("/api/states", request.path)
        assertEquals("Bearer secret-token", request.getHeader("Authorization"))
    }

    @Test fun addItemPostsNameAndDescriptionToTheTodoService() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("[]"))
        api.addItem("todo.bring_einkauf", " Milch ", "2 l")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/services/todo/add_item", request.path)
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals("todo.bring_einkauf", body["entity_id"].asString)
        assertEquals("Milch", body["item"].asString)
        assertEquals("2 l", body["description"].asString)
    }

    @Test fun descriptionIsLeftOutWhenThereIsNoAmount() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("[]"))
        api.addItem("todo.x", "Salz", null)
        assertFalse(JsonParser.parseString(server.takeRequest().body.readUtf8()).asJsonObject.has("description"))
    }

    @Test fun errorsAreExplainedInGerman() = withServer { server, api ->
        server.enqueue(MockResponse().setResponseCode(401))
        assertTrue(assertThrows(IOException::class.java) { api.check() }.message!!.contains("Token"))
        server.enqueue(MockResponse().setResponseCode(404))
        assertTrue(assertThrows(IOException::class.java) { api.addItem("todo.x", "Milch", null) }.message!!.contains("todo.add_item"))
        server.enqueue(MockResponse().setBody("""{"message":"API running."}"""))
        api.check()
        server.enqueue(MockResponse().setBody("<html>no</html>"))
        assertThrows(IllegalArgumentException::class.java) { api.check() }
    }

    @Test fun tokenNeverGoesIntoAnExportByDefault() {
        assertTrue("home_assistant_token" in StorageLayout.secretUserKeys)
        assertTrue("notion_token" in StorageLayout.secretUserKeys)
    }
}
