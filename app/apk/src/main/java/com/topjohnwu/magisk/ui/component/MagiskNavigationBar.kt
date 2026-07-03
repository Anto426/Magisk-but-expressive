package com.topjohnwu.magisk.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.magisk.navigation.AppNavigationConfig
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.navigation.AppRouteSpec
import com.topjohnwu.magisk.ui.motion.MagiskMotionDuration
import com.topjohnwu.magisk.ui.motion.MagiskMotionEngine

enum class MagiskNavigationBarStyle {
    Docked,
    Floating
}

@Composable
fun MagiskNavigationBar(
    currentSpec: AppRouteSpec,
    onRouteSelected: (AppRouteSpec) -> Unit,
    modifier: Modifier = Modifier,
    routes: List<AppRouteSpec> = AppNavigationConfig.topLevelRoutes,
    style: MagiskNavigationBarStyle = MagiskNavigationBarStyle.Floating,
    showLabels: Boolean = false
) {
    when (style) {
        MagiskNavigationBarStyle.Docked -> {
            NavigationBar(
                modifier = modifier,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                routes.forEach { spec ->
                    val selected = currentSpec.route.id == spec.route.id
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onRouteSelected(spec) },
                        icon = {
                            Icon(
                                imageVector = spec.route.navigationIcon(),
                                contentDescription = stringResource(spec.labelRes)
                            )
                        },
                        label = if (showLabels) {
                            {
                                MagiskNavigationLabel(text = stringResource(spec.labelRes))
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        MagiskNavigationBarStyle.Floating -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(if (showLabels) 76.dp else 64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    border = MagiskComponentDefaults.CardBorder
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            routes.forEach { spec ->
                                MagiskFloatingNavigationItem(
                                    spec = spec,
                                    selected = currentSpec.route.id == spec.route.id,
                                    showLabel = showLabels,
                                    onClick = { onRouteSelected(spec) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MagiskFloatingNavigationItem(
    spec: AppRouteSpec,
    selected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit
) {
    val itemAnimation = MagiskMotionEngine.tweenSpec<Float>(MagiskMotionDuration.Short)
    val colorAnimation = MagiskMotionEngine.tweenSpec<Color>(MagiskMotionDuration.Short)
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.58f,
        animationSpec = itemAnimation,
        label = "MagiskNavAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = itemAnimation,
        label = "MagiskNavScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = colorAnimation,
        label = "MagiskNavContainer"
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .width(if (showLabel) 84.dp else 54.dp)
            .height(if (showLabel) 62.dp else 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = containerColor,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = spec.route.navigationIcon(),
                contentDescription = stringResource(spec.labelRes),
                modifier = Modifier.size(24.dp)
            )
            if (showLabel) {
                MagiskNavigationLabel(text = stringResource(spec.labelRes))
            }
        }
    }
}

@Composable
private fun MagiskNavigationLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .width(72.dp)
            .basicMarquee(iterations = Int.MAX_VALUE)
    )
}

fun AppRoute.navigationIcon(): ImageVector = when (this) {
    AppRoute.Home -> Icons.Rounded.Home
    AppRoute.AppUpdate -> Icons.Rounded.SystemUpdate
    AppRoute.Superuser -> Icons.Rounded.Security
    AppRoute.SuperuserLogs -> Icons.Rounded.History
    AppRoute.Modules -> Icons.Rounded.Extension
    AppRoute.ModuleUpdates -> Icons.Rounded.SystemUpdate
    AppRoute.Logs -> Icons.AutoMirrored.Rounded.Article
    AppRoute.Settings -> Icons.Rounded.Settings
    AppRoute.Install -> Icons.Rounded.SystemUpdate
    AppRoute.DenyList -> Icons.Rounded.Security
    AppRoute.Theme -> Icons.Rounded.Palette
    AppRoute.Language -> Icons.Rounded.Language
    is AppRoute.Flash, is AppRoute.ModuleAction -> Icons.Rounded.Terminal
}
