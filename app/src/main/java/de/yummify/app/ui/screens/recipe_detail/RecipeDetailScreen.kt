package de.yummify.app.ui.screens.recipe_detail

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.yummify.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel = viewModel()
) {
    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }
    val state by viewModel.uiState.collectAsState()
    val recipe = state.recipe

    if (recipe == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val multiplier = viewModel.portionMultiplier()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Hero Image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(SurfaceBackground.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück", tint = OnSurface)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            modifier = Modifier.background(SurfaceBackground.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorit",
                                tint = Primary
                            )
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier.background(SurfaceBackground.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Mehr", tint = OnSurface)
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
                        Surface(shape = CircleShape, color = Secondary) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Eco, null, tint = OnSecondary, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(tag, style = MaterialTheme.typography.labelSmall, color = OnSecondary)
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
                QuickInfoBadge(icon = "⏱", label = "${recipe.cookTimeMinutes} Min.")
                QuickInfoBadge(icon = "🔥", label = "${recipe.calories} kcal")
                QuickInfoBadge(icon = "⭐", label = "${recipe.score}")
                QuickInfoBadge(icon = "👤", label = recipe.difficulty)
                QuickInfoBadge(icon = "💶", label = recipe.estimatedCost, containerColor = SecondaryContainer)
            }
        }

        // Notion Metadata Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(Modifier.size(8.dp).background(Secondary, CircleShape))
                            Text(
                                "Notion-Datenbank",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = SurfaceContainerHighest) {
                            Text(
                                "Bidirektional",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Datenbank" to "📁 Meine Rezepte",
                            "Zuletzt gekocht" to "🗓️ ${recipe.lastCookedDate}"
                        ).forEach { (label, value) ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceContainer
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                    Text(value, style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Servings Counter
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceContainer
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
                        Icon(Icons.Filled.Restaurant, null, tint = Primary, modifier = Modifier.size(22.dp))
                        Text(
                            "Portionen",
                            style = MaterialTheme.typography.titleSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(shape = CircleShape, color = SurfaceContainerHigh) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.adjustServings(-1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SurfaceBackground, CircleShape)
                            ) {
                                Icon(Icons.Filled.Remove, null, tint = OnSurface, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                "${state.servings}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 24.dp),
                            )
                            IconButton(
                                onClick = { viewModel.adjustServings(1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SurfaceBackground, CircleShape)
                            ) {
                                Icon(Icons.Filled.Add, null, tint = OnSurface, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Ingredients
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Zutaten",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Secondary, modifier = Modifier.size(14.dp))
                    Text(
                        "Vorrat abgeglichen",
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
                        fontWeight = FontWeight.SemiBold
                    )
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
                color = SurfaceContainerLow
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
                                if (isChecked) Secondary else SurfaceContainerHighest,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(Icons.Filled.Check, null, tint = OnSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "${ingredient.getFormattedAmount(multiplier)} ${ingredient.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isChecked) OnSurfaceVariant else OnSurface,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (ingredient.isAvailableInPantry) SecondaryContainer.copy(alpha = 0.5f) else PrimaryFixed.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = if (ingredient.isAvailableInPantry) "Im Vorrat" else "Kaufen",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ingredient.isAvailableInPantry) Secondary else Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Instructions
        item {
            Text(
                "Zubereitung",
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        itemsIndexed(recipe.instructions) { index, step ->
            val isActive = index == state.currentStep
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { viewModel.setCurrentStep(index) },
                shape = RoundedCornerShape(16.dp),
                color = if (isActive) PrimaryFixed else SurfaceContainerLow,
                border = if (isActive) BorderStroke(2.dp, Primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isActive) Primary else SurfaceContainerHigh,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isActive) OnPrimary else OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) OnPrimaryFixed else OnSurface,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickInfoBadge(
    icon: String,
    label: String,
    containerColor: Color = SurfaceContainerHigh,
    modifier: Modifier = Modifier
) {
    Surface(shape = CircleShape, color = containerColor, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, fontSize = 14.sp)
            Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurface)
        }
    }
}
