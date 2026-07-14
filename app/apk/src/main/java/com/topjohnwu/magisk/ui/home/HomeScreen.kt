package com.topjohnwu.magisk.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SettingsInputHdmi
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.resolve
import com.topjohnwu.magisk.utils.openExternalUri
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.ui.component.MagiskBottomSheet
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.ui.component.MagiskConfirmSheet
import com.topjohnwu.magisk.ui.component.MagiskIconBadge
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskListItem
import com.topjohnwu.magisk.ui.component.MagiskSettingsGroup
import com.topjohnwu.magisk.ui.component.MagiskSettingsListItem
import com.topjohnwu.magisk.ui.component.MagiskOptionsSheet
import com.topjohnwu.magisk.ui.component.MagiskSection
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.ui.component.card.MagiskCardAction
import com.topjohnwu.magisk.ui.component.card.MagiskCardActionStyle
import com.topjohnwu.magisk.ui.component.card.MagiskStatusCard
import com.topjohnwu.magisk.ui.component.card.MagiskStatusMetric
import com.topjohnwu.magisk.ui.component.card.MagiskSupportCard
import com.topjohnwu.magisk.ui.component.card.MagiskWarningCard
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.home.HomeViewModel
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun HomeScreen(
    onNavigate: (AppRoute) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showUninstallDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = text.resolve(context)
            SystemToastManager.show(context, messageString)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.OpenUri -> {
                    context.openExternalUri(effect.uri)
                }

                is UiEffect.Reboot -> {
                    com.topjohnwu.magisk.core.ktx.reboot(effect.reason)
                }

                is UiEffect.Navigate -> {
                    onNavigate(effect.route)
                }

                else -> {}
            }
        }
    }

    // --- DIALOGS ---

    if (state.envFixCode != 0) {
        val isFullFix = state.envFixCode == 1
        MagiskConfirmSheet(
            title = stringResource(CoreR.string.env_fix_title),
            text = stringResource(if (isFullFix) CoreR.string.env_full_fix_msg else CoreR.string.env_fix_msg),
            confirmText = stringResource(if (!isFullFix) CoreR.string.reboot else CoreR.string.reinstall),
            onDismiss = viewModel::onEnvFixConsumed,
            onConfirm = {
                viewModel.onEnvFixConsumed()
                if (!isFullFix) {
                    viewModel.requestReboot()
                } else {
                    onNavigate(AppRoute.Install)
                }
            }
        )
    }

    if (state.showHideRestore) {
        MagiskConfirmSheet(
            title = stringResource(CoreR.string.restore),
            text = stringResource(CoreR.string.restore_img_msg),
            onDismiss = viewModel::onHideRestoreConsumed,
            onConfirm = {
                viewModel.restoreImages()
                viewModel.onHideRestoreConsumed()
            }
        )
    }

    if (showUninstallDialog) {
        MagiskBottomSheet(
            onDismissRequest = { showUninstallDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                // Description warning message
                MagiskWarningCard(
                    message = stringResource(CoreR.string.uninstall_magisk_msg),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                MagiskSettingsGroup(
                    title = stringResource(CoreR.string.uninstall_magisk_title),
                    icon = Icons.Rounded.DeleteSweep,
                    items = listOf(
                        {
                            MagiskSettingsListItem(
                                title = stringResource(CoreR.string.restore_img),
                                subtitle = stringResource(CoreR.string.uninstall_restore_images_subtitle),
                                leadingIcon = Icons.Rounded.SettingsBackupRestore,
                                onClick = {
                                    showUninstallDialog = false
                                    viewModel.restoreImages()
                                }
                            )
                        },
                        {
                            MagiskSettingsListItem(
                                title = stringResource(CoreR.string.complete_uninstall),
                                subtitle = stringResource(CoreR.string.uninstall_complete_subtitle),
                                leadingIcon = Icons.Rounded.DeleteForever,
                                onClick = {
                                    showUninstallDialog = false
                                    onNavigate(AppRoute.Flash(Const.Value.UNINSTALL))
                                }
                            )
                        }
                    )
                )
            }
        }
    }

    // --- MAIN SCREEN LAYOUT ---

    MagiskLazyContent(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = MagiskComponentDefaults.ScreenHorizontalPadding,
            top = 12.dp,
            end = MagiskComponentDefaults.ScreenHorizontalPadding,
            bottom = 160.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Safety Notice
        if (state.noticeVisible) {
            item {
                MagiskWarningCard(
                    title = stringResource(CoreR.string.home_safety_warning),
                    message = stringResource(CoreR.string.home_notice_content),
                    dismissContentDescription = stringResource(android.R.string.cancel),
                    onDismiss = viewModel::hideNotice
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 2. Magisk Status Card
        item {
            val isInstalled = state.magiskState != HomeViewModel.State.INVALID
            MagiskStatusCard(
                title = "Magisk",
                statusText = if (isInstalled) {
                    stringResource(CoreR.string.home_installed_version)
                } else {
                    stringResource(CoreR.string.not_installed)
                },
                statusColor = if (isInstalled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                icon = Icons.Rounded.Security,
                iconContainerColor = MagiskComponentDefaults.AccentContainer,
                iconTint = MagiskComponentDefaults.AccentContent,
                metrics = listOfNotNull(
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_installed_version),
                        value = if (isInstalled) {
                            val suffix = if (state.runtime.envDebug) " (D)" else ""
                            "${state.runtime.envVersionName}$suffix"
                        } else {
                            stringResource(CoreR.string.not_available)
                        }
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_version_code),
                        value = if (isInstalled) state.runtime.envVersionCode.toString() else {
                            stringResource(CoreR.string.not_available)
                        }
                    ),
                    if (state.magiskState == HomeViewModel.State.OUTDATED && state.managerRemoteVersion.isNotEmpty()) {
                        MagiskStatusMetric(
                            label = stringResource(CoreR.string.home_latest_version),
                            value = state.managerRemoteVersion
                        )
                    } else null
                ),
                primaryAction = MagiskCardAction(
                    text = if (state.magiskState == HomeViewModel.State.OUTDATED) {
                        stringResource(CoreR.string.update)
                    } else if (isInstalled) {
                        stringResource(CoreR.string.reinstall)
                    } else {
                        stringResource(CoreR.string.install)
                    },
                    onClick = { onNavigate(AppRoute.Install) },
                    icon = if (state.magiskState == HomeViewModel.State.OUTDATED) {
                        Icons.Rounded.Update
                    } else {
                        Icons.Rounded.Download
                    }
                ),
                secondaryAction = if (isInstalled) {
                    MagiskCardAction(
                        text = stringResource(CoreR.string.uninstall),
                        onClick = { showUninstallDialog = true },
                        icon = Icons.Rounded.DeleteForever,
                        style = MagiskCardActionStyle.Destructive
                    )
                } else {
                    null
                },
                actionsStacked = true
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. Magisk App Status Card
        item {
            MagiskStatusCard(
                title = stringResource(CoreR.string.home_app_title),
                statusText = when (state.appState) {
                    HomeViewModel.State.LOADING -> stringResource(CoreR.string.loading)
                    HomeViewModel.State.INVALID -> stringResource(CoreR.string.no_connection)
                    HomeViewModel.State.OUTDATED -> stringResource(CoreR.string.update_available)
                    HomeViewModel.State.UP_TO_DATE -> stringResource(CoreR.string.home_state_up_to_date)
                },
                statusColor = when (state.appState) {
                    HomeViewModel.State.INVALID -> MaterialTheme.colorScheme.error
                    HomeViewModel.State.OUTDATED -> MaterialTheme.colorScheme.primary
                    HomeViewModel.State.LOADING,
                    HomeViewModel.State.UP_TO_DATE -> MaterialTheme.colorScheme.primary
                },
                icon = Icons.Rounded.Android,
                iconContainerColor = MagiskComponentDefaults.AccentContainer,
                iconTint = MagiskComponentDefaults.AccentContent,
                metrics = listOfNotNull(
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_installed_version),
                        value = state.managerInstalledVersion
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_version_code),
                        value = state.managerInstalledVersionCode
                    ),
                    if (state.appState == HomeViewModel.State.OUTDATED && state.managerRemoteVersion.isNotEmpty()) {
                        MagiskStatusMetric(
                            label = stringResource(CoreR.string.home_latest_version),
                            value = state.managerRemoteVersion
                        )
                    } else null
                ),
                primaryAction = MagiskCardAction(
                    text = if (state.appState == HomeViewModel.State.OUTDATED) {
                        stringResource(CoreR.string.update)
                    } else {
                        stringResource(CoreR.string.reinstall)
                    },
                    onClick = { onNavigate(AppRoute.AppUpdate) },
                    icon = if (state.appState == HomeViewModel.State.OUTDATED) {
                        Icons.Rounded.Update
                    } else {
                        Icons.Rounded.Download
                    }
                )
            )
        }

        // 4. Support & Contributors Link Card
        item {
            MagiskCard(
                onClick = { onNavigate(AppRoute.Support) },
                shape = MagiskComponentDefaults.CardShape,
                containerColor = MagiskComponentDefaults.CardContainer,
                border = MagiskComponentDefaults.CardBorder,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MagiskIconBadge(
                        icon = Icons.Rounded.Favorite,
                        containerColor = MagiskComponentDefaults.AccentContainer,
                        iconTint = MagiskComponentDefaults.AccentContent
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(CoreR.string.home_support_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MagiskComponentDefaults.PrimaryText
                        )
                        Text(
                            text = stringResource(CoreR.string.home_support_content_short),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MagiskComponentDefaults.SecondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MagiskComponentDefaults.SecondaryIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTopBarActions(
    viewModel: HomeViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (!state.runtime.isRooted) return

    var showRebootSheet by remember { mutableStateOf(false) }

    MagiskTopBarIconButton(
        icon = Icons.Rounded.RestartAlt,
        contentDescription = stringResource(CoreR.string.reboot),
        onClick = { showRebootSheet = true }
    )

    if (showRebootSheet) {
        MagiskOptionsSheet(
            title = stringResource(CoreR.string.reboot),
            icon = Icons.Rounded.RestartAlt,
            onDismiss = { showRebootSheet = false },
            items = listOf(
                Triple(
                    Icons.Rounded.RestartAlt,
                    stringResource(CoreR.string.reboot)
                ) { viewModel.requestReboot() },
                Triple(
                    Icons.Rounded.Refresh,
                    stringResource(CoreR.string.reboot_userspace)
                ) { viewModel.requestReboot("userspace") },
                Triple(
                    Icons.Rounded.SettingsBackupRestore,
                    stringResource(CoreR.string.reboot_recovery)
                ) { viewModel.requestReboot("recovery") },
                Triple(
                    Icons.Rounded.SettingsInputHdmi,
                    stringResource(CoreR.string.reboot_bootloader)
                ) { viewModel.requestReboot("bootloader") },
                Triple(
                    Icons.Rounded.Download,
                    stringResource(CoreR.string.reboot_download)
                ) { viewModel.requestReboot("download") },
                Triple(
                    Icons.Rounded.DeveloperMode,
                    stringResource(CoreR.string.reboot_edl)
                ) { viewModel.requestReboot("edl") },
            )
        )
    }
}
