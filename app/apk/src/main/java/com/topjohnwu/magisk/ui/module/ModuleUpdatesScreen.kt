package com.topjohnwu.magisk.ui.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.resolve
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.navigation.FlashPayloadStore
import com.topjohnwu.magisk.ui.component.MagiskConfirmSheet
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogAction
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskLoader
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.ui.motion.MagiskAnimatedVisibility
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val updates = remember(state.modules) { state.modules.updatable() }
    val updateUrls = remember(updates) {
        updates.mapNotNull { item ->
            item.update?.zipUrl?.trim()?.takeIf(String::isNotBlank)
        }.distinct()
    }
    var pendingInstall by remember { mutableStateOf<ModuleUpdateInstallTarget?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.refresh(force = true)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = text.resolve(context)
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
        MagiskLoader(modifier = modifier.fillMaxSize())
        return
    }

    MagiskLazyContent(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleUpdatesHeader(
                updateCount = updates.size,
                moduleCount = state.modules.size,
                checking = state.checkingUpdates,
                failed = state.updateCheckFailed,
                onRefresh = { viewModel.refresh(force = true) },
                onInstallAll = updateUrls.takeIf { it.isNotEmpty() }?.let { urls ->
                    {
                        pendingInstall = ModuleUpdateInstallTarget.All(urls)
                    }
                }
            )
        }

        if (updates.isEmpty() && !state.checkingUpdates) {
            item {
                MagiskEmptyState(
                    title = stringResource(
                        if (state.updateCheckFailed) CoreR.string.no_connection
                        else CoreR.string.module_updates_empty
                    ),
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
                onChangelog = updates[index].update
                    ?.changelog
                    ?.takeIf(String::isNotBlank)
                    ?.let {
                        {
                            onNavigate(
                                AppRoute.Changelog(
                                    moduleId = updates[index].id,
                                    title = updates[index].name
                                )
                            )
                        }
                    },
                onInstall = onInstall@{
                    val update = updates[index].update ?: return@onInstall
                    val zipUrl = update.zipUrl.trim().takeIf(String::isNotBlank) ?: return@onInstall
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
private fun ModuleUpdatesHeader(
    updateCount: Int,
    moduleCount: Int,
    checking: Boolean,
    failed: Boolean,
    onRefresh: () -> Unit,
    onInstallAll: (() -> Unit)?,
) {
    MagiskCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (checking) {
                        stringResource(CoreR.string.module_updates_checking)
                    } else {
                        stringResource(CoreR.string.module_updates_available_count, updateCount)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        failed -> stringResource(CoreR.string.no_connection)
                        updateCount > 0 -> stringResource(CoreR.string.module_updates_ready_summary)
                        else -> stringResource(CoreR.string.module_updates_empty)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(CoreR.string.module_updates_installed_count, moduleCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        MagiskAnimatedVisibility(visible = checking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (onInstallAll != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onInstallAll,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MagiskComponentDefaults.ControlShape
                ) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(CoreR.string.module_updates_install_all))
                }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MagiskComponentDefaults.ControlShape
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(CoreR.string.settings_check_update_title))
                }
            }
        } else {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                shape = MagiskComponentDefaults.ControlShape
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(CoreR.string.settings_check_update_title))
            }
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

    MagiskConfirmSheet(
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
        onDismiss = onDismiss,
        onConfirm = {
            val route = when (target) {
                is ModuleUpdateInstallTarget.Single -> {
                    AppRoute.Flash(
                        action = Const.Value.FLASH_ZIP,
                        additionalData = target.zipUrl
                    )
                }

                is ModuleUpdateInstallTarget.All -> {
                    val urls = target.urls.takeIf { it.isNotEmpty() } ?: return@MagiskConfirmSheet
                    AppRoute.Flash(
                        action = Const.Value.FLASH_MULTIPLE_ZIPS,
                        additionalData = FlashPayloadStore.putUrls(urls)
                    )
                }
            }
            onConfirm(route)
        }
    )
}

@Composable
private fun ModuleUpdateCard(
    item: ModuleUiItem,
    onChangelog: (() -> Unit)?,
    onInstall: () -> Unit,
) {
    val update = item.update ?: return

    MagiskCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MagiskComponentDefaults.CardContainer,
        contentPadding = PaddingValues(16.dp),
        border = MagiskComponentDefaults.CardBorder
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + Title & Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Extension,
                            contentDescription = null
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                        Text(
                            text = stringResource(CoreR.string.update_available),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Divider + Metrics
            HorizontalDivider(color = MagiskComponentDefaults.DividerColor)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Metric 1: Installed Version
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 24.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = stringResource(CoreR.string.home_installed_version),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.versionAuthor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Metric 2: Latest Version
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 24.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = stringResource(CoreR.string.home_latest_version),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = update.version,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            onChangelog?.let { openChangelog ->
                OutlinedButton(
                    onClick = openChangelog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MagiskComponentDefaults.ControlShape
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Article,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(CoreR.string.release_notes))
                }
            }

            // Install Button
            Button(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth(),
                shape = MagiskComponentDefaults.ControlShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null
                )
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
