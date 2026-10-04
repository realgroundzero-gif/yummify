package de.yummify.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.yummify.app.data.local.SampleData
import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.InventoryMath
import de.yummify.app.data.model.ShoppingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ShoppingListRepository private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("yummify_shopping_list", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _items = MutableStateFlow<List<ShoppingItem>>(loadItems())
    val items: StateFlow<List<ShoppingItem>> = _items.asStateFlow()

    private fun loadItems(): List<ShoppingItem> {
        val json = prefs.getString(KEY_ITEMS, null) ?: return SampleData.shoppingItems
        return try {
            val type = object : TypeToken<List<ShoppingItem>>() {}.type
            gson.fromJson(json, type) ?: SampleData.shoppingItems
        } catch (e: Exception) {
            SampleData.shoppingItems
        }
    }

    private fun saveItems(list: List<ShoppingItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_ITEMS, json).apply()
        _items.value = list
        de.yummify.app.widget.ShoppingListWidgetProvider.updateAllWidgets(context)
    }

    fun addInventoryNeeds(stock: List<InventoryItem>): Int {
        val current = _items.value.toMutableList()
        var count = 0
        stock.forEach { item ->
            if (current.none { !it.isChecked && InventoryMath.normalizedName(it.name) == InventoryMath.normalizedName(item.name) && it.unit == item.unit }) {
                val amount = (item.minimum - item.quantity).coerceAtLeast(1.0)
                current.add(ShoppingItem(UUID.randomUUID().toString(), item.name, "${InventoryMath.number(amount)} ${item.unit}", item.category,
                    note = "Inventar nachfüllen", quantity = amount, unit = item.unit))
                count++
            }
        }
        saveItems(current)
        return count
    }

    fun removeItems(ids: Set<String>) = saveItems(_items.value.filterNot { it.id in ids })

    fun toggleItem(id: String) {
        val updated = _items.value.map {
            if (it.id == id) it.copy(isChecked = !it.isChecked) else it
        }
        saveItems(updated)
    }

    fun clearDoneItems() {
        val remaining = _items.value.filter { !it.isChecked }
        saveItems(remaining)
    }

    fun addIngredients(recipeTitle: String, ingredients: List<Ingredient>, portionMultiplier: Double): Int {
        val current = _items.value.toMutableList()
        var addedCount = 0

        ingredients.forEach { ingredient ->
            val formattedAmount = ingredient.getFormattedAmount(portionMultiplier)
            val category = when {
                ingredient.name.contains("Gemüse", ignoreCase = true) ||
                ingredient.name.contains("Paprika", ignoreCase = true) ||
                ingredient.name.contains("Zwiebel", ignoreCase = true) ||
                ingredient.name.contains("Knoblauch", ignoreCase = true) ||
                ingredient.name.contains("Tomate", ignoreCase = true) ||
                ingredient.name.contains("Pilz", ignoreCase = true) ||
                ingredient.name.contains("Zitrone", ignoreCase = true) ||
                ingredient.name.contains("Avocado", ignoreCase = true) ||
                ingredient.name.contains("Salat", ignoreCase = true) ||
                ingredient.name.contains("Basilikum", ignoreCase = true) -> "Obst & Gemüse"

                ingredient.name.contains("Milch", ignoreCase = true) ||
                ingredient.name.contains("Käse", ignoreCase = true) ||
                ingredient.name.contains("Parmesan", ignoreCase = true) ||
                ingredient.name.contains("Sahne", ignoreCase = true) ||
                ingredient.name.contains("Butter", ignoreCase = true) ||
                ingredient.name.contains("Feta", ignoreCase = true) ||
                ingredient.name.contains("Ei", ignoreCase = true) ||
                ingredient.name.contains("Joghurt", ignoreCase = true) -> "Kühlregal & Milchprodukte"

                ingredient.name.contains("Hähnchen", ignoreCase = true) ||
                ingredient.name.contains("Fleisch", ignoreCase = true) ||
                ingredient.name.contains("Hackfleisch", ignoreCase = true) ||
                ingredient.name.contains("Lachs", ignoreCase = true) ||
                ingredient.name.contains("Speck", ignoreCase = true) ||
                ingredient.name.contains("Schinken", ignoreCase = true) -> "Fisch & Fleisch"

                ingredient.name.contains("Öl", ignoreCase = true) ||
                ingredient.name.contains("Salz", ignoreCase = true) ||
                ingredient.name.contains("Pfeffer", ignoreCase = true) ||
                ingredient.name.contains("Gewürz", ignoreCase = true) ||
                ingredient.name.contains("Erz", ignoreCase = true) ||
                ingredient.name.contains("Essig", ignoreCase = true) -> "Gewürze & Öle"

                else -> "Vorrat & Trockenwaren"
            }

            val newItem = ShoppingItem(
                id = UUID.randomUUID().toString(),
                name = ingredient.name,
                amountWithUnit = formattedAmount,
                category = category,
                recipeName = recipeTitle,
                isChecked = false,
                quantity = ingredient.amount * portionMultiplier,
                unit = ingredient.unit.ifBlank { "Stk" }
            )
            current.add(0, newItem)
            addedCount++
        }

        saveItems(current)
        return addedCount
    }

    companion object {
        private const val KEY_ITEMS = "shopping_items_json"

        @Volatile
        private var INSTANCE: ShoppingListRepository? = null

        fun getInstance(context: Context): ShoppingListRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShoppingListRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
