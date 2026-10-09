package de.yummify.app.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.yummify.app.R
import de.yummify.app.data.model.AppLanguage
import de.yummify.app.data.model.RecipeLayout
import de.yummify.app.data.model.StartDestination
import de.yummify.app.data.model.ThemeMode
import de.yummify.app.ui.AppLocale
import de.yummify.app.ui.theme.YummifyTheme

/** Untermenü „Darstellung“: Farbschema, dynamische Farben, Startseite, Rezeptansicht und Sprache. */
@Composable
fun AppearanceSection(onBack: () -> Unit, viewModel: AppearanceViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    SettingsPage(stringResource(R.string.settings_appearance_title), onBack) {
        AppearanceContent(
            state = state,
            onThemeMode = viewModel::onThemeMode,
            onDynamicColor = viewModel::onDynamicColor,
            onStartDestination = viewModel::onStartDestination,
            onRecipeLayout = viewModel::onRecipeLayout,
            // The choice is stored first: on Android 12 and older the restarted activity reads it again.
            onLanguage = { language ->
                if (language != state.appLanguage) {
                    viewModel.onAppLanguage(language)
                    AppLocale.apply(context, language)
                }
            }
        )
    }
}

@Composable
fun AppearanceContent(
    state: AppearanceUiState,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onStartDestination: (StartDestination) -> Unit,
    onRecipeLayout: (RecipeLayout) -> Unit,
    onLanguage: (AppLanguage) -> Unit
) {
    ChoiceCard(
        title = stringResource(R.string.appearance_theme_title),
        hint = if (state.themeMode == ThemeMode.SYSTEM) stringResource(R.string.appearance_theme_system_hint) else null,
        options = listOf(
            ThemeMode.LIGHT to stringResource(R.string.appearance_theme_light),
            ThemeMode.DARK to stringResource(R.string.appearance_theme_dark),
            ThemeMode.SYSTEM to stringResource(R.string.appearance_theme_system)
        ),
        selected = state.themeMode,
        onSelect = onThemeMode
    ) {
        DynamicColorRow(state.dynamicColorEnabled, state.dynamicColorSupported, onDynamicColor)
    }
    ChoiceCard(
        title = stringResource(R.string.appearance_start_title),
        hint = stringResource(R.string.appearance_start_hint),
        options = listOf(
            StartDestination.RECIPES to stringResource(R.string.appearance_start_recipes),
            StartDestination.PLANNER to stringResource(R.string.appearance_start_planner),
            StartDestination.SHOPPING to stringResource(R.string.appearance_start_shopping),
            StartDestination.INVENTORY to stringResource(R.string.appearance_start_inventory)
        ),
        selected = state.startDestination,
        onSelect = onStartDestination
    )
    ChoiceCard(
        title = stringResource(R.string.appearance_recipe_view_title),
        hint = stringResource(R.string.appearance_recipe_view_hint),
        options = listOf(
            RecipeLayout.LIST to stringResource(R.string.appearance_recipe_view_list),
            RecipeLayout.GRID to stringResource(R.string.appearance_recipe_view_grid)
        ),
        selected = state.recipeLayout,
        onSelect = onRecipeLayout
    )
    ChoiceCard(
        title = stringResource(R.string.appearance_language_title),
        hint = stringResource(R.string.appearance_language_hint),
        options = listOf(
            AppLanguage.SYSTEM to stringResource(R.string.appearance_language_system),
            AppLanguage.GERMAN to stringResource(R.string.appearance_language_german),
            AppLanguage.ENGLISH to stringResource(R.string.appearance_language_english)
        ),
        selected = state.appLanguage,
        onSelect = onLanguage
    )
}

/** A card with a title and a group of radio options; [extra] sits below the options. Rows are at least 48 dp high. */
@Composable
private fun <T> ChoiceCard(
    title: String,
    hint: String?,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            Column(Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) })
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
            if (hint != null) {
                Text(
                    hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            extra?.invoke(this)
        }
    }
}

@Composable
private fun DynamicColorRow(enabled: Boolean, supported: Boolean, onChange: (Boolean) -> Unit) {
    HorizontalDivider(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    val contentAlpha = if (supported) 1f else 0.5f
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = enabled, enabled = supported, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                stringResource(R.string.appearance_dynamic_title), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            Text(
                when {
                    !supported -> stringResource(R.string.appearance_dynamic_unavailable)
                    enabled -> stringResource(R.string.appearance_dynamic_on)
                    else -> stringResource(R.string.appearance_dynamic_off)
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }
        Switch(checked = enabled, onCheckedChange = null, enabled = supported)
    }
}

@Composable
private fun PreviewBody(state: AppearanceUiState, dark: Boolean = false) {
    YummifyTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth()) { AppearanceContent(state, {}, {}, {}, {}, {}) }
        }
    }
}

@Preview(name = "Hell", showBackground = true, heightDp = 1500)
@Composable
private fun AppearanceLightPreview() = PreviewBody(AppearanceUiState(dynamicColorEnabled = true))

@Preview(name = "Dunkel, Android 11", showBackground = true, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppearanceDarkPreview() = PreviewBody(
    AppearanceUiState(themeMode = ThemeMode.DARK, dynamicColorSupported = false, recipeLayout = RecipeLayout.GRID, appLanguage = AppLanguage.ENGLISH), dark = true
)

@Preview(name = "Große Schrift", showBackground = true, heightDp = 2400, fontScale = 1.6f)
@Composable
private fun AppearanceLargeFontPreview() = PreviewBody(AppearanceUiState(themeMode = ThemeMode.SYSTEM))
