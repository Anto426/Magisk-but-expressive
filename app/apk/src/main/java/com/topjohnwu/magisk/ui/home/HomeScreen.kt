package com.topjohnwu.magisk.ui.home

import android.R
import android.content.Intent
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
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.ui.component.MagiskBottomSheet
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogAction
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskListItem
import com.topjohnwu.magisk.ui.component.MagiskLoadingState
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
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showUninstallDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.OpenUri -> {
                    context.startActivity(Intent(Intent.ACTION_VIEW, effect.uri))
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
        MagiskDialog(
            title = stringResource(CoreR.string.env_fix_title),
            onDismissRequest = viewModel::onEnvFixConsumed,
            text = stringResource(if (isFullFix) CoreR.string.env_full_fix_msg else CoreR.string.env_fix_msg),
            confirmAction = if (!isFullFix) {
                MagiskDialogAction(
                    text = stringResource(android.R.string.ok), onClick = {
                        viewModel.onEnvFixConsumed()
                        viewModel.requestReboot()
                    })
            } else {
                MagiskDialogAction(
                    text = stringResource(CoreR.string.install), onClick = {
                        viewModel.onEnvFixConsumed()
                        onNavigate(AppRoute.Install)
                    })
            },
            dismissAction = MagiskDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = viewModel::onEnvFixConsumed
            )
        )
    }

    if (state.showHideRestore) {
        MagiskDialog(
            title = stringResource(CoreR.string.restore),
            onDismissRequest = viewModel::onHideRestoreConsumed,
            text = stringResource(CoreR.string.restore_img_msg),
            confirmAction = MagiskDialogAction(
                text = stringResource(android.R.string.ok), onClick = {
                    viewModel.restoreImages()
                    viewModel.onHideRestoreConsumed()
                }),
            dismissAction = MagiskDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = viewModel::onHideRestoreConsumed
            )
        )
    }

    if (state.showManagerInstall) {
        MagiskDialog(
            title = stringResource(CoreR.string.update),
            onDismissRequest = viewModel::onManagerInstallConsumed,
            text = state.managerReleaseNotes.ifEmpty { stringResource(CoreR.string.update_available) },
            confirmAction = MagiskDialogAction(
                text = stringResource(android.R.string.ok), onClick = {
                    viewModel.onManagerInstallConsumed()
                    viewModel.openLink(Info.update.link)
                }),
            dismissAction = MagiskDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = viewModel::onManagerInstallConsumed
            )
        )
    }

    if (showUninstallDialog) {
        MagiskDialog(
            title = stringResource(CoreR.string.uninstall_magisk_title),
            onDismissRequest = { showUninstallDialog = false },
            icon = Icons.Rounded.DeleteSweep,
            textContent = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(CoreR.string.uninstall_magisk_msg),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    MagiskCard(onClick = {
                        showUninstallDialog = false
                        viewModel.restoreImages()
                    }) {
                        MagiskListItem(
                            title = stringResource(CoreR.string.restore_img),
                            subtitle = stringResource(CoreR.string.uninstall_restore_images_subtitle),
                            leadingIcon = Icons.Rounded.SettingsBackupRestore
                        )
                    }
                    MagiskCard(onClick = {
                        showUninstallDialog = false
                        onNavigate(AppRoute.Flash(Const.Value.UNINSTALL))
                    }) {
                        MagiskListItem(
                            title = stringResource(CoreR.string.complete_uninstall),
                            subtitle = stringResource(CoreR.string.uninstall_complete_subtitle),
                            leadingIcon = Icons.Rounded.DeleteForever
                        )
                    }
                }
            },
            dismissAction = MagiskDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = { showUninstallDialog = false }
            )
        )
    }

    // --- MAIN SCREEN LAYOUT ---

    MagiskLazyContent(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = MagiskComponentDefaults.ScreenHorizontalPadding,
            top = 12.dp,
            end = MagiskComponentDefaults.ScreenHorizontalPadding,
            bottom = 132.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Safety Notice
        if (state.noticeVisible) {
            item {
                MagiskWarningCard(
                    title = stringResource(CoreR.string.home_safety_warning),
                    message = stringResource(CoreR.string.home_notice_content),
                    dismissContentDescription = stringResource(R.string.cancel),
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
                iconContainerColor = MaterialTheme.colorScheme.primary,
                iconTint = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 4.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 32.dp
                ),
                metrics = listOf(
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_installed_version),
                        value = if (isInstalled) state.runtime.envVersionName else stringResource(
                            CoreR.string.not_available
                        )
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_version_code),
                        value = if (isInstalled) state.runtime.envVersionCode.toString() else {
                            stringResource(CoreR.string.not_available)
                        }
                    )
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
                statusText = stringResource(CoreR.string.home_package) + ": " + state.packageName,
                statusColor = MaterialTheme.colorScheme.secondary,
                icon = Icons.Rounded.Android,
                iconContainerColor = MaterialTheme.colorScheme.secondary,
                iconTint = MaterialTheme.colorScheme.onSecondary,
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 32.dp,
                    bottomStart = 32.dp,
                    bottomEnd = 4.dp
                ),
                metrics = listOf(
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_installed_version),
                        value = state.managerInstalledVersion
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_version_code),
                        value = state.managerInstalledVersionCode
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_latest_version),
                        value = state.managerRemoteVersion.ifEmpty {
                            stringResource(CoreR.string.not_available)
                        }
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_latest_version_code),
                        value = state.managerRemoteVersionCode.ifEmpty {
                            stringResource(CoreR.string.not_available)
                        }
                    )
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
                    Surface(
                        modifier = Modifier.size(MagiskComponentDefaults.IconBadgeSize),
                        shape = MagiskComponentDefaults.ControlShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(CoreR.string.home_support_and_contributors),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MagiskComponentDefaults.PrimaryText
                        )
                        Text(
                            text = stringResource(CoreR.string.home_support_content),
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
    val state by viewModel.state.collectAsState()

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
