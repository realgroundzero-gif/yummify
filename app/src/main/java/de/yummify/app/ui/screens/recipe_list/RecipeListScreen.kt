package de.yummify.app.ui.screens.recipe_list

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.model.RecipeLayout
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import de.yummify.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecipeListScreen(
    onRecipeClick: (String) -> Unit,
    overviewRequest: Int = 0,
    bottomBarInset: () -> Dp = NoBottomInset,
    viewModel: RecipeListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var creating by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    // Logo and search move with the finger and give their space to the list or grid; the category chips stay.
    val headerBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    KeepHeaderReachable(headerBehavior, if (state.layout == RecipeLayout.GRID) gridState else listState)
    LaunchedEffect(overviewRequest) {
        if (overviewRequest > 0) { headerBehavior.state.heightOffset = 0f; listState.scrollToItem(0); gridState.scrollToItem(0) }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().bottomInset(bottomBarInset),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.beginCreation(); creating = true }) { Icon(Icons.Default.Add, "Rezept hinzufügen") }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .nestedScroll(headerBehavior.nestedScrollConnection)
        ) {
            CollapsingHeader(headerBehavior) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = de.yummify.app.R.drawable.ic_launcher_foreground),
                                contentDescription = "Yummify Logo",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "yummify",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SearchBarRow(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    RecipeStatusRow(state, onRetry = viewModel::onSyncClicked, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 6.dp)
            ) {
                item {
                    // Saved recipes (the bookmark in the recipe detail) as a filter; it combines with the categories.
                    FilterChip(
                        selected = state.favoritesOnly, onClick = viewModel::onFavoritesOnlyToggled,
                        label = { Text("Gespeichert", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = { Icon(if (state.favoritesOnly) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                item {
                    CategoryFilterChips(
                        selectedCategory = state.selectedCategory,
                        categories = state.categories,
                        totalRecipeCount = state.recipes.size,
                        onCategorySelected = viewModel::onCategorySelected
                    )
                }
            }
            val hero = state.heroRecipe
            if (state.layout == RecipeLayout.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    state = gridState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth().hideBottomBarOnScroll(gridState)
                ) {
                    // Hero card, loading and empty state use the full width above and between the cards.
                    if (hero != null) item(span = { GridItemSpan(maxLineSpan) }) { RecipeHeroSection(hero, onRecipeClick) }
                    if (state.isLoading) item(span = { GridItemSpan(maxLineSpan) }) { RecipeLoadingPlaceholder() }
                    else if (state.filteredRecipes.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { RecipeEmptyPlaceholder(state) }
                    else items(state.filteredRecipes, key = { it.id }) { recipe ->
                        RecipeGridCard(recipe = recipe, onCardClick = { onRecipeClick(it.id) })
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth().hideBottomBarOnScroll(listState)
                ) {
                    if (hero != null) item { RecipeHeroSection(hero, onRecipeClick, Modifier.padding(horizontal = 16.dp)) }
                    if (state.isLoading) item { RecipeLoadingPlaceholder() }
                    else if (state.filteredRecipes.isEmpty()) item { RecipeEmptyPlaceholder(state) }
                    else items(state.filteredRecipes, key = { it.id }) { recipe ->
                        RecipeListCard(
                            recipe = recipe,
                            onCardClick = { onRecipeClick(it.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
    if (creating) RecipeCreateScreen(state.isCreating, state.creationError, onDismiss = { creating = false }, onSave = { draft ->
        viewModel.createRecipe(draft) {
            creating = false
            scope.launch { listState.scrollToItem(0); gridState.scrollToItem(0) }
        }
    })

}

@Composable
private fun RecipeHeroSection(hero: Recipe, onRecipeClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REZEPT DES TAGES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Notion Empfehlung",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RecipeHeroCard(
            recipe = hero,
            onCardClick = { onRecipeClick(it.id) }
        )
    }
}

@Composable
private fun RecipeLoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun RecipeEmptyPlaceholder(state: RecipeListUiState) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    state.favoritesOnly -> "Noch keine Favoriten"
                    state.recipes.isEmpty() && state.error != null -> "Rezepte konnten nicht geladen werden"
                    state.recipes.isEmpty() -> "Noch keine Rezepte in Notion"
                    else -> "Keine Rezepte gefunden"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.favoritesOnly) Text("Öffne ein Rezept und tippe oben auf das Lesezeichen, um es zu speichern.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecipeStatusRow(state: RecipeListUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val error = state.error
    if (error != null) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.recipes.isEmpty()) error else "Offline · gespeicherte Rezepte. $error",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f), maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                TextButton(onClick = onRetry, enabled = !state.isSyncing && !state.isLoading) { Text("Erneut versuchen") }
            }
        }
    } else if (!state.fromNotion) {
        Text("Beispielrezepte · Verbinde Notion unter „Optionen“, um deine eigenen Rezepte zu sehen.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
    } else if (state.isSyncing) {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth())
    }
}

@Composable
private fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Rezept oder Zutaten suchen...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
