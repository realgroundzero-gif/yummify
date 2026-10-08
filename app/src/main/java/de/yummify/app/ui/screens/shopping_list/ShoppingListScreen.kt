package de.yummify.app.ui.screens.shopping_list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import de.yummify.app.ui.components.hideBottomBarOnScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    var showPlanDialog by remember { mutableStateOf(false) }
    var planRange by rememberSaveable { mutableStateOf(PlanRange.NEXT_7_DAYS) }
    var coveredExpanded by rememberSaveable { mutableStateOf(false) }
    if (showPlanDialog) AlertDialog(onDismissRequest = { showPlanDialog = false }, title = { Text("Aus Wochenplan hinzufügen") },
        text = { Column {
            Text("Zutaten der noch nicht gekochten Gerichte, abzüglich Vorrat und dem, was schon auf der Liste steht. Mengen gelten für die im Rezept angegebenen Portionen.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            PlanRange.values().forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { planRange = option }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = planRange == option, onClick = { planRange = option })
                    Text(option.title)
                }
            }
        } },
        confirmButton = { TextButton(onClick = { showPlanDialog = false; viewModel.generateFromPlan(planRange) }) { Text("Hinzufügen") } },
        dismissButton = { TextButton(onClick = { showPlanDialog = false }) { Text("Abbrechen") } })
    if (confirmTransfer) AlertDialog(onDismissRequest = { confirmTransfer = false }, title = { Text("Einkauf ins Inventar übernehmen?") },
        text = { Text("Die abgehakten Artikel werden mit ihren Mengen im Vorratsschrank erfasst und aus der Einkaufsliste entfernt. Lagerort und Ablaufdatum kannst du anschließend im Inventar ergänzen.") },
        confirmButton = { TextButton(onClick = { confirmTransfer = false; viewModel.transferPurchased() }) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = { confirmTransfer = false }) { Text("Abbrechen") } })

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .hideBottomBarOnScroll(listState)
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

        item {
            OutlinedButton(onClick = { showPlanDialog = true }, enabled = !state.planning, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                if (state.planning) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (state.planning) "Wird erstellt …" else "Aus Wochenplan hinzufügen")
            }
        }
        if (state.allItems.any { it.isChecked }) item {
            Button(onClick = { confirmTransfer = true }, enabled = !state.transferring, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Icon(Icons.Default.Inventory2, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(if (state.transferring) "Übernimmt …" else "Gekaufte Artikel ins Inventar (${state.allItems.count { it.isChecked }})")
            }
        }
        state.message?.let { message -> item { Text(message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) } }
        if (state.suggestions.isNotEmpty()) item {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("Vielleicht schon da", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    state.suggestions.forEach { suggestion ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text("${suggestion.itemName}: im Vorrat ${suggestion.stockName} (${suggestion.stockAmount})", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { viewModel.confirmSuggestion(suggestion) }) { Text("Ist vorrätig") }
                                TextButton(onClick = { viewModel.rejectSuggestion(suggestion) }) { Text("Nein, kaufen") }
                            }
                        }
                    }
                }
            }
        }
        if (state.covered.isNotEmpty()) item {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                Column {
                    Row(Modifier.fillMaxWidth().clickable { coveredExpanded = !coveredExpanded }.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Inventory2, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Bereits im Vorrat (${state.covered.size})", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Icon(if (coveredExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, if (coveredExpanded) "Einklappen" else "Ausklappen")
                        TextButton(onClick = viewModel::dismissCovered) { Text("Ausblenden") }
                    }
                    if (coveredExpanded) {
                        state.covered.forEach { entry ->
                            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                    Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("Bedarf ${entry.needed} · ${entry.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { viewModel.restoreCovered(entry.id) }) { Text("Doch kaufen") }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
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
