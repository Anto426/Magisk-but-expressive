package com.topjohnwu.magisk.ui.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.navigation.FlashPayloadStore
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogAction
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskLoadingState
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.ui.component.card.MagiskCardAction
import com.topjohnwu.magisk.ui.component.card.MagiskStatusCard
import com.topjohnwu.magisk.ui.component.card.MagiskStatusMetric
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.module.ModuleUiItem
import com.topjohnwu.magisk.viewmodel.module.ModuleViewModel
import com.topjohnwu.magisk.core.R as CoreR

private sealed interface ModuleUpdateInstallTarget {
    data class Single(val name: String, val zipUrl: String) : ModuleUpdateInstallTarget
    data class All(val urls: List<String>) : ModuleUpdateInstallTarget
}

@Composable
fun ModuleUpdatesScreen(
    onNavigate: (AppRoute) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ModuleViewModel = viewModel(factory = ModuleViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val updates = state.modules.updatable()
    var pendingInstall by remember { mutableStateOf<ModuleUpdateInstallTarget?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.refresh(force = true)
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

    pendingInstall?.let { target ->
        ModuleUpdateInstallDialog(
            target = target,
            onDismiss = { pendingInstall = null },
            onConfirm = { route ->
                pendingInstall = null
                onNavigate(route)
            }
        )
    }

    if (state.loading && state.modules.isEmpty()) {
        MagiskLoadingState(modifier = modifier.fillMaxSize())
        return
    }

    MagiskLazyContent(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MagiskStatusCard(
                title = stringResource(CoreR.string.module_updates_title),
                statusText = if (state.checkingUpdates) {
                    stringResource(CoreR.string.module_updates_checking)
                } else {
                    stringResource(CoreR.string.module_updates_available_count, updates.size)
                },
                statusColor = if (updates.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                icon = Icons.Rounded.SystemUpdate,
                metrics = listOf(
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.modules),
                        value = state.modules.size.toString()
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.update_available),
                        value = updates.size.toString()
                    )
                ),
                primaryAction = updates.takeIf { it.isNotEmpty() }?.let {
                    MagiskCardAction(
                        text = stringResource(CoreR.string.module_updates_install_all),
                        icon = Icons.Rounded.SystemUpdate,
                        onClick = {
                            pendingInstall = ModuleUpdateInstallTarget.All(
                                it.mapNotNull { item -> item.update?.zipUrl?.takeIf(String::isNotBlank) }
                            )
                        }
                    )
                },
                secondaryAction = MagiskCardAction(
                    text = stringResource(CoreR.string.settings_check_update_title),
                    icon = Icons.Rounded.Refresh,
                    onClick = { viewModel.refresh(force = true) }
                ),
                actionsStacked = true
            )
        }

        if (state.checkingUpdates) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (updates.isEmpty() && !state.checkingUpdates) {
            item {
                MagiskEmptyState(
                    title = stringResource(CoreR.string.module_updates_empty),
                    icon = Icons.Rounded.SystemUpdate
                )
            }
        }

        items(
            count = updates.size,
            key = { index -> updates[index].id }
        ) { index ->
            ModuleUpdateCard(
                item = updates[index],
                onInstall = onInstall@{
                    val update = updates[index].update ?: return@onInstall
                    val zipUrl = update.zipUrl.takeIf(String::isNotBlank) ?: return@onInstall
                    pendingInstall = ModuleUpdateInstallTarget.Single(
                        name = update.name.ifBlank { updates[index].name },
                        zipUrl = zipUrl
                    )
                }
            )
        }
    }
}

@Composable
private fun ModuleUpdateInstallDialog(
    target: ModuleUpdateInstallTarget,
    onDismiss: () -> Unit,
    onConfirm: (AppRoute) -> Unit,
) {
    val count = when (target) {
        is ModuleUpdateInstallTarget.Single -> 1
        is ModuleUpdateInstallTarget.All -> target.urls.size
    }

    MagiskDialog(
        title = stringResource(
            if (count == 1) {
                CoreR.string.confirm_install_title
            } else {
                CoreR.string.module_updates_confirm_all_title
            }
        ),
        text = when (target) {
            is ModuleUpdateInstallTarget.Single -> {
                stringResource(CoreR.string.confirm_install, target.name)
            }

            is ModuleUpdateInstallTarget.All -> {
                stringResource(CoreR.string.module_updates_confirm_all, count)
            }
        },
        icon = Icons.Rounded.SystemUpdate,
        onDismissRequest = onDismiss,
        confirmAction = MagiskDialogAction(
            text = stringResource(android.R.string.ok),
            onClick = {
                val route = when (target) {
                    is ModuleUpdateInstallTarget.Single -> {
                        AppRoute.Flash(
                            action = Const.Value.FLASH_ZIP,
                            additionalData = target.zipUrl
                        )
                    }

                    is ModuleUpdateInstallTarget.All -> {
                        AppRoute.Flash(
                            action = Const.Value.FLASH_MULTIPLE_ZIPS,
                            additionalData = FlashPayloadStore.putUrls(target.urls)
                        )
                    }
                }
                onConfirm(route)
            }
        ),
        dismissAction = MagiskDialogAction(
            text = stringResource(android.R.string.cancel),
            onClick = onDismiss
        )
    )
}

@Composable
private fun ModuleUpdateCard(
    item: ModuleUiItem,
    onInstall: () -> Unit,
) {
    val update = item.update ?: return

    MagiskCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.versionAuthor,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(CoreR.string.module_updates_latest_version, update.version),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            if (update.changelog.isNotBlank() && !update.changelog.startsWith("http")) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = update.changelog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(CoreR.string.module_updates_install_one))
            }
        }
    }
}

@Composable
fun ModuleUpdatesTopBarActions(
    onRefresh: () -> Unit,
) {
    MagiskTopBarIconButton(
        icon = Icons.Rounded.Refresh,
        contentDescription = stringResource(CoreR.string.settings_check_update_title),
        onClick = onRefresh
    )
}

private fun List<ModuleUiItem>.updatable(): List<ModuleUiItem> {
    return filter { it.updateReady && it.update?.zipUrl?.isNotBlank() == true }
}
