package de.yummify.app.data.model

import java.time.LocalDate

/** One ingredient to put on the shopping list; [ingredient].amount is what is still missing. */
data class PlannedPurchase(val ingredient: Ingredient, val recipes: List<String>, val note: String? = null)

data class WeekShoppingResult(
    val toBuy: List<PlannedPurchase> = emptyList(),
    /** Fully covered by the inventory; shown to the user so a wrong guess can be undone. */
    val covered: List<CoveredItem> = emptyList(),
    /** Needed ingredients that are already on the shopping list (open or checked). */
    val alreadyListed: Int = 0,
    /** Planned meals whose recipe is not available (deleted in Notion or not loaded yet). */
    val missingRecipes: List<String> = emptyList(),
    val mealCount: Int = 0
)

/** Short summary for the user after a plan run. */
fun WeekShoppingResult.summary(): String = buildList {
    add(if (toBuy.isEmpty()) "Nichts Neues für die Liste." else "${toBuy.size} ${if (toBuy.size == 1) "Zutat" else "Zutaten"} hinzugefügt.")
    if (covered.isNotEmpty()) add("${covered.size} schon im Vorrat.")
    if (alreadyListed > 0) add("$alreadyListed stehen schon auf der Liste.")
    if (missingRecipes.isNotEmpty()) add("Ohne Rezeptdaten: ${missingRecipes.joinToString(", ")}.")
}.joinToString(" ")

/**
 * Builds the shopping needs for the meals planned in a date range: adds up the same ingredient
 * across recipes, subtracts the inventory, then what is already on the list. Running it twice
 * therefore adds nothing the second time.
 */
object WeekShoppingPlanner {
    private const val EPSILON = 1e-6

    private class Need(val name: String, var unit: String, var amount: Double, val recipes: LinkedHashSet<String>)

    fun plan(meals: List<MealPlanItem>, recipes: List<Recipe>, stock: List<InventoryItem>, list: List<ShoppingItem>,
             from: LocalDate, to: LocalDate, today: LocalDate = LocalDate.now(), newId: () -> String = { java.util.UUID.randomUUID().toString() }): WeekShoppingResult {
        val dates = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
        val planned = meals.filter { meal -> !meal.isCooked && meal.recipeId != null && dates.any { meal.matchesDate(it) } }
        val missingRecipes = mutableListOf<String>()
        val needs = mutableListOf<Need>()

        planned.forEach { meal ->
            val recipe = recipes.firstOrNull { it.id == meal.recipeId }
            if (recipe == null) { missingRecipes += meal.recipeTitle; return@forEach }
            recipe.ingredients.filter { it.name.isNotBlank() }.forEach { ingredient ->
                val same = needs.filter { IngredientMatcher.same(it.name, ingredient.name) }
                val target = when {
                    ingredient.amount <= 0.0 -> same.firstOrNull()   // "Salz" without an amount is covered by any entry
                    else -> same.firstOrNull { it.amount > 0.0 && InventoryMath.convert(ingredient.amount, ingredient.unit, it.unit) != null }
                }
                val amountless = if (target == null && ingredient.amount > 0.0) same.firstOrNull { it.amount <= 0.0 } else null
                if (amountless != null) {
                    // An earlier mention without an amount now gets one.
                    amountless.amount = ingredient.amount; amountless.unit = ingredient.unit; amountless.recipes += recipe.title
                } else if (target == null) needs += Need(ingredient.name.trim(), ingredient.unit, ingredient.amount.coerceAtLeast(0.0), linkedSetOf(recipe.title))
                else {
                    if (ingredient.amount > 0.0 && target.amount > 0.0) target.amount += InventoryMath.convert(ingredient.amount, ingredient.unit, target.unit)!!
                    target.recipes += recipe.title
                }
            }
        }

        val toBuy = mutableListOf<PlannedPurchase>()
        val covered = mutableListOf<CoveredItem>()
        var alreadyListed = 0
        needs.forEach { need ->
            val ingredient = Ingredient(need.name, need.amount, need.unit)
            val recipeNames = need.recipes.toList()
            val recipeLabel = recipeNames.joinToString(" · ")
            val onList = list.filter { IngredientMatcher.same(it.name, need.name) }

            if (need.amount <= 0.0) {
                // No amount given: stock with any quantity, or any entry on the list, is enough.
                val inStock = stock.any { !it.deleted && !it.isExpired(today) && it.quantity > 0.0 && IngredientMatcher.same(it.name, need.name) }
                when {
                    inStock -> covered += CoveredItem(newId(), need.name, "nach Bedarf", "im Vorrat", null, null, recipeLabel)
                    onList.isNotEmpty() -> alreadyListed++
                    else -> toBuy += PlannedPurchase(ingredient, recipeNames)
                }
                return@forEach
            }

            val stockAmount = InventoryMath.available(ingredient, stock, today)
            var remaining = need.amount - stockAmount
            if (remaining <= EPSILON) {
                covered += CoveredItem(newId(), need.name, label(need.amount, need.unit), "Vorrat: ${label(stockAmount, need.unit)}", need.amount, need.unit, recipeLabel)
                return@forEach
            }
            val listedAmount = onList.sumOf { item ->
                if (item.quantity != null && item.unit != null) InventoryMath.convert(item.quantity, item.unit, need.unit) ?: 0.0
                else need.amount   // an entry without a number already stands for "buy it"
            }
            remaining -= listedAmount
            if (remaining <= EPSILON) { alreadyListed++; return@forEach }
            val note = if (stockAmount > EPSILON) "Vorrat deckt ${label(stockAmount, need.unit)}" else null
            toBuy += PlannedPurchase(Ingredient(need.name, remaining, need.unit), recipeNames, note)
        }
        return WeekShoppingResult(toBuy, covered, alreadyListed, missingRecipes.distinct(), planned.size)
    }

    private fun label(amount: Double, unit: String) = "${InventoryMath.number(amount)} $unit".trim()
}
