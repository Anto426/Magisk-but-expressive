package com.topjohnwu.magisk.ui

import androidx.compose.animation.animateContentSize
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.magisk.ui.animation.MagiskMotion
import com.topjohnwu.magisk.ui.component.MagiskUiDefaults
import com.topjohnwu.magisk.core.R as CoreR

data class RouteProcessTopBarState(
    val running: Boolean = false,
    val success: Boolean = false,
    val hasResult: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MagiskTopBar(
    currentRoute: String,
    currentRoot: AppDestination,
    isRootRoute: Boolean,
    moduleActionNameArg: String?,
    flashTitleOverride: String?,
    flashSubtitleOverride: String?,
    flashProcessStateOverride: RouteProcessTopBarState,
    moduleRunTitleOverride: String?,
    moduleRunSubtitleOverride: String?,
    moduleRunProcessStateOverride: RouteProcessTopBarState,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onHomePower: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val moduleActionName = remember(moduleActionNameArg) {
        moduleActionNameArg?.let { runCatching { Uri.decode(it) }.getOrDefault(it) }
    }

    val processState = when (currentRoute) {
        AppRoute.FlashPattern -> flashProcessStateOverride
        AppRoute.ModuleActionPattern -> moduleRunProcessStateOverride
        else -> RouteProcessTopBarState()
    }

    val baseTitleText = when (currentRoute) {
        AppRoute.DenyList -> stringResource(id = CoreR.string.denylist)
        AppRoute.Install -> stringResource(id = CoreR.string.install)
        AppRoute.Theme -> stringResource(id = CoreR.string.section_theme)
        AppRoute.FlashPattern -> stringResource(id = CoreR.string.flash_screen_title)
        AppRoute.ModuleActionPattern -> stringResource(id = CoreR.string.module_action)
        AppRoute.History -> stringResource(id = CoreR.string.superuser_logs)
        AppRoute.Settings -> stringResource(id = CoreR.string.settings)
        else -> stringResource(id = currentRoot.labelRes)
    }
    val resolvedTitleText = when (currentRoute) {
        AppRoute.FlashPattern -> flashTitleOverride ?: baseTitleText
        AppRoute.ModuleActionPattern -> moduleRunTitleOverride ?: baseTitleText
        else -> baseTitleText
    }
    val titleText =
        if (currentRoute == AppRoute.FlashPattern || currentRoute == AppRoute.ModuleActionPattern) {
            resolvedTitleText
        } else {
            resolvedTitleText.uppercase()
        }
    val subtitleText = when (currentRoute) {
        AppRoute.FlashPattern -> flashSubtitleOverride
        AppRoute.ModuleActionPattern -> moduleRunSubtitleOverride ?: moduleActionName
        else -> null
    }

    CenterAlignedTopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = titleText,
                    style = if (subtitleText != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitleText != null) {
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (!isRootRoute) {
                IconButton(enabled = backEnabled, onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
            } else {
                val displayIcon = when (currentRoute) {
                    AppRoute.DenyList -> Icons.Rounded.Block
                    AppRoute.Install -> Icons.Rounded.Download
                    AppRoute.Theme -> Icons.Rounded.Palette
                    AppRoute.FlashPattern -> Icons.Rounded.Terminal
                    AppRoute.ModuleActionPattern -> Icons.Rounded.PlayCircle
                    AppRoute.History -> Icons.Rounded.HistoryEdu
                    AppRoute.Settings -> Icons.Rounded.Settings
                    else -> currentRoot.selectedIcon
                }
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    if (processState.running) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                    } else if (processState.hasResult) {
                        Icon(
                            imageVector = if (processState.success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                            contentDescription = null,
                            tint = if (processState.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    } else {
                        Icon(displayIcon, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        actions = {
            if (currentRoute == AppRoute.Home) {
                IconButton(onClick = onHomePower) {
                    Icon(
                        Icons.Rounded.PowerSettingsNew,
                        contentDescription = stringResource(id = CoreR.string.reboot)
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = stringResource(id = CoreR.string.settings)
                    )
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.statusBarsPadding()
    )
}

@Composable
internal fun MagiskFloatingBottomBar(
    destinations: List<AppDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(MagiskUiDefaults.BottomBarHeight),
        shape = MagiskUiDefaults.PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = MagiskUiDefaults.FloatingBarTonalElevation,
        shadowElevation = MagiskUiDefaults.FloatingBarShadowElevation,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { dest ->
                val selected = currentRoute == dest.route
                MagiskBottomBarItem(
                    icon = if (selected) dest.selectedIcon else dest.icon,
                    label = stringResource(id = dest.labelRes),
                    selected = selected,
                    onClick = { if (!selected) onNavigate(dest.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MagiskBottomBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by MagiskMotion.animateColor(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = MagiskMotion.quickColorSpec(),
        label = "bottomBarContainer"
    )
    val contentColor by MagiskMotion.animateColor(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
        },
        animationSpec = MagiskMotion.quickColorSpec(),
        label = "bottomBarContent"
    )
    val itemScale by MagiskMotion.animateSelectionScale(
        selected = selected,
        selectedScale = 1.02f,
        label = "bottomBarScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .scale(itemScale)
            .animateContentSize(MagiskMotion.contentSizeSpring()),
        enabled = !selected,
        shape = MagiskUiDefaults.PillShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(if (selected) 24.dp else 22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                lineHeight = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
