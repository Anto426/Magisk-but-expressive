package com.topjohnwu.magisk.ui.deny

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.ui.component.AppIcon
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenu
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenuItem
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskExpandableListItem
import com.topjohnwu.magisk.ui.component.MagiskInfoPill
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskListItem
import com.topjohnwu.magisk.ui.component.MagiskLoadingState
import com.topjohnwu.magisk.ui.component.MagiskAnimatedSearchField
import com.topjohnwu.magisk.ui.component.MagiskSearchActionButton
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.ui.motion.MagiskAnimatedVisibility
import com.topjohnwu.magisk.ui.motion.MagiskMotionDuration
import com.topjohnwu.magisk.ui.motion.MagiskMotionEngine
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.deny.DenyListAppUi
import com.topjohnwu.magisk.viewmodel.deny.DenyListProcessUi
import com.topjohnwu.magisk.viewmodel.deny.DenyListSortMethod
import com.topjohnwu.magisk.viewmodel.deny.DenyListViewModel
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun DenyListScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: DenyListViewModel = viewModel(factory = DenyListViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = when (text) {
                is UiText.Plain -> text.value
                is UiText.Resource -> context.getString(text.resId, *text.args.toTypedArray())
            }
            SystemToastManager.show(context, messageString)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MagiskLazyContent(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp)
        ) {
            item {
                MagiskAnimatedSearchField(
                    visible = state.searchVisible,
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = stringResource(CoreR.string.hide_filter_hint)
                )
            }

            when {
                state.loading && state.items.isEmpty() -> {
                    item { MagiskLoadingState() }
                }

                state.items.isEmpty() -> {
                    item {
                        MagiskEmptyState(
                            title = stringResource(CoreR.string.denylist_empty),
                            icon = Icons.Rounded.Apps
                        )
                    }
                }

                else -> {
                    items(
                        items = state.items,
                        key = { it.packageName },
                        contentType = { "denylist-app" }
                    ) { app ->
                        DenyListAppItem(
                            app = app,
                            onToggleExpanded = { viewModel.toggleExpanded(app.packageName) },
                            onToggleApp = {
                                viewModel.setAppChecked(
                                    app.packageName,
                                    app.selectionState != DenyListAppUi.SelectionState.Checked
                                )
                            },
                            onToggleProcess = {
                                viewModel.toggleProcess(
                                    app.packageName,
                                    it.name,
                                    it.packageName
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DenyListTopBarActions(
    searchVisible: Boolean,
    onToggleSearch: () -> Unit,
    showSystem: Boolean,
    onShowSystemChange: (Boolean) -> Unit,
    showOs: Boolean,
    onShowOsChange: (Boolean) -> Unit,
    sortMethod: DenyListSortMethod,
    onSortMethodChange: (DenyListSortMethod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    MagiskSearchActionButton(
        searchVisible = searchVisible,
        onToggleSearch = onToggleSearch
    )

    Box {
        MagiskTopBarIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = stringResource(CoreR.string.more_options),
            onClick = { expanded = true }
        )
        MagiskDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MagiskDropdownMenuItem(
                text = stringResource(CoreR.string.show_system_app),
                subtitle = stringResource(CoreR.string.denylist_show_system_summary),
                selected = showSystem,
                leadingIcon = Icons.Rounded.Android,
                onClick = {
                    onShowSystemChange(!showSystem)
                    expanded = false
                }
            )
            MagiskDropdownMenuItem(
                text = stringResource(CoreR.string.show_os_app),
                subtitle = stringResource(CoreR.string.denylist_show_os_summary),
                selected = showOs,
                enabled = showSystem,
                leadingIcon = Icons.Rounded.Security,
                onClick = {
                    onShowOsChange(!showOs)
                    expanded = false
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            MagiskDropdownMenuItem(
                text = stringResource(CoreR.string.menu_sort),
                enabled = false,
                alignWithIcons = true,
                onClick = {}
            )
            DenyListSortMethod.entries.forEach { method ->
                MagiskDropdownMenuItem(
                    text = stringResource(method.labelRes),
                    selected = method == sortMethod,
                    alignWithIcons = true,
                    onClick = {
                        onSortMethodChange(method)
                        expanded = false
                    }
                )
            }
        }
    }
}



@Composable
private fun DenyListAppItem(
    app: DenyListAppUi,
    onToggleExpanded: () -> Unit,
    onToggleApp: () -> Unit,
    onToggleProcess: (DenyListProcessUi) -> Unit
) {
    val active = app.checkedCount > 0
    val alphaAnimation = MagiskMotionEngine.tweenSpec<Float>(MagiskMotionDuration.Short)
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.65f,
        animationSpec = alphaAnimation,
        label = "HideCardAlpha"
    )
    MagiskExpandableListItem(
        title = app.label,
        subtitle = app.packageName,
        expanded = app.expanded,
        onClick = onToggleExpanded,
        modifier = Modifier.graphicsLayer(alpha = alpha),
        leadingContent = {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier.size(40.dp)
            )
        },
        headerTrailingContent = {
            DenyListAppTrailing(app = app, onToggleApp = onToggleApp)
        }
    ) {
        app.processes.forEach { process ->
            DenyListProcessItem(
                appPackageName = app.packageName,
                process = process,
                onToggle = { onToggleProcess(process) }
            )
        }
    }
}

@Composable
private fun RowScope.DenyListAppTrailing(
    app: DenyListAppUi,
    onToggleApp: () -> Unit
) {
    if (app.checkedCount > 0) {
        MagiskInfoPill(text = "${app.checkedCount}/${app.processes.size}")
    }
    TriStateCheckbox(
        state = app.selectionState.toToggleableState(),
        onClick = onToggleApp
    )
}

@Composable
private fun DenyListProcessItem(
    appPackageName: String,
    process: DenyListProcessUi,
    onToggle: () -> Unit
) {
    MagiskListItem(
        title = process.displayName,
        subtitle = process.packageName.takeIf { it != appPackageName },
        selected = process.enabled,
        leadingContent = {
            Checkbox(
                checked = process.enabled,
                onCheckedChange = { onToggle() }
            )
        },
        trailingContent = if (process.defaultSelection) {
            {
                Text(
                    text = stringResource(CoreR.string.denylist_default_process),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            null
        },
        onClick = onToggle
    )
}

private fun DenyListAppUi.SelectionState.toToggleableState(): ToggleableState = when (this) {
    DenyListAppUi.SelectionState.Checked -> ToggleableState.On
    DenyListAppUi.SelectionState.Unchecked -> ToggleableState.Off
    DenyListAppUi.SelectionState.Indeterminate -> ToggleableState.Indeterminate
}
