package de.yummify.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import de.yummify.app.MainActivity
import de.yummify.app.R
import de.yummify.app.data.model.matchesDate
import de.yummify.app.data.repository.MealPlanRepository
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

class MealPlannerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, manager, it) }
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        updateAppWidget(context, manager, id)
    }
    override fun onDeleted(context: Context, ids: IntArray) {
        val edit = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit()
        ids.forEach { edit.remove("transparency_$it") }
        edit.apply()
    }
    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, MealPlannerWidgetProvider::class.java))
                .forEach { updateAppWidget(context, manager, it) }
        }
        fun sendUpdateNotice(context: Context) = updateAllWidgets(context)
        fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {
            manager.updateAppWidget(id, buildViews(context, id))
        }
        internal fun buildViews(context: Context, id: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_meal_planner)
            val transparency = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                .getInt("transparency_$id", 100).coerceIn(0, 100)
            if (transparency == 100) views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_background)
            else views.setInt(R.id.widget_container, "setBackgroundColor", Color.argb(255 * transparency / 100, 23, 20, 18))
            val today = LocalDate.now()
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            val meals = MealPlanRepository.getInstance(context).plannedMeals.value
            val weekMeals = meals.filter { meal -> (0..6).any { meal.matchesDate(monday.plusDays(it.toLong())) } }
            views.setTextViewText(R.id.widget_kw_text, "KW ${today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}")
            views.setTextViewText(R.id.widget_planned_count_text, "${weekMeals.size} geplante Gerichte")
            val containers = intArrayOf(R.id.day_mo_container, R.id.day_di_container, R.id.day_mi_container, R.id.day_do_container, R.id.day_fr_container, R.id.day_sa_container, R.id.day_so_container)
            val names = intArrayOf(R.id.day_mo_name, R.id.day_di_name, R.id.day_mi_name, R.id.day_do_name, R.id.day_fr_name, R.id.day_sa_name, R.id.day_so_name)
            val numbers = intArrayOf(R.id.day_mo_num, R.id.day_di_num, R.id.day_mi_num, R.id.day_do_num, R.id.day_fr_num, R.id.day_sa_num, R.id.day_so_num)
            val dots = intArrayOf(R.id.day_mo_dot, R.id.day_di_dot, R.id.day_mi_dot, R.id.day_do_dot, R.id.day_fr_dot, R.id.day_sa_dot, R.id.day_so_dot)
            val dayCodes = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
            for (i in 0..6) {
                val date = monday.plusDays(i.toLong())
                val dayMeals = weekMeals.filter { it.matchesDate(date) }
                views.setTextViewText(names[i], dayCodes[i])
                views.setTextViewText(numbers[i], date.dayOfMonth.toString())
                views.setInt(containers[i], "setBackgroundResource", if (date == today) R.drawable.widget_day_active else R.drawable.widget_day_inactive)
                views.setImageViewResource(dots[i], when {
                    date == today -> R.drawable.widget_dot_white
                    dayMeals.isEmpty() -> R.drawable.widget_dot_muted
                    dayMeals.any { !it.isCooked } -> R.drawable.widget_dot_green
                    else -> R.drawable.widget_dot_amber
                })
            }
            views.setTextViewText(R.id.widget_today_label, "Heute • ${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)}")
            val todayMeals = weekMeals.filter { it.matchesDate(today) }.sortedBy { it.mealType.ordinal }
            val hero = todayMeals.firstOrNull { !it.isCooked } ?: todayMeals.firstOrNull()
            val secondary = todayMeals.firstOrNull { it.id != hero?.id }
            views.setTextViewText(R.id.hero_recipe_title, hero?.recipeTitle ?: "Heute noch kein Gericht geplant")
            views.setTextViewText(R.id.hero_meal_type, hero?.mealType?.displayName?.uppercase() ?: "WOCHENPLAN")
            views.setTextViewText(R.id.hero_cook_time, hero?.let { "${it.cookTimeMinutes} Min." } ?: "")
            views.setViewVisibility(R.id.hero_rating, View.GONE)
            views.setTextViewText(R.id.hero_ingredients_status, when {
                hero == null -> "Tippen, um ein Gericht zu planen"
                hero.isCooked -> "✓ Gekocht"
                else -> "Geplant"
            })
            views.setTextViewText(R.id.btn_kochen, if (hero?.recipeId != null) "▷ Rezept" else "+ Planen")
            views.setViewVisibility(R.id.widget_secondary_card, if (secondary == null) View.GONE else View.VISIBLE)
            secondary?.let {
                views.setTextViewText(R.id.secondary_recipe_title, it.recipeTitle)
                views.setTextViewText(R.id.secondary_meal_type, it.mealType.displayName.uppercase())
                views.setTextViewText(R.id.secondary_status, if (it.isCooked) "✓ Gekocht" else "Geplant")
            }
            fun open(request: Int, recipeId: String? = null, config: Boolean = false): PendingIntent {
                val intent = Intent(context, if (config) MealPlannerWidgetConfigActivity::class.java else MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    if (recipeId != null) putExtra("recipeId", recipeId)
                    else putExtra("route", "planner")
                    data = android.net.Uri.parse("yummify://widget/$id/$request")
                }
                return PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            views.setOnClickPendingIntent(R.id.widget_container, open(0))
            views.setOnClickPendingIntent(R.id.btn_kochen, open(1, hero?.recipeId))
            views.setOnClickPendingIntent(R.id.widget_hero_card, open(1, hero?.recipeId))
            if (secondary != null) views.setOnClickPendingIntent(R.id.widget_secondary_card, open(2, secondary.recipeId))
            views.setOnClickPendingIntent(R.id.widget_settings, open(3, config = true))
            return views
        }
    }
}
