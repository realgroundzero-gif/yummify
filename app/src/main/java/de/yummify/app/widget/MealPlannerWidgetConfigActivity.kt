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

class MealPlannerWidgetConfigActivity : ComponentActivity() {

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
        val initialTransparency = prefs.getInt("transparency_$appWidgetId", 100)

        setContent {
            YummifyTheme {
                WidgetConfigScreen(
                    initialTransparency = initialTransparency,
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
            prefs.edit().putInt("transparency_$appWidgetId", transparency).apply()
        }
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            MealPlannerWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)
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
fun WidgetConfigScreen(initialTransparency: Int, onSave: (Int) -> Unit, onCancel: () -> Unit) {
    var transparency by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initialTransparency) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val meals by de.yummify.app.data.repository.MealPlanRepository.getInstance(context).plannedMeals.collectAsState()
    Scaffold(modifier = Modifier.systemBarsPadding(),
        topBar = { TopAppBar(title = { Text("Wochenwidget") }) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Abbrechen") }
                Button(onClick = { onSave(transparency) }, modifier = Modifier.weight(1f)) { Text("Speichern") }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Aktuelle Woche", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF36312D)).padding(12.dp)) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { android.widget.FrameLayout(it) },
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    update = { frame ->
                        frame.removeAllViews()
                        val preview = MealPlannerWidgetProvider.buildViews(context, AppWidgetManager.INVALID_APPWIDGET_ID,
                            heightDp = 240, transparencyOverride = transparency, plannedMeals = meals).apply(context, frame)
                        frame.addView(preview, android.widget.FrameLayout.LayoutParams(-1, -1))
                    })
            }
            Text("Montag bis Sonntag · Heute dezent hervorgehoben", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Hintergrund: $transparency %", style = MaterialTheme.typography.titleMedium)
            Slider(value = transparency.toFloat(), onValueChange = { transparency = it.toInt() }, valueRange = 0f..100f)
            Text("Die Transparenz gilt nur für dieses Widget. Text und Tagesmarkierung bleiben lesbar.", style = MaterialTheme.typography.bodySmall)
            Text("Die Größe kannst du auf dem Homescreen anpassen. Die kompakte Ansicht zeigt alle sieben Tage; größere Widgets bieten mehr Platz für lange Gerichte.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
