package de.yummify.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CategoryFilterChips(
    selectedCategory: String,
    categories: List<String>,
    totalRecipeCount: Int,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { categoryName ->
            val isAll = categoryName.equals("all", ignoreCase = true)
            val selected = if (isAll) selectedCategory.equals("all", ignoreCase = true) else selectedCategory.equals(categoryName, ignoreCase = true)
            val displayLabel = if (isAll) "Alle ($totalRecipeCount)" else categoryName

            FilterChip(
                selected = selected,
                onClick = { onCategorySelected(if (isAll) "all" else categoryName) },
                label = {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = CircleShape
            )
        }
    }
}

/** Recipe photo with a neutral placeholder when Notion has no cover or the image cannot be loaded. */
@Composable
fun RecipeImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    var failed by remember(url) { mutableStateOf(false) }
    if (url.isNullOrBlank() || failed) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Restaurant, contentDescription, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        }
    } else {
        coil.compose.AsyncImage(model = url, contentDescription = contentDescription,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = modifier, onError = { failed = true })
    }
}
