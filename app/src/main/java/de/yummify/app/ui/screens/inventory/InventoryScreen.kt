package de.yummify.app.ui.screens.inventory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import de.yummify.app.ui.components.*
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.InventoryMath
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(onSettings: () -> Unit, bottomBarInset: () -> Dp = NoBottomInset, viewModel: InventoryViewModel = viewModel()) {
    val stock by viewModel.items.collectAsState()
    val sync by viewModel.sync.collectAsState()
    val message by viewModel.message.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val choices by viewModel.choices.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(StockFilter.ALL) }
    var location by rememberSaveable { mutableStateOf("Alle Lagerorte") }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val newItem = remember(editing) { InventoryItem(id = editing?.removePrefix("new:") ?: java.util.UUID.randomUUID().toString()) }
    var deleting by remember { mutableStateOf<InventoryItem?>(null) }
    var showSyncDetails by remember { mutableStateOf(false) }
    var showLocations by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val visible = stock.filter { item ->
        (location == "Alle Lagerorte" || item.location == location) &&
        (query.isBlank() || listOf(item.name, item.category, item.location, item.barcode, item.notes).any { it.contains(query, ignoreCase = true) }) &&
        when (filter) { StockFilter.ALL -> true; StockFilter.LOW -> item.isLow; StockFilter.SOON -> item.expiresSoon(today); StockFilter.EXPIRED -> item.isExpired(today) }
    }.sortedWith(compareBy<InventoryItem> { it.expiry ?: "9999-12-31" }.thenBy { it.name.lowercase() })
    val grouped = visible.groupBy { it.category.trim().ifBlank { "Ohne Kategorie" } }.toSortedMap()
    var collapsedCategories by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.message.value = null } }
    Scaffold(
        modifier = Modifier.fillMaxSize().bottomInset(bottomBarInset),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = { editing = "new:${java.util.UUID.randomUUID()}" }) {
            Icon(Icons.Default.Add, "Artikel hinzufügen")
        } }
    ) { padding ->
        val listState = rememberLazyListState()
        val headerBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        KeepHeaderReachable(headerBehavior, listState)
        Column(Modifier.fillMaxSize().padding(padding).nestedScroll(headerBehavior.nestedScrollConnection)) {
          // Search and status scroll away with the finger; the filter chips stay.
          CollapsingHeader(headerBehavior, Modifier.padding(horizontal = 16.dp)) {
            Column(Modifier.padding(top = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(Modifier.weight(1f), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        BasicTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            decorationBox = { input ->
                                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Box(Modifier.weight(1f)) {
                                        if (query.isEmpty()) Text("Vorrat durchsuchen", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                                        input()
                                    }
                                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Suche löschen", Modifier.size(20.dp)) }
                                    else Spacer(Modifier.width(12.dp))
                                }
                            }
                        )
                    }
                    FilledTonalIconButton(onClick = viewModel::sync, enabled = !sync.busy) {
                        if (sync.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Sync, "Inventar synchronisieren")
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${visible.size} Artikel", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Row(Modifier.weight(1f).clickable { showSyncDetails = true }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                        Icon(if (sync.pending > 0) Icons.Default.CloudUpload else if (sync.configured) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(if (sync.pending > 0) "${sync.pending} ausstehend" else if (sync.configured) "Notion · Status" else "Lokal · Einrichten",
                            style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
          }
          run {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    item {
                        Box {
                            AssistChip(onClick = { showLocations = true }, label = { Text(if (location == "Alle Lagerorte") "Lagerort" else location) },
                                leadingIcon = { Icon(Icons.Default.Place, null, Modifier.size(16.dp)) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) })
                            DropdownMenu(expanded = showLocations, onDismissRequest = { showLocations = false }) {
                                (listOf("Alle Lagerorte") + stock.map { it.location }.filter { it.isNotBlank() }.distinct().sorted()).forEach { place ->
                                    DropdownMenuItem(text = { Text(place) }, onClick = { location = place; showLocations = false },
                                        trailingIcon = { if (location == place) Icon(Icons.Default.Check, null) })
                                }
                            }
                        }
                    }
                    items(StockFilter.values().toList()) { choice ->
                        FilterChip(selected = filter == choice, onClick = { filter = choice }, label = { Text(choice.title) },
                            leadingIcon = if (filter == choice) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null)
                    }
                }
            }
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().hideBottomBarOnScroll(listState), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (stock.any { it.isLow }) item {
                TextButton(onClick = { viewModel.toShopping(stock.filter { it.isLow }) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.AddShoppingCart, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("${stock.count { it.isLow }} Artikel nachkaufen")
                }
            }
            if (visible.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(if (stock.isEmpty()) "Dein Vorrat beginnt hier" else "Keine passenden Artikel", style = MaterialTheme.typography.titleMedium)
                    Text(if (stock.isEmpty()) "Erfasse Lebensmittel mit Menge, Lagerort und Ablaufdatum." else "Passe Suche oder Filter an.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            grouped.forEach { (category, categoryItems) ->
                stickyHeader(key = "category:$category") {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer, tonalElevation = 2.dp) {
                        Row(Modifier.fillMaxWidth().clickable {
                            collapsedCategories = if (category in collapsedCategories) collapsedCategories - category else collapsedCategories + category
                        }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary)
                            Text(category, modifier = Modifier.weight(1f).semantics { heading() },
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary) {
                                Text("${categoryItems.size}", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            Icon(if (category in collapsedCategories) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                if (category in collapsedCategories) "Kategorie ausklappen" else "Kategorie einklappen")
                        }
                    }
                }
                if (category !in collapsedCategories) items(categoryItems, key = { "article:${it.id}" }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) deleting = item
                        // Keep the row in place until deletion is explicitly confirmed.
                        false
                    })
                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier.semantics {
                            customActions = listOf(CustomAccessibilityAction("Artikel löschen") { deleting = item; true })
                        },
                        backgroundContent = {
                            Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                Box(Modifier.padding(horizontal = 20.dp), contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) {
                                    Text("Löschen", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    ) {
                        OutlinedCard(onClick = { editing = item.id }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        val details = listOf(item.category, item.location).filter { it.isNotBlank() }.joinToString(" · ")
                                        if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text("${InventoryMath.number(item.quantity)} ${item.unit}".trim(), Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                item.expiryDate()?.let { date ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val color = if (item.isExpired(today)) MaterialTheme.colorScheme.error else if (item.expiresSoon(today)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        Icon(if (item.isExpired(today)) Icons.Default.WarningAmber else Icons.Default.Event, null, Modifier.size(16.dp), tint = color)
                                        Text("${if (item.isExpired(today)) "Abgelaufen" else "MHD"} · ${date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                                            style = MaterialTheme.typography.labelMedium, color = color)
                                    }
                                }
                                if (item.isLow) Text("Nachkaufen · Mindestbestand ${InventoryMath.number(item.minimum)} ${item.unit}",
                                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.adjust(item.id, -1.0) }, enabled = item.quantity > 0) { Icon(Icons.Default.Remove, "1 ${item.unit} verbrauchen", Modifier.size(20.dp)) }
                                    Text("1 ${item.unit}".trim(), style = MaterialTheme.typography.labelMedium)
                                    IconButton(onClick = { viewModel.adjust(item.id, 1.0) }) { Icon(Icons.Default.Add, "1 ${item.unit} hinzufügen", Modifier.size(20.dp)) }
                                    Spacer(Modifier.weight(1f))
                                    if (item.dirty && sync.configured) Icon(Icons.Default.CloudUpload, "Wartet auf Synchronisation", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    IconButton(onClick = { viewModel.toShopping(listOf(item)) }) { Icon(Icons.Default.AddShoppingCart, "Artikel zur Einkaufsliste", Modifier.size(20.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
    if (showSyncDetails) AlertDialog(onDismissRequest = { showSyncDetails = false },
        title = { Text("Inventar-Synchronisation") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(sync.message)
            if (sync.pending > 0) Text("${sync.pending} Änderungen warten auf Notion.")
        } },
        confirmButton = { TextButton(onClick = { showSyncDetails = false; if (sync.configured) viewModel.sync() else onSettings() }, enabled = !sync.busy) {
            Text(if (sync.configured) "Synchronisieren" else "Einrichten")
        } }, dismissButton = { TextButton(onClick = { showSyncDetails = false }) { Text("Schließen") } })
    LaunchedEffect(editing) { if (editing != null) viewModel.refreshChoices(editing) }
    editing?.let { id ->
        val item = if (id.startsWith("new:")) newItem else stock.firstOrNull { it.id == id }
        item?.let { current ->
            val snapshot = remember(id) { current }
            InventoryDetailScreen(snapshot.copy(coverUrl = current.coverUrl), choices, saving, onDismiss = { if (!saving) editing = null }, onSave = { value -> viewModel.save(value) { editing = null } }, saveError = message, stock = stock, onOpenExisting = { editing = it }) }
    }
    deleting?.let { item -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Artikel löschen?") },
        text = { Text("„${item.name}“ wird aus deinem Inventar und beim nächsten Abgleich aus Notion entfernt.") },
        confirmButton = { TextButton(onClick = { viewModel.remove(item.id); deleting = null }) { Text("Löschen") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Abbrechen") } }) }
}
