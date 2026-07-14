package com.topjohnwu.magisk.ui.superuser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.resolve
import com.topjohnwu.magisk.ui.component.AppIcon
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenu
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenuItem
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskLoader
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.superuser.SuLogUiItem
import com.topjohnwu.magisk.viewmodel.superuser.SuperuserLogsViewModel
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun SuperuserLogsScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SuperuserLogsViewModel = viewModel(factory = SuperuserLogsViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = text.resolve(context)
            SystemToastManager.show(context, messageString)
        }
    }

    LaunchedEffect(state.items.firstOrNull()?.id) {
        if (state.items.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        when {
            state.loading -> {
                MagiskLoader(modifier = Modifier.weight(1f))
            }

            state.items.isEmpty() -> {
                MagiskEmptyState(
                    title = stringResource(CoreR.string.log_data_none),
                    icon = Icons.Rounded.FilterNone,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                MagiskLazyContent(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 4.dp,
                        end = 16.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = state.items.size,
                        key = { index -> state.items[index].id }
                    ) { index ->
                        SuperuserLogItem(item = state.items[index])
                    }
                }
            }
        }
    }
}

@Composable
fun SuperuserLogsTopBarActions(
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }

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
                text = stringResource(CoreR.string.save_log),
                leadingIcon = Icons.Rounded.Save,
                onClick = {
                    onSave()
                    expanded = false
                }
            )
            MagiskDropdownMenuItem(
                text = stringResource(CoreR.string.clear_log),
                leadingIcon = Icons.Rounded.DeleteSweep,
                onClick = {
                    onClear()
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun SuperuserLogItem(item: SuLogUiItem) {
    val allowedColor = if (item.allowed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    MagiskCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIcon(
                    packageName = item.iconPackageName.orEmpty(),
                    modifier = Modifier.size(40.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.iconPackageName.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = allowedColor.copy(alpha = 0.12f),
                    contentColor = allowedColor
                ) {
                    Text(
                        text = stringResource(
                            if (item.allowed) CoreR.string.grant else CoreR.string.deny
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.infoLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.command.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Text(
                        text = item.command,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
