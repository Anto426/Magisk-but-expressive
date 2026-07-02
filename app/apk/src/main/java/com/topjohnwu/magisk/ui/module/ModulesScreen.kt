package com.topjohnwu.magisk.ui.module

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.ExtensionOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogAction
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskExpandableListItem
import com.topjohnwu.magisk.ui.component.MagiskInfoPill
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskListItem
import com.topjohnwu.magisk.ui.component.MagiskLoadingState
import com.topjohnwu.magisk.ui.component.MagiskSearchField
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.ui.component.card.MagiskWarningCard
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.module.ModuleUiItem
import com.topjohnwu.magisk.viewmodel.module.ModuleViewModel
import com.topjohnwu.magisk.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(
    onNavigate: (AppRoute) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ModuleViewModel = viewModel(factory = ModuleViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Dialog state for update confirmation
    var pendingUpdateModule by remember { mutableStateOf<ModuleUiItem?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onNavigate(
                AppRoute.Flash(
                    action = Const.Value.FLASH_ZIP, additionalData = uri.toString()
                )
            )
        }
    }

    LaunchedEffect(viewModel) {
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

    // Update confirmation dialog
    pendingUpdateModule?.let { item ->
        val updateModule = item.update ?: return@let
        MagiskDialog(
            title = stringResource(CoreR.string.confirm_install_title),
            text = stringResource(CoreR.string.confirm_install, updateModule.name),
            icon = Icons.Rounded.SystemUpdate,
            onDismissRequest = { pendingUpdateModule = null },
            confirmAction = MagiskDialogAction(
                text = stringResource(android.R.string.ok), onClick = {
                    pendingUpdateModule = null
                    onNavigate(
                        AppRoute.Flash(
                            action = Const.Value.FLASH_ZIP, additionalData = updateModule.zipUrl
                        )
                    )
                }),
            dismissAction = MagiskDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = { pendingUpdateModule = null })
        )
    }

    if (state.loading) {
        MagiskLoadingState(modifier = modifier.fillMaxSize())
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            AnimatedVisibility(visible = state.searchVisible) {
                MagiskSearchField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = stringResource(CoreR.string.hide_search),
                    clearContentDescription = stringResource(CoreR.string.clear_search),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            MagiskLazyContent(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(
                    top = 8.dp, bottom = 120.dp, start = 16.dp, end = 16.dp
                )
            ) {
                item(key = "install_from_storage") {
                    InstallFromStorageModuleItem(
                        onClick = { filePickerLauncher.launch("application/zip") }
                    )
                }

                if (state.filteredModules.isEmpty()) {
                    item(key = "empty_modules") {
                        MagiskEmptyState(
                            title = stringResource(CoreR.string.module_empty),
                            icon = Icons.Rounded.ExtensionOff
                        )
                    }
                } else {
                    items(
                        count = state.filteredModules.size,
                        key = { index -> state.filteredModules[index].id }) { index ->
                        val item = state.filteredModules[index]
                        val isActive = item.enabled && !item.removed
                        val alpha by animateFloatAsState(
                            targetValue = if (isActive) 1f else 0.65f,
                            label = "ModuleCardAlpha"
                        )
                        MagiskExpandableListItem(
                            title = item.name,
                            subtitle = item.versionAuthor,
                            expanded = item.expanded,
                            onClick = { viewModel.toggleExpanded(item.id) },
                            leadingIcon = Icons.Rounded.Extension,
                            showArrow = false,
                            modifier = Modifier.graphicsLayer(alpha = alpha),
                            headerTrailingContent = {
                                if (item.updateReady && item.update != null) {
                                    MagiskInfoPill(
                                        text = "Aggiorna: " + item.update.version,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                }
                                if (item.removed) {
                                    Text(
                                        text = stringResource(CoreR.string.module_state_remove),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Switch(
                                        checked = item.enabled,
                                        onCheckedChange = { viewModel.toggleEnabled(item.id) })
                                }
                            }) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Text(
                                    text = item.description.ifBlank { stringResource(CoreR.string.no_info_provided) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (item.removed) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteForever,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Column {
                                                Text(
                                                    text = "Disinstallazione programmata",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    text = "Il modulo verrà rimosso al prossimo riavvio.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(
                                                        alpha = 0.8f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } else if (item.updated) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.35f
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.RestartAlt,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Column {
                                                Text(
                                                    text = "Riavvio richiesto",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "Modulo aggiornato/installato. Riavvia per applicare le modifiche.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                        alpha = 0.8f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } else if (item.updateReady && item.update != null) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                            alpha = 0.35f
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Update,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                                Text(
                                                    text = "Nuova versione disponibile: " + item.update.version,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                            if (item.update.changelog.isNotBlank()) {
                                                Text(
                                                    text = "Changelog:\n" + item.update.changelog,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                                        alpha = 0.8f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // Display notice text (if present)
                                item.noticeText?.let { notice ->
                                    val noticeStr = when (notice) {
                                        is UiText.Plain -> notice.value
                                        is UiText.Resource -> stringResource(
                                            notice.resId, *notice.args.toTypedArray()
                                        )
                                    }
                                    MagiskWarningCard(message = noticeStr)
                                }

                                // Badges SUGGESTION row
                                if (item.badges.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        item.badges.forEach { badge ->
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(text = badge) })
                                        }
                                    }
                                }

                                // Actions column
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (item.showAction) {
                                        Button(
                                            onClick = {
                                                onNavigate(
                                                    AppRoute.ModuleAction(
                                                        item.id, item.name
                                                    )
                                                )
                                            }, modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Rounded.PlayArrow,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = stringResource(CoreR.string.module_action))
                                        }
                                    }

                                    if (item.updateReady && item.update != null) {
                                        Button(
                                            onClick = { pendingUpdateModule = item },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Rounded.Update, contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = stringResource(CoreR.string.module_badge_update))
                                        }
                                    }

                                    if (item.removed) {
                                        Button(
                                            onClick = { viewModel.toggleRemove(item.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        ) {
                                            Icon(
                                                Icons.Rounded.Restore, contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = stringResource(CoreR.string.module_state_restore))
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.toggleRemove(item.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            border = BorderStroke(
                                                1.dp, MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Icon(
                                                Icons.Rounded.Delete, contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = stringResource(CoreR.string.module_state_remove))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallFromStorageModuleItem(
    onClick: () -> Unit
) {
    MagiskCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MagiskComponentDefaults.CardContainer,
        border = MagiskComponentDefaults.CardBorder,
        contentPadding = PaddingValues(0.dp)
    ) {
        MagiskListItem(
            title = stringResource(CoreR.string.module_action_install_external),
            subtitle = stringResource(CoreR.string.module_action_install_external_summary),
            leadingIcon = Icons.Rounded.FolderOpen,
            onClick = onClick,
            shape = MagiskComponentDefaults.CardShape,
            trailingContent = {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MagiskComponentDefaults.SecondaryIconTint
                )
            }
        )
    }
}

@Composable
fun ModulesTopBarActions(
    searchVisible: Boolean,
    onToggleSearch: () -> Unit,
    updateCount: Int,
    onUpdatesClick: () -> Unit
) {
    BadgedBox(
        badge = {
            if (updateCount > 0) {
                Badge {
                    Text(text = updateCount.coerceAtMost(99).toString())
                }
            }
        }
    ) {
        MagiskTopBarIconButton(
            icon = Icons.Rounded.SystemUpdate,
            contentDescription = stringResource(CoreR.string.module_updates_title),
            onClick = onUpdatesClick
        )
    }
    MagiskTopBarIconButton(
        icon = if (searchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
        contentDescription = stringResource(CoreR.string.hide_search),
        onClick = onToggleSearch
    )
}
