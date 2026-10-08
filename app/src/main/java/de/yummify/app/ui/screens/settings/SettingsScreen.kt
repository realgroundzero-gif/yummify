package de.yummify.app.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import de.yummify.app.R
import de.yummify.app.ui.components.hideBottomBarOnScroll

/** The sub menus of the settings. [title] is what the user sees in the list and on the page. */
enum class SettingsSection(
    val title: String, val subtitle: String, val icon: ImageVector,
    /** Übersetzte Texte (strings.xml) gehen vor [title] und [subtitle]. */
    @StringRes val titleRes: Int? = null, @StringRes val subtitleRes: Int? = null
) {
    PROFILE("Profil", "Name und Abgleich", Icons.Filled.Person),
    APPEARANCE("", "", Icons.Filled.Palette, R.string.settings_appearance_title, R.string.settings_appearance_subtitle),
    CONNECTIONS("Verbindungen", "Notion-Token und Datenbank-IDs", Icons.Filled.Cloud),
    DATA_CARE("Datenpflege", "Auswahllisten erweitern", Icons.Filled.Tune),
    DATA_BACKUP("", "", Icons.Filled.Storage, R.string.backup_menu_title, R.string.backup_menu_subtitle),
    INFO("Info", "", Icons.Filled.Info),
    LEGAL("Rechtliches", "", Icons.Filled.Gavel)
}

@Composable
fun SettingsScreen() {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val section = SettingsSection.values().firstOrNull { it.name == open }
    val back = { open = null }
    when (section) {
        null -> SettingsMenu { open = it.name }
        SettingsSection.PROFILE -> ProfileSection(back)
        SettingsSection.APPEARANCE -> AppearanceSection(back)
        SettingsSection.CONNECTIONS -> ConnectionsSection(back)
        SettingsSection.DATA_CARE -> DataCareSection(back)
        SettingsSection.DATA_BACKUP -> DataBackupSection(back)
        SettingsSection.INFO -> EmptySection(section.title, back)
        SettingsSection.LEGAL -> EmptySection(section.title, back)
    }
}

@Composable
private fun SettingsMenu(onOpen: (SettingsSection) -> Unit) {
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
        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
        Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(vertical = 8.dp)) {
                SettingsSection.values().forEach { section ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(section) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(section.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(section.titleRes?.let { stringResource(it) } ?: section.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            val subtitle = section.subtitleRes?.let { stringResource(it) } ?: section.subtitle
                            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
