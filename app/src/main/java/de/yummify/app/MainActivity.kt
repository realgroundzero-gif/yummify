package de.yummify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.yummify.app.ui.screens.meal_planner.MealPlannerScreen
import de.yummify.app.ui.screens.recipe_detail.RecipeDetailScreen
import de.yummify.app.ui.screens.recipe_list.RecipeListScreen
import de.yummify.app.ui.screens.settings.SettingsScreen
import de.yummify.app.ui.screens.inventory.InventoryScreen
import de.yummify.app.ui.screens.shopping_list.ShoppingListScreen
import de.yummify.app.ui.theme.YummifyTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val iconOutlined: ImageVector) {
    object Recipes : Screen("recipes", "Rezepte", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    object Planner : Screen("planner", "Wochenplaner", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Shopping : Screen("shopping", "Einkaufsliste", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart)
    object Inventory : Screen("inventory", "Inventar", Icons.Filled.Inventory2, Icons.Outlined.Inventory2)
    object Settings : Screen("settings", "Einstellungen", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(Screen.Recipes, Screen.Planner, Screen.Shopping, Screen.Inventory, Screen.Settings)

class MainActivity : ComponentActivity() {
    private val launchRecipeIdState = mutableStateOf<String?>(null)
    private val launchRouteState = mutableStateOf<String?>(null)
    private val launchRequestState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        val prefsRepo = de.yummify.app.data.repository.UserPreferencesRepository.getInstance(applicationContext)
        setContent {
            val userPrefs by prefsRepo.preferences.collectAsState()
            val launchRecipeId by launchRecipeIdState
            val launchRoute by launchRouteState
            val launchRequest by launchRequestState
            YummifyTheme(darkTheme = userPrefs.darkModeEnabled) {
                YummifyApp(initialRecipeId = launchRecipeId, initialRoute = launchRoute, launchRequest = launchRequest)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        de.yummify.app.data.repository.InventoryRepository.getInstance(applicationContext).syncOnResume()
        de.yummify.app.widget.MealPlannerWidgetProvider.updateAllWidgets(applicationContext)
        de.yummify.app.widget.ShoppingListWidgetProvider.updateAllWidgets(applicationContext)
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        launchRequestState.intValue++
        launchRouteState.value = intent?.getStringExtra("route")
        val recipeId = intent?.getStringExtra("recipeId")
        launchRecipeIdState.value = recipeId
        if (!recipeId.isNullOrEmpty()) {
            launchRecipeIdState.value = recipeId
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YummifyApp(initialRecipeId: String? = null, initialRoute: String? = null, launchRequest: Int = 0) {
    val navController = rememberNavController()
    LaunchedEffect(initialRoute, launchRequest) {
        if (initialRoute != null && bottomNavItems.any { it.route == initialRoute }) navController.navigate(initialRoute) { launchSingleTop = true }
    }

    LaunchedEffect(initialRecipeId, launchRequest) {
        if (!initialRecipeId.isNullOrEmpty()) {
            navController.navigate("recipe_detail/$initialRecipeId")
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: ""

    val showBottomBar = bottomNavItems.any { currentRoute == it.route }
    var isBarsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentRoute) { isBarsVisible = true }
    var recipeOverviewRequest by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    var bottomBarHeight by remember { mutableStateOf((88 * density.fontScale.coerceAtLeast(1f) + 0.5f).dp) }
    val bottomBarOffsetPx by animateDpAsState(
        targetValue = if (showBottomBar && (currentRoute != Screen.Recipes.route || isBarsVisible)) 0.dp else bottomBarHeight + 8.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "bottomBarAnim"
    )

    Scaffold(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Recipes.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(150)) }
            ) {
                composable(Screen.Recipes.route) {
                    RecipeListScreen(
                        overviewRequest = recipeOverviewRequest,
                        bottomBarInset = (bottomBarHeight - bottomBarOffsetPx).coerceAtLeast(0.dp),
                        onChromeVisibilityChanged = { if (navController.currentDestination?.route == Screen.Recipes.route) isBarsVisible = it },
                        onRecipeClick = { recipeId ->
                            navController.navigate("recipe_detail/$recipeId")
                        }
                    )
                }
                composable(Screen.Planner.route) {
                    MealPlannerScreen(
                        onRecipeClick = { recipeId ->
                            navController.navigate("recipe_detail/$recipeId")
                        }
                    )
                }
                composable(Screen.Shopping.route) {
                    ShoppingListScreen()
                }
                composable(Screen.Inventory.route) {
                    InventoryScreen(onSettings = { navController.navigate(Screen.Settings.route) }, bottomBarInset = bottomBarHeight)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
                composable(
                    route = "recipe_detail/{recipeId}",
                    arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                    RecipeDetailScreen(
                        recipeId = recipeId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            if (showBottomBar) {
                // Bottom Bar overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { bottomBarHeight = with(density) { it.height.toDp() } }
                        .graphicsLayer { translationY = bottomBarOffsetPx.toPx() }
                ) {
                    YummifyBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { screen ->
                            isBarsVisible = true
                            if (screen == Screen.Recipes) {
                                recipeOverviewRequest++
                                if (!navController.popBackStack(Screen.Recipes.route, false)) {
                                    navController.navigate(Screen.Recipes.route) { launchSingleTop = true }
                                }
                            } else navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun YummifyBottomBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.height((88 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp),
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(screen) },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.icon else screen.iconOutlined,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                when (screen) {
                                    Screen.Planner -> "Plan"
                                    Screen.Shopping -> "Einkauf"
                                    Screen.Settings -> "Optionen"
                                    else -> screen.title
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}
