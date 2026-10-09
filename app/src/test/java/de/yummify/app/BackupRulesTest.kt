package de.yummify.app

import de.yummify.app.data.backup.StorageLayout
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Prüft die Backup-Regeln und die Liste der Speicherorte direkt an den Quelldateien (Arbeitsverzeichnis ist das Modul `app`). */
class BackupRulesTest {
    private fun module(path: String) = File(path).takeIf { it.exists() } ?: File("app/$path")

    private fun excludes(parent: Element): List<Pair<String, String>> =
        (0 until parent.childNodes.length).map { parent.childNodes.item(it) }.filterIsInstance<Element>()
            .filter { it.tagName == "exclude" }.map { it.getAttribute("domain") to it.getAttribute("path") }

    private fun root(name: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(module("src/main/res/xml/$name").also { assertTrue(it.exists()) }).documentElement

    private val token = "sharedpref" to "yummify_user_prefs.xml"

    @Test fun legacyRulesExcludeTheFileWithTheNotionToken() {
        val rules = root("backup_rules.xml")
        assertEquals("full-backup-content", rules.tagName)
        assertTrue(token in excludes(rules))
        assertEquals(StorageLayout.PREFS_USER + ".xml", token.second)
    }

    @Test fun dataExtractionRulesExcludeTheTokenForCloudBackupAndDeviceTransfer() {
        val rules = root("data_extraction_rules.xml")
        val sections = (0 until rules.childNodes.length).map { rules.childNodes.item(it) }.filterIsInstance<Element>().associateBy { it.tagName }
        assertEquals(setOf("cloud-backup", "device-transfer"), sections.keys)
        sections.values.forEach { assertTrue("${it.tagName} schließt den Token nicht aus", token in excludes(it)) }
    }

    @Test fun noIncludeRuleCanReAddTheTokenFile() {
        listOf("backup_rules.xml", "data_extraction_rules.xml").forEach {
            assertFalse(module("src/main/res/xml/$it").readText().contains("<include"))
        }
    }

    @Test fun manifestRegistersTheBackupAgentAndRules() {
        val manifest = module("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:backupAgent=\".data.backup.YummifyBackupAgent\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(manifest.contains("android:fullBackupOnly=\"true\""))
    }

    @Test fun everyPreferencesFileUsedInTheSourceIsListedInTheStorageLayout() {
        val used = module("src/main/java").walkTopDown().filter { it.extension == "kt" }
            .flatMap { Regex("""getSharedPreferences\(\s*"([^"]+)"""").findAll(it.readText()).map { m -> m.groupValues[1] } }.toSet()
        assertTrue(used.isNotEmpty())
        assertTrue("Nicht in StorageLayout: ${used - StorageLayout.allPrefs.toSet()}", StorageLayout.allPrefs.containsAll(used))
    }
}
