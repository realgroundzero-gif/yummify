package de.yummify.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.annotation.StringRes
import de.yummify.app.R

/** Settings › Rechtliches: provider details, privacy notes, licences and notes. */
@Composable
fun LegalSection(onBack: () -> Unit) {
    SettingsPage(stringResource(R.string.settings_legal_title), onBack) {
        val name = stringResource(R.string.legal_provider_name)
        if (name.isNotBlank()) LegalCard(R.string.legal_title_provider) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            stringResource(R.string.legal_provider_address).takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            stringResource(R.string.legal_provider_email).takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        LegalCard(R.string.legal_title_privacy) {
            listOf(R.string.legal_privacy_intro, R.string.legal_privacy_notion, R.string.legal_privacy_homeassistant, R.string.legal_privacy_openfoodfacts,
                R.string.legal_privacy_images, R.string.legal_privacy_scanner, R.string.legal_privacy_backup, R.string.legal_privacy_delete).forEach { RichText(it) }
        }
        LegalCard(R.string.legal_title_licenses) {
            listOf(R.string.legal_licenses_openfoodfacts, R.string.legal_licenses_libraries).forEach { RichText(it) }
        }
        LegalCard(R.string.legal_title_notes) {
            listOf(R.string.legal_notes_nutrition, R.string.legal_notes_trademarks).forEach { RichText(it) }
        }
    }
}

@Composable
private fun LegalCard(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

/** A string resource with simple <b> tags, shown with bold parts. */
@Composable
private fun RichText(@StringRes id: Int) {
    val raw = androidx.compose.ui.platform.LocalContext.current.getText(id)   // keeps the <b> spans of the resource
    val text = buildAnnotatedString {
        append(raw.toString())
        (raw as? android.text.Spanned)?.let { spanned ->
            spanned.getSpans(0, spanned.length, android.text.style.StyleSpan::class.java).forEach { span ->
                if (span.style == android.graphics.Typeface.BOLD) addStyle(SpanStyle(fontWeight = FontWeight.Bold), spanned.getSpanStart(span), spanned.getSpanEnd(span))
            }
        }
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
