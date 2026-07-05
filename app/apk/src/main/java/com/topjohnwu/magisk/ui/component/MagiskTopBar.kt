package com.topjohnwu.magisk.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax
import com.topjohnwu.magisk.ui.motion.MagiskMotionDuration
import com.topjohnwu.magisk.ui.motion.MagiskMotionEngine

data class MagiskTopBarAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit = {},
    val subItems: List<MagiskTopBarAction> = emptyList(),
    val iconRotation: Float = 0f
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MagiskScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MagiskTopBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { MagiskSnackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagiskTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    compactTitle: Boolean = false,
    showNavigationIcon: Boolean = false,
    onNavigationClick: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actionItems: List<MagiskTopBarAction> = emptyList(),
    actions: @Composable RowScope.() -> Unit = {}
) {
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f

    LargeFlexibleTopAppBar(
        modifier = modifier,
        title = {
            MagiskTopBarTitle(
                title = title,
                compactTitle = compactTitle,
                collapsedFraction = collapsedFraction
            )
        },
        subtitle = subtitleContent ?: subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } ?: {},
        navigationIcon = {
            if (showNavigationIcon && onNavigationClick != null) {
                MagiskBackButton(onClick = onNavigationClick)
            } else {
                navigationIcon()
            }
        },
        actions = {
            actionItems.forEachIndexed { index, action ->
                MagiskTopBarActionButton(
                    action = action,
                    isLastItem = index == actionItems.lastIndex
                )
            }
            actions()
        },
        scrollBehavior = scrollBehavior
    )

}

@Composable
private fun MagiskTopBarTitle(
    title: String,
    compactTitle: Boolean,
    collapsedFraction: Float
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val baseStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp)
        val availableWidth = with(density) { maxWidth.roundToPx() }
        val measuredOverflow by remember(title, availableWidth, baseStyle, compactTitle) {
            derivedStateOf {
                compactTitle || textMeasurer.measure(
                    text = title,
                    style = baseStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = availableWidth)
                ).hasVisualOverflow
            }
        }
        val expandedFontSize = if (measuredOverflow) 22.sp else 32.sp
        val collapsedFontSize = if (measuredOverflow) 18.sp else 22.sp
        val fontAnimation = MagiskMotionEngine.tweenSpec<Float>(MagiskMotionDuration.Short)
        val fontSize by animateFloatAsState(
            targetValue = lerp(
                expandedFontSize.value,
                collapsedFontSize.value,
                collapsedFraction
            ),
            animationSpec = fontAnimation,
            label = "fontSizeAnimation"
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = fontSize.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = if (measuredOverflow) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MagiskBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier
            .padding(start = 12.dp, end = 12.dp)
            .size(MagiskComponentDefaults.IconButtonSize),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = Iconsax.Outline.ArrowLeft,
            contentDescription = contentDescription
        )
    }
}

@Composable
fun MagiskTopBarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f,
    enabled: Boolean = true
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .padding(start = 4.dp)
            .width(40.dp)
            .height(40.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        val iconTransform = MagiskMotionEngine.iconTransform()
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                iconTransform
            },
            label = "MagiskTopBarIconTransition"
        ) { imageVector ->
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.rotate(iconRotation)
            )
        }
    }
}

@Composable
private fun MagiskTopBarActionButton(
    action: MagiskTopBarAction,
    isLastItem: Boolean
) {
    val expanded = rememberSaveable { mutableStateOf(false) }
    val actionVisible = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        actionVisible.value = true
    }

    val enter = MagiskMotionEngine.scaleEnter()
    val exit = MagiskMotionEngine.scaleExit()
    AnimatedVisibility(
        visible = actionVisible.value,
        enter = enter,
        exit = exit
    ) {
        Box(modifier = Modifier.padding(start = 4.dp, end = if (isLastItem) 12.dp else 0.dp)) {
            MagiskTopBarIconButton(
                icon = action.icon,
                contentDescription = action.label,
                iconRotation = action.iconRotation,
                onClick = {
                    if (action.subItems.isEmpty()) {
                        action.onClick()
                    } else {
                        expanded.value = true
                    }
                }
            )
            if (action.subItems.isNotEmpty()) {
                MagiskActionDropdown(
                    expanded = expanded,
                    initialItems = action.subItems
                )
            }
        }
    }
}

@Composable
private fun MagiskActionDropdown(
    expanded: MutableState<Boolean>,
    initialItems: List<MagiskTopBarAction>
) {
    MagiskDropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false }
    ) {
        initialItems.forEach { item ->
            MagiskDropdownMenuItem(
                text = item.label,
                leadingIcon = item.icon,
                onClick = {
                    item.onClick()
                    expanded.value = false
                }
            )
        }
    }
}

@Composable
fun MagiskSearchActionButton(
    searchVisible: Boolean,
    onToggleSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagiskTopBarIconButton(
        icon = if (searchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
        contentDescription = stringResource(com.topjohnwu.magisk.core.R.string.hide_search),
        onClick = onToggleSearch,
        modifier = modifier
    )
}
