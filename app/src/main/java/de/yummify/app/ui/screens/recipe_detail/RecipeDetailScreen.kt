package de.yummify.app.ui.screens.recipe_detail

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.yummify.app.share.RecipeSharer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel = viewModel()
) {
    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }
    val state by viewModel.uiState.collectAsState()
    val stock by viewModel.stock.collectAsState()
    val shoppingItems by viewModel.shoppingItems.collectAsState()
    var showConsumeDialog by remember { mutableStateOf(false) }
    val recipe = state.recipe
    var showPlanDialog by remember { mutableStateOf(false) }

    if (recipe == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(), contentAlignment = Alignment.Center) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
            val error = state.loadError
            if (error == null) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            else Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Text("Rezept konnte nicht geladen werden", style = MaterialTheme.typography.titleMedium)
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { viewModel.loadRecipe(recipeId, force = true) }, enabled = !state.isLoading) { Text("Erneut versuchen") }
            }
        }
        return
    }
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }
    LaunchedEffect(state.inventoryMessage) {
        state.inventoryMessage?.let { snackbar.showSnackbar(it, withDismissAction = true); viewModel.messageShown() }
    }
    LaunchedEffect(state.isPlannedSaved) {
        if (state.isPlannedSaved) snackbar.showSnackbar("Eingeplant für ${state.plannedDateText}")
    }

    if (showPlanDialog) {
        PlanRecipeDialog(
            onDismiss = { showPlanDialog = false },
            onConfirm = { date, mealType ->
                viewModel.planRecipeForDate(date, mealType)
            }
        )
    }

    if (showConsumeDialog) {
        AlertDialog(onDismissRequest = { showConsumeDialog = false }, title = { Text("Zutaten abbuchen?") },
            text = { Text("Die Mengen für ${state.servings} Portionen werden vom Inventar abgezogen. Vorräte mit dem frühesten Ablaufdatum werden zuerst verwendet.") },
            confirmButton = { TextButton(onClick = { showConsumeDialog = false; viewModel.consumeIngredients() }) { Text("Abbuchen") } },
            dismissButton = { TextButton(onClick = { showConsumeDialog = false }) { Text("Abbrechen") } })
    }
    val defaultServings = (recipe.defaultServings).coerceAtLeast(1).toDouble()
    val multiplier = state.servings.toDouble() / defaultServings

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Hero Image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                de.yummify.app.ui.components.RecipeImage(url = recipe.imageUrl, contentDescription = recipe.title, modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000)),
                                startY = 150f
                            )
                        )
                )
                // Top bar (Back icon removed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                if (!sharing) scope.launch {
                                    sharing = true
                                    try { RecipeSharer.share(context, recipe, state.servings) }
                                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                    catch (_: Exception) { snackbar.showSnackbar("Teilen ist nicht möglich. Bitte erneut versuchen.") }
                                    finally { sharing = false }
                                }
                            },
                            enabled = !sharing,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                        ) {
                            if (sharing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            else Icon(
                                Icons.Filled.Share,
                                contentDescription = "Rezept teilen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showPlanDialog = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = "Datum planen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                if (state.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (state.isFavorite) "Aus Favoriten entfernen" else "Als Favorit merken",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                // Title overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    recipe.tags.firstOrNull()?.let { tag ->
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Eco, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Quick info badges
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipe.cookTimeMinutes?.let { QuickInfoBadge(icon = "⏱", label = "$it Min.") }
                recipe.calories?.let { QuickInfoBadge(icon = "🔥", label = "$it kcal") }
                recipe.difficulty?.let { QuickInfoBadge(icon = "👤", label = it) }
                recipe.estimatedCost?.let { QuickInfoBadge(icon = "💶", label = it, containerColor = MaterialTheme.colorScheme.secondaryContainer) }
            }
        }

        // Servings Counter
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Restaurant, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Text(
                            "Portionen",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.adjustServings(-1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                            ) {
                                Icon(Icons.Filled.Remove, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                "${state.servings}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 24.dp),
                            )
                            IconButton(
                                onClick = { viewModel.adjustServings(1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                            ) {
                                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Interactive Star Rating
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            null,
                            tint = Color(0xFFFBBC04),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Bewertung",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { viewModel.updateRating(star) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= state.userRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "$star Sterne",
                                    tint = if (star <= state.userRating) Color(0xFFFBBC04) else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        if (state.isRatingSaved) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Ingredients Header & Add to Shopping List
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Zutaten",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        Text(
                            "Vorrat abgeglichen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Button to add all ingredients to Shopping List
                Button(
                    onClick = { viewModel.addIngredientsToShoppingList() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        if (state.addedShoppingCount != null) Icons.Filled.Check else Icons.Filled.ShoppingCart,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.addedShoppingCount != null)
                            "${state.addedShoppingCount} Zutaten zur Einkaufsliste hinzugefügt!"
                        else
                            "Fehlende Zutaten zur Einkaufsliste",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedButton(onClick = { showConsumeDialog = true }, enabled = !state.consuming, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.consuming) "Bucht ab …" else "Gekocht · Zutaten vom Vorrat abbuchen")
                }
            }
        }
        itemsIndexed(recipe.ingredients) { _, ingredient ->
            val isChecked = ingredient.name in state.checkedIngredients
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .clickable { viewModel.toggleIngredient(ingredient.name) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                if (isChecked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    val cover = de.yummify.app.data.model.InventoryMath.cover(ingredient, multiplier, stock)
                    val unit = ingredient.unit
                    val label = when (cover.state) {
                        de.yummify.app.data.model.InventoryMath.Coverage.ENOUGH -> "Im Vorrat"
                        de.yummify.app.data.model.InventoryMath.Coverage.PARTIAL -> "Fehlt ${de.yummify.app.data.model.InventoryMath.number(cover.shortfall)} $unit".trim()
                        de.yummify.app.data.model.InventoryMath.Coverage.OTHER_UNIT -> "Andere Einheit"
                        de.yummify.app.data.model.InventoryMath.Coverage.SIMILAR -> "Ähnlich"
                        de.yummify.app.data.model.InventoryMath.Coverage.MISSING -> "Kaufen"
                    }
                    val covered = cover.state == de.yummify.app.data.model.InventoryMath.Coverage.ENOUGH
                    val hint = cover.state in setOf(de.yummify.app.data.model.InventoryMath.Coverage.OTHER_UNIT, de.yummify.app.data.model.InventoryMath.Coverage.PARTIAL, de.yummify.app.data.model.InventoryMath.Coverage.SIMILAR)
                    // Long explanations go under the name, so the chip stays short and the name keeps its width.
                    val detail = when (cover.state) {
                        de.yummify.app.data.model.InventoryMath.Coverage.PARTIAL -> "Vorrat deckt nur ${de.yummify.app.data.model.InventoryMath.number(cover.available)} $unit".trim()
                        de.yummify.app.data.model.InventoryMath.Coverage.OTHER_UNIT -> "Vorrat: ${cover.otherUnit} (Einheit nicht vergleichbar)"
                        de.yummify.app.data.model.InventoryMath.Coverage.SIMILAR -> "Ähnlich im Vorrat: ${cover.otherUnit}"
                        else -> null
                    }
                    Column(Modifier.weight(1f)) {
                    Text(
                        text = if (ingredient.amount > 0.0) {
                            "${ingredient.getFormattedAmount(multiplier)} ${ingredient.name}"
                        } else {
                            ingredient.name
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Surface(
                        shape = CircleShape,
                        color = when { covered -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f); hint -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f); else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = when { covered -> MaterialTheme.colorScheme.secondary; hint -> MaterialTheme.colorScheme.onTertiaryContainer; else -> MaterialTheme.colorScheme.primary },
                            maxLines = 1, softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    // One tap puts just this ingredient on the shopping list; afterwards it shows that it is there.
                    val onList = shoppingItems.any { de.yummify.app.data.model.IngredientMatcher.same(it.name, ingredient.name) }
                    IconButton(onClick = { viewModel.addIngredientToShoppingList(ingredient) }, enabled = !onList, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (onList) Icons.Filled.Check else Icons.Filled.AddShoppingCart,
                            if (onList) "${ingredient.name} steht auf der Einkaufsliste" else "${ingredient.name} zur Einkaufsliste",
                            tint = if (onList) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Instructions Header
        item {
            Text(
                "Zubereitung",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
            )
        }

        itemsIndexed(recipe.instructions) { _, stepText ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = parseMarkdownToAnnotatedString(stepText),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }
}

@Composable
private fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    val boldColor = MaterialTheme.colorScheme.onSurface
    return remember(text, boldColor) {
        buildAnnotatedString {
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            var lastIndex = 0
            boldRegex.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val innerText = matchResult.groupValues[1]

                if (start > lastIndex) {
                    append(text.substring(lastIndex, start))
                }

                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(innerText)
                }

                lastIndex = end
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}

@Composable
private fun QuickInfoBadge(
    icon: String,
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier: Modifier = Modifier
) {
    Surface(shape = CircleShape, color = containerColor, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, fontSize = 14.sp)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanRecipeDialog(
    onDismiss: () -> Unit,
    onConfirm: (java.time.LocalDate, de.yummify.app.data.model.MealType) -> Unit
) {
    var selectedMealType by remember { mutableStateOf(de.yummify.app.data.model.MealType.DINNER) }
    // Material 3 DatePicker works with UTC midnight; local time zones would shift the day.
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant().toEpochMilli()
    )

    // Helper to get the currently selected LocalDate from the picker state
    val selectedDate: java.time.LocalDate = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
        } ?: java.time.LocalDate.now()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        "Rezept planen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Schließen")
                }
            }

            // Quick date preset chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val today = java.time.LocalDate.now()
                val presets = listOf(
                    "Heute" to today,
                    "Morgen" to today.plusDays(1),
                    "Übermorgen" to today.plusDays(2)
                )
                presets.forEach { (label, date) ->
                    val dateMillis = date.atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant().toEpochMilli()
                    FilterChip(
                        selected = selectedDate == date,
                        onClick = { datePickerState.selectedDateMillis = dateMillis },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Inline Material 3 DatePicker
            DatePicker(
                state = datePickerState,
                modifier = Modifier.fillMaxWidth(),
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Selected date display
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Event,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            "Gewähltes Datum",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            selectedDate.format(
                                java.time.format.DateTimeFormatter.ofPattern(
                                    "EEEE, dd. MMMM yyyy",
                                    java.util.Locale.GERMAN
                                )
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Meal Type Selection
            Text(
                "Mahlzeit wählen",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                de.yummify.app.data.model.MealType.values().forEach { type ->
                    FilterChip(
                        selected = selectedMealType == type,
                        onClick = { selectedMealType = type },
                        label = { Text(type.displayName) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Confirm button
            Button(
                onClick = {
                    onConfirm(selectedDate, selectedMealType)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "In Essensplan eintragen",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
