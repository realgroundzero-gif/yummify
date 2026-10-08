package de.yummify.app.ui.screens.settings

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.yummify.app.R
import de.yummify.app.data.backup.BackupException
import de.yummify.app.data.backup.BackupManager
import de.yummify.app.data.backup.BackupSettings
import de.yummify.app.data.backup.ImportMode
import de.yummify.app.data.backup.ParsedBackup
import de.yummify.app.data.backup.StorageLayout
import de.yummify.app.data.backup.StorageUsage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

/** Welcher Dialog gerade offen ist; immer höchstens einer. */
sealed interface BackupDialog {
    data object None : BackupDialog
    /** [thenExport]: Die Warnung kam beim Klick auf "Exportieren" (sonst beim Einschalten der Option). */
    data class TokenExportWarning(val thenExport: Boolean) : BackupDialog
    data object ImportSummary : BackupDialog
    data object ReplaceConfirm : BackupDialog
    data object TokenImportQuestion : BackupDialog
    data class DeleteAll(val pendingInventoryChanges: Int) : BackupDialog
}

data class DataBackupUiState(
    val includeToken: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val autoBackup: Boolean = true,
    val usage: StorageUsage? = null,
    val pendingImport: ParsedBackup? = null,
    val importMode: ImportMode = ImportMode.MERGE,
    val dialog: BackupDialog = BackupDialog.None,
    val deleteWord: String = ""
)

class DataBackupViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = BackupManager.create(application)
    private val _uiState = MutableStateFlow(DataBackupUiState(autoBackup = BackupSettings.isAutoBackupEnabled(application)))
    val uiState: StateFlow<DataBackupUiState> = _uiState.asStateFlow()

    private val exportRequests = Channel<String>(Channel.BUFFERED)
    /** Dateiname für den Android-Dateiwähler; die Oberfläche startet damit `CreateDocument`. */
    val exportFileNames = exportRequests.receiveAsFlow()

    private fun text(id: Int, vararg args: Any) = getApplication<Application>().getString(id, *args)
    private fun update(change: (DataBackupUiState) -> DataBackupUiState) { _uiState.value = change(_uiState.value) }
    private fun show(message: String, error: Boolean = false) = update { it.copy(message = message, messageIsError = error, busy = false) }
    private fun fail(e: Exception, id: Int) = show(text(id, e.message ?: e.javaClass.simpleName), error = true)

    init { refreshUsage() }

    fun refreshUsage() {
        viewModelScope.launch {
            val usage = withContext(Dispatchers.IO) { manager.storageUsage() }
            update { it.copy(usage = usage) }
        }
    }

    fun dismissMessage() = update { it.copy(message = null) }
    fun dismissDialog() = update { it.copy(dialog = BackupDialog.None, deleteWord = "") }

    // ---- Export ----

    fun onIncludeTokenChange(enabled: Boolean) {
        if (enabled) update { it.copy(dialog = BackupDialog.TokenExportWarning(thenExport = false)) }
        else update { it.copy(includeToken = false) }
    }

    fun onExportClick() {
        if (_uiState.value.includeToken) update { it.copy(dialog = BackupDialog.TokenExportWarning(thenExport = true)) }
        else exportRequests.trySend(exportFileName())
    }

    fun confirmTokenWarning() {
        val dialog = _uiState.value.dialog as? BackupDialog.TokenExportWarning ?: return
        update { it.copy(includeToken = true, dialog = BackupDialog.None) }
        if (dialog.thenExport) exportRequests.trySend(exportFileName())
    }

    private fun exportFileName() = "yummify-backup-${LocalDate.now()}.zip"

    /** Ergebnis des Dateiwählers; null = abgebrochen. */
    fun export(uri: Uri?) {
        if (uri == null) { show(text(R.string.backup_export_cancelled)); return }
        val includeToken = _uiState.value.includeToken
        update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    try {
                        val stream = resolver.openOutputStream(uri, "wt") ?: throw BackupException("Die Zieldatei konnte nicht geöffnet werden.")
                        stream.use { manager.export(it, includeToken) }
                    } catch (e: CancellationException) { throw e
                    } catch (e: Exception) {
                        // Keine halbfertige Datei zurücklassen: Das Dokument wurde schon vom Dateiwähler angelegt.
                        val removed = runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)
                        throw BackupException((e.message ?: "unbekannter Fehler") + text(if (removed) R.string.backup_export_partial_removed else R.string.backup_export_partial_kept), e)
                    }
                }
                val c = result.counts
                show(text(R.string.backup_export_success, c.total, c.recipes, c.favorites, c.meals, c.shoppingItems, c.inventoryItems, c.images) +
                    (if (result.includesToken) text(R.string.backup_export_success_token) else "") +
                    result.warnings.joinToString("") { " $it" })
            } catch (e: CancellationException) { throw e
            } catch (e: BackupException) { fail(e, R.string.backup_export_failed)
            } catch (e: IOException) { fail(e, R.string.backup_export_failed) }
        }
    }

    // ---- Import ----

    /** Ergebnis des Dateiwählers; null = abgebrochen. Liest die Datei vollständig, ändert aber nichts. */
    fun onImportFilePicked(uri: Uri?) {
        if (uri == null) { show(text(R.string.backup_import_cancelled)); return }
        update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val parsed = withContext(Dispatchers.IO) {
                    val stream = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?: throw BackupException("Die Datei konnte nicht geöffnet werden.")
                    stream.buffered().use { manager.prepareImport(it) }
                }
                update { it.copy(busy = false, pendingImport = parsed, importMode = ImportMode.MERGE, dialog = BackupDialog.ImportSummary) }
            } catch (e: CancellationException) { throw e
            } catch (e: BackupException) { fail(e, R.string.backup_import_failed)
            } catch (e: IOException) { fail(e, R.string.backup_import_failed) }
        }
    }

    fun setImportMode(mode: ImportMode) = update { it.copy(importMode = mode) }

    fun cancelImport() {
        _uiState.value.pendingImport?.let { manager.discardImport(it) }
        update { it.copy(pendingImport = null, dialog = BackupDialog.None) }
        show(text(R.string.backup_import_cancelled))
    }

    fun confirmImportSummary() {
        if (_uiState.value.importMode == ImportMode.REPLACE) update { it.copy(dialog = BackupDialog.ReplaceConfirm) } else askTokenOrApply()
    }

    fun confirmReplace() = askTokenOrApply()

    private fun askTokenOrApply() {
        val parsed = _uiState.value.pendingImport ?: return
        val hasToken = parsed.data.userPrefs.any { (key, value) -> key in StorageLayout.secretUserKeys && (value as? String).orEmpty().isNotBlank() }
        if (hasToken) update { it.copy(dialog = BackupDialog.TokenImportQuestion) } else applyImport(takeToken = false)
    }

    fun answerTokenQuestion(takeToken: Boolean) = applyImport(takeToken)

    private fun applyImport(takeToken: Boolean) {
        val state = _uiState.value
        val parsed = state.pendingImport ?: return
        update { it.copy(busy = true, dialog = BackupDialog.None, message = null) }
        viewModelScope.launch {
            try {
                val result = manager.applyImport(parsed, state.importMode, takeToken)
                update { it.copy(pendingImport = null) }
                show(text(if (result.mode == ImportMode.MERGE) R.string.backup_import_success_merge else R.string.backup_import_success_replace) +
                    result.warnings.joinToString("") { " $it" })
                refreshUsage()
            } catch (e: CancellationException) { throw e
            } catch (e: BackupException) {
                update { it.copy(pendingImport = null) }
                manager.discardImport(parsed)
                show(e.message.orEmpty(), error = true)
                refreshUsage()
            }
        }
    }

    // ---- Automatisches Backup ----

    fun setAutoBackup(enabled: Boolean) {
        try {
            BackupSettings.setAutoBackupEnabled(getApplication(), enabled)
            update { it.copy(autoBackup = enabled) }
        } catch (e: IllegalStateException) { fail(e, R.string.backup_auto_failed) }
    }

    // ---- Speicher und Löschen ----

    fun clearCache() {
        update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { manager.clearCache() }
                show(text(R.string.backup_storage_cache_cleared))
            } catch (e: CancellationException) { throw e
            } catch (e: BackupException) { fail(e, R.string.backup_storage_failed) }
            refreshUsage()
        }
    }

    fun onDeleteAllClick() = update { it.copy(dialog = BackupDialog.DeleteAll(manager.pendingInventoryChanges), deleteWord = "") }

    fun onDeleteWordChange(value: String) = update { it.copy(deleteWord = value) }

    fun confirmDeleteAll() {
        val state = _uiState.value
        if (!state.deleteWord.trim().equals(text(R.string.backup_delete_word), ignoreCase = true)) return
        update { it.copy(busy = true, dialog = BackupDialog.None, deleteWord = "", message = null) }
        viewModelScope.launch {
            try {
                manager.deleteAllLocalData()
                show(text(R.string.backup_delete_success))
            } catch (e: CancellationException) { throw e
            } catch (e: BackupException) { fail(e, R.string.backup_delete_failed) }
            refreshUsage()
        }
    }

    override fun onCleared() {
        _uiState.value.pendingImport?.let { manager.discardImport(it) }
    }
}
