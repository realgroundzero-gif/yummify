package de.yummify.app

import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.repository.InventoryRepository
import de.yummify.app.data.repository.MealPlanRepository
import de.yummify.app.data.repository.UserPreferencesRepository
import de.yummify.app.widget.MealPlannerWidgetProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetAndOfflineInventoryTest {
    private lateinit var context: Context
    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("yummify_inventory", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("yummify_user_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        UserPreferencesRepository.getInstance(context).apply { saveNotionConfig("", "", ""); setAutoSyncEnabled(false) }
    }
    @Test fun firstWidgetLoadAndRemoteViewsInflationWork() {
        // Regression: loadMeals used to write _plannedMeals before it was initialized.
        val repository = MealPlanRepository.getInstance(context)
        assertNotNull(repository.plannedMeals.value)
        val views = MealPlannerWidgetProvider.buildViews(context, 42)
        val inflated = views.apply(context, FrameLayout(context))
        assertTrue(inflated.findViewById<TextView>(R.id.widget_kw_text).text.startsWith("KW "))
        assertNotNull(inflated.findViewById<TextView>(R.id.day_mo_meal).text)
    }
    @Test fun offlineEditsSurviveRestartAndKeepDeleteTombstone() = runBlocking {
        val repository = InventoryRepository(context)
        val item = InventoryItem(id = "milk", name = "Milch", quantity = 2.0, unit = "l")
        repository.save(item)
        repository.adjust("milk", -0.5)
        val restarted = InventoryRepository(context)
        assertEquals(1.5, restarted.items.value.single().quantity, 0.0)
        repository.remove("milk")
        assertTrue(InventoryRepository(context).items.value.isEmpty())
        val raw = context.getSharedPreferences("yummify_inventory", Context.MODE_PRIVATE).getString("items_local", "")!!
        assertTrue(raw.contains("\"deleted\":true"))
    }
    @Test fun purchaseReplayDoesNotDoubleStock() = runBlocking {
        val item = InventoryItem(name = "Milch", quantity = 2.0, unit = "l")
        InventoryRepository(context).addPurchased(item, "shopping-row")
        val restarted = InventoryRepository(context)
        restarted.addPurchased(item, "shopping-row")
        assertEquals(2.0, restarted.items.value.single().quantity, 0.0)
    }
    @Test fun insufficientRecipeDoesNotPartiallyDeductInventory() = runBlocking {
        val repository = InventoryRepository(context)
        repository.save(InventoryItem(name = "Mehl", quantity = 100.0, unit = "g"))
        repository.save(InventoryItem(name = "Milch", quantity = 1.0, unit = "l"))
        try {
            repository.consume(listOf(Ingredient("Milch", 200.0, "ml"), Ingredient("Mehl", 200.0, "g")), 1.0)
            fail("Expected insufficient stock")
        } catch (_: IllegalArgumentException) { }
        assertEquals(1.0, repository.items.value.first { it.name == "Milch" }.quantity, 0.0)
        assertEquals(100.0, InventoryRepository(context).items.value.first { it.name == "Mehl" }.quantity, 0.0)
    }
    @Test fun pendingPhotoSurvivesOfflineRestart() = runBlocking {
        val item = InventoryItem(id = "photo", name = "Mais", localCoverPath = "/private/photo.jpg", coverPending = true)
        InventoryRepository(context).save(item)
        val loaded = InventoryRepository(context).items.value.single()
        assertEquals(item.localCoverPath, loaded.localCoverPath)
        assertTrue(loaded.coverPending)
        assertTrue(loaded.dirty)
    }

    @Test fun widgetShowsSevenDatesAcrossMonthsAndAllPlannedMeals() {
        val today = java.time.LocalDate.of(2026, 10, 3)
        fun meal(id: String, date: String, title: String, type: de.yummify.app.data.model.MealType = de.yummify.app.data.model.MealType.DINNER) =
            de.yummify.app.data.model.MealPlanItem(id, "", 0, type, "r", title, "", 0, 0, 0, plannedDate = date)
        val meals = listOf(meal("a", "2026-09-28", "Pasta"), meal("b", "2026-09-28", "Porridge", de.yummify.app.data.model.MealType.BREAKFAST),
            meal("c", "2026-10-03", "Curry"), meal("d", "2026-10-05", "Andere Woche"))
        val views = MealPlannerWidgetProvider.buildViews(context, 42, today, heightDp = 180, plannedMeals = meals)
        val root = views.apply(context, FrameLayout(context))
        assertEquals("28", root.findViewById<TextView>(R.id.day_mo_num).text)
        assertEquals("4", root.findViewById<TextView>(R.id.day_so_num).text)
        assertEquals("Porridge · Pasta", root.findViewById<TextView>(R.id.day_mo_meal).text)
        assertEquals("Curry", root.findViewById<TextView>(R.id.day_sa_meal).text)
        assertEquals("Noch nichts geplant", root.findViewById<TextView>(R.id.day_so_meal).text)
        assertEquals(android.view.View.VISIBLE, root.findViewById<TextView>(R.id.day_sa_today).visibility)
        assertEquals(android.view.View.GONE, root.findViewById<TextView>(R.id.day_so_today).visibility)
        assertEquals("KW 40", root.findViewById<TextView>(R.id.widget_kw_text).text)
    }
    @Test fun compactAndExpandedWidgetBothInflateAndTransparencyKeepsTextVisible() {
        for (height in listOf(156, 300)) {
            val root = MealPlannerWidgetProvider.buildViews(context, 42, heightDp = height, transparencyOverride = 0, plannedMeals = emptyList()).apply(context, FrameLayout(context))
            assertEquals(0, root.findViewById<android.widget.ImageView>(R.id.widget_background_image).imageAlpha)
            assertEquals(android.view.View.VISIBLE, root.findViewById<TextView>(R.id.day_mo_meal).visibility)
            assertEquals(if (height >= 280) 2 else 1, root.findViewById<TextView>(R.id.day_mo_meal).maxLines)
        }
    }

    @Test fun duplicateBarcodeIsRejectedAndExistingStockCanBeEdited() = runBlocking {
        val repository = InventoryRepository(context)
        repository.save(InventoryItem(id = "milk", name = "Milch", barcode = "12345678", quantity = 1.0))
        try {
            repository.save(InventoryItem(id = "duplicate", name = "Milch", barcode = "12345678"))
            fail("Expected duplicate barcode rejection")
        } catch (_: IllegalArgumentException) { }
        assertEquals(1, repository.items.value.size)
        repository.save(repository.items.value.single().copy(quantity = 2.0))
        assertEquals(2.0, repository.items.value.single().quantity, 0.0)
        repository.remove("milk")
        repository.save(InventoryItem(id = "new", name = "Milch", barcode = "12345678"))
        assertEquals("new", repository.items.value.single().id)
    }
    @Test fun productImageAndSourceSurviveOfflineRestart() = runBlocking {
        val item = InventoryItem(name = "Haferflocken", barcode = "12345678", quantity = 500.0, unit = "g",
            coverUrl = "https://images.openfoodfacts.org/product.jpg", coverPending = true,
            notes = "Quelle: Open Food Facts · https://world.openfoodfacts.org/product/12345678")
        InventoryRepository(context).save(item)
        val loaded = InventoryRepository(context).items.value.single()
        assertEquals(item.coverUrl, loaded.coverUrl)
        assertEquals(item.notes, loaded.notes)
        assertTrue(loaded.coverPending)
    }

}
