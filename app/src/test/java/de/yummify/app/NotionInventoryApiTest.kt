package de.yummify.app

import com.google.gson.Gson
import com.google.gson.JsonObject
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.remote.NotionInventoryApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotionInventoryApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NotionInventoryApi
    private val gson = Gson()
    @Before fun setup() { server = MockWebServer(); server.start(); api = NotionInventoryApi("test-token", baseUrl = server.url("v1").toString().removeSuffix("/")) }
    @After fun close() { server.shutdown() }
    private fun respond(json: String, code: Int = 200) { server.enqueue(MockResponse().setResponseCode(code).setBody(json)) }
    private fun page(id: String, clientId: String) = gson.toJson(mapOf("id" to id, "properties" to NotionInventoryApi.properties(InventoryItem(id = clientId, name = "Milch", quantity = 2.0, unit = "l"))))
    @Test fun paginatesWithoutLosingRows() {
        respond("""{"results":[${page("p1", "c1")}],"has_more":true,"next_cursor":"next"}""")
        respond("""{"results":[${page("p2", "c2")}],"has_more":false}""")
        val result = api.all("source")
        assertEquals(listOf("c1", "c2"), result.map { it.id })
        assertEquals("/v1/data_sources/source/query", server.takeRequest().path)
        val request = server.takeRequest()
        assertTrue(request.body.readUtf8().contains("next"))
        assertEquals("2025-09-03", request.getHeader("Notion-Version"))
    }
    @Test fun recoversExistingCreateByStableId() {
        respond("""{"results":[${page("page", "stable")}],"has_more":false}""")
        respond("""{"id":"page"}""")
        assertEquals("page", api.save("source", InventoryItem(id = "stable", name = "Milch")))
        assertTrue(server.takeRequest().body.readUtf8().contains("stable"))
        val write = server.takeRequest(); assertEquals("PATCH", write.method); assertEquals("/v1/pages/page", write.path)
    }
    @Test fun createsUnderDataSourceAndClearsExpiry() {
        respond("""{"results":[],"has_more":false}"""); respond("""{"id":"new"}""")
        api.save("source", InventoryItem(name = "Milch", expiry = null))
        server.takeRequest()
        val request = server.takeRequest()
        val json = gson.fromJson(request.body.readUtf8(), JsonObject::class.java)
        assertEquals("source", json.getAsJsonObject("parent")["data_source_id"].asString)
        assertTrue(json.getAsJsonObject("properties").getAsJsonObject("Ablaufdatum")["date"].isJsonNull)
    }
    @Test fun pendingDeleteRecoversPageAndArchivesIt() {
        respond("""{"results":[${page("page", "stable")}],"has_more":false}"""); respond("{}")
        api.delete("source", InventoryItem(id = "stable", deleted = true))
        server.takeRequest()
        val archive = server.takeRequest()
        assertEquals("PATCH", archive.method); assertTrue(archive.body.readUtf8().contains("in_trash"))
    }
    @Test fun failedFetchIsNotAnEmptyInventory() {
        respond("""{"message":"Rate limited"}""", 429)
        val error = assertThrows(java.io.IOException::class.java) { api.all("source") }
        assertTrue(error.message!!.contains("429"))
    }
    @Test fun rejectsMultipleSourcesInsteadOfWritingToWrongTable() {
        respond("""{"data_sources":[{"id":"one"},{"id":"two"}]}""")
        assertThrows(IllegalArgumentException::class.java) { api.source("11111111-1111-1111-1111-111111111111") }
    }
    private fun screenshotSchema() = JsonObject().apply {
        NotionInventoryApi.screenshotSchemaTypes.forEach { (key, type) ->
            add(key, JsonObject().apply { addProperty("type", type) })
        }
    }
    @Test fun screenshotFieldsRoundTripWithoutLosingCategories() {
        val mapping = NotionInventoryApi.resolveSchema(screenshotSchema())
        val item = InventoryItem(name = "Dosentomaten", quantity = 12.0, unit = "Dose(n)",
            category = "Konserven, Gemüse", categoryOptions = listOf("Konserven", "Gemüse"),
            location = "Keller", expiry = "2025-12-25")
        val properties = gson.toJsonTree(NotionInventoryApi.properties(item, mapping)).asJsonObject
        assertEquals("Dose(n)", properties.getAsJsonObject("Einheit").getAsJsonObject("select")["name"].asString)
        assertEquals(2, properties.getAsJsonObject("Kategorie").getAsJsonArray("multi_select").size())
        val decoded = NotionInventoryApi.decode(JsonObject().apply { addProperty("id", "page"); add("properties", properties) }, mapping)
        assertEquals(item.name, decoded.name)
        assertEquals(item.quantity, decoded.quantity, 0.0)
        assertEquals(item.expiry, decoded.expiry)
        assertEquals(item.categoryOptions, decoded.categoryOptions)
        assertEquals(item.location, decoded.location)
        assertEquals(item.unit, decoded.unit)
        val edited = gson.toJsonTree(NotionInventoryApi.properties(decoded.copy(category = "Vorrat"), mapping)).asJsonObject
        assertEquals("Vorrat", edited.getAsJsonObject("Kategorie").getAsJsonArray("multi_select")[0].asJsonObject["name"].asString)
    }
    @Test fun screenshotEmptyFieldsStayEmptyAndQuantityDefaultsToZero() {
        val mapping = NotionInventoryApi.resolveSchema(screenshotSchema())
        val properties = gson.toJsonTree(NotionInventoryApi.properties(InventoryItem(name = "Mais", unit = "", category = "", location = ""), mapping)).asJsonObject
        properties.getAsJsonObject("Bestand").add("number", com.google.gson.JsonNull.INSTANCE)
        val item = NotionInventoryApi.decode(JsonObject().apply { addProperty("id", "page"); add("properties", properties) }, mapping)
        assertEquals(0.0, item.quantity, 0.0)
        assertEquals("", item.unit)
        assertEquals("", item.category)
        assertEquals("", item.location)
        assertNull(item.expiry)
        assertNull(item.validate())
    }
    @Test fun sourceAddsOnlyMissingMetadataAndWritesScreenshotNames() {
        respond("""{"data_sources":[{"id":"source"}]}""")
        respond(gson.toJson(mapOf("properties" to screenshotSchema())))
        respond("{}")
        assertEquals("source", api.source("11111111-1111-1111-1111-111111111111"))
        server.takeRequest(); server.takeRequest()
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        val fields = gson.fromJson(patch.body.readUtf8(), JsonObject::class.java).getAsJsonObject("properties")
        assertEquals(setOf("Mindestbestand", "Barcode", "Notizen", "Artikel-ID"), fields.keySet())
        respond("""{"id":"page"}""")
        api.save("source", InventoryItem(pageId = "page", name = "Reis", quantity = 3.0, unit = "Packung"))
        val write = gson.fromJson(server.takeRequest().body.readUtf8(), JsonObject::class.java).getAsJsonObject("properties")
        assertTrue(write.has("Artikel")); assertTrue(write.has("Bestand")); assertTrue(write.has("nächstes MHD"))
        assertFalse(write.has("Name")); assertFalse(write.has("Menge")); assertFalse(write.has("Ablaufdatum"))
    }
    @Test fun legacySchemaRemainsSupportedAndWrongTypesFailBeforeWriting() {
        val legacy = JsonObject().apply { NotionInventoryApi.schemaTypes.forEach { (name, type) -> add(name, JsonObject().apply { addProperty("type", type) }) } }
        assertEquals(NotionInventoryApi.legacyMapping, NotionInventoryApi.resolveSchema(legacy))
        val schema = screenshotSchema()
        schema.getAsJsonObject("Bestand").addProperty("type", "rich_text")
        assertThrows(IllegalArgumentException::class.java) { NotionInventoryApi.resolveSchema(schema) }
    }

    @Test fun readsExternalAndUploadedHeaderImages() {
        val external = gson.fromJson("""{"cover":{"type":"external","external":{"url":"https://example.com/header.jpg"}}}""", JsonObject::class.java)
        val hosted = gson.fromJson("""{"cover":{"type":"file","file":{"url":"https://example.com/notion.jpg"}}}""", JsonObject::class.java)
        assertEquals("https://example.com/header.jpg", NotionInventoryApi.coverUrl(external))
        assertEquals("https://example.com/notion.jpg", NotionInventoryApi.coverUrl(hosted))
        assertNull(NotionInventoryApi.coverUrl(JsonObject()))
    }
    @Test fun schemaChoicesIncludeUnusedOptionsAndPreserveMultiSelect() {
        val schema = screenshotSchema()
        schema.getAsJsonObject("Kategorie").add("multi_select", gson.toJsonTree(mapOf("options" to listOf(mapOf("name" to "Konserven"), mapOf("name" to "Noch unbenutzt")))))
        schema.getAsJsonObject("Lagerort").add("select", gson.toJsonTree(mapOf("options" to listOf(mapOf("name" to "Keller")))))
        val choices = NotionInventoryApi.decodeChoices(schema, NotionInventoryApi.resolveSchema(schema))
        assertEquals(listOf("Konserven", "Noch unbenutzt"), choices.categories)
        assertEquals(listOf("Keller"), choices.locations)
        assertTrue(choices.categoryMultiSelect)
    }
    @Test fun uploadsLocalPhotoAsPageCoverAndKeepsPropertyMapping() {
        val file = java.io.File.createTempFile("inventory", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        try {
            respond("""{"id":"upload"}""")
            respond("""{"status":"uploaded"}""")
            respond("""{"id":"page","cover":{"type":"file","file":{"url":"https://example.com/cover.jpg"}}}""")
            assertEquals("page", api.save("source", InventoryItem(pageId = "page", name = "Milch", localCoverPath = file.absolutePath, coverPending = true)))
            val create = server.takeRequest()
            assertEquals("/v1/file_uploads", create.path)
            assertTrue(create.body.readUtf8().contains("image/jpeg"))
            val send = server.takeRequest()
            assertEquals("/v1/file_uploads/upload/send", send.path)
            assertTrue(send.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
            assertTrue(send.body.readUtf8().contains("name=\"file\""))
            val patch = gson.fromJson(server.takeRequest().body.readUtf8(), JsonObject::class.java)
            assertEquals("upload", patch.getAsJsonObject("cover").getAsJsonObject("file_upload")["id"].asString)
            assertTrue(patch.getAsJsonObject("properties").has("Name"))
            assertEquals("https://example.com/cover.jpg", api.savedCover("page"))
        } finally { file.delete() }
    }
    @Test fun failedPhotoUploadDoesNotPatchPageOrDiscardPendingImage() {
        val file = java.io.File.createTempFile("inventory", ".jpg").apply { writeText("photo") }
        try {
            respond("""{"id":"upload"}"""); respond("{}", 500)
            val item = InventoryItem(pageId = "page", name = "Milch", localCoverPath = file.absolutePath, coverPending = true)
            assertThrows(java.io.IOException::class.java) { api.save("source", item) }
            assertEquals(2, server.requestCount)
            assertTrue(item.coverPending); assertTrue(file.exists())
        } finally { file.delete() }
    }
    @Test fun editingFieldsWithoutPhotoChangeLeavesNotionCoverUntouched() {
        respond("""{"id":"page"}""")
        api.save("source", InventoryItem(pageId = "page", name = "Milch", coverUrl = "https://example.com/cover.jpg"))
        val body = gson.fromJson(server.takeRequest().body.readUtf8(), JsonObject::class.java)
        assertFalse(body.has("cover"))
    }

    @Test fun savesProductImageAsExternalCoverWithoutUpload() {
        respond("""{"id":"product"}""")
        api.save("source", InventoryItem(pageId = "product", name = "Milch", coverUrl = "https://images.openfoodfacts.org/product.jpg", coverPending = true))
        val request = server.takeRequest()
        val payload = gson.fromJson(request.body.readUtf8(), JsonObject::class.java)
        assertEquals("external", payload.getAsJsonObject("cover")["type"].asString)
        assertEquals("https://images.openfoodfacts.org/product.jpg", payload.getAsJsonObject("cover").getAsJsonObject("external")["url"].asString)
        assertEquals(1, server.requestCount)
    }
    @Test fun writesStockStatusOnlyWhenSupportedByExistingSchema() {
        val schema = screenshotSchema()
        schema.add("Status", gson.fromJson("""{"type":"status","status":{"options":[{"name":"Vorhanden"}]}}""", JsonObject::class.java))
        val mapping = NotionInventoryApi.resolveSchema(schema)
        val stock = NotionInventoryApi.properties(InventoryItem(name = "Milch", quantity = 1.0), mapping)
        assertTrue(gson.toJson(stock["Status"]).contains("Vorhanden"))
        assertFalse(NotionInventoryApi.properties(InventoryItem(name = "Milch", quantity = 0.0), mapping).containsKey("Status"))
    }

}
