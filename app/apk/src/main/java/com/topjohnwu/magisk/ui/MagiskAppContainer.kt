package com.topjohnwu.magisk.ui

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.topjohnwu.magisk.arch.VMFactory
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.navigation.AppNavigationConfig
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.navigation.AppRouteSpec
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import com.topjohnwu.magisk.runtime.MagiskRuntimeState
import com.topjohnwu.magisk.ui.component.MagiskBackButton
import com.topjohnwu.magisk.ui.component.MagiskNavigationBar
import com.topjohnwu.magisk.ui.component.MagiskNavigationBarStyle
import com.topjohnwu.magisk.ui.component.MagiskSnackbarHost
import com.topjohnwu.magisk.ui.component.MagiskTopBar
import com.topjohnwu.magisk.ui.deny.DenyListScreen
import com.topjohnwu.magisk.ui.deny.DenyListTopBarActions
import com.topjohnwu.magisk.ui.flash.FlashScreen
import com.topjohnwu.magisk.ui.flash.FlashTopBarActions
import com.topjohnwu.magisk.ui.home.HomeScreen
import com.topjohnwu.magisk.ui.home.HomeTopBarActions
import com.topjohnwu.magisk.ui.install.InstallScreen
import com.topjohnwu.magisk.ui.install.InstallTopBarActions
import com.topjohnwu.magisk.ui.log.LogsScreen
import com.topjohnwu.magisk.ui.log.LogsTopBarActions
import com.topjohnwu.magisk.ui.motion.MagiskMotionDuration
import com.topjohnwu.magisk.ui.motion.MagiskMotionEngine
import com.topjohnwu.magisk.ui.module.ModuleActionScreen
import com.topjohnwu.magisk.ui.module.ModuleActionTopBarActions
import com.topjohnwu.magisk.ui.module.ModuleUpdatesScreen
import com.topjohnwu.magisk.ui.module.ModuleUpdatesTopBarActions
import com.topjohnwu.magisk.ui.module.ModulesScreen
import com.topjohnwu.magisk.ui.module.ModulesTopBarActions
import com.topjohnwu.magisk.ui.settings.LanguageScreen
import com.topjohnwu.magisk.ui.settings.LanguageTopBarActions
import com.topjohnwu.magisk.ui.settings.SettingsScreen
import com.topjohnwu.magisk.ui.settings.SettingsTopBarActions
import com.topjohnwu.magisk.ui.superuser.SuperuserLogsScreen
import com.topjohnwu.magisk.ui.superuser.SuperuserLogsTopBarActions
import com.topjohnwu.magisk.ui.superuser.SuperuserScreen
import com.topjohnwu.magisk.ui.superuser.SuperuserTopBarActions
import com.topjohnwu.magisk.ui.theme.MagiskTheme
import com.topjohnwu.magisk.ui.theme.MagiskThemeController
import com.topjohnwu.magisk.ui.theme.ThemeScreen
import com.topjohnwu.magisk.ui.theme.shouldUseDarkTheme
import com.topjohnwu.magisk.ui.update.AppUpdateScreen
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.deny.DenyListViewModel
import com.topjohnwu.magisk.viewmodel.flash.FlashViewModel
import com.topjohnwu.magisk.viewmodel.home.HomeViewModel
import com.topjohnwu.magisk.viewmodel.install.InstallViewModel
import com.topjohnwu.magisk.viewmodel.log.MagiskLogViewModel
import com.topjohnwu.magisk.viewmodel.module.ModuleActionViewModel
import com.topjohnwu.magisk.viewmodel.module.ModuleViewModel
import com.topjohnwu.magisk.viewmodel.settings.SettingsViewModel
import com.topjohnwu.magisk.viewmodel.superuser.SuperuserLogsViewModel
import com.topjohnwu.magisk.viewmodel.superuser.SuperuserViewModel
import com.topjohnwu.magisk.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagiskAppContainer(
    openSection: String? = null, overlay: @Composable () -> Unit = {}
) {
    val navController = rememberNavController()
    val themeState by MagiskThemeController.state.collectAsState()

    MagiskTheme(
        themeOption = themeState.themeOption,
        darkTheme = shouldUseDarkTheme(themeState.darkThemeMode),
        darkThemeMode = themeState.darkThemeMode,
        themeVersion = themeState.customColorVersion
    ) {
        LaunchedEffect(openSection) {
            val route = AppNavigationConfig.routeFromSection(openSection)
            if (route != AppNavigationConfig.startRoute) {
                if (route.isAllowedByRuntime(MagiskRuntimeEngine.snapshot())) {
                    navController.navigateTopLevel(AppNavigationConfig.specFor(route))
                } else {
                    SystemToastManager.show(AppContext, CoreR.string.root_required_operation)
                }
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val runtime = MagiskRuntimeEngine.snapshot()
        val visibleTopLevelRoutes = AppNavigationConfig.topLevelRoutes.filter {
            it.route.isVisibleInNavigation(runtime)
        }
        val navigateToRoute: (AppRoute) -> Unit = { route ->
            if (route.isAllowedByRuntime(MagiskRuntimeEngine.snapshot())) {
                navController.navigateToAppRoute(route)
            } else {
                SystemToastManager.show(AppContext, CoreR.string.root_required_operation)
            }
        }
        val currentSpec = AppNavigationConfig.specForGraphKey(
            currentBackStackEntry?.destination?.route
        )
        val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        val topBarActions = currentBackStackEntry?.let { entry ->
            appTopBarActions(
                spec = currentSpec, entry = entry, onNavigate = navigateToRoute
            )
        }
        val flashTopBarState = currentBackStackEntry
            ?.takeIf { currentSpec.route is AppRoute.Flash }
            ?.let { entry ->
                val viewModel: FlashViewModel = viewModel(
                    viewModelStoreOwner = entry, factory = FlashViewModel.Factory
                )
                val state by viewModel.state.collectAsState()
                state
            }

        Scaffold(
            modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
            topBar = {
                val title = appBarTitle(
                    currentSpec, currentBackStackEntry, flashTopBarState?.title
                )
                MagiskTopBar(
                    title = title, navigationIcon = {
                        if (!currentSpec.isNavigationBarDestination && flashTopBarState?.running != true) {
                            MagiskBackButton(
                                onClick = { navController.navigateUp() },
                                contentDescription = stringResource(CoreR.string.back)
                            )
                        }
                    }, actions = {
                        topBarActions?.invoke(this)
                    }, compactTitle = currentSpec.route is AppRoute.Flash ||
                        currentSpec.route is AppRoute.ModuleAction,
                    subtitleContent = if (flashTopBarState?.running == true) {
                        {
                            LinearWavyProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 16.dp)
                            )
                        }
                    } else {
                        null
                    }, scrollBehavior = topBarScrollBehavior
                )
            },
            bottomBar = {
                if (currentSpec.isNavigationBarDestination) {
                    val navigationBottomInset =
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val navigationBarStyle = when (themeState.bottomBarStyle) {
                        Config.Value.BOTTOM_BAR_FLOATING -> {
                            MagiskNavigationBarStyle.Floating
                        }

                        Config.Value.BOTTOM_BAR_FIXED -> {
                            MagiskNavigationBarStyle.Docked
                        }

                        else -> {
                            if (navigationBottomInset <= 32.dp) {
                                MagiskNavigationBarStyle.Floating
                            } else {
                                MagiskNavigationBarStyle.Docked
                            }
                        }
                    }

                    // Hide bottom bar when scrolling
                    val bottomBarAnimation = MagiskMotionEngine.tweenSpec<Float>(
                        MagiskMotionDuration.Medium
                    )
                    val bottomBarOffset by animateFloatAsState(
                        targetValue = if (topBarScrollBehavior.state.heightOffset < -10f) 400f else 0f,
                        animationSpec = bottomBarAnimation,
                        label = "BottomBarOffset"
                    )

                    MagiskNavigationBar(
                        currentSpec = currentSpec,
                        onRouteSelected = { navigateToRoute(it.route) },
                        routes = visibleTopLevelRoutes,
                        style = navigationBarStyle,
                        modifier = Modifier.graphicsLayer {
                            translationY = bottomBarOffset
                        })
                }
            },
            snackbarHost = { MagiskSnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val adjustedPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = 0.dp
            )

            Box(
                modifier = Modifier
                    .padding(adjustedPadding)
                    .background(Color.Transparent)
                    .clipToBounds()
            ) {
                MagiskNavHost(
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    onNavigate = navigateToRoute
                )
            }
        }
        overlay()
    }
}

@Composable
private fun MagiskNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onNavigate: (AppRoute) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = AppNavigationConfig.startGraphKey,
        enterTransition = { magiskEnterTransition(isPop = false) },
        exitTransition = { magiskExitTransition(isPop = false) },
        popEnterTransition = { magiskEnterTransition(isPop = true) },
        popExitTransition = { magiskExitTransition(isPop = true) }) {
        composable("home") {
            HomeScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        composable("app_update") {
            AppUpdateScreen(
                snackbarHostState = snackbarHostState
            )
        }
        composable("superuser") {
            SuperuserScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        composable("superuser_logs") {
            SuperuserLogsScreen(
                snackbarHostState = snackbarHostState
            )
        }
        composable("modules") {
            ModulesScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        composable("module_updates") {
            ModuleUpdatesScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        composable("logs") {
            LogsScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        composable("install") {
            InstallScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        composable("denylist") { entry ->
            val denyListViewModel: DenyListViewModel = viewModel(
                viewModelStoreOwner = entry, factory = DenyListViewModel.Factory
            )
            DenyListScreen(
                snackbarHostState = snackbarHostState, viewModel = denyListViewModel
            )
        }
        composable("theme") {
            ThemeScreen()
        }
        composable("language") {
            LanguageScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        composable(route = "flash/{action}?data={data}", arguments = listOf(navArgument("action") {
            type = NavType.StringType
        }, navArgument("data") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        })) { entry ->
            FlashScreen(
                action = entry.arguments?.getString("action").orEmpty(),
                additionalData = entry.arguments?.getString("data"),
                onBack = { navController.navigateUp() },
                snackbarHostState = snackbarHostState
            )
        }
        composable(route = "module/{id}/action?name={name}", arguments = listOf(navArgument("id") {
            type = NavType.StringType
        }, navArgument("name") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        })) { entry ->
            val actionName = entry.arguments?.getString("name").orEmpty()
            ModuleActionScreen(
                actionId = entry.arguments?.getString("id").orEmpty(),
                actionName = actionName,
                onBack = { navController.navigateUp() },
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
private fun appTopBarActions(
    spec: AppRouteSpec, entry: NavBackStackEntry, onNavigate: (AppRoute) -> Unit
): (@Composable RowScope.() -> Unit)? {
    return when (spec.route) {
        AppRoute.Home -> {
            val viewModel: HomeViewModel = viewModel(
                viewModelStoreOwner = entry, factory = HomeViewModel.Factory
            )
            val lambda: @Composable RowScope.() -> Unit = {
                HomeTopBarActions(viewModel)
            }
            lambda
        }

        AppRoute.Language -> {
            val viewModel: SettingsViewModel = viewModel(
                viewModelStoreOwner = entry, factory = SettingsViewModel.Factory
            )
            val state by viewModel.state.collectAsState()
            val lambda: @Composable RowScope.() -> Unit = {
                LanguageTopBarActions(
                    searchVisible = state.languageSearchVisible,
                    onToggleSearch = viewModel::toggleLanguageSearch
                )
            }
            lambda
        }

        AppRoute.DenyList -> {
            val viewModel: DenyListViewModel = viewModel(
                viewModelStoreOwner = entry, factory = DenyListViewModel.Factory
            )
            val state by viewModel.state.collectAsState()
            val lambda: @Composable RowScope.() -> Unit = {
                DenyListTopBarActions(
                    searchVisible = state.searchVisible,
                    onToggleSearch = viewModel::toggleSearch,
                    showSystem = state.showSystem,
                    onShowSystemChange = viewModel::setShowSystem,
                    showOs = state.showOs,
                    onShowOsChange = viewModel::setShowOs,
                    sortMethod = state.sortMethod,
                    onSortMethodChange = viewModel::setSortMethod
                )
            }
            lambda
        }

        AppRoute.Modules -> {
            val viewModel: ModuleViewModel = viewModel(
                viewModelStoreOwner = entry, factory = ModuleViewModel.Factory
            )
            val state by viewModel.state.collectAsState()
            val updateCount = state.modules.count { it.updateReady && it.update != null }
            val lambda: @Composable RowScope.() -> Unit = {
                ModulesTopBarActions(
                    searchVisible = state.searchVisible,
                    onToggleSearch = viewModel::toggleSearch,
                    updateCount = updateCount,
                    onUpdatesClick = { onNavigate(AppRoute.ModuleUpdates) }
                )
            }
            lambda
        }

        AppRoute.ModuleUpdates -> {
            val moduleViewModel: ModuleViewModel = viewModel(
                viewModelStoreOwner = entry, factory = ModuleViewModel.Factory
            )
            val lambda: @Composable RowScope.() -> Unit = {
                ModuleUpdatesTopBarActions(
                    onRefresh = { moduleViewModel.refresh(force = true) }
                )
            }
            lambda
        }

        AppRoute.Logs -> {
            val viewModel: MagiskLogViewModel = viewModel(
                viewModelStoreOwner = entry, factory = MagiskLogViewModel.Factory
            )
            val state by viewModel.state.collectAsState()
            val lambda: @Composable RowScope.() -> Unit = {
                LogsTopBarActions(
                    searchVisible = state.searchVisible,
                    selectedFilter = state.filter,
                    onToggleSearch = viewModel::toggleSearch,
                    onFilterSelected = viewModel::setFilter,
                    onSave = viewModel::saveMagiskLog,
                    onClear = viewModel::clearMagiskLogs
                )
            }
            lambda
        }

        AppRoute.Superuser -> {
            val viewModel: SuperuserViewModel = viewModel(
                viewModelStoreOwner = entry, factory = SuperuserViewModel.Factory
            )
            val state by viewModel.state.collectAsState()
            val lambda: @Composable RowScope.() -> Unit = {
                SuperuserTopBarActions(
                    searchVisible = state.searchVisible,
                    onToggleSearch = viewModel::toggleSearch,
                    onLogsClick = { onNavigate(AppRoute.SuperuserLogs) }
                )
            }
            lambda
        }

        AppRoute.Settings -> {
            val viewModel: SettingsViewModel = viewModel(
                viewModelStoreOwner = entry, factory = SettingsViewModel.Factory
            )
            val state by viewModel.state.collectAsState()
            val lambda: @Composable RowScope.() -> Unit = {
                SettingsTopBarActions(
                    searchVisible = state.settingsSearchVisible,
                    onToggleSearch = viewModel::toggleSettingsSearch
                )
            }
            lambda
        }

        AppRoute.SuperuserLogs -> {
            val viewModel: SuperuserLogsViewModel = viewModel(
                viewModelStoreOwner = entry, factory = SuperuserLogsViewModel.Factory
            )
            val lambda: @Composable RowScope.() -> Unit = {
                SuperuserLogsTopBarActions(
                    onSave = viewModel::saveLogs, onClear = viewModel::clearLogs
                )
            }
            lambda
        }

        is AppRoute.Flash -> {
            val viewModel: FlashViewModel = viewModel(
                viewModelStoreOwner = entry, factory = FlashViewModel.Factory
            )
            val lambda: @Composable RowScope.() -> Unit = {
                FlashTopBarActions(onSaveLog = viewModel::saveLog)
            }
            lambda
        }

        is AppRoute.ModuleAction -> {
            val viewModel: ModuleActionViewModel = viewModel(
                viewModelStoreOwner = entry, factory = ModuleActionViewModel.Factory
            )
            val actionName = entry.arguments?.getString("name")?.let(Uri::decode).orEmpty()
            val lambda: @Composable RowScope.() -> Unit = {
                ModuleActionTopBarActions(
                    onSaveLog = { viewModel.saveLog(actionName) })
            }
            lambda
        }

        AppRoute.Install -> {
            val viewModel: InstallViewModel = viewModel(
                viewModelStoreOwner = entry, factory = VMFactory
            )
            val state by viewModel.uiState.collectAsState()
            val lambda: @Composable RowScope.() -> Unit = {
                InstallTopBarActions(
                    state = state, canInstall = viewModel.canInstall, onInstall = viewModel::install
                )
            }
            lambda
        }

        else -> null
    }
}

@Composable
private fun appBarTitle(
    spec: AppRouteSpec,
    entry: NavBackStackEntry?,
    flashTitle: String? = null
): String {
    if (spec.route.id == "flash" && !flashTitle.isNullOrBlank()) {
        return flashTitle
    }
    if (spec.route.id == "module_action") {
        val actionName =
            entry?.arguments?.getString("name")?.let(Uri::decode)?.takeIf { it.isNotBlank() }
        if (actionName != null) {
            return actionName
        }
    }
    return stringResource(spec.labelRes)
}

private fun NavHostController.navigateToAppRoute(route: AppRoute) {
    if (AppNavigationConfig.isNavigationBarRoute(route)) {
        navigateTopLevel(AppNavigationConfig.specFor(route))
    } else {
        navigate(AppNavigationConfig.routeString(route))
    }
}

private fun AppRoute.isVisibleInNavigation(runtime: MagiskRuntimeState): Boolean {
    return when (this) {
        AppRoute.Superuser -> runtime.isRooted && runtime.canShowSuperuser
        AppRoute.Modules -> runtime.isRooted && runtime.isInstalled

        else -> true
    }
}

private fun AppRoute.isAllowedByRuntime(runtime: MagiskRuntimeState): Boolean {
    return when (this) {
        AppRoute.Superuser,
        AppRoute.SuperuserLogs -> runtime.isRooted && runtime.canShowSuperuser

        AppRoute.Modules,
        AppRoute.ModuleUpdates -> runtime.isRooted && runtime.isInstalled

        AppRoute.DenyList -> runtime.isRooted && runtime.canShowDenyListConfig

        is AppRoute.Flash -> !MagiskRuntimeEngine.requiresRoot(action) || runtime.isRooted

        else -> true
    }
}

private fun NavHostController.navigateTopLevel(spec: AppRouteSpec) {
    navigate(spec.graphKey) {
        popUpTo(AppNavigationConfig.startGraphKey) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun magiskEnterTransition(isPop: Boolean): EnterTransition {
    return fadeIn(tween(300)) + slideInHorizontally(
        initialOffsetX = { (it * 0.1f * (if (isPop) -1 else 1)).toInt() },
        animationSpec = tween(300)
    )
}

private fun magiskExitTransition(isPop: Boolean): ExitTransition {
    return fadeOut(tween(300)) + slideOutHorizontally(
        targetOffsetX = { (it * 0.1f * (if (isPop) 1 else -1)).toInt() }, animationSpec = tween(300)
    )
}
