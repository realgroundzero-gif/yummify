package de.yummify.app.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import de.yummify.app.data.model.AppLanguage
import java.util.Locale

/**
 * Sprache pro App ohne AppCompat. Ab Android 13 übernimmt das System sie (LocaleManager, auch in den Systemeinstellungen
 * sichtbar); davor setzt die Activity die Sprache in attachBaseContext und startet sich neu.
 */
object AppLocale {
    /** Die Standard-Locale vor dem ersten Eingriff, damit „System“ sie wiederherstellen kann. */
    private val systemDefault: Locale = Locale.getDefault()

    /** Für Activity.attachBaseContext: Android 13+ regelt das System, davor wird die Sprache hier eingesetzt. */
    fun wrap(base: Context, language: AppLanguage): Context {
        val tag = language.tag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        if (tag == null) {
            Locale.setDefault(systemDefault)
            return base
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }

    /** Wendet die Sprache an. Die Wahl muss vorher gespeichert sein, weil ein Neustart der Activity sie neu liest. */
    fun apply(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            manager.applicationLocales = language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        } else {
            context.findActivity()?.recreate()
        }
    }

    /** Ab Android 13 kann die Sprache auch in den Systemeinstellungen geändert werden; diese Wahl gilt dann. */
    fun systemChoice(context: Context): AppLanguage? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales ?: return null
        return if (locales.isEmpty) AppLanguage.SYSTEM else AppLanguage.fromTag(locales[0].toLanguageTag())
    }

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
