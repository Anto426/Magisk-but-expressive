package com.topjohnwu.magisk.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.resolve
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.ui.component.LogItem
import com.topjohnwu.magisk.ui.motion.MagiskAnimatedVisibility
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenu
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenuItem
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskLoader
import com.topjohnwu.magisk.ui.component.MagiskAnimatedSearchField
import com.topjohnwu.magisk.ui.component.MagiskSearchActionButton
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.log.LogDisplayFilter
import com.topjohnwu.magisk.viewmodel.log.MagiskLogViewModel
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun LogsScreen(
    onNavigate: (AppRoute) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: MagiskLogViewModel = viewModel(factory = MagiskLogViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val logListState = rememberLazyListState()
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = text.resolve(context)
            SystemToastManager.show(context, messageString)
        }
    }

    LaunchedEffect(logListState) {
        snapshotFlow {
            Triple(
                logListState.firstVisibleItemIndex,
                logListState.firstVisibleItemScrollOffset,
                logListState.lastScrolledBackward
            )
        }.collect { (index, offset, scrollingBack) ->
            controlsVisible = scrollingBack || (index == 0 && offset < 24)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.loading) {
            MagiskLoader(modifier = Modifier.fillMaxSize())
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                MagiskAnimatedSearchField(
                    visible = controlsVisible && state.searchVisible,
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = stringResource(CoreR.string.log_search_hint)
                )
                // Log List
                if (state.filteredLogs.isEmpty()) {
                    MagiskEmptyState(
                        title = stringResource(CoreR.string.log_filtered_empty_title),
                        icon = Icons.Rounded.FilterNone,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MagiskLazyContent(
                        modifier = Modifier.weight(1f),
                        state = logListState,
                        contentPadding = PaddingValues(
                            top = 4.dp, bottom = 160.dp, start = 16.dp, end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            count = state.filteredLogs.size,
                            key = { index -> state.filteredLogs[index].id }) { index ->
                            LogItem(item = state.filteredLogs[index])
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogsTopBarActions(
    searchVisible: Boolean,
    selectedFilter: LogDisplayFilter,
    onToggleSearch: () -> Unit,
    onFilterSelected: (LogDisplayFilter) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    MagiskSearchActionButton(
        searchVisible = searchVisible,
        onToggleSearch = onToggleSearch
    )

    Box {
        MagiskTopBarIconButton(
            icon = if (selectedFilter == LogDisplayFilter.ALL) {
                Icons.Rounded.MoreVert
            } else {
                Icons.Rounded.FilterList
            },
            contentDescription = stringResource(CoreR.string.denylist_search_filters),
            onClick = { expanded = true }
        )
        MagiskDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MagiskDropdownMenuItem(
                text = stringResource(CoreR.string.save_log),
                subtitle = stringResource(CoreR.string.log_save_summary),
                leadingIcon = Icons.Rounded.Save,
                onClick = {
                    onSave()
                    expanded = false
                }
            )
            MagiskDropdownMenuItem(
                text = stringResource(CoreR.string.clear_log),
                subtitle = stringResource(CoreR.string.log_clear_summary),
                destructive = true,
                leadingIcon = Icons.Rounded.DeleteSweep,
                onClick = {
                    onClear()
                    expanded = false
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            LogDisplayFilter.entries.forEach { filter ->
                MagiskDropdownMenuItem(
                    text = stringResource(filter.labelRes),
                    selected = filter == selectedFilter,
                    alignWithIcons = true,
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}
