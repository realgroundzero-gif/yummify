package de.yummify.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** Connection to Home Assistant, used to hand the shopping list to the to-do list of the Bring integration. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAssistantCard(viewModel: HomeAssistantViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showToken by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Home Assistant (Bring)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Die Einkaufsliste lässt sich an eine Aufgabenliste in Home Assistant senden, etwa die der Bring-Integration. Lege dafür in Home Assistant unter Profil › Sicherheit einen Langzeit-Zugriffstoken an.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(state.url, viewModel::onUrlChanged, label = { Text("Adresse") }, placeholder = { Text("http://homeassistant.local:8123") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            if (state.url.trim().startsWith("http://")) Text("HTTP ist nur im Heimnetz erlaubt. Der Token wird dabei unverschlüsselt übertragen; für Zugriff von außen HTTPS verwenden.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(state.token, viewModel::onTokenChanged, label = { Text("Langzeit-Zugriffstoken") }, singleLine = true,
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { IconButton(onClick = { showToken = !showToken }) { Icon(if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (showToken) "Token verbergen" else "Token anzeigen") } },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            if (state.lists.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = !menu }) {
                    val current = state.lists.firstOrNull { it.entityId == state.todo }
                    OutlinedTextField(current?.let { it.name + if (it.isBring) " (Bring)" else "" } ?: state.todo, {}, readOnly = true, label = { Text("Einkaufsliste") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) }, singleLine = true,
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        state.lists.forEach { list ->
                            DropdownMenuItem(text = { Text(list.name + if (list.isBring) " (Bring)" else "") }, onClick = { viewModel.onTodoChosen(list.entityId); menu = false })
                        }
                    }
                }
            } else if (state.todo.isNotBlank()) Text("Gewählte Liste: ${state.todo}", style = MaterialTheme.typography.bodyMedium)
            state.message?.let { message ->
                Surface(shape = RoundedCornerShape(12.dp), color = if (state.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer) {
                    Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = viewModel::connect, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                    if (state.busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(if (state.busy) "Prüft …" else "Verbinden & Listen laden")
                }
                Button(onClick = viewModel::save, enabled = !state.busy && state.todo.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Speichern") }
            }
            if (state.url.isNotBlank() || state.token.isNotBlank()) TextButton(onClick = viewModel::disconnect) { Text("Verbindung entfernen") }
        }
    }
}
