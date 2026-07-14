package com.topjohnwu.magisk.navigation

import android.net.Uri
import androidx.annotation.StringRes
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.R as CoreR

enum class AppDestinationType {
    NavigationBar,
    SubScreen
}

data class AppRouteSpec(
    val route: AppRoute,
    val graphKey: String,
    @StringRes val labelRes: Int,
    val type: AppDestinationType = AppDestinationType.SubScreen
) {
    val isNavigationBarDestination: Boolean
        get() = type == AppDestinationType.NavigationBar
}

object AppNavigationConfig {
    val startRoute: AppRoute = AppRoute.Home

    val routes: List<AppRouteSpec> = listOf(
        AppRouteSpec(
            AppRoute.Home,
            "home",
            CoreR.string.section_home,
            AppDestinationType.NavigationBar
        ),
        AppRouteSpec(AppRoute.AppUpdate, "app_update", CoreR.string.update),
        AppRouteSpec(
            AppRoute.Superuser,
            "superuser",
            CoreR.string.superuser,
            AppDestinationType.NavigationBar
        ),
        AppRouteSpec(AppRoute.SuperuserLogs, "superuser_logs", CoreR.string.superuser_logs),
        AppRouteSpec(
            AppRoute.Modules,
            "modules",
            CoreR.string.modules,
            AppDestinationType.NavigationBar
        ),
        AppRouteSpec(AppRoute.ModuleUpdates, "module_updates", CoreR.string.module_updates_title),
        AppRouteSpec(
            AppRoute.Changelog(moduleId = "{module}", title = "{title}"),
            "changelog?module={module}&title={title}",
            CoreR.string.release_notes
        ),
        AppRouteSpec(AppRoute.Logs, "logs", CoreR.string.logs, AppDestinationType.NavigationBar),
        AppRouteSpec(
            AppRoute.Settings,
            "settings",
            CoreR.string.settings,
            AppDestinationType.NavigationBar
        ),
        AppRouteSpec(AppRoute.Install, "install", CoreR.string.install),
        AppRouteSpec(AppRoute.Support, "support", CoreR.string.home_support_title),
        AppRouteSpec(AppRoute.DenyList, "denylist", CoreR.string.denylist),
        AppRouteSpec(AppRoute.Theme, "theme", CoreR.string.section_theme),
        AppRouteSpec(AppRoute.Language, "language", CoreR.string.language),
        AppRouteSpec(
            AppRoute.Flash(action = "{action}", additionalData = "{data}"),
            "flash/{action}?data={data}",
            CoreR.string.flash_screen_title
        ),
        AppRouteSpec(
            AppRoute.ModuleAction(idValue = "{id}", name = "{name}"),
            "module/{id}/action?name={name}",
            CoreR.string.module_action
        )
    )

    val topLevelRoutes: List<AppRouteSpec> = routes.filter { it.isNavigationBarDestination }
    val startGraphKey: String get() = routeString(startRoute)

    fun routeFromSection(section: String?): AppRoute = when (section) {
        Const.Nav.SUPERUSER -> AppRoute.Superuser
        Const.Nav.MODULES -> AppRoute.Modules
        Const.Nav.SETTINGS -> AppRoute.Settings
        Const.Nav.APP_UPDATE -> AppRoute.AppUpdate
        Const.Nav.MODULE_UPDATES -> AppRoute.ModuleUpdates
        else -> startRoute
    }

    fun specFor(route: AppRoute): AppRouteSpec {
        return routes.first { it.route.id == route.id }
    }

    fun specForGraphKey(graphKey: String?): AppRouteSpec {
        val baseGraphKey = graphKey?.substringBefore("?")
        return routes.firstOrNull {
            it.graphKey == graphKey || it.graphKey.substringBefore("?") == baseGraphKey
        } ?: specFor(startRoute)
    }

    fun isNavigationBarDestination(graphKey: String?): Boolean {
        return specForGraphKey(graphKey).isNavigationBarDestination
    }

    fun isNavigationBarRoute(route: AppRoute): Boolean {
        return specFor(route).isNavigationBarDestination
    }

    fun routeString(route: AppRoute): String = when (route) {
        AppRoute.Home -> "home"
        AppRoute.AppUpdate -> "app_update"
        AppRoute.Superuser -> "superuser"
        AppRoute.SuperuserLogs -> "superuser_logs"
        AppRoute.Modules -> "modules"
        AppRoute.ModuleUpdates -> "module_updates"
        is AppRoute.Changelog -> {
            val query = buildList {
                route.moduleId?.takeIf(String::isNotBlank)?.let {
                    add("module=${Uri.encode(it)}")
                }
                route.title?.takeIf(String::isNotBlank)?.let {
                    add("title=${Uri.encode(it)}")
                }
            }.joinToString("&")
            if (query.isEmpty()) "changelog" else "changelog?$query"
        }
        AppRoute.Logs -> "logs"
        AppRoute.Settings -> "settings"
        AppRoute.Install -> "install"
        AppRoute.Support -> "support"
        AppRoute.DenyList -> "denylist"
        AppRoute.Theme -> "theme"
        AppRoute.Language -> "language"
        is AppRoute.Flash -> {
            val action = Uri.encode(route.action)
            val data = route.additionalData?.let(Uri::encode).orEmpty()
            "flash/$action?data=$data"
        }

        is AppRoute.ModuleAction -> {
            val id = Uri.encode(route.idValue)
            val name = Uri.encode(route.name)
            "module/$id/action?name=$name"
        }
    }
}
