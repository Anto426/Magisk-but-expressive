package com.topjohnwu.magisk.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.ui.graphics.vector.ImageVector
import com.topjohnwu.magisk.ui.animation.MagiskMotion
import kotlin.math.abs
import com.topjohnwu.magisk.core.R as CoreR

internal enum class AppDestination(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val labelRes: Int
) {
    Home(AppRoute.Home, Icons.Rounded.Home, Icons.Rounded.Home, CoreR.string.section_home),
    Modules(
        AppRoute.Modules,
        Icons.Rounded.Extension,
        Icons.Rounded.Extension,
        CoreR.string.modules
    ),
    Superuser(
        AppRoute.Superuser,
        Icons.Rounded.Shield,
        Icons.Rounded.Shield,
        CoreR.string.superuser
    ),
    Logs(AppRoute.Logs, Icons.Rounded.Terminal, Icons.Rounded.Terminal, CoreR.string.logs),
    Settings(
        AppRoute.Settings,
        Icons.Rounded.Settings,
        Icons.Rounded.Settings,
        CoreR.string.settings
    );

    companion object {
        val entries = values().toList()
    }
}

internal object AppRoute {
    const val Home = "home"
    const val Modules = "modules"
    const val Superuser = "superuser"
    const val History = "history"
    const val Logs = "logs"
    const val Settings = "settings"
    const val Theme = "theme"
    const val DenyList = "denylist"
    const val Install = "install"
    const val FlashPattern = "flash/{action}?uri={uri}"
    const val ModuleActionPattern = "module-action/{id}/{name}"

    fun flash(a: String, u: String?): String =
        "flash/${Uri.encode(a)}?uri=${u?.let { Uri.encode(it) } ?: ""}"

    fun moduleAction(i: String, n: String): String =
        "module-action/${Uri.encode(i)}/${Uri.encode(n)}"
}

private fun routePriority(route: String?): Int = when (route) {
    AppRoute.Home -> 0
    AppRoute.Modules -> 1
    AppRoute.Superuser -> 2
    AppRoute.Logs -> 3
    AppRoute.Settings -> 4
    AppRoute.Theme -> 5
    else -> 6
}

internal fun routeDirection(
    initial: String?,
    target: String?,
    rootRouteOrder: Map<String, Int>
): AnimatedContentTransitionScope.SlideDirection {
    val initialRootIndex = rootRouteOrder[initial]
    val targetRootIndex = rootRouteOrder[target]
    if (initialRootIndex != null && targetRootIndex != null) {
        return if (targetRootIndex >= initialRootIndex) {
            AnimatedContentTransitionScope.SlideDirection.Left
        } else {
            AnimatedContentTransitionScope.SlideDirection.Right
        }
    }

    return if (routePriority(target) >= routePriority(initial)) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
}

internal fun rootRouteDistance(
    initial: String?,
    target: String?,
    rootRouteOrder: Map<String, Int>
): Int? {
    val initialRootIndex = rootRouteOrder[initial] ?: return null
    val targetRootIndex = rootRouteOrder[target] ?: return null
    return abs(targetRootIndex - initialRootIndex)
}

internal fun AnimatedContentTransitionScope<*>.routeEnterTransition(
    direction: AnimatedContentTransitionScope.SlideDirection,
    rootTabDistance: Int?
): EnterTransition = MagiskMotion.routeEnter(this, direction, rootTabDistance)

internal fun AnimatedContentTransitionScope<*>.routeExitTransition(
    direction: AnimatedContentTransitionScope.SlideDirection,
    rootTabDistance: Int?
): ExitTransition = MagiskMotion.routeExit(this, direction, rootTabDistance)
