package de.yummify.app.ui.screens.inventory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import de.yummify.app.data.model.InventoryChoices
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.InventoryMath
import de.yummify.app.data.remote.OpenFoodFactsApi
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InventoryDetailScreen(item: InventoryItem, choices: InventoryChoices, saving: Boolean,
                          onDismiss: () -> Unit, onSave: (InventoryItem) -> Unit, saveError: String? = null,
                          stock: List<InventoryItem> = emptyList(), onOpenExisting: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var editName by rememberSaveable(item.id) { mutableStateOf(item.name.isBlank()) }
    var quantity by rememberSaveable(item.id) { mutableDoubleStateOf(item.quantity) }
    var unit by rememberSaveable(item.id) { mutableStateOf(if (item.name.isBlank()) "" else item.unit) }
    var categories by rememberSaveable(item.id) { mutableStateOf(ArrayList(item.categoryOptions.ifEmpty { listOf(item.category).filter { it.isNotBlank() && item.name.isNotBlank() } })) }
    var location by rememberSaveable(item.id) { mutableStateOf(if (item.name.isBlank()) "" else item.location) }
    var minimum by rememberSaveable(item.id) { mutableStateOf(InventoryMath.number(item.minimum)) }
    var expiry by rememberSaveable(item.id) { mutableStateOf(item.expiry) }
    var barcode by rememberSaveable(item.id) { mutableStateOf(item.barcode) }
    var notes by rememberSaveable(item.id) { mutableStateOf(item.notes) }
    var photo by rememberSaveable(item.id) { mutableStateOf(item.localCoverPath) }
    var productCover by rememberSaveable(item.id) { mutableStateOf(item.coverUrl) }
    var observedCover by remember(item.id) { mutableStateOf(item.coverUrl) }
    LaunchedEffect(item.coverUrl) {
        if (productCover == observedCover) productCover = item.coverUrl
        observedCover = item.coverUrl
    }
    var productSource by rememberSaveable(item.id) { mutableStateOf<String?>(null) }
    var lookupMessage by remember(item.id) { mutableStateOf<String?>(null) }
    var lookupBusy by remember(item.id) { mutableStateOf(false) }
    var existingProduct by remember(item.id) { mutableStateOf<InventoryItem?>(null) }
    val foodApi = remember { OpenFoodFactsApi() }
    var cameraPath by rememberSaveable(item.id) { mutableStateOf<String?>(null) }
    var imageMenu by remember { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(false) }
    var scanBusy by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(saveError) { saveError?.let { error = it } }
    val draft = item.copy(name = name.trim(), quantity = quantity,
        unit = unit, category = categories.joinToString(", "), categoryOptions = categories.toList(), location = location,
        minimum = minimum.replace(',', '.').toDoubleOrNull() ?: Double.NaN, expiry = expiry,
        barcode = barcode.trim(), notes = notes.trim(), localCoverPath = photo, coverUrl = productCover,
        coverPending = item.coverPending || photo != item.localCoverPath || productCover != item.coverUrl)
    val changed = name != item.name || quantity != item.quantity || unit != item.unit ||
        categories.joinToString(", ") != item.category || location != item.location || minimum != InventoryMath.number(item.minimum) ||
        expiry != item.expiry || barcode != item.barcode || notes != item.notes || photo != item.localCoverPath || productCover != item.coverUrl
    val close = { if (!saving && !photoBusy && !scanBusy && !lookupBusy) { if (changed) discard = true else onDismiss() } }
    fun importPhoto(uri: Uri, originalCameraFile: String? = null) {
        photoBusy = true
        scope.launch {
            try {
                val path = withContext(Dispatchers.IO) { InventoryImages.import(context, uri) }
                photo?.takeIf { it != item.localCoverPath }?.let { File(it).delete() }
                photo = path
                error = null
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (_: Exception) { error = "Das Bild konnte nicht geladen werden. Bitte ein anderes Foto wählen."
            } finally {
                originalCameraFile?.let { File(it).delete() }
                photoBusy = false
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { importPhoto(it) } }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = cameraPath
        cameraPath = null
        if (path != null) {
            if (success) importPhoto(FileProvider.getUriForFile(context, "${context.packageName}.inventory.files", File(path)), path)
            else File(path).delete()
        }
    }
    fun lookupProduct() {
        if (lookupBusy || saving || item.name.isNotBlank()) return
        val code = try { OpenFoodFactsApi.normalizeBarcode(barcode) }
            catch (e: IllegalArgumentException) { error = e.message; return }
        barcode = code
        stock.firstOrNull { it.barcode.trim() == code }?.let {
            existingProduct = it
            lookupMessage = "Dieser Barcode ist bereits im Inventar."
            return
        }
        lookupBusy = true
        error = null
        lookupMessage = null
        val originalName = name
        val originalUnit = unit
        val originalQuantity = quantity
        val originalCategories = categories.toList()
        val originalNotes = notes
        val originalPhoto = photo
        val originalCover = productCover
        scope.launch {
            try {
                val product = withContext(Dispatchers.IO) { foodApi.product(code) }
                if (barcode.trim() == code) {
                    if (product == null) lookupMessage = "Produkt nicht gefunden. Bitte die Angaben manuell ergänzen."
                    else {
                        if (name == originalName && name.isBlank()) product.name?.let { name = it; editName = false }
                        if (unit == originalUnit && unit.isBlank() && quantity == originalQuantity && quantity == item.quantity) {
                            // Count purchased packages instead of using their weight as stock.
                            unit = "Stück"
                        }
                        if (categories == originalCategories && categories.isEmpty()) product.suggestedCategory(choices.categories)?.let { categories = arrayListOf(it) }
                        if (notes == originalNotes && notes.isBlank()) notes = product.notes()
                        if (photo == originalPhoto && photo == null && productCover == originalCover && productCover == null) productCover = product.imageUrl
                        productSource = product.sourceUrl
                        lookupMessage = "Produktdaten geladen. Bitte prüfen und fehlende Angaben ergänzen."
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (_: Exception) {
                if (barcode.trim() == code) lookupMessage = "Produktdaten derzeit nicht verfügbar. Du kannst den Artikel manuell erfassen oder erneut versuchen."
            } finally { lookupBusy = false }
        }
    }
    fun scan() {
        scanBusy = true
        error = null
        val scanner = GmsBarcodeScanning.getClient(context, GmsBarcodeScannerOptions.Builder().enableAutoZoom().build())
        // Sideloaded APKs also need the scanner module; request it explicitly before opening the camera.
        ModuleInstall.getClient(context).installModules(ModuleInstallRequest.newBuilder().addApi(scanner).build())
            .addOnSuccessListener {
                scanner.startScan().addOnSuccessListener { result ->
                    val value = result.rawValue
                    if (value.isNullOrBlank()) error = "Kein lesbarer Barcode erkannt." else barcode = value
                    scanBusy = false
                    if (!value.isNullOrBlank() && item.name.isBlank()) {
                        if (unit.isBlank()) unit = "Stück"
                        lookupProduct()
                    }
                }.addOnCanceledListener { scanBusy = false }
                    .addOnFailureListener { failure ->
                        scanBusy = false
                        val cancelled = (failure as? MlKitException)?.errorCode in setOf(MlKitException.CODE_SCANNER_CANCELLED, MlKitException.CANCELLED)
                        if (!cancelled) error = "Scanner konnte nicht geöffnet werden. Bitte Google Play-Dienste prüfen und erneut versuchen."
                    }
            }.addOnFailureListener { scanBusy = false; error = "Scanner-Modul konnte nicht geladen werden. Bitte Internetverbindung prüfen." }
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = close)
        Scaffold(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
            topBar = { TopAppBar(title = { Text(if (item.name.isBlank()) "Neuer Artikel" else "Artikeldetails") },
                navigationIcon = { IconButton(onClick = close, enabled = !saving && !photoBusy && !scanBusy && !lookupBusy) { Icon(Icons.Default.Close, "Schließen") } }) },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Button(onClick = { error = draft.validate(); if (error == null) onSave(draft) }, enabled = !saving && !photoBusy && !scanBusy && !lookupBusy,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 48.dp)) {
                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp)); Text(if (saving) "Speichert …" else "Änderungen speichern")
                    }
                }
            }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                val image = if (photo != item.localCoverPath || item.coverPending) photo?.let { File(it) } ?: productCover else productCover ?: photo?.let { File(it) }
                var imageFailed by remember(image) { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp))) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        if (image != null && !imageFailed) AsyncImage(onError = { imageFailed = true }, model = image, contentDescription = "Headerbild von $name", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else if (imageFailed && photo != null) AsyncImage(model = File(photo!!), contentDescription = "Lokal gespeichertes Artikelbild", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp)); Text("Artikelbild hinzufügen", style = MaterialTheme.typography.titleMedium)
                            Text("Foto aufnehmen oder auswählen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FilledTonalIconButton(onClick = { imageMenu = true }, enabled = !photoBusy && !saving, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                        if (photoBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.PhotoLibrary, "Bild auswählen oder aufnehmen")
                    }
                }
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    DetailSection("Barcode", Icons.Default.QrCodeScanner) {
                        OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                        FilledTonalButton(onClick = { scan() }, enabled = !scanBusy && !saving && !lookupBusy, modifier = Modifier.fillMaxWidth()) {
                            if (scanBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.QrCodeScanner, null)
                            Spacer(Modifier.width(8.dp)); Text(if (scanBusy) "Scanner wird geöffnet …" else "Mit Kamera scannen")
                        }
                        if (item.name.isBlank()) {
                            OutlinedButton(onClick = { lookupProduct() }, enabled = barcode.isNotBlank() && !lookupBusy && !scanBusy && !saving, modifier = Modifier.fillMaxWidth()) {
                                if (lookupBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Search, null)
                                Spacer(Modifier.width(8.dp)); Text(if (lookupBusy) "Produktdaten werden geladen …" else "Produktdaten laden")
                            }
                            lookupMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            Text("Produktdaten: Open Food Facts (ODbL), Bilder: CC BY-SA", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            productSource?.let { source -> TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source))) }) { Text("Quelle bei Open Food Facts öffnen") } }
                        }
                    }
                    if (editName) OutlinedTextField(name, { name = it }, label = { Text("Artikelname") }, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { if (name.isNotBlank()) editName = false }) { Icon(Icons.Default.Check, "Namen übernehmen") } }, singleLine = true)
                    else Row(Modifier.fillMaxWidth().clickable { editName = true }, verticalAlignment = Alignment.CenterVertically) {
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Edit, "Artikelname bearbeiten", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp).size(20.dp))
                    }
                    error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                    if (photo != item.localCoverPath || item.coverPending) Text("Das Foto wird beim nächsten Abgleich als Notion-Headerbild gespeichert.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DetailSection("Bestand", Icons.Default.Inventory2) {
                        QuantityStepper(quantity, unit, onChange = { quantity = it })
                        ChoiceField("Einheit", unit, choices.units, choices.unitType == "rich_text") { unit = it }
                        OutlinedTextField(minimum, { minimum = it }, label = { Text("Mindestbestand") }, supportingText = { Text("Ab dieser Menge erinnert dich Yummify ans Nachkaufen.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    DetailSection("Einordnung", Icons.Default.Label) {
                        if (choices.categoryMultiSelect) {
                            Text("Kategorie", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (choices.categories + categories).distinct().forEach { choice ->
                                    FilterChip(selected = choice in categories, onClick = {
                                        categories = ArrayList(if (choice in categories) categories - choice else categories + choice)
                                    }, label = { Text(choice) }, leadingIcon = {
                                        if (choice in categories) {
                                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                        }
                                    })
                                }
                            }
                            if (choices.categories.isEmpty()) Text("Auswahlwerte werden aus Notion geladen.", style = MaterialTheme.typography.bodySmall)
                        } else ChoiceField("Kategorie", categories.firstOrNull().orEmpty(), choices.categories, choices.categoryType == "rich_text") {
                            categories = ArrayList(listOf(it).filter { value -> value.isNotBlank() })
                        }
                        ChoiceField("Lagerort", location, choices.locations, choices.locationType == "rich_text") { location = it }
                    }
                    DetailSection("Haltbarkeit", Icons.Default.Event) {
                        Surface(onClick = { showDate = true }, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            ListItem(headlineContent = { Text(expiry?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) } ?: "MHD auswählen") },
                                supportingContent = { Text("Nächstes Mindesthaltbarkeitsdatum") }, leadingContent = { Icon(Icons.Default.CalendarMonth, null) },
                                trailingContent = { if (expiry != null) IconButton(onClick = { expiry = null }) { Icon(Icons.Default.Close, "MHD entfernen") } },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh))
                        }
                    }
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notizen") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
            }
        }
        existingProduct?.let { existing ->
            AlertDialog(onDismissRequest = { existingProduct = null }, title = { Text("Artikel bereits vorhanden") },
                text = { Text("„${existing.name}“ ist bereits mit diesem Barcode gespeichert. Öffne den Artikel, um den Bestand zu erhöhen.") },
                confirmButton = { TextButton(onClick = { existingProduct = null; onOpenExisting(existing.id) }) { Text("Artikel öffnen") } },
                dismissButton = { TextButton(onClick = { existingProduct = null }) { Text("Abbrechen") } })
        }
        if (imageMenu) AlertDialog(onDismissRequest = { imageMenu = false },
            title = { Text("Artikelbild wählen") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Wähle ein vorhandenes Bild oder nimm ausdrücklich ein neues Foto auf.")
                Button(onClick = { imageMenu = false; picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("Bild auswählen")
                }
                OutlinedButton(onClick = {
                    imageMenu = false
                    try {
                        val file = InventoryImages.cameraFile(context)
                        cameraPath = file.absolutePath
                        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.inventory.files", file))
                    } catch (_: Exception) { error = "Keine Kamera-App verfügbar." }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Neues Foto aufnehmen")
                }
            } }, confirmButton = { TextButton(onClick = { imageMenu = false }) { Text("Abbrechen") } })
        if (showDate) {
            val dateState = rememberDatePickerState(initialSelectedDateMillis = expiry?.let { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() })
            DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
                TextButton(onClick = { expiry = dateState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; showDate = false }) { Text("Übernehmen") }
            }, dismissButton = { TextButton(onClick = { showDate = false }) { Text("Abbrechen") } }) { DatePicker(state = dateState) }
        }
        if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Änderungen verwerfen?") }, text = { Text("Deine Änderungen sind noch nicht gespeichert.") },
            confirmButton = { TextButton(onClick = { photo?.takeIf { it != item.localCoverPath }?.let { File(it).delete() }; onDismiss() }) { Text("Verwerfen") } },
            dismissButton = { TextButton(onClick = { discard = false }) { Text("Weiter bearbeiten") } })
    }
}

@Composable
private fun QuantityStepper(value: Double, unit: String, onChange: (Double) -> Unit) {
    var step by rememberSaveable(unit) { mutableDoubleStateOf(InventoryMath.quantityStep(unit)) }
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { onChange(InventoryMath.stepQuantity(value, -step)) }, enabled = value > 0, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Remove, "Menge um ${InventoryMath.number(step)} verringern")
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(InventoryMath.number(value), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(unit.ifBlank { "Menge" }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = { onChange(InventoryMath.stepQuantity(value, step)) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Add, "Menge um ${InventoryMath.number(step)} erhöhen")
                }
            }
        }
        Box {
            AssistChip(onClick = { expanded = true }, label = { Text("Schritte: ${InventoryMath.number(step)} ${unit}".trim()) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp)) })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(0.1, 0.25, 0.5, 1.0, 10.0, 50.0, 100.0).forEach { amount ->
                    DropdownMenuItem(text = { Text("${InventoryMath.number(amount)} ${unit}".trim()) },
                        onClick = { step = amount; expanded = false }, trailingIcon = { if (step == amount) Icon(Icons.Default.Check, null) })
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceField(label: String, value: String, options: List<String>, freeText: Boolean,
                        modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    if (freeText) {
        OutlinedTextField(value, onChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(), singleLine = true)
    } else {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
            OutlinedTextField(value, {}, readOnly = true, label = { Text(label) }, placeholder = { Text("Auswählen") }, singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Keine Auswahl") }, onClick = { onChange(""); expanded = false })
                (options + listOf(value).filter { it.isNotBlank() }).distinct().forEach { choice ->
                    DropdownMenuItem(text = { Text(choice) }, onClick = { onChange(choice); expanded = false },
                        trailingIcon = { if (choice == value) Icon(Icons.Default.Check, null) })
                }
            }
        }
    }
}

/** Copy picker/camera data into durable private storage, normalize orientation, and bound upload size. */
internal object InventoryImages {
    fun cameraFile(context: Context) = File(context.filesDir, "inventory_images").apply { mkdirs() }
        .let { File.createTempFile("capture-", ".jpg", it) }
    fun import(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Ungültiges Bild." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val bitmap = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: error("Bild konnte nicht gelesen werden.")
        val orientation = runCatching { resolver.openInputStream(uri).use { ExifInterface(requireNotNull(it)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            }
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val file = File(context.filesDir, "inventory_images").apply { mkdirs() }.let { File(it, "${UUID.randomUUID()}.jpg") }
        try {
            file.outputStream().use { check(rotated.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
            check(file.length() in 1..(5L * 1024 * 1024)) { "Bild ist zu groß." }
            return file.absolutePath
        } catch (e: Exception) { file.delete(); throw e
        } finally { if (rotated !== bitmap) rotated.recycle(); bitmap.recycle() }
    }
}
