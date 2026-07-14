package com.topjohnwu.magisk.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Rule
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UIActivity
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.resolve
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.core.tasks.AppMigration
import com.topjohnwu.magisk.core.utils.LocaleSetting
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.ui.component.MagiskBottomSheet
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogAction
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskOptionsSheet
import com.topjohnwu.magisk.ui.component.MagiskAnimatedSearchField
import com.topjohnwu.magisk.ui.component.MagiskSearchActionButton
import com.topjohnwu.magisk.ui.component.MagiskSettingsGroup
import com.topjohnwu.magisk.ui.component.MagiskSettingsListItem
import com.topjohnwu.magisk.ui.component.MagiskSettingsSwitchItem
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.view.NotificationCenter
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.settings.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

private enum class PickerType {
    LANGUAGE, UPDATE_CHANNEL, SU_ACCESS, MULTIUSER, NAMESPACE, AUTO_RESPONSE, TIMEOUT, NOTIFICATION
}

@Composable
fun SettingsScreen(
    onNavigate: (AppRoute) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun withAuthentication(action: () -> Unit) {
        val uiActivity = context as? UIActivity<*> ?: return
        uiActivity.withAuthentication { granted ->
            if (granted) {
                action()
            }
        }
    }

    fun withSuAuthentication(action: () -> Unit) {
        if (Config.suAuth) {
            withAuthentication(action)
        } else {
            action()
        }
    }

    // Dialog & Picker States
    var activePicker by remember { mutableStateOf<PickerType?>(null) }
    var showCustomChannelUrlDialog by remember { mutableStateOf(false) }
    var showDownloadPathDialog by remember { mutableStateOf(false) }
    var showHideAppDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.refreshState()
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = text.resolve(context)
            SystemToastManager.show(context, messageString)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val uiActivity = context as? UIActivity<*> ?: return@collect
            when (effect) {
                is UiEffect.RequestHideApp -> {
                    uiActivity.withPermission("android.permission.REQUEST_INSTALL_PACKAGES") { granted ->
                        if (granted) {
                            coroutineScope.launch {
                                val success = AppMigration.patchAndHide(context, effect.label)
                                viewModel.onAppMigrationResult(success)
                            }
                        } else {
                            viewModel.onAppMigrationResult(false)
                        }
                    }
                }

                UiEffect.RequestRestoreApp -> {
                    uiActivity.withPermission("android.permission.REQUEST_INSTALL_PACKAGES") { granted ->
                        if (granted) {
                            coroutineScope.launch {
                                val success = AppMigration.restoreApp(context)
                                viewModel.onAppMigrationResult(success)
                            }
                        } else {
                            viewModel.onAppMigrationResult(false)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    // Custom Channel URL Bottom Sheet
    if (showCustomChannelUrlDialog) {
        var tempUrl by remember { mutableStateOf(state.customChannelUrl) }
        val customUrlValid = SettingsViewModel.normalizeCustomChannelUrl(tempUrl) != null
        MagiskBottomSheet(onDismissRequest = { showCustomChannelUrlDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title group header (same style as MagiskSettingsGroup)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = MagiskComponentDefaults.PrimaryIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(CoreR.string.settings_update_custom),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MagiskComponentDefaults.PrimaryIconTint
                    )
                }

                // Input field inside a card (same style as MagiskSettingsGroup Surface)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MagiskComponentDefaults.CardShape,
                    color = MagiskComponentDefaults.CardContainer,
                    border = MagiskComponentDefaults.CardBorder
                ) {
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = {
                            Text(
                                text = stringResource(CoreR.string.settings_update_custom_msg),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MagiskComponentDefaults.ControlShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MagiskComponentDefaults.PrimaryIconTint,
                            unfocusedBorderColor = MagiskComponentDefaults.DividerColor,
                            focusedLabelColor = MagiskComponentDefaults.PrimaryIconTint,
                            unfocusedLabelColor = MagiskComponentDefaults.SecondaryText,
                            cursorColor = MagiskComponentDefaults.PrimaryIconTint
                        )
                    )
                }

                // Action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = { showCustomChannelUrlDialog = false }) {
                        Text(
                            text = stringResource(android.R.string.cancel),
                            color = MagiskComponentDefaults.SecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = {
                            if (viewModel.setCustomChannelUrl(tempUrl)) {
                                showCustomChannelUrlDialog = false
                            }
                        },
                        enabled = customUrlValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MagiskComponentDefaults.PrimaryIconTint
                        ),
                        shape = MagiskComponentDefaults.ControlShape
                    ) {
                        Text(
                            text = stringResource(android.R.string.ok),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showDownloadPathDialog) {
        var tempDir by remember(state.downloadDir) { mutableStateOf(state.downloadDir) }
        val downloadDirectoryValid = MediaStoreUtils.normalizeDownloadDirectory(tempDir) != null
        MagiskBottomSheet(onDismissRequest = { showDownloadPathDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = MagiskComponentDefaults.PrimaryIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(CoreR.string.settings_download_path_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MagiskComponentDefaults.PrimaryIconTint
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MagiskComponentDefaults.CardShape,
                    color = MagiskComponentDefaults.CardContainer,
                    border = MagiskComponentDefaults.CardBorder
                ) {
                    OutlinedTextField(
                        value = tempDir,
                        onValueChange = { tempDir = it },
                        label = {
                            Text(
                                text = stringResource(CoreR.string.settings_download_path_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MagiskComponentDefaults.ControlShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MagiskComponentDefaults.PrimaryIconTint,
                            unfocusedBorderColor = MagiskComponentDefaults.DividerColor,
                            focusedLabelColor = MagiskComponentDefaults.PrimaryIconTint,
                            unfocusedLabelColor = MagiskComponentDefaults.SecondaryText,
                            cursorColor = MagiskComponentDefaults.PrimaryIconTint
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = { showDownloadPathDialog = false }) {
                        Text(
                            text = stringResource(android.R.string.cancel),
                            color = MagiskComponentDefaults.SecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = {
                            if (viewModel.setDownloadDir(tempDir)) {
                                showDownloadPathDialog = false
                            }
                        },
                        enabled = downloadDirectoryValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MagiskComponentDefaults.PrimaryIconTint
                        ),
                        shape = MagiskComponentDefaults.ControlShape
                    ) {
                        Text(
                            text = stringResource(android.R.string.ok),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Hide App Bottom Sheet
    if (showHideAppDialog) {
        val defaultLabel = stringResource(CoreR.string.settings)
        var tempLabel by remember(defaultLabel) { mutableStateOf(defaultLabel) }

        MagiskBottomSheet(onDismissRequest = { showHideAppDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title group header (same style as MagiskSettingsGroup)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HideSource,
                        contentDescription = null,
                        tint = MagiskComponentDefaults.PrimaryIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(CoreR.string.settings_hide_app_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MagiskComponentDefaults.PrimaryIconTint
                    )
                }

                // Input field inside a card (same style as MagiskSettingsGroup Surface)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MagiskComponentDefaults.CardShape,
                    color = MagiskComponentDefaults.CardContainer,
                    border = MagiskComponentDefaults.CardBorder
                ) {
                    OutlinedTextField(
                        value = tempLabel,
                        onValueChange = { tempLabel = it },
                        label = {
                            Text(
                                text = stringResource(CoreR.string.settings_app_name_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MagiskComponentDefaults.ControlShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MagiskComponentDefaults.PrimaryIconTint,
                            unfocusedBorderColor = MagiskComponentDefaults.DividerColor,
                            focusedLabelColor = MagiskComponentDefaults.PrimaryIconTint,
                            unfocusedLabelColor = MagiskComponentDefaults.SecondaryText,
                            cursorColor = MagiskComponentDefaults.PrimaryIconTint
                        )
                    )
                }

                // Action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = { showHideAppDialog = false }) {
                        Text(
                            text = stringResource(android.R.string.cancel),
                            color = MagiskComponentDefaults.SecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = {
                            showHideAppDialog = false
                            viewModel.requestHideApp(tempLabel)
                        },
                        enabled = tempLabel.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MagiskComponentDefaults.PrimaryIconTint
                        ),
                        shape = MagiskComponentDefaults.ControlShape
                    ) {
                        Text(
                            text = stringResource(android.R.string.ok),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Picker Dialog picker
    activePicker?.let { picker ->
        val dialogTitle = when (picker) {
            PickerType.LANGUAGE -> stringResource(CoreR.string.language)
            PickerType.UPDATE_CHANNEL -> stringResource(CoreR.string.settings_update_channel_title)
            PickerType.SU_ACCESS -> stringResource(CoreR.string.superuser_access)
            PickerType.MULTIUSER -> stringResource(CoreR.string.multiuser_mode)
            PickerType.NAMESPACE -> stringResource(CoreR.string.mount_namespace_mode)
            PickerType.AUTO_RESPONSE -> stringResource(CoreR.string.auto_response)
            PickerType.TIMEOUT -> stringResource(CoreR.string.request_timeout)
            PickerType.NOTIFICATION -> stringResource(CoreR.string.superuser_notification)
        }

        val options: List<String> = when (picker) {
            PickerType.LANGUAGE -> LocaleSetting.available.names.toList()
            PickerType.UPDATE_CHANNEL -> stringArrayResource(CoreR.array.update_channel).toList()

            PickerType.SU_ACCESS -> stringArrayResource(CoreR.array.su_access).toList()
            PickerType.MULTIUSER -> stringArrayResource(CoreR.array.multiuser_mode)
                .toList()

            PickerType.NAMESPACE -> stringArrayResource(CoreR.array.namespace).toList()
            PickerType.AUTO_RESPONSE -> stringArrayResource(CoreR.array.auto_response)
                .toList()

            PickerType.TIMEOUT -> stringArrayResource(CoreR.array.request_timeout)
                .toList()

            PickerType.NOTIFICATION -> stringArrayResource(CoreR.array.su_notification)
                .toList()
        }

        val selectedIndex = when (picker) {
            PickerType.LANGUAGE -> state.languageIndex
            PickerType.UPDATE_CHANNEL -> state.updateChannel
            PickerType.SU_ACCESS -> state.rootMode
            PickerType.MULTIUSER -> state.suMultiuserMode
            PickerType.NAMESPACE -> state.suMntNamespaceMode
            PickerType.AUTO_RESPONSE -> state.suAutoResponse
            PickerType.TIMEOUT -> state.suTimeoutIndex
            PickerType.NOTIFICATION -> state.suNotification
        }

        MagiskOptionsSheet(
            title = dialogTitle,
            selectedIndex = selectedIndex,
            onDismiss = { activePicker = null },
            items = options.mapIndexed { index, label ->
                Triple(null, label) {
                    when (picker) {
                        PickerType.LANGUAGE -> viewModel.setLanguageByIndex(index)
                        PickerType.UPDATE_CHANNEL -> {
                            if (index == Config.Value.CUSTOM_CHANNEL) {
                                showCustomChannelUrlDialog = true
                            } else {
                                viewModel.setUpdateChannel(index)
                            }
                        }
                        PickerType.SU_ACCESS -> viewModel.setRootMode(index)
                        PickerType.MULTIUSER -> viewModel.setSuMultiuserMode(index)
                        PickerType.NAMESPACE -> viewModel.setSuMntNamespaceMode(index)
                        PickerType.AUTO_RESPONSE -> withSuAuthentication {
                            viewModel.setSuAutoResponse(index)
                        }
                        PickerType.TIMEOUT -> viewModel.setSuTimeoutIndex(index)
                        PickerType.NOTIFICATION -> {
                            if (index == Config.Value.NOTIFICATION_STATUS_BAR) {
                                val activity = context as? UIActivity<*>
                                if (activity == null) {
                                    viewModel.setMessageRes(CoreR.string.post_notifications_denied)
                                } else {
                                    activity.withPermission("android.permission.POST_NOTIFICATIONS") { granted ->
                                        if (granted && NotificationCenter.canPostRootNotifications()) {
                                            viewModel.setSuNotification(index)
                                        } else {
                                            viewModel.setMessageRes(CoreR.string.post_notifications_denied)
                                            runCatching {
                                                activity.startActivity(
                                                    if (granted) {
                                                        NotificationCenter.rootNotificationSettingsIntent()
                                                    } else {
                                                        NotificationCenter.appNotificationSettingsIntent()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                viewModel.setSuNotification(index)
                            }
                        }
                    }
                    activePicker = null
                }
            }
        )
    }

    val groups = buildList {
        fun addGroup(title: String, icon: ImageVector, items: List<SettingScreenItem>) {
            if (items.isNotEmpty()) {
                add(SettingScreenGroup(title = title, icon = icon, items = items))
            }
        }

        val modeLabel = when (state.darkThemeMode) {
            -1 -> stringResource(CoreR.string.settings_dark_mode_system)
            0 -> stringResource(CoreR.string.settings_dark_mode_light)
            1 -> stringResource(CoreR.string.settings_dark_mode_dark)
            -2 -> stringResource(CoreR.string.settings_dark_mode_amoled)
            else -> stringResource(CoreR.string.settings_dark_mode_system)
        }
        val colorLabel = stringResource(state.themeNameRes)
        val updateChannelSubtitle = if (state.isCustomChannel && state.customChannelUrl.isNotBlank()) {
            state.customChannelUrl
        } else {
            state.updateChannelName
        }
        val customChannelSubtitle = state.customChannelUrl.ifBlank {
            stringResource(CoreR.string.settings_update_custom_msg)
        }
        val downloadPathSummary = stringResource(
            CoreR.string.settings_download_path_message,
            state.downloadDirPath
        )

        addGroup(
            title = stringResource(CoreR.string.settings_group_appearance_language),
            icon = Icons.Rounded.Palette,
            items = buildList {
                add(
                    SettingScreenItem(
                        title = stringResource(CoreR.string.theme_appearance),
                        subtitle = "$modeLabel / $colorLabel",
                        content = {
                            MagiskSettingsListItem(
                                title = stringResource(CoreR.string.theme_appearance),
                                subtitle = "$modeLabel / $colorLabel",
                                leadingIcon = Icons.Rounded.Palette,
                                onClick = { onNavigate(AppRoute.Theme) })
                        }
                    )
                )
                add(
                    SettingScreenItem(
                        title = stringResource(CoreR.string.language),
                        subtitle = state.languageName,
                        content = {
                            MagiskSettingsListItem(
                                title = stringResource(CoreR.string.language),
                                subtitle = state.languageName,
                                leadingIcon = Icons.Rounded.Language,
                                onClick = { onNavigate(AppRoute.Language) })
                        }
                    )
                )
            }
        )

        addGroup(
            title = stringResource(CoreR.string.settings_group_app_identity),
            icon = Icons.Rounded.Android,
            items = buildList {
                if (state.canAddShortcut) {
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.add_shortcut_title),
                            subtitle = stringResource(CoreR.string.setting_add_shortcut_summary),
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.add_shortcut_title),
                                    subtitle = stringResource(CoreR.string.setting_add_shortcut_summary),
                                    leadingIcon = Icons.AutoMirrored.Rounded.AddToHomeScreen,
                                    onClick = viewModel::addShortcut
                                )
                            }
                        )
                    )
                }
                if (state.canRestoreApp) {
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.settings_restore_app_title),
                            subtitle = stringResource(CoreR.string.settings_restore_app_summary),
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.settings_restore_app_title),
                                    subtitle = stringResource(CoreR.string.settings_restore_app_summary),
                                    leadingIcon = Icons.Rounded.SettingsBackupRestore,
                                    onClick = viewModel::requestRestoreApp
                                )
                            }
                        )
                    )
                } else if (state.canHideApp) {
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.settings_hide_app_title),
                            subtitle = stringResource(CoreR.string.settings_hide_app_summary),
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.settings_hide_app_title),
                                    subtitle = stringResource(CoreR.string.settings_hide_app_summary),
                                    leadingIcon = Icons.Rounded.HideSource,
                                    onClick = { showHideAppDialog = true })
                            }
                        )
                    )
                }
            }
        )

        addGroup(
            title = stringResource(CoreR.string.settings_group_updates),
            icon = Icons.Rounded.Update,
            items = buildList {
                add(
                    SettingScreenItem(
                        title = stringResource(CoreR.string.settings_check_update_title),
                        subtitle = stringResource(CoreR.string.settings_check_update_summary),
                        content = {
                            MagiskSettingsSwitchItem(
                                title = stringResource(CoreR.string.settings_check_update_title),
                                subtitle = stringResource(CoreR.string.settings_check_update_summary),
                                checked = state.checkUpdate,
                                onCheckedChange = viewModel::setCheckUpdate,
                                leadingIcon = Icons.Rounded.Update
                            )
                        }
                    )
                )
                add(
                    SettingScreenItem(
                        title = stringResource(CoreR.string.settings_update_channel_title),
                        subtitle = updateChannelSubtitle,
                        content = {
                            MagiskSettingsListItem(
                                title = stringResource(CoreR.string.settings_update_channel_title),
                                subtitle = updateChannelSubtitle,
                                leadingIcon = Icons.AutoMirrored.Rounded.CallSplit,
                                onClick = { activePicker = PickerType.UPDATE_CHANNEL })
                        }
                    )
                )
                if (state.isCustomChannel) {
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.settings_update_custom),
                            subtitle = customChannelSubtitle,
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.settings_update_custom),
                                    subtitle = customChannelSubtitle,
                                    leadingIcon = Icons.Rounded.Link,
                                    onClick = { showCustomChannelUrlDialog = true }
                                )
                            }
                        )
                    )
                }
            }
        )

        addGroup(
            title = stringResource(CoreR.string.settings_group_network_downloads),
            icon = Icons.Rounded.Dns,
            items = buildList {
                add(
                    SettingScreenItem(
                        title = stringResource(CoreR.string.settings_doh_title),
                        subtitle = stringResource(CoreR.string.settings_doh_description),
                        content = {
                            MagiskSettingsSwitchItem(
                                title = stringResource(CoreR.string.settings_doh_title),
                                subtitle = stringResource(CoreR.string.settings_doh_description),
                                checked = state.doh,
                                onCheckedChange = viewModel::setDoH,
                                leadingIcon = Icons.Rounded.Lock
                            )
                        }
                    )
                )
                add(
                    SettingScreenItem(
                        title = stringResource(CoreR.string.settings_download_path_title),
                        subtitle = downloadPathSummary,
                        content = {
                            MagiskSettingsListItem(
                                title = stringResource(CoreR.string.settings_download_path_title),
                                subtitle = downloadPathSummary,
                                leadingIcon = Icons.Rounded.Download,
                                onClick = { showDownloadPathDialog = true })
                        }
                    )
                )
                add(
                    SettingScreenItem(
                        title = stringResource(CoreR.string.settings_random_name_title),
                        subtitle = stringResource(CoreR.string.settings_random_name_description),
                        content = {
                            MagiskSettingsSwitchItem(
                                title = stringResource(CoreR.string.settings_random_name_title),
                                subtitle = stringResource(CoreR.string.settings_random_name_description),
                                checked = state.randName,
                                onCheckedChange = viewModel::setRandName,
                                leadingIcon = Icons.Rounded.Shuffle
                            )
                        }
                    )
                )
            }
        )

        if (state.showMagisk) {
            addGroup(
                title = stringResource(CoreR.string.settings_group_magisk_features),
                icon = Icons.Rounded.Security,
                items = buildList {
                    if (state.showMagiskAdvanced) {
                    add(
                        SettingScreenItem(
                            title = "Zygisk",
                            subtitle = stringResource(CoreR.string.settings_zygisk_summary),
                            content = {
                                MagiskSettingsSwitchItem(
                                    title = "Zygisk",
                                    checked = state.zygisk,
                                    onCheckedChange = viewModel::setZygisk,
                                    leadingIcon = Icons.Rounded.Bolt,
                                    subtitle = stringResource(CoreR.string.settings_zygisk_summary)
                                )
                            }
                        )
                    )
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.settings_denylist_title),
                            subtitle = stringResource(CoreR.string.settings_denylist_summary),
                            content = {
                                MagiskSettingsSwitchItem(
                                    title = stringResource(CoreR.string.settings_denylist_title),
                                    checked = state.denyList,
                                    onCheckedChange = viewModel::setDenyList,
                                    leadingIcon = Icons.Rounded.Block,
                                    subtitle = stringResource(CoreR.string.settings_denylist_summary)
                                )
                            }
                        )
                    )
                    if (state.showDenyListConfig) {
                        add(
                            SettingScreenItem(
                                title = stringResource(CoreR.string.settings_denylist_config_title),
                                subtitle = stringResource(CoreR.string.settings_denylist_config_summary),
                                content = {
                                    MagiskSettingsListItem(
                                        title = stringResource(CoreR.string.settings_denylist_config_title),
                                        subtitle = stringResource(CoreR.string.settings_denylist_config_summary),
                                        leadingIcon = Icons.AutoMirrored.Rounded.Rule,
                                        onClick = { onNavigate(AppRoute.DenyList) })
                                }
                            )
                        )
                    }
                    }
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.settings_hosts_title),
                            subtitle = stringResource(CoreR.string.settings_hosts_summary),
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.settings_hosts_title),
                                    subtitle = stringResource(CoreR.string.settings_hosts_summary),
                                    leadingIcon = Icons.Rounded.Dns,
                                    onClick = viewModel::createSystemlessHosts
                                )
                            }
                        )
                    )
                }
            )
        }

        if (state.showSuperuser) {
            addGroup(
                title = stringResource(CoreR.string.settings_group_superuser_policy),
                icon = Icons.Rounded.SupervisorAccount,
                items = buildList {
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.superuser_access),
                            subtitle = state.accessModeName,
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.superuser_access),
                                    subtitle = state.accessModeName,
                                    leadingIcon = Icons.Rounded.SupervisorAccount,
                                    onClick = { activePicker = PickerType.SU_ACCESS })
                            }
                        )
                    )
                    if (state.multiuserModeEnabled) {
                        add(
                            SettingScreenItem(
                                title = stringResource(CoreR.string.multiuser_mode),
                                subtitle = state.multiuserModeName,
                                content = {
                                    MagiskSettingsListItem(
                                        title = stringResource(CoreR.string.multiuser_mode),
                                        subtitle = state.multiuserModeName,
                                        leadingIcon = Icons.Rounded.People,
                                        onClick = { activePicker = PickerType.MULTIUSER })
                                }
                            )
                        )
                    }
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.mount_namespace_mode),
                            subtitle = state.mountNamespaceName,
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.mount_namespace_mode),
                                    subtitle = state.mountNamespaceName,
                                    leadingIcon = Icons.Rounded.AccountTree,
                                    onClick = { activePicker = PickerType.NAMESPACE })
                            }
                        )
                    )
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.auto_response),
                            subtitle = state.autoResponseName,
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.auto_response),
                                    subtitle = state.autoResponseName,
                                    leadingIcon = Icons.AutoMirrored.Rounded.Reply,
                                    onClick = { activePicker = PickerType.AUTO_RESPONSE })
                            }
                        )
                    )
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.request_timeout),
                            subtitle = state.requestTimeoutName,
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.request_timeout),
                                    subtitle = state.requestTimeoutName,
                                    leadingIcon = Icons.Rounded.HourglassEmpty,
                                    onClick = { activePicker = PickerType.TIMEOUT })
                            }
                        )
                    )
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.superuser_notification),
                            subtitle = state.suNotificationName,
                            content = {
                                MagiskSettingsListItem(
                                    title = stringResource(CoreR.string.superuser_notification),
                                    subtitle = state.suNotificationName,
                                    leadingIcon = Icons.Rounded.Notifications,
                                    onClick = { activePicker = PickerType.NOTIFICATION })
                            }
                        )
                    )
                }
            )

            addGroup(
                title = stringResource(CoreR.string.settings_group_superuser_security),
                icon = Icons.Rounded.AdminPanelSettings,
                items = buildList {
                    if (state.showReauthenticate) {
                    add(
                        SettingScreenItem(
                            title = stringResource(CoreR.string.settings_su_reauth_title),
                            subtitle = stringResource(CoreR.string.settings_su_reauth_summary),
                            content = {
                                MagiskSettingsSwitchItem(
                                    title = stringResource(CoreR.string.settings_su_reauth_title),
                                    subtitle = stringResource(CoreR.string.settings_su_reauth_summary),
                                    checked = state.suReAuth,
                                    onCheckedChange = viewModel::setSuReAuth,
                                    leadingIcon = Icons.Rounded.Refresh
                                )
                            }
                        )
                    )
                    }
                    if (state.deviceSecure) {
                        add(
                            SettingScreenItem(
                                title = stringResource(CoreR.string.settings_su_auth_title),
                                subtitle = stringResource(CoreR.string.settings_su_auth_summary),
                                content = {
                                    MagiskSettingsSwitchItem(
                                        title = stringResource(CoreR.string.settings_su_auth_title),
                                        subtitle = stringResource(CoreR.string.settings_su_auth_summary),
                                        checked = state.suAuth,
                                        onCheckedChange = { checked ->
                                            withAuthentication {
                                                viewModel.setSuAuth(checked)
                                            }
                                        },
                                        leadingIcon = Icons.Rounded.Fingerprint
                                    )
                                }
                            )
                        )
                    }
                    if (!state.hideTapjackOnSPlus) {
                        add(
                            SettingScreenItem(
                                title = stringResource(CoreR.string.settings_su_tapjack_title),
                                subtitle = stringResource(CoreR.string.settings_su_tapjack_summary),
                                content = {
                                    MagiskSettingsSwitchItem(
                                        title = stringResource(CoreR.string.settings_su_tapjack_title),
                                        subtitle = stringResource(CoreR.string.settings_su_tapjack_summary),
                                        checked = state.suTapjack,
                                        onCheckedChange = viewModel::setSuTapjack,
                                        leadingIcon = Icons.Rounded.Security
                                    )
                                }
                            )
                        )
                    }
                    if (state.showRestrict) {
                        add(
                            SettingScreenItem(
                                title = stringResource(CoreR.string.settings_su_restrict_title),
                                subtitle = stringResource(CoreR.string.settings_su_restrict_summary),
                                content = {
                                    MagiskSettingsSwitchItem(
                                        title = stringResource(CoreR.string.settings_su_restrict_title),
                                        subtitle = stringResource(CoreR.string.settings_su_restrict_summary),
                                        checked = state.suRestrict,
                                        onCheckedChange = viewModel::setSuRestrict,
                                        leadingIcon = Icons.Rounded.AdminPanelSettings
                                    )
                                }
                            )
                        )
                    }
                }
            )
        }
    }

    val filteredGroups = groups.mapNotNull { group ->
        val matchingItems = if (state.settingsSearchQuery.isBlank()) {
            group.items
        } else {
            val normalizedQuery = state.settingsSearchQuery.trim().lowercase(Locale.ROOT)
            group.items.filter { item ->
                item.title.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                        item.subtitle.lowercase(Locale.ROOT).contains(normalizedQuery)
            }
        }
        if (matchingItems.isNotEmpty()) {
            SettingScreenGroup(
                title = group.title,
                icon = group.icon,
                items = matchingItems
            )
        } else {
            null
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MagiskAnimatedSearchField(
            visible = state.settingsSearchVisible,
            value = state.settingsSearchQuery,
            onValueChange = viewModel::setSettingsSearchQuery,
            placeholder = stringResource(CoreR.string.hide_search)
        )

        MagiskLazyContent(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 12.dp, bottom = 160.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (filteredGroups.isEmpty()) {
                item {
                    MagiskEmptyState(
                        title = stringResource(CoreR.string.superuser_policy_none),
                        icon = Icons.Rounded.Search,
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
            } else {
                items(filteredGroups.size, key = { index -> filteredGroups[index].title }) { index ->
                    val group = filteredGroups[index]
                    MagiskSettingsGroup(
                        title = group.title,
                        icon = group.icon,
                        items = group.items.map { item ->
                            { item.content() }
                        }
                    )
                }
            }
        }
    }
}

class SettingScreenItem(
    val title: String,
    val subtitle: String = "",
    val content: @Composable () -> Unit
)

class SettingScreenGroup(
    val title: String,
    val icon: ImageVector,
    val items: List<SettingScreenItem>
)

@Composable
fun SettingsTopBarActions(
    searchVisible: Boolean,
    onToggleSearch: () -> Unit
) {
    MagiskSearchActionButton(
        searchVisible = searchVisible,
        onToggleSearch = onToggleSearch
    )
}
