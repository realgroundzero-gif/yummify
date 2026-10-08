package de.yummify.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.yummify.app.data.model.CoveredItem
import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.InventoryMath
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.model.WeekShoppingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class ShoppingListRepository private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("yummify_shopping_list", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val lock = Any()

    private val _items = MutableStateFlow(loadItems())
    val items: StateFlow<List<ShoppingItem>> = _items.asStateFlow()

    private fun loadItems(): List<ShoppingItem> {
        val json = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        val stored: List<ShoppingItem> = runCatching {
            gson.fromJson<List<ShoppingItem>>(json, object : TypeToken<List<ShoppingItem>>() {}.type)
        }.getOrNull() ?: run {
            prefs.edit().putString("${KEY_ITEMS}_corrupt_${System.currentTimeMillis()}", json).apply()
            return emptyList()
        }
        // Earlier versions showed demo items until the list was first saved; drop them once.
        val cleaned = stored.filterNot { it.id in SAMPLE_IDS && it.quantity == null && it.unit == null }
        if (cleaned != stored) prefs.edit().putString(KEY_ITEMS, gson.toJson(cleaned)).apply()
        return cleaned
    }

    /** Read, change and store under one lock, so widget taps and app actions cannot overwrite each other. */
    private fun update(change: (List<ShoppingItem>) -> List<ShoppingItem>) {
        synchronized(lock) {
            val list = change(_items.value)
            prefs.edit().putString(KEY_ITEMS, gson.toJson(list)).apply()
            _items.value = list
        }
        de.yummify.app.widget.ShoppingListWidgetProvider.updateAllWidgets(context)
    }

    fun addInventoryNeeds(stock: List<InventoryItem>): Int {
        var count = 0
        update { list ->
            val current = list.toMutableList()
            stock.forEach { item ->
                if (current.none { !it.isChecked && InventoryMath.sameIngredient(it.name, item.name) && it.unit == item.unit }) {
                    val amount = (item.minimum - item.quantity).coerceAtLeast(1.0)
                    current.add(ShoppingItem(UUID.randomUUID().toString(), item.name, "${InventoryMath.number(amount)} ${item.unit}".trim(), item.category,
                        note = "Inventar nachfüllen", quantity = amount, unit = item.unit))
                    count++
                }
            }
            current
        }
        return count
    }

    private val _covered = MutableStateFlow(loadCovered())
    /** Ingredients of the last week plan run that the inventory already covers. */
    val covered: StateFlow<List<CoveredItem>> = _covered.asStateFlow()

    private fun loadCovered(): List<CoveredItem> = runCatching {
        gson.fromJson<List<CoveredItem>>(prefs.getString(KEY_COVERED, null), object : TypeToken<List<CoveredItem>>() {}.type)
    }.getOrNull().orEmpty()

    private fun setCovered(list: List<CoveredItem>) {
        prefs.edit().putString(KEY_COVERED, gson.toJson(list)).apply()
        _covered.value = list
    }

    /** Puts the planned purchases on the list and remembers what the inventory already covers. */
    fun applyPlan(result: WeekShoppingResult) {
        update { list ->
            result.toBuy.fold(list) { acc, purchase ->
                mergeIngredients(acc, purchase.recipes.joinToString(" · "), listOf(purchase.ingredient), 1.0, note = purchase.note)
            }
        }
        setCovered(result.covered)
    }

    /** "Doch kaufen": moves a covered ingredient to the list. */
    fun restoreCovered(id: String) {
        val item = _covered.value.firstOrNull { it.id == id } ?: return
        update { list ->
            mergeIngredients(list, item.recipeName.orEmpty(), listOf(Ingredient(item.name, item.quantity ?: 0.0, item.unit.orEmpty())), 1.0)
        }
        setCovered(_covered.value.filterNot { it.id == id })
    }

    fun dismissCovered() = setCovered(emptyList())

    fun removeItems(ids: Set<String>) = update { list -> list.filterNot { it.id in ids } }

    fun toggleItem(id: String) = update { list -> list.map { if (it.id == id) it.copy(isChecked = !it.isChecked) else it } }

    fun clearDoneItems() = update { list -> list.filter { !it.isChecked } }

    /** Adds ingredients; an open entry with the same name and a convertible unit is increased instead of duplicated. */
    fun addIngredients(recipeTitle: String, ingredients: List<Ingredient>, portionMultiplier: Double): Int {
        update { list -> mergeIngredients(list, recipeTitle, ingredients, portionMultiplier) }
        return ingredients.size
    }

    companion object {
        private const val KEY_ITEMS = "shopping_items_json"
        private const val KEY_COVERED = "covered_items_json"
        private val SAMPLE_IDS = (1..14).map { "s$it" }.toSet()

        internal fun mergeIngredients(list: List<ShoppingItem>, recipeTitle: String, ingredients: List<Ingredient>,
                                      multiplier: Double, note: String? = null,
                                      newId: () -> String = { UUID.randomUUID().toString() }): List<ShoppingItem> {
            val current = list.toMutableList()
            ingredients.forEach { ingredient ->
                val unit = ingredient.unit.ifBlank { "Stk" }
                val amount = ingredient.amount * multiplier
                val sameName = { item: ShoppingItem -> !item.isChecked && InventoryMath.sameIngredient(item.name, ingredient.name) }
                val index = current.indexOfFirst { item ->
                    sameName(item) && item.quantity != null && item.unit != null && InventoryMath.convert(amount, unit, item.unit) != null
                }
                val unquantified = if (amount > 0) -1 else current.indexOfFirst(sameName)
                if (unquantified >= 0) {
                    // "Salz" without an amount: one open entry is enough, just note the extra recipe.
                    val existing = current[unquantified]
                    current[unquantified] = existing.copy(recipeName = joinRecipes(existing.recipeName, recipeTitle))
                } else if (index >= 0 && amount > 0) {
                    val existing = current[index]
                    val total = existing.quantity!! + InventoryMath.convert(amount, unit, existing.unit!!)!!
                    current[index] = existing.copy(quantity = total, amountWithUnit = format(total, existing.unit), recipeName = joinRecipes(existing.recipeName, recipeTitle))
                } else {
                    current.add(0, ShoppingItem(id = newId(), name = ingredient.name,
                        amountWithUnit = if (amount > 0) format(amount, ingredient.unit) else "",
                        category = categoryFor(ingredient.name), recipeName = recipeTitle, note = note,
                        quantity = amount.takeIf { it > 0 }, unit = unit))
                }
            }
            return current
        }

        private fun joinRecipes(existing: String?, added: String) =
            listOfNotNull(existing, added).flatMap { it.split(" · ") }.filter { it.isNotBlank() }.distinct().joinToString(" · ")

        private fun format(amount: Double, unit: String) = "${InventoryMath.number(amount)} $unit".trim()

        /**
         * Ordered rules; the first match wins. "=x" matches a whole word, "*x" a word ending in x
         * (compounds such as "Olivenöl"), anything else a part of the name. So "Ei" never hits "Reis".
         */
        private val categoryRules: List<Pair<String, List<String>>> = listOf(
            "Fisch & Fleisch" to listOf("fleisch", "hähnchen", "huhn", "pute", "rind", "schwein", "hack", "lachs", "thunfisch", "fisch",
                "garnele", "speck", "schinken", "wurst", "salami", "chorizo", "bacon"),
            "Gewürze & Öle" to listOf("*öl", "*salz", "pfeffer", "gewürz", "pulver", "essig", "zimt", "kreuzkümmel", "curry",
                "oregano", "thymian", "chili", "muskat", "brühe"),
            "Obst & Gemüse" to listOf("gemüse", "paprika", "zwiebel", "knoblauch", "tomate", "pilz", "champignon", "zitrone", "limette",
                "avocado", "salat", "basilikum", "petersilie", "koriander", "kartoffel", "möhre", "karotte", "zucchini", "gurke",
                "spinat", "brokkoli", "apfel", "banane", "beere", "ingwer", "lauch", "aubergine", "kürbis"),
            "Kühlregal & Milchprodukte" to listOf("milch", "käse", "parmesan", "mozzarella", "burrata", "sahne", "butter", "feta",
                "joghurt", "quark", "schmand", "crème fraîche", "=ei", "=eier", "eigelb", "eiweiß")
        )

        /** Picks a shopping category from the ingredient name. */
        fun categoryFor(name: String): String {
            val lower = name.lowercase(Locale.GERMAN)
            val words = lower.split(Regex("""[^\p{L}]+""")).filter { it.isNotEmpty() }
            return categoryRules.firstOrNull { (_, keywords) ->
                keywords.any { keyword ->
                    when (keyword.first()) {
                        '=' -> keyword.drop(1) in words
                        '*' -> words.any { it.endsWith(keyword.drop(1)) }
                        else -> lower.contains(keyword)
                    }
                }
            }?.first ?: "Vorrat & Trockenwaren"
        }

        @Volatile
        private var INSTANCE: ShoppingListRepository? = null

        fun getInstance(context: Context): ShoppingListRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShoppingListRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
