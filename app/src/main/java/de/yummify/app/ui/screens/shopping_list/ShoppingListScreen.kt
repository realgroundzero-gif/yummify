package de.yummify.app.ui.screens.shopping_list

import androidx.compose.animation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.yummify.app.data.model.ShoppingItem
import de.yummify.app.ui.theme.*

val CategoryEmoji = mapOf(
    "Obst & Gemüse" to "🥦",
    "Kühlregal & Milchprodukte" to "🧀",
    "Vorrat & Trockenwaren" to "🌾",
    "Gewürze & Öle" to "🧄",
    "Fisch & Fleisch" to "🐟",
    "Bäckerei" to "🥐",
    "Tiefkühl" to "❄️"
)

@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val syncRotation by rememberInfiniteTransition(label = "sync_rot").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "r"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Einkaufsliste",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(shape = CircleShape, color = PrimaryFixed) {
                        Text(
                            "${state.openItems} offen",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnPrimaryFixed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.MoreVert, null, tint = OnSurfaceVariant)
                }
            }
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.filter == "missing",
                    onClick = { viewModel.applyFilter("missing") },
                    label = { Text("Nur fehlende (${state.openItems})", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary
                    ),
                    shape = CircleShape
                )
                FilterChip(
                    selected = state.filter == "all",
                    onClick = { viewModel.applyFilter("all") },
                    label = { Text("Alle (${state.allItems.size})", style = MaterialTheme.typography.labelMedium) },
                    shape = CircleShape
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = viewModel::clearDoneItems) {
                    Icon(Icons.Filled.ClearAll, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Erledigte löschen", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Sync banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Secondary.copy(alpha = 0.12f),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Sync,
                                    null,
                                    tint = Secondary,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .then(if (state.isSyncing) Modifier.rotate(syncRotation) else Modifier)
                                )
                            }
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(Secondary, CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                            )
                        }
                        Column {
                            Text(
                                "Notion-Sync: Yummify - Einkaufe",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Vor 2 Min. synchronisiert • Bidirektional aktiv",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = viewModel::triggerSync) {
                        Icon(
                            Icons.Filled.Refresh,
                            null,
                            tint = Secondary,
                            modifier = if (state.isSyncing) Modifier.rotate(syncRotation) else Modifier
                        )
                    }
                }
            }
        }

        // Preview banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = SurfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "WOCHENPLAN ZUTATEN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Frische Vorräte bereitstellen",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Automatisch aggregiert aus ${state.allItems.distinctBy { it.recipeName }.size} Rezepten",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=200&q=80",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Grouped category sections
        state.groupedItems.forEach { (category, items) ->
            item {
                CollapsibleSection(
                    title = category,
                    emoji = CategoryEmoji[category] ?: "🛒",
                    openCount = items.count { !it.isChecked },
                    items = items,
                    onToggle = viewModel::toggleItem
                )
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    emoji: String,
    openCount: Int,
    items: List<ShoppingItem>,
    onToggle: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$openCount von ${items.size} offen",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    tint = OnSurfaceVariant
                )
            }

            // Items
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEach { item ->
                        ShoppingItemRow(item = item, onToggle = { onToggle(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItem,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        color = if (item.isChecked) SurfaceContainer.copy(alpha = 0.5f) else SurfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (item.isChecked) Secondary else SurfaceContainerHighest,
                        RoundedCornerShape(7.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.isChecked) {
                    Icon(Icons.Filled.Check, null, tint = OnSecondary, modifier = Modifier.size(15.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${item.amountWithUnit} ${item.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isChecked) OnSurfaceVariant else OnSurface,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                item.recipeName?.let { recipe ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.MenuBook, null, tint = Primary, modifier = Modifier.size(11.dp))
                        Text(recipe, style = MaterialTheme.typography.labelSmall, color = Primary)
                    }
                }
                item.note?.let { note ->
                    if (item.recipeName == null) {
                        Text(note, style = MaterialTheme.typography.labelSmall, color = Tertiary)
                    }
                }
            }
            Icon(
                Icons.Filled.DragIndicator,
                null,
                tint = Outline.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
