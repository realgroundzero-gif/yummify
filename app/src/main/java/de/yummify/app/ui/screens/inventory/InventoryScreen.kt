package de.yummify.app.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.InventoryMath
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(onSettings: () -> Unit, viewModel: InventoryViewModel = viewModel()) {
    val stock by viewModel.items.collectAsState()
    val sync by viewModel.sync.collectAsState()
    val message by viewModel.message.collectAsState()
    val saving by viewModel.saving.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(StockFilter.ALL) }
    var location by rememberSaveable { mutableStateOf("Alle Lagerorte") }
    var editing by remember { mutableStateOf<InventoryItem?>(null) }
    var deleting by remember { mutableStateOf<InventoryItem?>(null) }
    val today = LocalDate.now()
    val visible = stock.filter { item ->
        (location == "Alle Lagerorte" || item.location == location) &&
        (query.isBlank() || listOf(item.name, item.category, item.location, item.barcode, item.notes).any { it.contains(query, ignoreCase = true) }) &&
        when (filter) { StockFilter.ALL -> true; StockFilter.LOW -> item.isLow; StockFilter.SOON -> item.expiresSoon(today); StockFilter.EXPIRED -> item.isExpired(today) }
    }.sortedWith(compareBy<InventoryItem> { it.expiry ?: "9999-12-31" }.thenBy { it.name.lowercase() })
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.message.value = null } }
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = 96.dp)) },
        floatingActionButton = { FloatingActionButton(onClick = { editing = InventoryItem() }, modifier = Modifier.padding(bottom = 96.dp)) {
            Icon(Icons.Default.Add, "Artikel hinzufügen")
        } }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Mein Inventar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${stock.size} Artikel · ${stock.count { it.isLow }} nachkaufen", style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = viewModel::sync, enabled = !sync.busy) {
                        if (sync.busy) CircularProgressIndicator(Modifier.size(24.dp)) else Icon(Icons.Default.Sync, "Inventar synchronisieren")
                    }
                }
            }
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(sync.message, style = MaterialTheme.typography.bodySmall)
                        if (sync.pending > 0) Text("${sync.pending} Änderungen warten auf Notion", style = MaterialTheme.typography.labelMedium)
                        if (!sync.configured) TextButton(onClick = onSettings) { Text("Notion-Inventar einrichten") }
                    }
                }
            }
            item {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Artikel, Kategorie oder Barcode suchen") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StockFilter.values().toList()) { choice -> FilterChip(selected = filter == choice, onClick = { filter = choice }, label = { Text(choice.title) }) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Alle Lagerorte") + stock.map { it.location }.distinct().sorted()) { place ->
                        FilterChip(selected = location == place, onClick = { location = place }, label = { Text(place) })
                    }
                }
            }
            if (stock.any { it.isLow }) item {
                OutlinedButton(onClick = { viewModel.toShopping(stock.filter { it.isLow }) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Mindestbestände nachkaufen")
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
            items(visible, key = { it.id }) { item ->
                Card(onClick = { editing = item }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${item.category} · ${item.location}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${InventoryMath.number(item.quantity)} ${item.unit}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        item.expiryDate()?.let { date ->
                            Text("${if (item.isExpired(today)) "Abgelaufen" else "Haltbar bis"}: ${date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                                color = if (item.isExpired(today)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (item.isLow) Text("Mindestbestand: ${InventoryMath.number(item.minimum)} ${item.unit} · Nachkaufen", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        if (item.dirty && sync.configured) Text("Wartet auf Synchronisation", style = MaterialTheme.typography.labelSmall)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.adjust(item.id, -1.0) }, enabled = item.quantity > 0) { Icon(Icons.Default.Remove, "1 ${item.unit} verbrauchen") }
                                Text("1 ${item.unit}", style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = { viewModel.adjust(item.id, 1.0) }) { Icon(Icons.Default.Add, "1 ${item.unit} hinzufügen") }
                            }
                            IconButton(onClick = { viewModel.toShopping(listOf(item)) }) { Icon(Icons.Default.AddShoppingCart, "Artikel zur Einkaufsliste") }
                            IconButton(onClick = { deleting = item }) { Icon(Icons.Default.DeleteOutline, "Artikel löschen") }
                        }
                    }
                }
            }
        }
    }
    editing?.let { item -> InventoryEditor(item, saving, onDismiss = { if (!saving) editing = null }, onSave = { viewModel.save(it) { editing = null } }) }
    deleting?.let { item -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Artikel löschen?") },
        text = { Text("„${item.name}“ wird aus deinem Inventar und beim nächsten Abgleich aus Notion entfernt.") },
        confirmButton = { TextButton(onClick = { viewModel.remove(item.id); deleting = null }) { Text("Löschen") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Abbrechen") } }) }
}

@Composable
private fun InventoryEditor(item: InventoryItem, saving: Boolean, onDismiss: () -> Unit, onSave: (InventoryItem) -> Unit) {
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var quantity by rememberSaveable(item.id) { mutableStateOf(InventoryMath.number(item.quantity)) }
    var unit by rememberSaveable(item.id) { mutableStateOf(item.unit) }
    var category by rememberSaveable(item.id) { mutableStateOf(item.category) }
    var location by rememberSaveable(item.id) { mutableStateOf(item.location) }
    var minimum by rememberSaveable(item.id) { mutableStateOf(InventoryMath.number(item.minimum)) }
    var expiry by rememberSaveable(item.id) { mutableStateOf(item.expiry.orEmpty()) }
    var barcode by rememberSaveable(item.id) { mutableStateOf(item.barcode) }
    var notes by rememberSaveable(item.id) { mutableStateOf(item.notes) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (item.name.isBlank()) "Artikel erfassen" else "Artikel bearbeiten") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(quantity, { quantity = it }, Modifier.weight(1f), label = { Text("Menge") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(unit, { unit = it }, Modifier.weight(1f), label = { Text("Einheit") }, singleLine = true)
                }
                Text("z. B. g, kg, ml, l, Stk, EL oder TL", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(category, { category = it }, label = { Text("Kategorie") }, singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("Lagerort") }, singleLine = true)
                OutlinedTextField(minimum, { minimum = it }, label = { Text("Mindestbestand ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(expiry, { expiry = it }, label = { Text("Ablaufdatum (optional)") }, placeholder = { Text("JJJJ-MM-TT") }, singleLine = true)
                OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode (optional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notizen") }, minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(enabled = !saving, onClick = {
            val edited = item.copy(name = name.trim(), quantity = quantity.replace(',', '.').toDoubleOrNull() ?: Double.NaN,
                unit = unit.trim(), category = category.trim(), location = location.trim(), minimum = minimum.replace(',', '.').toDoubleOrNull() ?: Double.NaN,
                expiry = expiry.trim().ifBlank { null }, barcode = barcode.trim(), notes = notes.trim())
            error = edited.validate()
            if (error == null) onSave(edited)
        }) { Text(if (saving) "Speichert …" else "Speichern") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Abbrechen") } })
}
