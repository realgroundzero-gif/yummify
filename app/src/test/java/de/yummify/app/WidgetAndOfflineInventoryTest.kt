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
        assertNotNull(inflated.findViewById<TextView>(R.id.hero_recipe_title).text)
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
}
