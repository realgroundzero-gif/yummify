package de.yummify.app

import android.content.Context
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.remote.NotionInventoryApi
import de.yummify.app.data.repository.InventoryRepository
import de.yummify.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InventoryRobustnessTest {
    private lateinit var context: Context
    private val prefs get() = context.getSharedPreferences("yummify_inventory", Context.MODE_PRIVATE)
    private var server: MockWebServer? = null

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        prefs.edit().clear().commit()
        context.getSharedPreferences("yummify_user_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        UserPreferencesRepository.getInstance(context).apply { saveNotionConfig("", "", ""); setAutoSyncEnabled(false) }
    }
    @After fun close() { server?.shutdown() }

    @Test fun corruptStorageDoesNotCrashAndIsKeptAsBackup() {
        prefs.edit().putString("items_local", "{not json").commit()
        val repository = InventoryRepository(context)
        assertTrue(repository.items.value.isEmpty())
        assertTrue(repository.syncState.value.message.contains("beschädigt"))
        assertEquals("{not json", prefs.all.entries.single { it.key.startsWith("items_local_corrupt_") }.value)
    }

    @Test fun itemsFromOlderVersionsWithoutNewFieldsCanBeEdited() = runBlocking {
        // Written before categoryOptions and product fields existed.
        prefs.edit().putString("items_local", """[{"id":"a","name":"Mehl","quantity":1.0,"unit":"kg","category":"Vorrat","location":"Schrank","minimum":0.0,"barcode":"","notes":"","dirty":true,"deleted":false}]""").commit()
        val repository = InventoryRepository(context)
        val item = repository.items.value.single()
        assertEquals(emptyList<String>(), item.categoryOptions)
        assertNull(item.validate())
        repository.save(item.copy(quantity = 2.0))
        assertEquals(2.0, InventoryRepository(context).items.value.single().quantity, 0.0)
    }

    @Test fun replacedPhotoIsDeletedOnceNoItemUsesIt() = runBlocking {
        val old = File(context.filesDir, "old.jpg").apply { writeText("x") }
        val new = File(context.filesDir, "new.jpg").apply { writeText("y") }
        val repository = InventoryRepository(context)
        val item = InventoryItem(name = "Käse", localCoverPath = old.absolutePath, coverPending = true)
        repository.save(item)
        repository.save(item.copy(localCoverPath = new.absolutePath))
        assertFalse(old.exists())
        assertTrue(new.exists())
    }

    @Test fun editsAreNotBlockedWhileAnUploadIsRunning() = runBlocking {
        val uploadStarted = CountDownLatch(1)
        val releaseUpload = CountDownLatch(1)
        val schema = """{"properties":{"Name":{"type":"title"},"Menge":{"type":"number"},"Einheit":{"type":"rich_text"},"Kategorie":{"type":"rich_text"},"Lagerort":{"type":"rich_text"},"Mindestbestand":{"type":"number"},"Ablaufdatum":{"type":"date"},"Barcode":{"type":"rich_text"},"Notizen":{"type":"rich_text"},"Artikel-ID":{"type":"rich_text"}}}"""
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.path!!.startsWith("/v1/databases/") -> MockResponse().setBody("""{"data_sources":[{"id":"source"}]}""")
                    request.path == "/v1/data_sources/source" -> MockResponse().setBody(schema)
                    request.path == "/v1/data_sources/source/query" -> MockResponse().setBody("""{"results":[],"has_more":false}""")
                    request.path == "/v1/pages" -> {
                        uploadStarted.countDown()
                        releaseUpload.await(10, TimeUnit.SECONDS)
                        MockResponse().setBody("""{"id":"page-1"}""")
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        val baseUrl = server!!.url("v1").toString().removeSuffix("/")
        UserPreferencesRepository.getInstance(context).saveNotionConfig("token", "", "11111111-1111-1111-1111-111111111111")
        val repository = InventoryRepository(context) { NotionInventoryApi(it, OkHttpClient(), baseUrl) }
        val item = InventoryItem(id = "milk", name = "Milch", quantity = 1.0, unit = "l")
        repository.save(item)
        val sync = async(kotlinx.coroutines.Dispatchers.IO) { repository.sync() }
        assertTrue(uploadStarted.await(10, TimeUnit.SECONDS))
        // The save must finish while the upload is still waiting for Notion.
        withTimeout(2_000) { repository.save(item.copy(quantity = 3.0)) }
        releaseUpload.countDown()
        sync.await()
        var stored = repository.items.value.single()
        repeat(50) { if (stored.pageId == null) { delay(20); stored = repository.items.value.single() } }
        assertEquals(3.0, stored.quantity, 0.0)
        assertEquals("page-1", stored.pageId)
        assertTrue("Changed during upload, so it must be sent again", stored.dirty)
    }
}
