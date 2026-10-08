package de.yummify.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import de.yummify.app.ui.components.HideOnScrollState
import de.yummify.app.ui.components.LocalBottomBarScroll
import kotlinx.coroutines.launch
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
import de.yummify.app.data.model.StartDestination
import de.yummify.app.data.model.resolveLaunchTarget
import de.yummify.app.data.repository.UserPreferencesRepository
import de.yummify.app.ui.AppLocale
import de.yummify.app.ui.theme.YummifyTheme
import de.yummify.app.ui.theme.isDark

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val iconOutlined: ImageVector) {
    object Recipes : Screen("recipes", "Rezepte", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook)
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

    // Android 12 and older: the chosen app language is put into the context here; Android 13+ handles it in the system.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase, UserPreferencesRepository.readAppLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // After rotation or a theme change the navigation state is restored; replaying the intent would open screens twice.
        if (savedInstanceState == null) handleIntent(intent)

        val prefsRepo = UserPreferencesRepository.getInstance(applicationContext)
        // From Android 13 on the language can also be changed in the system settings; that choice wins.
        AppLocale.systemChoice(this)?.let { if (it != prefsRepo.preferences.value.appLanguage) prefsRepo.setAppLanguage(it) }
        setContent {
            val userPrefs by prefsRepo.preferences.collectAsState()
            val launchRecipeId by launchRecipeIdState
            val launchRoute by launchRouteState
            val launchRequest by launchRequestState
            YummifyTheme(darkTheme = userPrefs.themeMode.isDark(), dynamicColor = userPrefs.dynamicColorEnabled) {
                YummifyApp(
                    initialRecipeId = launchRecipeId, initialRoute = launchRoute, launchRequest = launchRequest,
                    preferredStartRoute = userPrefs.startDestination.route
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        de.yummify.app.data.repository.InventoryRepository.getInstance(applicationContext).syncOnResume()
        de.yummify.app.widget.MealPlannerWidgetProvider.updateAllWidgets(applicationContext)
        de.yummify.app.widget.ShoppingListWidgetProvider.updateAllWidgets(applicationContext)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        launchRequestState.intValue++
        launchRouteState.value = intent?.getStringExtra("route")
        launchRecipeIdState.value = intent?.getStringExtra("recipeId")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YummifyApp(
    initialRecipeId: String? = null,
    initialRoute: String? = null,
    launchRequest: Int = 0,
    preferredStartRoute: String = StartDestination.Default.route
) {
    val navController = rememberNavController()
    // Decided once per task and saved with the instance state: a rotation, a theme change or coming back from
    // the background restores the navigation state and never jumps to the start page again.
    val startRoute = rememberSaveable {
        resolveLaunchTarget(initialRoute, initialRecipeId, preferredStartRoute, bottomNavItems.map { it.route }.toSet()).startRoute
    }
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
    var recipeOverviewRequest by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    // The bar follows the scrolling of the screens (see hideBottomBarOnScroll); the estimate is replaced by the measured height.
    val bottomBar = remember { HideOnScrollState(with(density) { (88 * density.fontScale.coerceAtLeast(1f)).dp.toPx() }) }
    val barScope = rememberCoroutineScope()
    // Every screen starts with a visible bar.
    LaunchedEffect(currentRoute) { bottomBar.show() }
    val bottomBarInset: () -> Dp = { with(density) { bottomBar.visiblePx.toDp() } }

    Scaffold(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CompositionLocalProvider(LocalBottomBarScroll provides bottomBar) {
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(150)) }
            ) {
                composable(Screen.Recipes.route) {
                    RecipeListScreen(
                        overviewRequest = recipeOverviewRequest,
                        bottomBarInset = bottomBarInset,
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
                    InventoryScreen(onSettings = { navController.navigate(Screen.Settings.route) }, bottomBarInset = bottomBarInset)
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
            }

            if (showBottomBar) {
                // Bottom Bar overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { bottomBar.updateHeight(it.height.toFloat()) }
                        // The extra 8 dp keep the bar's shadow from peeking out at the bottom edge while it is hidden.
                        .graphicsLayer { translationY = bottomBar.offsetPx + bottomBar.hiddenFraction * 8.dp.toPx() }
                ) {
                    YummifyBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { screen ->
                            barScope.launch { bottomBar.show() }
                            if (screen == Screen.Recipes) {
                                recipeOverviewRequest++
                                if (!navController.popBackStack(Screen.Recipes.route, false)) {
                                    // Recipes is not the start page: replace the stack above the start page.
                                    navController.navigate(Screen.Recipes.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                    }
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
