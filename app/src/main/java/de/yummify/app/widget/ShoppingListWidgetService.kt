package de.yummify.app.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.data.repository.MealPlanRepository
import de.yummify.app.data.repository.ShoppingListRepository
import java.time.LocalDate

class ShoppingListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = ShoppingListWidgetFactory(applicationContext)
}

internal class ShoppingListWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var items = emptyList<ShoppingItem>()
    private var recipes = emptySet<String>()
    override fun onCreate() = onDataSetChanged()
    override fun onDataSetChanged() {
        recipes = ShoppingListWidgetProvider.todayRecipes(MealPlanRepository.getInstance(context).plannedMeals.value, LocalDate.now())
        items = ShoppingListWidgetProvider.sortedItems(ShoppingListRepository.getInstance(context).items.value, recipes)
    }
    override fun onDestroy() { items = emptyList() }
    override fun getCount() = items.size
    override fun getViewAt(position: Int): RemoteViews? = items.getOrNull(position)?.let { ShoppingListWidgetProvider.buildRow(context, it, recipes) }
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds() = false
}
