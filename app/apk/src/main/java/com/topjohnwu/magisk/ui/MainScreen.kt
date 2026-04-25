package com.topjohnwu.magisk.ui

import android.net.Uri
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.ui.animation.MagiskMotion
import com.topjohnwu.magisk.ui.component.MagiskSnackbarHost
import com.topjohnwu.magisk.ui.component.MagiskUiDefaults
import com.topjohnwu.magisk.ui.deny.DenyListScreen
import com.topjohnwu.magisk.ui.flash.FlashScreen
import com.topjohnwu.magisk.ui.home.HomeScreen
import com.topjohnwu.magisk.ui.install.InstallScreen
import com.topjohnwu.magisk.ui.log.LogsScreen
import com.topjohnwu.magisk.ui.module.ModuleActionScreen
import com.topjohnwu.magisk.ui.module.ModuleScreen
import com.topjohnwu.magisk.ui.settings.SettingsScreen
import com.topjohnwu.magisk.ui.settings.ThemeScreen
import com.topjohnwu.magisk.ui.superuser.SuperuserLogsScreen
import com.topjohnwu.magisk.ui.superuser.SuperuserScreen
import com.topjohnwu.magisk.ui.theme.MagiskExpressiveTheme
import com.topjohnwu.magisk.ui.theme.Theme
import com.topjohnwu.magisk.ui.theme.magiskComposeColorScheme
import kotlinx.coroutines.CancellationException

@Composable
fun MagiskAppContainer(
    useDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    darkTheme: Boolean = isSystemInDarkTheme(),
    openSection: String? = null
) {
    val context = LocalContext.current
    var currentThemeState by remember { mutableStateOf(Theme.selected) }

    val colorScheme = magiskComposeColorScheme(
        useDynamicColor = useDynamicColor,
        darkTheme = darkTheme,
        selectedTheme = currentThemeState
    )

    MagiskExpressiveTheme(colorScheme = colorScheme) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        var homeRebootRequestToken by remember { mutableStateOf(0) }
        var homeRebootConsumedToken by remember { mutableStateOf(0) }

        val rootDestinations = remember {
            buildList {
                add(AppDestination.Home)
                if (Info.isRooted && Info.env.isActive) add(AppDestination.Modules)
                if (Info.showSuperUser) add(AppDestination.Superuser)
                add(AppDestination.Logs)
            }
        }
        val rootRoutes = remember(rootDestinations) { rootDestinations.map { it.route }.toSet() }
        val rootRouteOrder = remember(rootDestinations) {
            rootDestinations.mapIndexed { index, destination -> destination.route to index }.toMap()
        }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: AppRoute.Home
        val currentRoot =
            rootDestinations.firstOrNull { it.route == currentRoute } ?: AppDestination.Home
        val isRootRoute = currentRoute in rootRoutes
        val moduleActionNameArg = backStackEntry?.arguments?.getString("name")
        var flashTitleOverride by remember { mutableStateOf<String?>(null) }
        var flashSubtitleOverride by remember { mutableStateOf<String?>(null) }
        var flashProcessStateOverride by remember { mutableStateOf(RouteProcessTopBarState()) }
        var moduleRunTitleOverride by remember { mutableStateOf<String?>(null) }
        var moduleRunSubtitleOverride by remember { mutableStateOf<String?>(null) }
        var moduleRunProcessStateOverride by remember { mutableStateOf(RouteProcessTopBarState()) }
        val backEnabled = when (currentRoute) {
            AppRoute.FlashPattern -> !flashProcessStateOverride.running
            AppRoute.ModuleActionPattern -> !moduleRunProcessStateOverride.running
            else -> true
        }
        val predictiveBackEnabled = !isRootRoute && backEnabled

        PredictiveBackHandler(enabled = predictiveBackEnabled) { backEvents ->
            try {
                // Consume progress events so Android can drive predictive back animations.
                backEvents.collect { }
                navController.popBackStack()
            } catch (_: CancellationException) {
                // Gesture was cancelled; keep current route.
            }
        }

        LaunchedEffect(openSection) {
            val route = when (openSection) {
                Const.Nav.SUPERUSER -> AppRoute.Superuser
                Const.Nav.MODULES -> AppRoute.Modules
                Const.Nav.SETTINGS -> AppRoute.Settings
                else -> null
            } ?: return@LaunchedEffect
            if (route == currentRoute) return@LaunchedEffect

            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        }

        LaunchedEffect(currentRoute) {
            if (currentRoute != AppRoute.FlashPattern) {
                flashTitleOverride = null
                flashSubtitleOverride = null
                flashProcessStateOverride = RouteProcessTopBarState()
            }
            if (currentRoute != AppRoute.ModuleActionPattern) {
                moduleRunTitleOverride = null
                moduleRunSubtitleOverride = null
                moduleRunProcessStateOverride = RouteProcessTopBarState()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MagiskTopBar(
                    currentRoute = currentRoute,
                    currentRoot = currentRoot,
                    isRootRoute = isRootRoute,
                    moduleActionNameArg = moduleActionNameArg,
                    flashTitleOverride = flashTitleOverride,
                    flashSubtitleOverride = flashSubtitleOverride,
                    flashProcessStateOverride = flashProcessStateOverride,
                    moduleRunTitleOverride = moduleRunTitleOverride,
                    moduleRunSubtitleOverride = moduleRunSubtitleOverride,
                    moduleRunProcessStateOverride = moduleRunProcessStateOverride,
                    backEnabled = backEnabled,
                    onBack = { navController.popBackStack() },
                    onHomePower = { homeRebootRequestToken++ },
                    onOpenSettings = { navController.navigate(AppRoute.Settings) }
                )
            },
            snackbarHost = {
                MagiskSnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Home,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        val initialRoute = initialState.destination.route
                        val targetRoute = targetState.destination.route
                        routeEnterTransition(
                            direction = routeDirection(initialRoute, targetRoute, rootRouteOrder),
                            rootTabDistance = rootRouteDistance(initialRoute, targetRoute, rootRouteOrder)
                        )
                    },
                    exitTransition = {
                        val initialRoute = initialState.destination.route
                        val targetRoute = targetState.destination.route
                        routeExitTransition(
                            direction = routeDirection(initialRoute, targetRoute, rootRouteOrder),
                            rootTabDistance = rootRouteDistance(initialRoute, targetRoute, rootRouteOrder)
                        )
                    },
                    popEnterTransition = {
                        val initialRoute = initialState.destination.route
                        val targetRoute = targetState.destination.route
                        routeEnterTransition(
                            direction = routeDirection(initialRoute, targetRoute, rootRouteOrder),
                            rootTabDistance = rootRouteDistance(initialRoute, targetRoute, rootRouteOrder)
                        )
                    },
                    popExitTransition = {
                        val initialRoute = initialState.destination.route
                        val targetRoute = targetState.destination.route
                        routeExitTransition(
                            direction = routeDirection(initialRoute, targetRoute, rootRouteOrder),
                            rootTabDistance = rootRouteDistance(initialRoute, targetRoute, rootRouteOrder)
                        )
                    }
                ) {
                    composable(AppRoute.Home) {
                        HomeScreen(
                            rebootRequestToken = if (homeRebootRequestToken > homeRebootConsumedToken) homeRebootRequestToken else 0,
                            onRebootTokenConsumed = { homeRebootConsumedToken = homeRebootRequestToken },
                            onOpenInstall = { navController.navigate(AppRoute.Install) },
                            onOpenUninstall = { navController.navigate(AppRoute.flash(Const.Value.UNINSTALL, null)) }
                        )
                    }
                    composable(AppRoute.Modules) {
                        ModuleScreen(
                            onInstallZip = { uri -> navController.navigate(AppRoute.flash(Const.Value.FLASH_ZIP, uri.toString())) },
                            onRunAction = { id, name -> navController.navigate(AppRoute.moduleAction(id, name)) }
                        )
                    }
                    composable(AppRoute.Superuser) {
                        SuperuserScreen(onOpenLogs = { navController.navigate(AppRoute.History) })
                    }
                    composable(AppRoute.Logs) { LogsScreen() }
                    composable(AppRoute.Settings) {
                        SettingsScreen(
                            onOpenDenyList = { navController.navigate(AppRoute.DenyList) },
                            onOpenTheme = { navController.navigate(AppRoute.Theme) }
                        )
                    }
                    composable(AppRoute.Theme) {
                        ThemeScreen(onThemeChanged = {
                            currentThemeState = Theme.selected
                            (context as? android.app.Activity)?.recreate()
                        })
                    }
                    composable(AppRoute.History) { SuperuserLogsScreen() }
                    composable(AppRoute.DenyList) { DenyListScreen(onBack = { navController.popBackStack() }) }
                    composable(AppRoute.Install) {
                        InstallScreen(onStartFlash = { action, uri -> navController.navigate(AppRoute.flash(action, uri?.toString())) })
                    }
                    composable(
                        route = AppRoute.FlashPattern,
                        arguments = listOf(
                            navArgument("action") { type = NavType.StringType },
                            navArgument("uri") { type = NavType.StringType; nullable = true; defaultValue = null }
                        )
                    ) { entry ->
                        val action = entry.arguments?.getString("action").orEmpty()
                        val uriArg = entry.arguments?.getString("uri")
                        FlashScreen(
                            action = action,
                            uriArg = uriArg,
                            onTitleStateChange = { title, subtitle, processState ->
                                flashTitleOverride = title
                                flashSubtitleOverride = subtitle
                                flashProcessStateOverride = processState
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = AppRoute.ModuleActionPattern,
                        arguments = listOf(
                            navArgument("id") { type = NavType.StringType },
                            navArgument("name") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { entry ->
                        val id = entry.arguments?.getString("id").orEmpty()
                        val name = entry.arguments?.getString("name").orEmpty()
                        val safeName = runCatching { Uri.decode(name) }.getOrDefault(name)
                        ModuleActionScreen(
                            actionId = id,
                            actionName = safeName,
                            onTitleStateChange = { title, subtitle, processState ->
                                moduleRunTitleOverride = title
                                moduleRunSubtitleOverride = subtitle
                                moduleRunProcessStateOverride = processState
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                /* Floating Bottom Navigation Bar */
                AnimatedVisibility(
                    visible = isRootRoute,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            horizontal = MagiskUiDefaults.ListItemSpacing,
                            vertical = MagiskUiDefaults.ListItemSpacing + 4.dp
                        )
                        .navigationBarsPadding(),
                    enter = MagiskMotion.floatingBarEnter(),
                    exit = MagiskMotion.floatingBarExit()
                ) {
                    MagiskFloatingBottomBar(
                        destinations = rootDestinations,
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
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
