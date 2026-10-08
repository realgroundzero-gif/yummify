package de.yummify.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import de.yummify.app.MainActivity
import de.yummify.app.R
import de.yummify.app.data.model.InventoryMath
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.model.matchesDate
import de.yummify.app.data.repository.MealPlanRepository
import de.yummify.app.data.repository.ShoppingListRepository
import java.time.LocalDate

class ShoppingListWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, manager, it) }
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        updateAppWidget(context, manager, id)
    }
    override fun onDeleted(context: Context, ids: IntArray) {
        val edit = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit()
        ids.forEach { edit.remove("shopping_transparency_$it") }
        edit.apply()
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_ITEM -> {
                val id = intent.getStringExtra("item_id") ?: return
                if (intent.getBooleanExtra("toggle", false)) ShoppingListRepository.getInstance(context).toggleItem(id)
                else context.startActivity(openShopping(context))
            }
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> updateAllWidgets(context)
        }
    }
    companion object {
        const val ACTION_ITEM = "de.yummify.app.widget.SHOPPING_ITEM"
        fun openShopping(context: Context) = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "shopping")
        }
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, ShoppingListWidgetProvider::class.java))
                .forEach { updateAppWidget(context, manager, it) }
        }
        fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {
            manager.updateAppWidget(id, buildViews(context, id))
            manager.notifyAppWidgetViewDataChanged(id, R.id.shopping_widget_list)
        }
        internal fun todayRecipes(meals: List<MealPlanItem>, today: LocalDate): Set<String> = meals
            .filter { it.matchesDate(today) && !it.isCooked }.map { InventoryMath.normalizedName(it.recipeTitle) }.toSet()
        /** Merged entries list several recipes as "A · B"; any of them planned today counts. */
        internal fun isToday(item: ShoppingItem, recipes: Set<String>) = !item.isChecked &&
            item.recipeName?.split(" · ")?.any { InventoryMath.normalizedName(it) in recipes } == true
        internal fun sortedItems(items: List<ShoppingItem>, recipes: Set<String>) = items.sortedWith(
            compareBy<ShoppingItem> { it.isChecked }.thenBy { !isToday(it, recipes) }
                .thenBy { it.category.lowercase() }.thenBy { it.recipeName.orEmpty().lowercase() }.thenBy { it.name.lowercase() })

        internal fun buildViews(context: Context, id: Int, items: List<ShoppingItem>? = null,
                                meals: List<MealPlanItem>? = null, today: LocalDate = LocalDate.now(),
                                transparencyOverride: Int? = null, preview: Boolean = false): RemoteViews {
            val list = items ?: ShoppingListRepository.getInstance(context).items.value
            val recipes = todayRecipes(meals ?: MealPlanRepository.getInstance(context).plannedMeals.value, today)
            val views = RemoteViews(context.packageName, R.layout.widget_shopping_list)
            val transparency = (transparencyOverride ?: context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                .getInt("shopping_transparency_$id", 100)).coerceIn(0, 100)
            views.setInt(R.id.shopping_widget_background, "setImageAlpha", 255 * transparency / 100)
            views.setTextViewText(R.id.shopping_widget_count, "OFFEN ${list.count { !it.isChecked }} / ${list.size}")
            views.setTextViewText(R.id.shopping_widget_empty, "Deine Einkaufsliste ist leer")
            views.setViewVisibility(R.id.shopping_widget_empty, if (list.isEmpty()) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.shopping_widget_list, if (preview || list.isEmpty()) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.shopping_widget_preview, if (preview && list.isNotEmpty()) View.VISIBLE else View.GONE)
            val open = openShopping(context).apply { data = Uri.parse("yummify://shopping-widget/$id/open") }
            val pending = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.shopping_widget_header, pending)
            views.setOnClickPendingIntent(R.id.shopping_widget_empty, pending)
            if (preview) {
                views.removeAllViews(R.id.shopping_widget_preview)
                sortedItems(list, recipes).take(7).forEach { views.addView(R.id.shopping_widget_preview, buildRow(context, it, recipes)) }
            } else {
                val adapter = Intent(context, ShoppingListWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    data = Uri.parse("yummify://shopping-widget/$id/list")
                }
                views.setRemoteAdapter(R.id.shopping_widget_list, adapter)
                views.setEmptyView(R.id.shopping_widget_list, R.id.shopping_widget_empty)
                val template = Intent(context, ShoppingListWidgetProvider::class.java).apply {
                    action = ACTION_ITEM
                    data = Uri.parse("yummify://shopping-widget/$id/item")
                }
                views.setPendingIntentTemplate(R.id.shopping_widget_list, PendingIntent.getBroadcast(context, id, template,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))
            }
            return views
        }
        internal fun buildRow(context: Context, item: ShoppingItem, recipes: Set<String>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_shopping_item)
            val highlight = isToday(item, recipes)
            val title = item.name + item.amountWithUnit.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
            views.setTextViewText(R.id.shopping_widget_item_name, title)
            views.setInt(R.id.shopping_widget_item_name, "setPaintFlags", Paint.ANTI_ALIAS_FLAG or if (item.isChecked) Paint.STRIKE_THRU_TEXT_FLAG else 0)
            views.setTextColor(R.id.shopping_widget_item_name, Color.parseColor(if (item.isChecked) "#918782" else "#F7F1EE"))
            views.setTextViewText(R.id.shopping_widget_item_tag, if (highlight) "HEUTE" else item.category.ifBlank { "Sonstiges" })
            views.setTextColor(R.id.shopping_widget_item_tag, Color.parseColor(if (highlight) "#F08C65" else "#A69D97"))
            views.setInt(R.id.shopping_widget_item_tag, "setBackgroundResource", if (highlight) R.drawable.shopping_widget_today_badge else 0)
            views.setInt(R.id.shopping_widget_item, "setBackgroundResource", if (highlight) R.drawable.shopping_widget_today_row else 0)
            views.setImageViewResource(R.id.shopping_widget_item_check, when {
                item.isChecked -> R.drawable.shopping_widget_checked
                highlight -> R.drawable.shopping_widget_today_check
                else -> R.drawable.shopping_widget_unchecked
            })
            views.setContentDescription(R.id.shopping_widget_item_check, "${if (item.isChecked) "Wieder öffnen" else "Abhaken"}: $title")
            views.setContentDescription(R.id.shopping_widget_item, "$title, ${item.category}${if (highlight) ", für heute" else ""}${if (item.isChecked) ", erledigt" else ""}")
            views.setOnClickFillInIntent(R.id.shopping_widget_item, Intent().putExtra("item_id", item.id).putExtra("toggle", false))
            views.setOnClickFillInIntent(R.id.shopping_widget_item_check, Intent().putExtra("item_id", item.id).putExtra("toggle", true))
            return views
        }
    }
}
