package com.topjohnwu.magisk.ui

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NamedNavArgument
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
import com.topjohnwu.magisk.ui.component.MagiskLoadingController
import com.topjohnwu.magisk.ui.component.MagiskLoadingOverlay
import com.topjohnwu.magisk.ui.component.MagiskSnackbarHost
import com.topjohnwu.magisk.ui.component.MagiskTopBar
import com.topjohnwu.magisk.ui.deny.DenyListScreen
import com.topjohnwu.magisk.ui.deny.DenyListTopBarActions
import com.topjohnwu.magisk.ui.flash.FlashScreen
import com.topjohnwu.magisk.ui.flash.FlashTopBarActions
import com.topjohnwu.magisk.ui.home.HomeScreen
import com.topjohnwu.magisk.ui.home.HomeTopBarActions
import com.topjohnwu.magisk.ui.home.SupportScreen
import com.topjohnwu.magisk.ui.install.InstallScreen
import com.topjohnwu.magisk.ui.log.LogsScreen
import com.topjohnwu.magisk.ui.log.LogsTopBarActions
import com.topjohnwu.magisk.ui.motion.MagiskMotionDuration
import com.topjohnwu.magisk.ui.motion.MotionCenter
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
import com.topjohnwu.magisk.ui.update.AppUpdateTopBarActions
import com.topjohnwu.magisk.ui.update.ChangelogScreen
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
import com.topjohnwu.magisk.viewmodel.update.AppUpdateViewModel
import com.topjohnwu.magisk.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagiskAppContainer(
    openSection: String? = null, overlay: @Composable () -> Unit = {}
) {
    val navController = rememberNavController()
    val themeState by MagiskThemeController.state.collectAsState()
    val loadingTransition by MagiskLoadingController.transition.collectAsState()

    MagiskTheme(
        themeOption = themeState.themeOption,
        darkTheme = shouldUseDarkTheme(themeState.darkThemeMode),
        darkThemeMode = themeState.darkThemeMode,
        themeVersion = themeState.customColorVersion
    ) {
        LaunchedEffect(loadingTransition.generation, loadingTransition.active) {
            if (loadingTransition.active) {
                // Wait for the applied theme/locale to reach a rendered frame, not a timer.
                withFrameNanos { }
                MagiskLoadingController.complete(loadingTransition.generation)
            }
        }

        LaunchedEffect(openSection) {
            val route = AppNavigationConfig.routeFromSection(openSection)
            if (route != AppNavigationConfig.startRoute) {
                if (route.isAllowedByRuntime(MagiskRuntimeEngine.snapshot())) {
                    navController.navigateToAppRoute(route)
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

        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    val bottomBarAnimation = MotionCenter.tweenSpec<Float>(
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
                            alpha = themeState.bottomBarOpacity / 100f
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
                bottom = if (currentSpec.isNavigationBarDestination) {
                    0.dp
                } else {
                    innerPadding.calculateBottomPadding()
                }
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
                    onNavigate = navigateToRoute,
                    topBarScrollBehavior = topBarScrollBehavior
                )
            }
        }
        overlay()
        if (loadingTransition.active) {
            MagiskLoadingOverlay()
        }
        }
    }
}

@Composable
private fun MagiskNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onNavigate: (AppRoute) -> Unit,
    topBarScrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    fun NavGraphBuilder.destination(
        route: String,
        arguments: List<NamedNavArgument> = emptyList(),
        content: @Composable (NavBackStackEntry) -> Unit
    ) {
        composable(route = route, arguments = arguments) { entry ->
            MagiskDestinationScaffold(
                entry = entry,
                onNavigate = onNavigate,
                onBack = { navController.navigateUp() },
                scrollBehavior = topBarScrollBehavior
            ) {
                content(entry)
            }
        }
    }

    val profile = MotionCenter.profile()

    NavHost(
        navController = navController,
        startDestination = AppNavigationConfig.startGraphKey,
        enterTransition = {
            val fromIndex = initialState.destination.tabIndex()
            val toIndex = targetState.destination.tabIndex()
            if (fromIndex != -1 && toIndex != -1) {
                val sign = if (toIndex > fromIndex) 1 else -1
                slideInHorizontally(
                    initialOffsetX = { it * sign },
                    animationSpec = tween(300)
                ) + fadeIn(tween(300))
            } else {
                MotionCenter.navigationEnter(isPop = false, enabled = profile.enabled)
            }
        },
        exitTransition = {
            val fromIndex = initialState.destination.tabIndex()
            val toIndex = targetState.destination.tabIndex()
            if (fromIndex != -1 && toIndex != -1) {
                val sign = if (toIndex > fromIndex) 1 else -1
                slideOutHorizontally(
                    targetOffsetX = { -it * sign },
                    animationSpec = tween(300)
                ) + fadeOut(tween(300))
            } else {
                MotionCenter.navigationExit(isPop = false, enabled = profile.enabled)
            }
        },
        popEnterTransition = {
            MotionCenter.navigationEnter(isPop = true, enabled = profile.enabled)
        },
        popExitTransition = {
            MotionCenter.navigationExit(isPop = true, enabled = profile.enabled)
        }) {
        destination("home") {
            HomeScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination("support") {
            SupportScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination("app_update") {
            AppUpdateScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination(
            route = "changelog?module={module}&title={title}",
            arguments = listOf(
                navArgument("module") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            ChangelogScreen(
                moduleId = entry.arguments?.getString("module")?.let(Uri::decode),
                title = entry.arguments?.getString("title")?.let(Uri::decode)
            )
        }
        destination("superuser") {
            SuperuserScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination("superuser_logs") {
            SuperuserLogsScreen(
                snackbarHostState = snackbarHostState
            )
        }
        destination("modules") {
            ModulesScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination("module_updates") {
            ModuleUpdatesScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination("logs") {
            LogsScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination("settings") {
            SettingsScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination("install") {
            InstallScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination("denylist") { entry ->
            val denyListViewModel: DenyListViewModel = viewModel(
                viewModelStoreOwner = entry, factory = DenyListViewModel.Factory
            )
            DenyListScreen(
                snackbarHostState = snackbarHostState, viewModel = denyListViewModel
            )
        }
        destination("theme") {
            ThemeScreen()
        }
        destination("language") {
            LanguageScreen(
                onNavigate = onNavigate,
                snackbarHostState = snackbarHostState
            )
        }
        destination(route = "flash/{action}?data={data}", arguments = listOf(navArgument("action") {
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
        destination(route = "module/{id}/action?name={name}", arguments = listOf(navArgument("id") {
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
private fun MagiskDestinationScaffold(
    entry: NavBackStackEntry,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    content: @Composable () -> Unit
) {
    val spec = AppNavigationConfig.specForGraphKey(entry.destination.route)
    val flashTopBarState = entry
        .takeIf { spec.route is AppRoute.Flash }
        ?.let { flashEntry ->
            val viewModel: FlashViewModel = viewModel(
                viewModelStoreOwner = flashEntry,
                factory = FlashViewModel.Factory
            )
            val state by viewModel.state.collectAsState()
            state
        }
    val topBarActions = appTopBarActions(spec, entry, onNavigate)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MagiskTopBar(
                title = appBarTitle(spec, entry, flashTopBarState?.title),
                navigationIcon = {
                    if (!spec.isNavigationBarDestination && flashTopBarState?.running != true) {
                        MagiskBackButton(
                            onClick = onBack,
                            contentDescription = stringResource(CoreR.string.back)
                        )
                    }
                },
                actions = { topBarActions?.invoke(this) },
                compactTitle = spec.route is AppRoute.Flash || spec.route is AppRoute.ModuleAction,
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
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .clipToBounds()
        ) {
            content()
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

        AppRoute.AppUpdate -> {
            val viewModel: AppUpdateViewModel = viewModel(
                viewModelStoreOwner = entry,
                factory = AppUpdateViewModel.Factory
            )
            val lambda: @Composable RowScope.() -> Unit = {
                AppUpdateTopBarActions(onRefresh = { viewModel.refresh(force = true) })
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

private fun NavDestination?.tabIndex(): Int {
    val route = this?.route ?: return -1
    return AppNavigationConfig.topLevelRoutes.indexOfFirst { it.graphKey == route }
}
