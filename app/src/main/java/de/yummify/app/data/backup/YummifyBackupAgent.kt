package de.yummify.app.data.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor

/**
 * `android:allowBackup` lässt sich zur Laufzeit nicht ändern. Dieser Agent sichert deshalb nur, wenn der Schalter
 * "Automatisches Backup" an ist; dann gelten unverändert `res/xml/backup_rules.xml` und `data_extraction_rules.xml`
 * (ohne Notion-Token). Ist der Schalter aus, werden weder Cloud-Backup noch Geräte-Übertragung mit App-Daten befüllt.
 */
class YummifyBackupAgent : BackupAgent() {
    // Yummify nutzt ausschließlich Full-Backup (android:fullBackupOnly), Key-Value-Methoden bleiben leer.
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit
    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit

    override fun onFullBackup(data: FullBackupDataOutput) {
        if (shouldBackUp(this)) super.onFullBackup(data)
    }

    companion object {
        fun shouldBackUp(context: Context) = BackupSettings.isAutoBackupEnabled(context)
    }
}
