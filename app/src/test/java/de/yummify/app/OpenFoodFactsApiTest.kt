package de.yummify.app

import de.yummify.app.data.remote.OpenFoodFactsApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class OpenFoodFactsApiTest {
    private fun withServer(block: (MockWebServer, OpenFoodFactsApi) -> Unit) {
        MockWebServer().use { server ->
            server.start()
            block(server, OpenFoodFactsApi(baseUrl = server.url("/").toString().removeSuffix("/")))
        }
    }
    @Test fun prefersGermanNameAndPreservesLeadingZerosAndAttribution() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("""{"status":1,"product":{"product_name":"Oats","product_name_de":"Haferflocken","brands":"Alnatura","quantity":"500 g","categories_tags":["en:cereals"],"image_front_url":"https://images.openfoodfacts.org/front.jpg","ingredients_text_de":"Vollkornhafer","nutriments":{"proteins_100g":13}}}"""))
        val p = api.product(" 0012345678901 ")!!
        assertEquals("Alnatura Haferflocken", p.name)
        assertEquals(500.0, p.quantity!!, 0.0)
        assertEquals("g", p.unit)
        assertEquals("Vorrat & Trockenwaren", p.suggestedCategory(listOf("Vorrat & Trockenwaren", "Tiefkühl")))
        assertTrue(p.notes().contains("Vollkornhafer"))
        assertTrue(p.notes().contains("13 g Protein"))
        assertTrue(p.notes().contains(p.sourceUrl))
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/api/v2/product/0012345678901.json?"))
        assertTrue(request.getHeader("User-Agent")!!.contains("Yummify/"))
        assertNull(request.getHeader("Authorization"))
    }
    @Test fun missingProductAnd404UseManualFallback() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("""{"status":0}"""))
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"status":0}"""))
        assertNull(api.product("12345678"))
        assertNull(api.product("12345678"))
    }
    @Test fun partialAndNullProductFieldsDoNotInventValues() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("""{"status":1,"product":{"product_name_de":null,"product_name":"Milch","brands":"","quantity":"6 x 1 l","image_url":"http://unsafe/image.jpg","nutriments":null}}"""))
        val p = api.product("12345678")!!
        assertEquals("Milch", p.name)
        assertNull(p.quantity)
        assertNull(p.imageUrl)
        assertNull(p.suggestedCategory(listOf("Gemüse")))
    }
    @Test fun structuredQuantityFallbackAndDecimalCommaWork() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("""{"status":1,"product":{"product_name":"Saft","product_quantity":1.5,"product_quantity_unit":"l"}}"""))
        assertEquals(1.5, api.product("12345678")!!.quantity!!, 0.0)
        assertEquals(0.5 to "kg", OpenFoodFactsApi.parseQuantity("0,5 kg"))
        assertEquals(1.0 to "l", OpenFoodFactsApi.parseQuantity("1 Liter"))
        assertEquals(330.0 to "ml", OpenFoodFactsApi.parseQuantity("33 cl"))
        assertNull(OpenFoodFactsApi.parseQuantity("0 g"))
        assertNull(OpenFoodFactsApi.parseQuantity("2 x 500 g"))
    }
    @Test fun networkAndMalformedResponsesAreNotMissingProducts() = withServer { server, api ->
        server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))
        assertThrows(IOException::class.java) { api.product("12345678") }
        server.enqueue(MockResponse().setBody("not-json"))
        assertThrows(RuntimeException::class.java) { api.product("12345678") }
    }
    @Test fun rejectsNonProductBarcodesWithoutSendingRequest() = withServer { server, api ->
        assertThrows(IllegalArgumentException::class.java) { api.product("https://example.org") }
        assertThrows(IllegalArgumentException::class.java) { api.product("123") }
        assertEquals(0, server.requestCount)
    }
    @Test fun extractsDedicatedNutritionIngredientsAndProductUrl() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("""{"status":1,"product":{"product_name":"Pesto","ingredients_text_de":"Basilikum, Öl","ingredients_text":"Basil, oil","nutriments":{"energy-kcal_100g":460,"fat_100g":45.5,"carbohydrates_100g":0,"proteins_100g":"5.2"}}}"""))
        val product = api.product("4056489202974")!!
        assertEquals(460.0, product.calories!!, 0.0)
        assertEquals(45.5, product.fat!!, 0.0)
        assertEquals(0.0, product.carbohydrates!!, 0.0)
        assertEquals(5.2, product.protein!!, 0.0)
        assertEquals("Basilikum, Öl", product.ingredients)
        assertEquals("https://world.openfoodfacts.org/product/4056489202974", product.sourceUrl)
    }
    @Test fun missingAndInvalidNutritionIsUnknownAndIngredientsUseLanguageFallback() = withServer { server, api ->
        server.enqueue(MockResponse().setBody("""{"status":1,"product":{"ingredients_text_de":"","ingredients_text":"Rice","nutriments":{"energy-kcal_100g":null,"fat_100g":"NaN","carbohydrates_100g":-1,"proteins_100g":"unknown"}}}"""))
        val product = api.product("12345678")!!
        assertNull(product.calories)
        assertNull(product.fat)
        assertNull(product.carbohydrates)
        assertNull(product.protein)
        assertEquals("Rice", product.ingredients)
    }

}
