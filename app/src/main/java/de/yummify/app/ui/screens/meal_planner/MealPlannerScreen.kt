package de.yummify.app.ui.screens.meal_planner

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.yummify.app.data.model.MealPlanItem
import de.yummify.app.data.model.MealType
import de.yummify.app.ui.theme.*

@Composable
fun MealPlannerScreen(
    onRecipeClick: (String) -> Unit,
    viewModel: MealPlannerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val syncRotation by rememberInfiniteTransition(label = "sync").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotation"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Week Navigator
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceContainer, CircleShape)
                ) {
                    Icon(Icons.Filled.ChevronLeft, null, tint = OnSurface)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AKTUELLE WOCHE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        buildString {
                            append(state.weekLabel)
                            append("  ")
                            append(state.weekRange)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceContainer, CircleShape)
                ) {
                    Icon(Icons.Filled.ChevronRight, null, tint = OnSurface)
                }
            }
        }

        // AI Generate pill
        item {
            Button(
                onClick = viewModel::syncFromNotion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryFixed,
                    contentColor = OnPrimaryFixed
                )
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    null,
                    modifier = if (state.isSyncing) Modifier.rotate(syncRotation) else Modifier
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Plan aus Notion generieren",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = SurfaceBackground.copy(alpha = 0.5f)) {
                    Text(
                        "KI Sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnPrimaryFixed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Day selector
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.days.forEach { day ->
                    val isSelected = day.key == state.selectedDayKey
                    Surface(
                        modifier = Modifier
                            .width(if (day.isToday) 56.dp else 48.dp)
                            .clickable { viewModel.selectDay(day.key) },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) Primary else SurfaceContainerLow,
                        shadowElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (day.isToday) "Heute" else day.key,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) OnPrimary else OnSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${day.dayOfMonth}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) OnPrimary else OnSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isSelected) PrimaryFixed else if (day.isToday) Secondary else OutlineVariant,
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Macro tracker card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = SurfaceContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = Primary, modifier = Modifier.size(20.dp))
                            Text(
                                "Tagesübersicht (${state.selectedDayKey})",
                                style = MaterialTheme.typography.titleSmall,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val pct = if (state.targetCalories > 0)
                            (state.totalCalories * 100 / state.targetCalories)
                        else 0
                        Surface(shape = CircleShape, color = SecondaryContainer) {
                            Text(
                                "$pct% Erreicht",
                                style = MaterialTheme.typography.labelMedium,
                                color = Secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Calories bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            buildString {
                                append(state.totalCalories)
                                append(" / ${state.targetCalories} kcal")
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${state.targetCalories - state.totalCalories} kcal übrig",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = {
                            (state.totalCalories.toFloat() / state.targetCalories).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = Primary,
                        trackColor = SurfaceContainerHighest
                    )
                    Spacer(Modifier.height(12.dp))
                    // Protein / Carbs mini bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniMacroBar(
                            label = "Protein",
                            current = state.totalProtein,
                            target = state.targetProtein,
                            color = Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        MiniMacroBar(
                            label = "Kohlenhydrate",
                            current = state.totalCarbs,
                            target = state.targetCarbs,
                            color = Tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Meals header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Restaurant, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Text(
                        "Mahlzeiten-Plan",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "${state.dayMeals.size} Mahlzeiten geplant",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }

        // Meal Slots: always show 4 slots
        val allMealTypes = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)
        items(allMealTypes) { mealType ->
            val meal = state.dayMeals.firstOrNull { it.mealType == mealType }
            MealSlotCard(
                mealType = mealType,
                meal = meal,
                onCardClick = { meal?.recipeId?.let { onRecipeClick(it) } },
                onToggleCooked = { meal?.let { viewModel.toggleCooked(it.id) } },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun MiniMacroBar(
    label: String,
    current: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = SurfaceContainerLow) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                Text(
                    "${current}g / ${target}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (current.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = SurfaceContainerHighest
            )
        }
    }
}

@Composable
private fun MealSlotCard(
    mealType: MealType,
    meal: MealPlanItem?,
    onCardClick: () -> Unit,
    onToggleCooked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = CircleShape, color = SurfaceContainerHigh) {
                        Text(
                            mealType.timeSlot,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        mealType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (meal != null) {
                    Surface(
                        shape = CircleShape,
                        color = if (meal.isCooked) SecondaryContainer else SurfaceContainerHigh,
                        modifier = Modifier.clickable { onToggleCooked() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (meal.isCooked) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked,
                                null,
                                tint = if (meal.isCooked) Secondary else OnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                if (meal.isCooked) "Gekocht" else "Geplant",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (meal.isCooked) Secondary else OnSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (meal != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCardClick() },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = meal.imageUrl,
                        contentDescription = meal.recipeTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            meal.recipeTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.LocalFireDepartment, null, tint = OnSurfaceVariant, modifier = Modifier.size(13.dp))
                                Text("${meal.calories} kcal", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.Schedule, null, tint = OnSurfaceVariant, modifier = Modifier.size(13.dp))
                                Text("${meal.cookTimeMinutes} Min.", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                // Empty slot
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable {},
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceContainer,
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Outline, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Rezept hinzufügen",
                            style = MaterialTheme.typography.labelMedium,
                            color = Outline
                        )
                    }
                }
            }
        }
    }
}
