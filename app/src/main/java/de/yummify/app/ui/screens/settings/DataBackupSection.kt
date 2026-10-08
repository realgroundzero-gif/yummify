package de.yummify.app.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.yummify.app.R
import de.yummify.app.ui.components.hideBottomBarOnScroll
import de.yummify.app.data.backup.ImportMode
import de.yummify.app.data.backup.formatBytes
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Untermenü "Daten und Backup" der Einstellungen. */
@Composable
fun DataBackupSection(onBack: () -> Unit, viewModel: DataBackupViewModel = viewModel()) {
    BackHandler(onBack = onBack)
    DataBackupScreen(viewModel, onBack)
}

@Composable
private fun DataBackupScreen(viewModel: DataBackupViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { viewModel.export(it) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { viewModel.onImportFilePicked(it) }
    LaunchedEffect(viewModel) { viewModel.exportFileNames.collect { exportLauncher.launch(it) } }
    LaunchedEffect(Unit) { viewModel.refreshUsage() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.backup_back)) }
            Text(stringResource(R.string.backup_menu_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        val scrollState = rememberScrollState()
        Column(Modifier.weight(1f).hideBottomBarOnScroll(scrollState).verticalScroll(scrollState).padding(bottom = 100.dp)) {
            state.message?.let { message ->
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp),
                    color = if (state.messageIsError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.backup_ok)) }
                    }
                }
            }

            BackupCard(stringResource(R.string.backup_export_title), stringResource(R.string.backup_export_description)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.backup_token_switch), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.backup_token_switch_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.includeToken, onCheckedChange = viewModel::onIncludeTokenChange, enabled = !state.busy)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = viewModel::onExportClick, enabled = !state.busy) { Text(stringResource(R.string.backup_export_button)) }
            }

            BackupCard(stringResource(R.string.backup_import_title), stringResource(R.string.backup_import_description)) {
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                    enabled = !state.busy
                ) { Text(stringResource(R.string.backup_import_button)) }
            }

            BackupCard(stringResource(R.string.backup_auto_title), null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (state.autoBackup) R.string.backup_auto_on else R.string.backup_auto_off),
                        Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(checked = state.autoBackup, onCheckedChange = viewModel::setAutoBackup)
                }
            }

            BackupCard(stringResource(R.string.backup_storage_title), null) {
                val usage = state.usage
                UsageRow(stringResource(R.string.backup_storage_data), usage?.dataBytes)
                UsageRow(stringResource(R.string.backup_storage_images), usage?.imageBytes)
                UsageRow(stringResource(R.string.backup_storage_cache), usage?.cacheBytes)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = viewModel::clearCache, enabled = !state.busy) { Text(stringResource(R.string.backup_storage_clear_cache)) }
            }

            BackupCard(stringResource(R.string.backup_delete_title), stringResource(R.string.backup_delete_description)) {
                OutlinedButton(
                    onClick = viewModel::onDeleteAllClick, enabled = !state.busy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.backup_delete_button)) }
            }
        }
    }
    BackupDialogs(state, viewModel)
}

@Composable
private fun BackupCard(title: String, description: String?, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun UsageRow(label: String, bytes: Long?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(bytes?.let { formatBytes(it) } ?: "…", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BackupDialogs(state: DataBackupUiState, viewModel: DataBackupViewModel) {
    when (val dialog = state.dialog) {
        BackupDialog.None -> Unit
        is BackupDialog.TokenExportWarning -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(stringResource(R.string.backup_token_warning_title)) },
            text = { Text(stringResource(R.string.backup_token_warning_text)) },
            confirmButton = { TextButton(onClick = viewModel::confirmTokenWarning) { Text(stringResource(R.string.backup_token_warning_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text(stringResource(R.string.backup_cancel)) } }
        )
        BackupDialog.ImportSummary -> state.pendingImport?.let { parsed ->
            val created = runCatching {
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(parsed.createdAt.atZone(ZoneId.systemDefault()))
            }.getOrDefault(parsed.manifest.createdAt.orEmpty())
            val c = parsed.counts
            AlertDialog(
                onDismissRequest = viewModel::cancelImport,
                title = { Text(stringResource(R.string.backup_import_summary_title)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.backup_import_summary_text, created, parsed.manifest.appVersion.orEmpty(),
                                c.recipes, c.favorites, c.meals, c.shoppingItems, c.inventoryItems, c.images) +
                                if (parsed.manifest.includesToken == true) stringResource(R.string.backup_import_summary_token) else ""
                        )
                        ModeOption(state.importMode == ImportMode.MERGE, R.string.backup_import_mode_merge, R.string.backup_import_mode_merge_hint) { viewModel.setImportMode(ImportMode.MERGE) }
                        ModeOption(state.importMode == ImportMode.REPLACE, R.string.backup_import_mode_replace, R.string.backup_import_mode_replace_hint) { viewModel.setImportMode(ImportMode.REPLACE) }
                    }
                },
                confirmButton = { TextButton(onClick = viewModel::confirmImportSummary) { Text(stringResource(R.string.backup_import_confirm)) } },
                dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.backup_cancel)) } }
            )
        }
        BackupDialog.ReplaceConfirm -> AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.backup_replace_title)) },
            text = { Text(stringResource(R.string.backup_replace_text)) },
            confirmButton = { TextButton(onClick = viewModel::confirmReplace) { Text(stringResource(R.string.backup_replace_confirm), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.backup_cancel)) } }
        )
        BackupDialog.TokenImportQuestion -> AlertDialog(
            onDismissRequest = { viewModel.answerTokenQuestion(false) },
            title = { Text(stringResource(R.string.backup_token_import_title)) },
            text = { Text(stringResource(R.string.backup_token_import_text)) },
            confirmButton = { TextButton(onClick = { viewModel.answerTokenQuestion(true) }) { Text(stringResource(R.string.backup_token_import_yes)) } },
            dismissButton = { TextButton(onClick = { viewModel.answerTokenQuestion(false) }) { Text(stringResource(R.string.backup_token_import_no)) } }
        )
        is BackupDialog.DeleteAll -> {
            val word = stringResource(R.string.backup_delete_word)
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(R.string.backup_delete_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.backup_delete_dialog_text))
                        if (dialog.pendingInventoryChanges > 0) {
                            Text(stringResource(R.string.backup_delete_dirty, dialog.pendingInventoryChanges), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedTextField(
                            value = state.deleteWord, onValueChange = viewModel::onDeleteWordChange, singleLine = true,
                            label = { Text(stringResource(R.string.backup_delete_word_label, word)) }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteAll, enabled = state.deleteWord.trim().equals(word, ignoreCase = true)) {
                        Text(stringResource(R.string.backup_delete_confirm), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text(stringResource(R.string.backup_cancel)) } }
            )
        }
    }
}

@Composable
private fun ModeOption(selected: Boolean, title: Int, hint: Int, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect), verticalAlignment = Alignment.Top) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
