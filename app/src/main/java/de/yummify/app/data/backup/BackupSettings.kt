package de.yummify.app.data.backup

import android.content.Context

/** Schalter "Automatisches Backup" (Standard: an). Gerätespezifisch: nicht Teil von Export und Löschen. */
object BackupSettings {
    private const val KEY_AUTO_BACKUP = "auto_backup"

    private fun prefs(context: Context) = context.getSharedPreferences(StorageLayout.PREFS_BACKUP_SETTINGS, Context.MODE_PRIVATE)

    fun isAutoBackupEnabled(context: Context) = prefs(context).getBoolean(KEY_AUTO_BACKUP, true)

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        check(prefs(context).edit().putBoolean(KEY_AUTO_BACKUP, enabled).commit()) { "Einstellung konnte nicht gespeichert werden." }
    }
}
