package de.yummify.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.yummify.app.ui.theme.*

@Composable
fun NotionSyncBadge(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Surface(
        shape = CircleShape,
        color = SurfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isActive) Secondary.copy(alpha = alpha) else Outline,
                        shape = CircleShape
                    )
            )
            Text(
                text = if (isActive) "Aktiv" else "Getrennt",
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) Secondary else Outline,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun NotionStatusBanner(
    recipeCount: Int,
    lastSyncTime: String,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by rememberInfiniteTransition(label = "sync_rotate").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "sync_angle"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Secondary, CircleShape)
                )
                Column {
                    Text(
                        text = "Notion DB: $recipeCount Rezepte synchronisiert",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Zuletzt aktualisiert: $lastSyncTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            FilledTonalButton(
                onClick = onSyncClick,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SurfaceContainerHigh,
                    contentColor = OnSurfaceVariant
                )
            ) {
                Icon(
                    Icons.Filled.Sync,
                    contentDescription = "Sync",
                    modifier = Modifier
                        .size(16.dp)
                        .then(if (isSyncing) Modifier.rotate(rotation) else Modifier)
                )
                Spacer(Modifier.width(4.dp))
                Text("Sync", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun CategoryFilterChips(
    selectedCategory: String,
    recipeCount: Int,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "all" to "Alle ($recipeCount)",
        "quick" to "⚡ Schnell",
        "veggie" to "🌱 Vegetarisch",
        "protein" to "💪 High Protein",
        "pasta" to "🍝 Pasta",
        "onepot" to "🥘 One-Pot",
        "baking" to "🍰 Backen"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (id, label) ->
            val selected = selectedCategory == id
            FilterChip(
                selected = selected,
                onClick = { onCategorySelected(id) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryFixed,
                    selectedLabelColor = OnPrimaryFixed,
                    containerColor = SurfaceContainerLow,
                    labelColor = OnSurfaceVariant
                ),
                shape = CircleShape
            )
        }
    }
}
