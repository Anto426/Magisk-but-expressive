package com.topjohnwu.magisk.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

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
    showNavigationIcon: Boolean = false,
    onNavigationClick: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actionItems: List<MagiskTopBarAction> = emptyList(),
    actions: @Composable RowScope.() -> Unit = {}
) {
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    val expandedFontSize = 32.sp
    val collapsedFontSize = 22.sp
    val fontSize by animateFloatAsState(
        targetValue = lerp(
            expandedFontSize.value,
            collapsedFontSize.value,
            collapsedFraction
        ),
        label = "fontSizeAnimation"
    )

    LargeFlexibleTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = fontSize.sp),
                fontWeight = FontWeight.SemiBold
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
fun MagiskBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(MagiskComponentDefaults.IconButtonSize),
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
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.82f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 0.82f))
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

    AnimatedVisibility(
        visible = actionVisible.value,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
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
    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        initialItems.forEach { item ->
            DropdownMenuItem(
                text = {
                    Row {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = item.label)
                    }
                },
                onClick = {
                    item.onClick()
                    expanded.value = false
                }
            )
        }
    }
}
