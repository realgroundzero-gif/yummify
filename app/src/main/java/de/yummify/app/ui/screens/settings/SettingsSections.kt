package de.yummify.app.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import de.yummify.app.ui.components.hideBottomBarOnScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import de.yummify.app.ui.theme.*


/** Page frame of a settings sub menu: back arrow, title, scrollable body that hides the bottom bar like the lists do. */
@Composable
fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .hideBottomBarOnScroll(scrollState)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(bottom = 100.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
fun ProfileSection(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var name by remember(state.profileName) { mutableStateOf(state.profileName) }
    SettingsPage("Profil", onBack) {
        Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = "Benutzerprofil", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                }
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(name, { name = it.take(60) }, label = { Text("Anzeigename") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp))
                    Text("Bleibt auf diesem Gerät.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        TextButton(onClick = { viewModel.saveProfileName(name) }, enabled = name.trim() != state.profileName, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Namen speichern") }
        // General settings card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    "Abgleich",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                SettingsRow(
                    icon = Icons.Filled.Sync,
                    title = "Auto-Sync",
                    subtitle = if (state.autoSyncEnabled) "Inventar beim Öffnen und nach Änderungen abgleichen" else "Manuelle Synchronisation"
                ) {
                    Switch(
                        checked = state.autoSyncEnabled,
                        onCheckedChange = viewModel::toggleAutoSync,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

    }
}

@Composable
fun ConnectionsSection(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showToken by remember { mutableStateOf(false) }
    SettingsPage("Verbindungen", onBack) {
        // Connection status
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (state.config.isConfigured) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (state.config.isConfigured) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (state.config.isConfigured) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                            null,
                            tint = if (state.config.isConfigured) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (state.config.isConfigured) "Notion verbunden" else "Nicht verbunden",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (state.config.isConfigured) "Zuletzt: ${state.config.lastSyncTime}"
                            else "Konfiguriere deine Notion Integration unten",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (state.config.isConfigured) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::syncNotion,
                        enabled = !state.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Jetzt synchronisieren",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Notion Setup Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Notion Integration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Erstelle einen Notion Integration Token unter notion.so/my-integrations und gib deine Datenbank-IDs ein.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                // Token field
                OutlinedTextField(
                    value = state.tokenInput,
                    onValueChange = viewModel::onTokenChanged,
                    label = { Text("Integration Token (ntn_... oder secret_...)") },
                    placeholder = { Text("ntn_xxxxxxxxxxxxxxxx") },
                    singleLine = true,
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(10.dp))

                // Database ID field
                OutlinedTextField(
                    value = state.databaseIdInput,
                    onValueChange = viewModel::onDatabaseIdChanged,
                    label = { Text("Rezept Datenbank-ID") },
                    placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.TableChart, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.inventoryDatabaseIdInput,
                    onValueChange = viewModel::onInventoryDatabaseIdChanged,
                    label = { Text("Inventar Datenbank-ID (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Das Inventar nutzt denselben Token. Bestehende Tabellen mit Artikel, Bestand, nächstes MHD und Auswahlfeldern werden unterstützt; fehlende Zusatzfelder ergänzt die App beim Verbindungscheck. Neue Datenbanken werden neben der Rezept-Datenbank angelegt. Die übergeordnete Notion-Seite muss für die Integration freigegeben sein.", style = MaterialTheme.typography.bodySmall)
                if (state.inventoryDatabaseIdInput.isBlank()) {
                    OutlinedButton(onClick = viewModel::createInventoryDatabase, enabled = !state.isCreatingInventory, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.isCreatingInventory) "Inventar wird angelegt …" else "Inventar-Datenbank in Notion anlegen")
                    }
                } else {
                    TextButton(onClick = viewModel::testInventoryConnection, enabled = !state.isTestingConnection) { Text("Inventar-Verbindung prüfen") }
                }
                Spacer(Modifier.height(8.dp))

                // Connection result
                state.connectionResult?.let { result ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (result.startsWith("✅")) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            result,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result.startsWith("✅")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::testConnection,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !state.isTestingConnection
                    ) {
                        if (state.isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Filled.Wifi, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Testen")
                    }
                    Button(
                        onClick = viewModel::saveConfig,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Speichern")
                    }
                }
            }
        }

        // Field Mapping info card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Erwartete Notion-Felder",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                val fields = listOf(
                    "Name" to "Titel des Rezepts",
                    "Beschreibung" to "Rich Text – Kurzbeschreibung",
                    "Zutaten" to "Rich Text – Zutaten",
                    "Kalorien" to "Zahl",
                    "Zubereitungszeit" to "Zahl (Minuten)",
                    "Tags" to "Multi-Select (Kategorie)",
                    "Bild" to "URL"
                )
                fields.forEach { (field, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Text(
                                field,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        HomeAssistantCard()
    }
}

/** Info and Rechtliches: sub menus that exist but have no content yet. */
@Composable
fun EmptySection(title: String, onBack: () -> Unit) {
    SettingsPage(title, onBack) { }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}
