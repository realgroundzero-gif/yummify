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
import androidx.compose.material.icons.automirrored.filled.*
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
    var confirmTransfer by remember { mutableStateOf(false) }
    if (confirmTransfer) AlertDialog(onDismissRequest = { confirmTransfer = false }, title = { Text("Einkauf ins Inventar übernehmen?") },
        text = { Text("Die abgehakten Artikel werden mit ihren Mengen im Vorratsschrank erfasst und aus der Einkaufsliste entfernt. Lagerort und Ablaufdatum kannst du anschließend im Inventar ergänzen.") },
        confirmButton = { TextButton(onClick = { confirmTransfer = false; viewModel.transferPurchased() }) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = { confirmTransfer = false }) { Text("Abbrechen") } })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "${state.openItems} offen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
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
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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

        if (state.allItems.any { it.isChecked }) item {
            Button(onClick = { confirmTransfer = true }, enabled = !state.transferring, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Icon(Icons.Default.Inventory2, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(if (state.transferring) "Übernimmt …" else "Gekaufte Artikel ins Inventar (${state.allItems.count { it.isChecked }})")
            }
        }
        state.message?.let { message -> item { Text(message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) } }
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
        color = MaterialTheme.colorScheme.surfaceContainerLow
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$openCount von ${items.size} offen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
        color = if (item.isChecked) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer
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
                        if (item.isChecked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(7.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.isChecked) {
                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(15.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${item.amountWithUnit} ${item.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                item.recipeName?.let { recipe ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp))
                        Text(recipe, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                item.note?.let { note ->
                    if (item.recipeName == null) {
                        Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            Icon(
                Icons.Filled.DragIndicator,
                null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
