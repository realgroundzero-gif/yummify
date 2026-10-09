package de.yummify.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.MealType
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.matchesDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.util.UUID

class MealPlanRepository internal constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("yummify_meal_plan", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _plannedMeals = MutableStateFlow(loadMeals())
    val plannedMeals: StateFlow<List<MealPlanItem>> = _plannedMeals.asStateFlow()

    private fun loadMeals(): List<MealPlanItem> {
        val json = prefs.getString(KEY_MEALS, null) ?: return emptyList()
        val stored: List<MealPlanItem> = runCatching {
            gson.fromJson<List<MealPlanItem>>(json, object : TypeToken<List<MealPlanItem>>() {}.type)
        }.getOrNull() ?: run {
            // Keep unreadable data for support instead of overwriting it with the next save.
            prefs.edit().putString("${KEY_MEALS}_corrupt_${System.currentTimeMillis()}", json).apply()
            return emptyList()
        }
        val migrated = migrate(stored)
        if (migrated != stored) prefs.edit().putString(KEY_MEALS, gson.toJson(migrated)).apply()
        return migrated
    }

    /** Reads the stored plan again, e.g. after a data import or "Alle lokalen Daten löschen". */
    fun reloadFromStorage() { _plannedMeals.value = loadMeals() }

    private fun saveMeals(list: List<MealPlanItem>) {
        prefs.edit().putString(KEY_MEALS, gson.toJson(list)).apply()
        _plannedMeals.value = list
        de.yummify.app.widget.MealPlannerWidgetProvider.updateAllWidgets(context)
        de.yummify.app.widget.ShoppingListWidgetProvider.updateAllWidgets(context)
    }

    fun addMealPlan(recipe: Recipe, date: LocalDate, mealType: MealType): MealPlanItem {
        val newItem = MealPlanItem(
            id = UUID.randomUUID().toString(),
            dayOfWeek = DAY_CODES[date.dayOfWeek.value - 1],
            dayOfMonth = date.dayOfMonth,
            mealType = mealType,
            recipeId = recipe.id,
            recipeTitle = recipe.title,
            imageUrl = recipe.imageUrl,
            calories = recipe.calories,
            proteinGrams = recipe.proteinGrams,
            cookTimeMinutes = recipe.cookTimeMinutes,
            isCooked = false,
            plannedDate = date.toString()
        )
        saveMeals(_plannedMeals.value.filterNot { it.matchesDate(date) && it.mealType == mealType } + newItem)
        return newItem
    }

    fun toggleCooked(id: String) {
        saveMeals(_plannedMeals.value.map { if (it.id == id) it.copy(isCooked = !it.isCooked) else it })
    }

    fun removeMealPlan(id: String) {
        saveMeals(_plannedMeals.value.filterNot { it.id == id })
    }

    /** The date to show in Notion's single "Geplant am" column for a recipe planned zero or more times. */
    fun plannedDateFor(recipeId: String, today: LocalDate = LocalDate.now()): LocalDate? =
        nextPlannedDate(_plannedMeals.value, recipeId, today)

    companion object {
        private const val KEY_MEALS = "planned_meals_json"
        private val DAY_CODES = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        /** Demo entries that earlier versions wrote on first start. */
        private val SAMPLE_IDS = (1..11).map { "m$it" }.toSet()

        internal fun migrate(stored: List<MealPlanItem>): List<MealPlanItem> = stored
            .filterNot { it.id in SAMPLE_IDS && it.plannedDate == null }
            .map { meal ->
                // Earlier versions stored the same placeholder nutrition for every Notion recipe.
                if (meal.calories == 450 && meal.proteinGrams == 25 && meal.cookTimeMinutes == 25)
                    meal.copy(calories = null, proteinGrams = null, cookTimeMinutes = null) else meal
            }

        /** Earliest upcoming date; otherwise the most recent past one; null when the recipe is no longer planned. */
        internal fun nextPlannedDate(meals: List<MealPlanItem>, recipeId: String, today: LocalDate): LocalDate? {
            val dates = meals.filter { it.recipeId == recipeId }.mapNotNull { meal -> meal.plannedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
            return dates.filter { !it.isBefore(today) }.minOrNull() ?: dates.maxOrNull()
        }

        @Volatile
        private var INSTANCE: MealPlanRepository? = null

        fun getInstance(context: Context): MealPlanRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MealPlanRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
