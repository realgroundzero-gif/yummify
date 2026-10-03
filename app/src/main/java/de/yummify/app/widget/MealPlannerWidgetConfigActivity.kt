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
fun WidgetConfigScreen(
    initialTransparency: Int,
    onSave: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var transparency by remember { mutableStateOf(initialTransparency) }

    val presetValues = listOf(
        100 to "Deckend (100%)",
        80 to "Leicht Transparent (80%)",
        50 to "Halbtransparent (50%)",
        20 to "Glasmorphism (20%)",
        0 to "Vollständig Transparent (0%)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Widget Anpassen", fontWeight = FontWeight.Bold)
                        Text(
                            "yummify Wochenplaner Widget",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }

                    Button(
                        onClick = { onSave(transparency) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC85A32)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Speichern")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Preview Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "LIVE VORSCHAU",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC85A32),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated Wallpaper Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2C3E50),
                                        Color(0xFF4CA1AF),
                                        Color(0xFFC85A32)
                                    )
                                )
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val alpha = transparency / 100f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF171412).copy(alpha = alpha))
                                .border(1.dp, Color(0xFF36302C).copy(alpha = alpha.coerceAtLeast(0.3f)), RoundedCornerShape(20.dp))
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFC85A32)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("y", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("yummify", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Surface(
                                        color = Color(0xFFC85A32),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("KW 40", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEachIndexed { index, day ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (index == 4) Color(0xFFC85A32) else Color(0xFF211E1C))
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(day, color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                }

                                Surface(
                                    color = Color(0xFF2A221F),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("FREITAG • ABENDESSEN", color = Color(0xFFFF9364), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text("Cremige Tomaten-Burrata-Pasta", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Surface(
                                            color = Color(0xFFC85A32),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("▷ Kochen", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Transparency Control Card
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFC85A32))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Hintergrund-Transparenz", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Stelle die Transparenz des Widgets ein", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Transparenz", fontSize = 14.sp)
                            Text("$transparency%", fontWeight = FontWeight.Bold, color = Color(0xFFC85A32))
                        }

                        Slider(
                            value = transparency.toFloat(),
                            onValueChange = { transparency = it.toInt() },
                            valueRange = 0f..100f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFC85A32),
                                activeTrackColor = Color(0xFFC85A32)
                            )
                        )
                    }

                    HorizontalDivider()

                    Text("Schnellauswahl", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    presetValues.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { transparency = value }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 14.sp)
                            RadioButton(
                                selected = transparency == value,
                                onClick = { transparency = value },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC85A32))
                            )
                        }
                    }
                }
            }
        }
    }
}
