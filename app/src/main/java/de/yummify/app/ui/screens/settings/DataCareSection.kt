package de.yummify.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.yummify.app.data.remote.DropdownField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DataCareSection(onBack: () -> Unit, viewModel: DataCareViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    SettingsPage("Datenpflege", onBack) {
        Text(
            "Hier erweiterst du die Auswahllisten der Notion-Datenbanken, zum Beispiel Kategorie oder Lagerort. Neue Einträge werden sofort in Notion angelegt und stehen danach in der App zur Wahl. Vorhandene Einträge bleiben unverändert, die App benennt oder löscht nichts.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        state.message?.let { message ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(message, Modifier.weight(1f).padding(vertical = 10.dp), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = viewModel::clearMessage) { Text("OK") }
                }
            }
        }
        DataArea.values().forEach { area ->
            val areaState = state.areas.getValue(area)
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(area.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (areaState.configured) IconButton(onClick = { viewModel.load(area) }, enabled = !areaState.loading) { Icon(Icons.Filled.Refresh, "${area.title} neu laden") }
            }
            when {
                !areaState.configured -> Hint(if (area == DataArea.RECIPES) "Keine Rezept-Datenbank verbunden. Das richtest du unter „Verbindungen“ ein."
                    else "Keine Inventar-Datenbank verbunden. Das richtest du unter „Verbindungen“ ein.")
                areaState.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
                areaState.error != null -> Hint("Notion konnte nicht gelesen werden: ${areaState.error}")
                areaState.fields.isEmpty() -> Hint("Diese Datenbank hat keine Auswahlfelder (Select oder Multi-Select). Textfelder lassen sich nicht erweitern, und die App wandelt keine Spalten um.")
                else -> areaState.fields.forEach { field ->
                    FieldCard(area, field, saving = state.saving == "${area.name}/${field.name}", busy = state.saving != null,
                        onAdd = { entry, done -> viewModel.add(area, field.name, entry, done) })
                }
            }
        }
        DataBackupSection()
    }
}

@Composable
private fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldCard(area: DataArea, field: DropdownField, saving: Boolean, busy: Boolean, onAdd: (String, () -> Unit) -> Unit) {
    var entry by rememberSaveable("${area.name}/${field.name}") { mutableStateOf("") }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp)) {
            Text(field.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(if (field.type == "multi_select") "Mehrfachauswahl · ${field.options.size} Einträge" else "Einzelauswahl · ${field.options.size} Einträge",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (field.options.isEmpty()) Text("Noch keine Einträge.", style = MaterialTheme.typography.bodySmall)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                field.options.forEach { AssistChip(onClick = {}, label = { Text(it.name) }, enabled = false) }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(entry, { entry = it }, label = { Text("Neuer Eintrag") }, singleLine = true, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp), enabled = !busy)
                FilledTonalIconButton(onClick = { onAdd(entry) { entry = "" } }, enabled = !busy && entry.isNotBlank()) {
                    if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.Add, "Zu ${field.name} hinzufügen")
                }
            }
        }
    }
}
