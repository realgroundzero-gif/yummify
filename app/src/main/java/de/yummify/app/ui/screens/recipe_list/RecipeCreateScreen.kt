package de.yummify.app.ui.screens.recipe_list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.yummify.app.data.model.RecipeDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCreateScreen(saving: Boolean, saveError: String?, onDismiss: () -> Unit, onSave: (RecipeDraft) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var servings by rememberSaveable { mutableStateOf("2") }
    var category by rememberSaveable { mutableStateOf("") }
    var ingredients by rememberSaveable { mutableStateOf("") }
    var instructions by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    val changed = listOf(title, description, category, ingredients, instructions, imageUrl).any { it.isNotBlank() } || servings != "2"
    val close = { if (!saving) { if (changed) discard = true else onDismiss() } }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = close)
        Scaffold(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(), topBar = {
            TopAppBar(title = { Text("Neues Rezept") }, navigationIcon = { IconButton(onClick = close, enabled = !saving) { Icon(Icons.Default.Close, "Schließen") } })
        }, bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(onClick = {
                    val draft = RecipeDraft(title.trim(), description.trim(), servings.toIntOrNull() ?: 0, category.trim(), ingredients.trim(), instructions.trim(), imageUrl.trim())
                    validation = draft.validate()
                    if (validation == null) onSave(draft)
                }, enabled = !saving, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp)) {
                    if (saving) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)) }
                    Text(if (saving) "Speichert …" else "Rezept speichern")
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Dein Rezept wird in der verbundenen Notion-Rezeptdatenbank angelegt.", style = MaterialTheme.typography.bodyMedium)
                (validation ?: saveError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(title, { title = it; validation = null }, label = { Text("Rezeptname *") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Beschreibung") }, minLines = 2, enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(servings, { servings = it }, label = { Text("Portionen *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Kategorie") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(ingredients, { ingredients = it }, label = { Text("Zutaten *") }, supportingText = { Text("Eine Zutat pro Zeile, z. B. 200 g Mehl oder 2 Eier") }, minLines = 4, enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(instructions, { instructions = it }, label = { Text("Zubereitung *") }, supportingText = { Text("Ein Schritt pro Zeile") }, minLines = 4, enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text("Bildadresse (optional)") }, supportingText = { Text("HTTPS-Adresse eines Rezeptbildes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
            }
        }
        if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Rezept verwerfen?") }, text = { Text("Deine Eingaben sind noch nicht gespeichert.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Verwerfen") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Weiter bearbeiten") } })
    }
}
