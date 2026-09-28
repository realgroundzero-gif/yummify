package de.yummify.app.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import de.yummify.app.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showToken by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Header
        Text(
            "Notion Hub",
            style = MaterialTheme.typography.headlineMedium,
            color = OnSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Connection status
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (state.config.isConfigured) SecondaryContainer.copy(alpha = 0.4f)
            else SurfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (state.config.isConfigured) Secondary.copy(alpha = 0.15f) else SurfaceContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (state.config.isConfigured) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                        null,
                        tint = if (state.config.isConfigured) Secondary else Outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        if (state.config.isConfigured) "Notion verbunden" else "Nicht verbunden",
                        style = MaterialTheme.typography.titleSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (state.config.isConfigured) "Zuletzt: ${state.config.lastSyncTime}"
                        else "Konfiguriere deine Notion Integration unten",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        }

        // Notion Setup Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Notion Integration",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Erstelle einen Notion Integration Token unter notion.so/my-integrations und gib deine Datenbank-IDs ein.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
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
                                tint = OnSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
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
                        Icon(Icons.Filled.TableChart, null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                Spacer(Modifier.height(16.dp))

                // Connection result
                state.connectionResult?.let { result ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (result.startsWith("✅")) SecondaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            result,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result.startsWith("✅")) Secondary else MaterialTheme.colorScheme.onErrorContainer,
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Primary)
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
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnPrimary)
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
            color = SurfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Erwartete Notion-Felder",
                    style = MaterialTheme.typography.titleSmall,
                    color = OnSurface,
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
                        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerHigh) {
                            Text(
                                field,
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                }
            }
        }

        // General settings card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    "Einstellungen",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                SettingsRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Dark Mode",
                    subtitle = if (state.darkModeEnabled) "Dunkles Erscheinungsbild aktiv" else "Helles Erscheinungsbild aktiv"
                ) {
                    Switch(
                        checked = state.darkModeEnabled,
                        onCheckedChange = viewModel::toggleDarkMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    title = "Mahlzeiten-Erinnerungen",
                    subtitle = if (state.remindersEnabled) "Kochzeiten & Pläne benachrichtigen" else "Erinnerungen stummgeschaltet"
                ) {
                    Switch(
                        checked = state.remindersEnabled,
                        onCheckedChange = viewModel::toggleReminders,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Sync,
                    title = "Auto-Sync",
                    subtitle = if (state.autoSyncEnabled) "Alle 15 Minuten synchronisieren" else "Manuelle Synchronisation"
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

        // App info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🍊 yummify",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Version 1.0.0 • Powered by Notion",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
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
                    .background(SurfaceContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = OnSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
        trailing()
    }
}
