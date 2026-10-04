package de.yummify.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.yummify.app.ui.theme.YummifyTheme

open class MealPlannerWidgetConfigActivity : ComponentActivity() {
    protected open val shoppingWidget = false

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(Activity.RESULT_CANCELED)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val initialTransparency = prefs.getInt("${if (shoppingWidget) "shopping_transparency" else "transparency"}_$appWidgetId", 100)

        setContent {
            YummifyTheme {
                WidgetConfigScreen(
                    initialTransparency = initialTransparency,
                    shoppingWidget = shoppingWidget,
                    onSave = { transparency ->
                        saveWidgetSettings(transparency)
                    },
                    onCancel = {
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(Activity.RESULT_CANCELED, resultValue)
                        finish()
                    }
                )
            }
        }
    }

    private fun saveWidgetSettings(transparency: Int) {
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

        val appWidgetManager = AppWidgetManager.getInstance(this)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            prefs.edit().putInt("${if (shoppingWidget) "shopping_transparency" else "transparency"}_$appWidgetId", transparency).apply()
        }
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (shoppingWidget) ShoppingListWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)
            else MealPlannerWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)
        } else {
            finish()
            return
        }

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(initialTransparency: Int, onSave: (Int) -> Unit, onCancel: () -> Unit, shoppingWidget: Boolean = false) {
    var transparency by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initialTransparency) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val meals by de.yummify.app.data.repository.MealPlanRepository.getInstance(context).plannedMeals.collectAsState()
    val shopping by de.yummify.app.data.repository.ShoppingListRepository.getInstance(context).items.collectAsState()
    Scaffold(modifier = Modifier.systemBarsPadding(),
        topBar = { TopAppBar(title = { Text(if (shoppingWidget) "Einkaufslisten-Widget" else "Wochenwidget") }) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Abbrechen") }
                Button(onClick = { onSave(transparency) }, modifier = Modifier.weight(1f)) { Text("Speichern") }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(if (shoppingWidget) "Deine Einkaufsliste" else "Aktuelle Woche", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF36312D)).padding(12.dp)) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { android.widget.FrameLayout(it) },
                    modifier = Modifier.fillMaxWidth().height(if (shoppingWidget) 360.dp else 240.dp),
                    update = { frame ->
                        frame.removeAllViews()
                        val views = if (shoppingWidget) ShoppingListWidgetProvider.buildViews(context, AppWidgetManager.INVALID_APPWIDGET_ID,
                            items = shopping, meals = meals, transparencyOverride = transparency, preview = true)
                        else MealPlannerWidgetProvider.buildViews(context, AppWidgetManager.INVALID_APPWIDGET_ID,
                            heightDp = 240, transparencyOverride = transparency, plannedMeals = meals)
                        val preview = views.apply(context, frame)
                        frame.addView(preview, android.widget.FrameLayout.LayoutParams(-1, -1))
                    })
            }
            Text(if (shoppingWidget) "Vorschau der ersten sieben Artikel · Zutaten für heutige Gerichte hervorgehoben" else "Montag bis Sonntag · Heute dezent hervorgehoben", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Hintergrund: $transparency %", style = MaterialTheme.typography.titleMedium)
            Slider(value = transparency.toFloat(), onValueChange = { transparency = it.toInt() }, valueRange = 0f..100f)
            Text("Die Transparenz gilt nur für dieses Widget. Text und Tagesmarkierung bleiben lesbar.", style = MaterialTheme.typography.bodySmall)
            Text(if (shoppingWidget) "Die Größe kannst du auf dem Homescreen anpassen (4×2 oder 4×3). Die Liste lässt sich vertikal scrollen. Tippe auf den Kreis zum Abhaken oder auf einen Artikel, um die Einkaufsliste zu öffnen." else "Die Größe kannst du auf dem Homescreen anpassen. Die kompakte Ansicht zeigt alle sieben Tage; größere Widgets bieten mehr Platz für lange Gerichte.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
