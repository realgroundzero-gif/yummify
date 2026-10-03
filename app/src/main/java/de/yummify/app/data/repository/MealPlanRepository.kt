package de.yummify.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.matchesDate
import de.yummify.app.data.model.MealType
import de.yummify.app.data.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class MealPlanRepository private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("yummify_meal_plan", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _plannedMeals = MutableStateFlow<List<MealPlanItem>>(loadMeals())
    val plannedMeals: StateFlow<List<MealPlanItem>> = _plannedMeals.asStateFlow()

    private fun loadMeals(): List<MealPlanItem> {
        val json = prefs.getString(KEY_MEALS, null)
        if (json.isNullOrEmpty()) {
            val initial = de.yummify.app.data.local.SampleData.mealPlanItems
            prefs.edit().putString(KEY_MEALS, gson.toJson(initial)).apply()
            return initial
        }
        return try {
            val type = object : TypeToken<List<MealPlanItem>>() {}.type
            gson.fromJson(json, type) ?: de.yummify.app.data.local.SampleData.mealPlanItems
        } catch (e: Exception) {
            de.yummify.app.data.local.SampleData.mealPlanItems
        }
    }

    private fun saveMeals(list: List<MealPlanItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_MEALS, json).apply()
        _plannedMeals.value = list
        de.yummify.app.widget.MealPlannerWidgetProvider.sendUpdateNotice(context)
    }

    fun addMealPlan(recipe: Recipe, date: LocalDate, mealType: MealType): MealPlanItem {
        val dayNames = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        val dayOfWeekStr = dayNames.getOrElse(date.dayOfWeek.value - 1) { "Mo" }

        val newItem = MealPlanItem(
            id = UUID.randomUUID().toString(),
            dayOfWeek = dayOfWeekStr,
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

        val current = _plannedMeals.value.toMutableList()
        current.removeAll { it.matchesDate(date) && it.mealType == mealType }
        current.add(newItem)
        saveMeals(current)
        return newItem
    }

    fun toggleCooked(id: String) {
        saveMeals(_plannedMeals.value.map { if (it.id == id) it.copy(isCooked = !it.isCooked) else it })
    }

    fun removeMealPlan(id: String) {
        val current = _plannedMeals.value.filterNot { it.id == id }
        saveMeals(current)
    }

    companion object {
        private const val KEY_MEALS = "planned_meals_json"

        @Volatile
        private var INSTANCE: MealPlanRepository? = null

        fun getInstance(context: Context): MealPlanRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MealPlanRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
