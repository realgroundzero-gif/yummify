package de.yummify.app

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import de.yummify.app.data.model.*
import de.yummify.app.data.repository.InventoryRepository
import de.yummify.app.data.repository.ShoppingListRepository
import de.yummify.app.data.repository.UserPreferencesRepository
import de.yummify.app.widget.ShoppingListWidgetFactory
import de.yummify.app.widget.ShoppingListWidgetProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShoppingListWidgetTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private fun item(id: String, checked: Boolean = false, recipe: String? = null) =
        ShoppingItem(id, "Tomaten", "500 g", "Gemüse", recipeName = recipe, isChecked = checked)
    private fun meal(date: String, cooked: Boolean = false) = MealPlanItem("m", "", 0, MealType.DINNER,
        "recipe", "Pasta", "", 0, 0, 0, isCooked = cooked, plannedDate = date)

    @Test fun todayHighlightsOnlyOpenIngredientsForUncookedMealsOnTheExactDate() {
        val today = LocalDate.of(2026, 10, 4)
        val recipes = ShoppingListWidgetProvider.todayRecipes(listOf(meal("2026-10-04"),
            meal("2026-10-05").copy(recipeTitle = "Curry"), meal("2026-10-04", true).copy(recipeTitle = "Salat")), today)
        assertTrue(ShoppingListWidgetProvider.isToday(item("today", recipe = " PASTA "), recipes))
        assertFalse(ShoppingListWidgetProvider.isToday(item("checked", true, "Pasta"), recipes))
        assertFalse(ShoppingListWidgetProvider.isToday(item("other", recipe = "Curry"), recipes))
        assertFalse(ShoppingListWidgetProvider.isToday(item("cooked", recipe = "Salat"), recipes))
        val sorted = ShoppingListWidgetProvider.sortedItems(listOf(item("done", true), item("normal"), item("today", recipe = "Pasta")), recipes)
        assertEquals(listOf("today", "normal", "done"), sorted.map { it.id })
    }
    @Test fun rowsRenderQuantityTodayBadgeAndCheckedStateWithoutReusingOldStyle() {
        val recipes = setOf("pasta")
        val checked = ShoppingListWidgetProvider.buildRow(context, item("done", true), recipes).apply(context, FrameLayout(context))
        assertTrue(checked.findViewById<TextView>(R.id.shopping_widget_item_name).paintFlags and Paint.STRIKE_THRU_TEXT_FLAG != 0)
        val today = ShoppingListWidgetProvider.buildRow(context, item("today", recipe = "Pasta"), recipes)
        val view = today.apply(context, FrameLayout(context))
        assertEquals("Tomaten (500 g)", view.findViewById<TextView>(R.id.shopping_widget_item_name).text.toString())
        assertEquals("HEUTE", view.findViewById<TextView>(R.id.shopping_widget_item_tag).text.toString())
        ShoppingListWidgetProvider.buildRow(context, item("normal"), recipes).reapply(context, view)
        assertEquals("Gemüse", view.findViewById<TextView>(R.id.shopping_widget_item_tag).text.toString())
        assertNull(view.background)
        assertTrue(view.findViewById<TextView>(R.id.shopping_widget_item_name).paintFlags and Paint.STRIKE_THRU_TEXT_FLAG == 0)
    }
    @Test fun previewKeepsFullCounterAndActualRowsAndHasAnEmptyState() {
        val items = (1..12).map { item("$it", checked = it == 1) }
        val view = ShoppingListWidgetProvider.buildViews(context, 42, items = items, meals = emptyList(), preview = true,
            transparencyOverride = 0).apply(context, FrameLayout(context))
        assertEquals("OFFEN 11 / 12", view.findViewById<TextView>(R.id.shopping_widget_count).text.toString())
        assertEquals(7, view.findViewById<LinearLayout>(R.id.shopping_widget_preview).childCount)
        assertEquals(0, view.findViewById<ImageView>(R.id.shopping_widget_background).imageAlpha)
        val empty = ShoppingListWidgetProvider.buildViews(context, 42, items = emptyList(), meals = emptyList(), preview = true).apply(context, FrameLayout(context))
        assertEquals(View.VISIBLE, empty.findViewById<TextView>(R.id.shopping_widget_empty).visibility)
        assertEquals("OFFEN 0 / 0", empty.findViewById<TextView>(R.id.shopping_widget_count).text.toString())
        assertEquals("shopping", ShoppingListWidgetProvider.openShopping(context).getStringExtra("route"))
    }
    @Test fun widgetToggleUpdatesTheSameShoppingRepositoryAndFactory() {
        val repo = ShoppingListRepository.getInstance(context)
        repo.removeItems(repo.items.value.map { it.id }.toSet())
        repo.addIngredients("Pasta", listOf(Ingredient("Tomaten", 500.0, "g")), 1.0)
        val id = repo.items.value.single().id
        val factory = ShoppingListWidgetFactory(context)
        factory.onDataSetChanged()
        assertEquals(1, factory.count)
        ShoppingListWidgetProvider().onReceive(context, Intent(ShoppingListWidgetProvider.ACTION_ITEM).putExtra("item_id", id).putExtra("toggle", true))
        assertTrue(repo.items.value.single().isChecked)
        factory.onDataSetChanged()
        val row = factory.getViewAt(0)!!.apply(context, FrameLayout(context))
        assertTrue(row.findViewById<TextView>(R.id.shopping_widget_item_name).paintFlags and Paint.STRIKE_THRU_TEXT_FLAG != 0)
        repo.clearDoneItems()
        factory.onDataSetChanged()
        assertEquals(0, factory.count)
        assertNull(factory.getViewAt(0))
    }
    @Test fun legacyProductNotesAreCompactedDurablyAndKeepManualAdditionsAndProductFields() = runBlocking {
        UserPreferencesRepository.getInstance(context).apply { saveNotionConfig("", "", ""); setAutoSyncEnabled(false) }
        val source = "https://world.openfoodfacts.org/product/4075600113463"
        val item = InventoryItem(id = "legacy", name = "Schupfnudeln", barcode = "4075600113463", productUrl = source,
            calories = 166.0, ingredients = "Kartoffeln", dirty = false,
            notes = "Packungsinhalt: 400 g\nMarke: Bürger\nZutaten: Kartoffeln\nNährwerte pro 100 g/ml: 166 kcal\nQuelle: Open Food Facts · $source\nDaten: ODbL · Bilder: CC BY-SA\nFür Sonntag aufheben")
        val prefs = context.getSharedPreferences("yummify_inventory", Context.MODE_PRIVATE)
        prefs.edit().putString("items_local", com.google.gson.Gson().toJson(listOf(item))).commit()
        val loaded = InventoryRepository(context).items.value.single()
        assertEquals("Packungsinhalt: 400 g\nMarke: Bürger\nFür Sonntag aufheben", loaded.notes)
        assertEquals(item.calories, loaded.calories)
        assertEquals(item.ingredients, loaded.ingredients)
        assertTrue(loaded.dirty)
        assertEquals(loaded, InventoryRepository(context).items.value.single())
        assertEquals(loaded, loaded.compactProductNotes())
        assertEquals(item.copy(productUrl = null), item.copy(productUrl = null).compactProductNotes())
    }
}
