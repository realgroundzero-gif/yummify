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
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.matchesDate
import de.yummify.app.data.repository.MealPlanRepository
import java.time.LocalDate
import java.time.temporal.IsoFields

/** A quiet seven-day overview; configuration remains available through the launcher. */
class MealPlannerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, manager, it) }
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) updateAllWidgets(context)
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
        fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {
            manager.updateAppWidget(id, buildViews(context, id))
        }
        internal fun buildViews(context: Context, id: Int, today: LocalDate = LocalDate.now(),
                                heightDp: Int? = null, transparencyOverride: Int? = null,
                                plannedMeals: List<MealPlanItem>? = null): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_meal_planner)
            val transparency = (transparencyOverride ?: context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                .getInt("transparency_$id", 100)).coerceIn(0, 100)
            // Fade only the rounded background, keeping text and the Today highlight readable.
            views.setInt(R.id.widget_background_image, "setImageAlpha", 255 * transparency / 100)
            val height = heightDp ?: AppWidgetManager.getInstance(context).getAppWidgetOptions(id)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
            val compact = height < 220
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            val meals = plannedMeals ?: MealPlanRepository.getInstance(context).plannedMeals.value
            views.setTextViewText(R.id.widget_kw_text, "KW ${today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}")
            val containers = intArrayOf(R.id.day_mo_container, R.id.day_di_container, R.id.day_mi_container, R.id.day_do_container, R.id.day_fr_container, R.id.day_sa_container, R.id.day_so_container)
            val names = intArrayOf(R.id.day_mo_name, R.id.day_di_name, R.id.day_mi_name, R.id.day_do_name, R.id.day_fr_name, R.id.day_sa_name, R.id.day_so_name)
            val numbers = intArrayOf(R.id.day_mo_num, R.id.day_di_num, R.id.day_mi_num, R.id.day_do_num, R.id.day_fr_num, R.id.day_sa_num, R.id.day_so_num)
            val dots = intArrayOf(R.id.day_mo_dot, R.id.day_di_dot, R.id.day_mi_dot, R.id.day_do_dot, R.id.day_fr_dot, R.id.day_sa_dot, R.id.day_so_dot)
            val titles = intArrayOf(R.id.day_mo_meal, R.id.day_di_meal, R.id.day_mi_meal, R.id.day_do_meal, R.id.day_fr_meal, R.id.day_sa_meal, R.id.day_so_meal)
            val badges = intArrayOf(R.id.day_mo_today, R.id.day_di_today, R.id.day_mi_today, R.id.day_do_today, R.id.day_fr_today, R.id.day_sa_today, R.id.day_so_today)
            val dayCodes = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
            for (i in 0..6) {
                val date = monday.plusDays(i.toLong())
                val dayMeals = meals.filter { it.matchesDate(date) }.sortedBy { it.mealType.ordinal }
                val label = dayMeals.joinToString(" · ") { it.recipeTitle }.ifBlank { "Noch nichts geplant" }
                val isToday = date == today
                views.setTextViewText(names[i], dayCodes[i])
                views.setTextViewText(numbers[i], date.dayOfMonth.toString())
                views.setTextViewText(titles[i], label)
                views.setInt(titles[i], "setMaxLines", if (height >= 280) 2 else 1)
                views.setTextViewTextSize(titles[i], android.util.TypedValue.COMPLEX_UNIT_SP, if (compact) 10f else 12f)
                views.setTextColor(titles[i], Color.parseColor(if (isToday) "#FFFFFF" else if (dayMeals.isEmpty()) "#A69D97" else "#DDD3CD"))
                views.setTextColor(names[i], Color.parseColor(if (isToday) "#F08C65" else "#A69D97"))
                views.setInt(containers[i], "setBackgroundResource", if (isToday) R.drawable.widget_day_active else 0)
                views.setImageViewResource(dots[i], if (isToday) R.drawable.widget_dot_terracotta else R.drawable.widget_dot_muted)
                views.setViewVisibility(badges[i], if (isToday) View.VISIBLE else View.GONE)
                views.setContentDescription(containers[i], "${dayCodes[i]}, $date${if (isToday) ", heute" else ""}: $label")
            }
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", "planner")
                data = android.net.Uri.parse("yummify://widget/$id/week")
            }
            views.setOnClickPendingIntent(R.id.widget_container, PendingIntent.getActivity(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            return views
        }
    }
}
