package com.topjohnwu.magisk.ui.settings

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UIActivity
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.isRunningAsStub
import com.topjohnwu.magisk.core.tasks.AppMigration
import com.topjohnwu.magisk.core.utils.LocaleSetting
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.core.utils.RootUtils
import com.topjohnwu.magisk.ui.POST_NOTIFICATIONS_PERMISSION
import com.topjohnwu.magisk.ui.RefreshOnResume
import com.topjohnwu.magisk.ui.component.ExpressiveSection
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogConfirmButton
import com.topjohnwu.magisk.ui.component.MagiskDialogDismissButton
import com.topjohnwu.magisk.ui.component.MagiskDialogOption
import com.topjohnwu.magisk.ui.component.MagiskSnackbarHost
import com.topjohnwu.magisk.ui.component.MagiskUiDefaults
import com.topjohnwu.magisk.ui.theme.Theme
import com.topjohnwu.magisk.view.Shortcuts
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun SettingsScreen(
    onOpenDenyList: () -> Unit = {},
    onOpenTheme: () -> Unit = {},
    viewModel: SettingsComposeViewModel = viewModel(factory = SettingsComposeViewModel.Factory)
) {
    val context = LocalContext.current
    val activity = context as? UIActivity<*>
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    var selector by remember { mutableStateOf<SelectorSpec?>(null) }
    var input by remember { mutableStateOf<InputSpec?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    RefreshOnResume { viewModel.refreshState() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = MagiskUiDefaults.screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.ListItemSpacing)
        ) {
            item {
                SettingsExpressiveHeader()
            }

            item {
                OrganicSettingsSection(
                    stringResource(id = CoreR.string.settings_customization),
                    Icons.Rounded.Palette
                ) {
                    ExpressiveSettingItem(
                        title = stringResource(id = CoreR.string.section_theme),
                        subtitle = "${darkModeLabel(state.darkThemeMode)} | ${state.themeName}",
                        icon = Icons.Rounded.Palette,
                        onClick = onOpenTheme
                    )

                    ExpressiveSettingItem(
                        title = stringResource(id = CoreR.string.language),
                        subtitle = if (state.useLocaleManager) state.languageSystemName else state.languageName,
                        icon = Icons.Rounded.Language,
                        onClick = {
                            if (state.useLocaleManager) {
                                runCatching { context.startActivity(LocaleSetting.localeSettingsIntent) }
                            } else {
                                selector = SelectorSpec(
                                    title = AppContext.getString(CoreR.string.language),
                                    icon = Icons.Rounded.Language,
                                    options = LocaleSetting.available.names.toList(),
                                    selectedIndex = state.languageIndex,
                                    onSelect = viewModel::setLanguageByIndex
                                )
                            }
                        }
                    )

                    if (state.canAddShortcut) {
                        ExpressiveSettingItem(
                            title = stringResource(id = CoreR.string.add_shortcut_title),
                            subtitle = stringResource(id = CoreR.string.setting_add_shortcut_summary),
                            icon = Icons.Rounded.AddLink,
                            onClick = viewModel::addShortcut
                        )
                    }
                }
            }

            item {
                OrganicSettingsSection(
                    stringResource(id = CoreR.string.home_app_title),
                    Icons.Rounded.Settings
                ) {
                    ExpressiveSettingItem(
                        title = stringResource(id = CoreR.string.settings_update_channel_title),
                        subtitle = state.updateChannelName,
                        icon = Icons.Rounded.Update,
                        onClick = {
                            selector = SelectorSpec(
                                title = AppContext.getString(CoreR.string.settings_update_channel_title),
                                icon = Icons.Rounded.Update,
                                options = context.resources.getStringArray(CoreR.array.update_channel)
                                    .toList(),
                                selectedIndex = state.updateChannel.coerceAtLeast(0),
                                onSelect = { index ->
                                    viewModel.setUpdateChannel(index)
                                    if (index == Config.Value.CUSTOM_CHANNEL && viewModel.state.customChannelUrl.isBlank()) {
                                        input = InputSpec(
                                            title = AppContext.getString(CoreR.string.settings_update_custom),
                                            initialValue = "",
                                            onConfirm = viewModel::setCustomChannelUrl
                                        )
                                    }
                                }
                            )
                        }
                    )
                    ExpressiveSettingItem(
                        title = stringResource(id = CoreR.string.settings_update_custom),
                        subtitle = state.customChannelUrl.ifBlank { stringResource(id = CoreR.string.settings_update_custom_msg) },
                        enabled = state.isCustomChannel,
                        icon = Icons.Rounded.Link,
                        onClick = {
                            input = InputSpec(
                                title = AppContext.getString(CoreR.string.settings_update_custom),
                                initialValue = state.customChannelUrl,
                                onConfirm = viewModel::setCustomChannelUrl
                            )
                        }
                    )
                    ExpressiveToggleItem(
                        title = stringResource(id = CoreR.string.settings_doh_title),
                        subtitle = stringResource(id = CoreR.string.settings_doh_description),
                        checked = state.doh,
                        icon = Icons.Rounded.Public,
                        onChecked = viewModel::setDoH
                    )
                    ExpressiveToggleItem(
                        title = stringResource(id = CoreR.string.settings_check_update_title),
                        subtitle = stringResource(id = CoreR.string.settings_check_update_summary),
                        checked = state.checkUpdate,
                        icon = Icons.Rounded.NotificationAdd,
                        onChecked = { checked ->
                            activity?.withPermission(POST_NOTIFICATIONS_PERMISSION) { granted ->
                                if (granted) viewModel.setCheckUpdate(checked)
                                else viewModel.setMessageRes(CoreR.string.post_notifications_denied)
                            } ?: viewModel.setMessageRes(CoreR.string.app_not_found)
                        }
                    )
                    ExpressiveSettingItem(
                        title = stringResource(id = CoreR.string.settings_download_path_title),
                        subtitle = state.downloadDirPath.ifBlank { "-" },
                        icon = Icons.Rounded.FolderOpen,
                        onClick = {
                            activity?.withPermission(WRITE_EXTERNAL_STORAGE) { granted ->
                                if (granted) {
                                    input = InputSpec(
                                        title = AppContext.getString(CoreR.string.settings_download_path_title),
                                        initialValue = state.downloadDir,
                                        onConfirm = viewModel::setDownloadDir
                                    )
                                } else {
                                    viewModel.setMessageRes(CoreR.string.external_rw_permission_denied)
                                }
                            } ?: viewModel.setMessageRes(CoreR.string.app_not_found)
                        }
                    )
                    ExpressiveToggleItem(
                        title = stringResource(id = CoreR.string.settings_random_name_title),
                        subtitle = stringResource(id = CoreR.string.settings_random_name_description),
                        checked = state.randName,
                        icon = Icons.Rounded.Shuffle,
                        onChecked = viewModel::setRandName
                    )
                    if (state.canMigrateApp) {
                        ExpressiveSettingItem(
                            title = stringResource(id = if (state.isHiddenApp) CoreR.string.settings_restore_app_title else CoreR.string.settings_hide_app_title),
                            subtitle = stringResource(id = if (state.isHiddenApp) CoreR.string.settings_restore_app_summary else CoreR.string.settings_hide_app_summary),
                            icon = if (state.isHiddenApp) Icons.Rounded.RestorePage else Icons.Rounded.Masks,
                            onClick = {
                                if (state.isHiddenApp) confirmRestore = true
                                else {
                                    input = InputSpec(
                                        title = AppContext.getString(CoreR.string.settings_hide_app_title),
                                        initialValue = AppContext.getString(CoreR.string.settings),
                                        onConfirm = { label -> viewModel.hideApp(activity, label) }
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (state.showMagisk) {
                item {
                    OrganicSettingsSection(
                        stringResource(id = CoreR.string.magisk),
                        Icons.Rounded.Security
                    ) {
                        ExpressiveSettingItem(
                            title = stringResource(id = CoreR.string.settings_hosts_title),
                            subtitle = stringResource(id = CoreR.string.settings_hosts_summary),
                            icon = Icons.Rounded.Dns,
                            onClick = viewModel::createSystemlessHosts
                        )
                        if (state.showMagiskAdvanced) {
                            ExpressiveToggleItem(
                                title = stringResource(id = CoreR.string.zygisk),
                                subtitle = if (state.zygiskMismatch) stringResource(id = CoreR.string.reboot_apply_change) else stringResource(
                                    id = CoreR.string.settings_zygisk_summary
                                ),
                                checked = state.zygisk,
                                icon = Icons.Rounded.Bolt,
                                onChecked = viewModel::setZygisk
                            )
                            ExpressiveToggleItem(
                                title = stringResource(id = CoreR.string.settings_denylist_title),
                                subtitle = stringResource(id = CoreR.string.settings_denylist_summary),
                                checked = state.denyList,
                                icon = Icons.Rounded.Security,
                                onChecked = viewModel::setDenyList
                            )
                            if (state.showDenyListConfig) {
                                ExpressiveSettingItem(
                                    title = stringResource(id = CoreR.string.settings_denylist_config_title),
                                    subtitle = stringResource(id = CoreR.string.settings_denylist_config_summary),
                                    icon = Icons.Rounded.Block,
                                    onClick = onOpenDenyList
                                )
                            }
                        }
                    }
                }
            }

            if (state.showSuperuser) {
                item {
                    OrganicSettingsSection(
                        stringResource(id = CoreR.string.superuser),
                        Icons.Rounded.Shield
                    ) {
                        if (!state.hideTapjackOnSPlus) {
                            ExpressiveToggleItem(
                                title = stringResource(id = CoreR.string.settings_su_tapjack_title),
                                subtitle = stringResource(id = CoreR.string.settings_su_tapjack_summary),
                                checked = state.suTapjack,
                                icon = Icons.Rounded.TouchApp,
                                onChecked = viewModel::setSuTapjack
                            )
                        }
                        ExpressiveToggleItem(
                            title = stringResource(id = CoreR.string.settings_su_auth_title),
                            subtitle = if (state.deviceSecure) stringResource(id = CoreR.string.settings_su_auth_summary) else stringResource(
                                id = CoreR.string.settings_su_auth_insecure
                            ),
                            checked = state.suAuth,
                            enabled = state.deviceSecure,
                            icon = Icons.Rounded.Fingerprint,
                            onChecked = { checked ->
                                activity?.withAuthentication { ok ->
                                    if (ok) viewModel.setSuAuth(
                                        checked
                                    )
                                }
                                    ?: viewModel.setMessageRes(CoreR.string.app_not_found)
                            }
                        )
                        ExpressiveSettingItem(
                            title = stringResource(id = CoreR.string.superuser_access),
                            subtitle = state.accessModeName,
                            icon = Icons.Rounded.Key,
                            onClick = {
                                selector = SelectorSpec(
                                    title = AppContext.getString(CoreR.string.superuser_access),
                                    icon = Icons.Rounded.Key,
                                    options = context.resources.getStringArray(CoreR.array.su_access)
                                        .toList(),
                                    selectedIndex = state.rootMode.coerceAtLeast(0),
                                    onSelect = viewModel::setRootMode
                                )
                            }
                        )
                        ExpressiveSettingItem(
                            title = stringResource(id = CoreR.string.multiuser_mode),
                            subtitle = state.multiuserSummary,
                            enabled = state.multiuserModeEnabled,
                            icon = Icons.Rounded.People,
                            onClick = {
                                selector = SelectorSpec(
                                    title = AppContext.getString(CoreR.string.multiuser_mode),
                                    icon = Icons.Rounded.People,
                                    options = context.resources.getStringArray(CoreR.array.multiuser_mode)
                                        .toList(),
                                    selectedIndex = state.suMultiuserMode.coerceAtLeast(0),
                                    onSelect = viewModel::setSuMultiuserMode
                                )
                            }
                        )
                        ExpressiveSettingItem(
                            title = stringResource(id = CoreR.string.mount_namespace_mode),
                            subtitle = state.mountNamespaceSummary,
                            icon = Icons.Rounded.Layers,
                            onClick = {
                                selector = SelectorSpec(
                                    title = AppContext.getString(CoreR.string.mount_namespace_mode),
                                    icon = Icons.Rounded.Layers,
                                    options = context.resources.getStringArray(CoreR.array.namespace)
                                        .toList(),
                                    selectedIndex = state.suMntNamespaceMode.coerceAtLeast(0),
                                    onSelect = viewModel::setSuMntNamespaceMode
                                )
                            }
                        )
                        ExpressiveSettingItem(
                            title = stringResource(id = CoreR.string.auto_response),
                            subtitle = state.autoResponseName,
                            icon = Icons.AutoMirrored.Rounded.Reply,
                            onClick = {
                                val showSelector = {
                                    selector = SelectorSpec(
                                        title = AppContext.getString(CoreR.string.auto_response),
                                        icon = Icons.AutoMirrored.Rounded.Reply,
                                        options = context.resources.getStringArray(CoreR.array.auto_response)
                                            .toList(),
                                        selectedIndex = state.suAutoResponse.coerceAtLeast(0),
                                        onSelect = viewModel::setSuAutoResponse
                                    )
                                }
                                if (state.suAuth) activity?.withAuthentication { ok -> if (ok) showSelector() }
                                else showSelector()
                            }
                        )
                        ExpressiveSettingItem(
                            title = stringResource(id = CoreR.string.request_timeout),
                            subtitle = state.requestTimeoutName,
                            icon = Icons.Rounded.Timer,
                            onClick = {
                                selector = SelectorSpec(
                                    title = AppContext.getString(CoreR.string.request_timeout),
                                    icon = Icons.Rounded.Timer,
                                    options = context.resources.getStringArray(CoreR.array.request_timeout)
                                        .toList(),
                                    selectedIndex = state.suTimeoutIndex.coerceIn(
                                        0,
                                        SU_TIMEOUT_VALUES.lastIndex
                                    ),
                                    onSelect = viewModel::setSuTimeoutIndex
                                )
                            }
                        )
                        ExpressiveSettingItem(
                            title = stringResource(id = CoreR.string.superuser_notification),
                            subtitle = state.suNotificationName,
                            icon = Icons.Rounded.NotificationsActive,
                            onClick = {
                                selector = SelectorSpec(
                                    title = AppContext.getString(CoreR.string.superuser_notification),
                                    icon = Icons.Rounded.NotificationsActive,
                                    options = context.resources.getStringArray(CoreR.array.su_notification)
                                        .toList(),
                                    selectedIndex = state.suNotification.coerceAtLeast(0),
                                    onSelect = viewModel::setSuNotification
                                )
                            }
                        )
                        if (state.showReauthenticate) {
                            ExpressiveToggleItem(
                                title = stringResource(id = CoreR.string.settings_su_reauth_title),
                                subtitle = stringResource(id = CoreR.string.settings_su_reauth_summary),
                                checked = state.suReAuth,
                                icon = Icons.Rounded.VerifiedUser,
                                onChecked = viewModel::setSuReAuth
                            )
                        }
                        if (state.showRestrict) {
                            ExpressiveToggleItem(
                                title = stringResource(id = CoreR.string.settings_su_restrict_title),
                                subtitle = stringResource(id = CoreR.string.settings_su_restrict_summary),
                                checked = state.suRestrict,
                                icon = Icons.Rounded.Lock,
                                onChecked = viewModel::setSuRestrict
                            )
                        }
                    }
                }
            }
        }

        MagiskSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MagiskUiDefaults.SnackbarBottomPadding)
        )
    }

    selector?.let { spec ->
        MagiskDialog(
            onDismissRequest = { selector = null },
            title = spec.title,
            icon = spec.icon,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    spec.options.forEachIndexed { index, label ->
                        MagiskDialogOption(
                            title = label,
                            selected = index == spec.selectedIndex,
                            showRadio = true,
                            onClick = {
                                spec.onSelect(index)
                                selector = null
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    input?.let { spec ->
        var value by remember(spec.initialValue) { mutableStateOf(spec.initialValue) }
        MagiskDialog(
            onDismissRequest = { input = null },
            title = spec.title,
            icon = Icons.Rounded.Edit,
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MagiskUiDefaults.SmallShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            },
            confirmButton = {
                MagiskDialogConfirmButton(
                    onClick = {
                        spec.onConfirm(value.trim())
                        input = null
                    }
                )
            },
            dismissButton = {
                MagiskDialogDismissButton(onClick = { input = null })
            }
        )
    }

    if (confirmRestore) {
        MagiskDialog(
            onDismissRequest = { confirmRestore = false },
            title = stringResource(id = CoreR.string.settings_restore_app_title),
            icon = Icons.Rounded.Warning,
            iconTint = MaterialTheme.colorScheme.error,
            text = { Text(stringResource(id = CoreR.string.restore_app_confirmation)) },
            confirmButton = {
                MagiskDialogConfirmButton(
                    onClick = {
                        confirmRestore = false
                        viewModel.restoreApp(activity)
                    },
                    destructive = true
                )
            },
            dismissButton = {
                MagiskDialogDismissButton(onClick = { confirmRestore = false })
            }
        )
    }
}

@Composable
private fun SettingsExpressiveHeader() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MagiskUiDefaults.HeroShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Configura l'ambiente e l'app",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Settings, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganicSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ExpressiveSection(
        title = title,
        icon = icon,
        iconContainerSize = 38.dp,
        iconPadding = 9.dp,
        iconShape = MagiskUiDefaults.SmallShape,
        iconContainerAlpha = 0.85f,
        cardShape = MagiskUiDefaults.ExtraLargeShape,
        content = content
    )
}

@Composable
private fun ExpressiveSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.4f
                )
            )
        }
    }
}

@Composable
private fun ExpressiveToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onChecked: (Boolean) -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChecked(!checked) }) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                enabled = enabled,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                } else null)
        }
    }
}

private fun darkModeLabel(mode: Int): String = when (mode) {
    AppCompatDelegate.MODE_NIGHT_NO -> AppContext.getString(CoreR.string.settings_dark_mode_light)
    AppCompatDelegate.MODE_NIGHT_YES -> AppContext.getString(CoreR.string.settings_dark_mode_dark)
    Config.Value.DARK_THEME_AMOLED -> AppContext.getString(CoreR.string.settings_dark_mode_amoled)
    else -> AppContext.getString(CoreR.string.settings_dark_mode_system)
}

data class SelectorSpec(
    val title: String,
    val icon: ImageVector,
    val options: List<String>,
    val selectedIndex: Int,
    val onSelect: (Int) -> Unit
)

data class InputSpec(val title: String, val initialValue: String, val onConfirm: (String) -> Unit)

data class SettingsUiState(
    val darkThemeMode: Int = Config.darkTheme,
    val themeOrdinal: Int = Config.themeOrdinal,
    val selectedThemeIndex: Int = Theme.values().indexOf(Theme.selected).coerceAtLeast(0),
    val themeName: String = Theme.selected.themeName,
    val useLocaleManager: Boolean = LocaleSetting.useLocaleManager,
    val languageSystemName: String = LocaleSetting.instance.appLocale?.let { it.getDisplayName(it) }
        ?: AppContext.getString(CoreR.string.system_default),
    val languageIndex: Int = LocaleSetting.available.tags.indexOf(Config.locale)
        .let { if (it < 0) 0 else it },
    val languageName: String = LocaleSetting.available.names.getOrElse(
        LocaleSetting.available.tags.indexOf(
            Config.locale
        ).let { if (it < 0) 0 else it }) { AppContext.getString(CoreR.string.system_default) },
    val canAddShortcut: Boolean = isRunningAsStub && ShortcutManagerCompat.isRequestPinShortcutSupported(
        AppContext
    ),
    val canMigrateApp: Boolean = Info.env.isActive && Const.USER_ID == 0,
    val isHiddenApp: Boolean = AppContext.packageName != BuildConfig.APP_PACKAGE_NAME,
    val checkUpdate: Boolean = Config.checkUpdate,
    val updateChannel: Int = Config.updateChannel,
    val isCustomChannel: Boolean = Config.updateChannel == Config.Value.CUSTOM_CHANNEL,
    val updateChannelName: String = AppContext.resources.getStringArray(CoreR.array.update_channel)
        .getOrElse(Config.updateChannel) { "-" },
    val customChannelUrl: String = Config.customChannelUrl,
    val doh: Boolean = Config.doh,
    val downloadDir: String = Config.downloadDir,
    val downloadDirPath: String = MediaStoreUtils.fullPath(Config.downloadDir),
    val randName: Boolean = Config.randName,
    val zygisk: Boolean = Config.zygisk,
    val zygiskMismatch: Boolean = Config.zygisk != Info.isZygiskEnabled,
    val denyList: Boolean = Config.denyList,
    val showMagisk: Boolean = Info.env.isActive,
    val showMagiskAdvanced: Boolean = Info.env.isActive && Const.Version.atLeast_24_0(),
    val showDenyListConfig: Boolean = Const.Version.atLeast_24_0(),
    val showSuperuser: Boolean = Info.showSuperUser,
    val deviceSecure: Boolean = Info.isDeviceSecure,
    val suTapjack: Boolean = Config.suTapjack,
    val suAuth: Boolean = Config.suAuth,
    val hideTapjackOnSPlus: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val rootMode: Int = Config.rootMode,
    val accessModeName: String = AppContext.resources.getStringArray(CoreR.array.su_access)
        .getOrElse(Config.rootMode) { "-" },
    val suMultiuserMode: Int = Config.suMultiuserMode,
    val multiuserModeName: String = AppContext.resources.getStringArray(CoreR.array.multiuser_mode)
        .getOrElse(Config.suMultiuserMode) { "-" },
    val multiuserModeEnabled: Boolean = Const.USER_ID == 0,
    val multiuserSummary: String = AppContext.resources.getStringArray(CoreR.array.multiuser_summary)
        .getOrElse(Config.suMultiuserMode) { "-" },
    val suMntNamespaceMode: Int = Config.suMntNamespaceMode,
    val mountNamespaceName: String = AppContext.resources.getStringArray(CoreR.array.namespace)
        .getOrElse(Config.suMntNamespaceMode) { "-" },
    val mountNamespaceSummary: String = AppContext.resources.getStringArray(CoreR.array.namespace_summary)
        .getOrElse(Config.suMntNamespaceMode) { "-" },
    val suAutoResponse: Int = Config.suAutoResponse,
    val autoResponseName: String = AppContext.resources.getStringArray(CoreR.array.auto_response)
        .getOrElse(Config.suAutoResponse) { "-" },
    val suTimeoutIndex: Int = SU_TIMEOUT_VALUES.indexOf(Config.suDefaultTimeout)
        .let { if (it < 0) 0 else it },
    val requestTimeoutName: String = AppContext.resources.getStringArray(CoreR.array.request_timeout)
        .getOrElse(
            SU_TIMEOUT_VALUES.indexOf(Config.suDefaultTimeout)
                .let { if (it < 0) 0 else it }) { "-" },
    val suNotification: Int = Config.suNotification,
    val suNotificationName: String = AppContext.resources.getStringArray(CoreR.array.su_notification)
        .getOrElse(Config.suNotification) { "-" },
    val suReAuth: Boolean = Config.suReAuth,
    val showReauthenticate: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O,
    val suRestrict: Boolean = Config.suRestrict,
    val showRestrict: Boolean = Const.Version.atLeast_30_1()
)

class SettingsComposeViewModel : ViewModel() {
    var state by mutableStateOf(snapshotState())
        private set
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private var refreshJob: Job? = null

    fun refreshState() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { state = snapshotState() }
    }

    fun setDarkMode(mode: Int) {
        Config.darkTheme = mode; state = snapshotState()
    }

    fun setThemeOrdinal(index: Int) {
        val theme = Theme.values().getOrNull(index) ?: Theme.Default; Config.themeOrdinal =
            if (theme == Theme.Default) -1 else theme.ordinal; state = snapshotState()
    }

    fun setLanguageByIndex(index: Int) {
        if (state.useLocaleManager) return;
        val tags = LocaleSetting.available.tags; if (tags.isEmpty()) return;
        val safe = index.coerceIn(0, tags.lastIndex); Config.locale = tags[safe]; state =
            snapshotState()
    }

    fun addShortcut() {
        runCatching {
            Shortcuts.addHomeIcon(AppContext); state = snapshotState()
        }.onFailure { _messages.tryEmit(AppContext.getString(CoreR.string.failure)) }
    }

    fun hideApp(activity: UIActivity<*>?, label: String) {
        val safeLabel = label.trim()
        if (activity == null || safeLabel.isBlank() || safeLabel.length > AppMigration.MAX_LABEL_LENGTH) {
            _messages.tryEmit(AppContext.getString(CoreR.string.failure))
            return
        }
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                AppMigration.patchAndHide(activity, safeLabel)
            }
            if (!success) {
                _messages.emit(AppContext.getString(CoreR.string.failure))
            }
            state = snapshotState()
        }
    }

    fun restoreApp(activity: UIActivity<*>?) {
        if (activity == null) {
            _messages.tryEmit(AppContext.getString(CoreR.string.failure))
            return
        }
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                AppMigration.restoreApp(activity)
            }
            if (!success) {
                _messages.emit(AppContext.getString(CoreR.string.failure))
            }
            state = snapshotState()
        }
    }

    fun setCheckUpdate(v: Boolean) {
        Config.checkUpdate = v; state = snapshotState()
    }

    fun setUpdateChannel(c: Int) {
        Config.updateChannel = c; Info.resetUpdate(); state = snapshotState()
    }

    fun setCustomChannelUrl(u: String) {
        Config.customChannelUrl = u; Info.resetUpdate(); state = snapshotState()
    }

    fun setDoH(v: Boolean) {
        Config.doh = v; state = snapshotState()
    }

    fun setDownloadDir(v: String) {
        Config.downloadDir = v; state = snapshotState()
    }

    fun setRandName(v: Boolean) {
        Config.randName = v; state = snapshotState()
    }

    fun createSystemlessHosts() {
        viewModelScope.launch {
            val ok = RootUtils.addSystemlessHosts(); _messages.tryEmit(
            AppContext.getString(if (ok) CoreR.string.settings_hosts_toast else CoreR.string.failure)
        )
        }
    }

    fun setZygisk(v: Boolean) {
        Config.zygisk = v; state =
            snapshotState(); if (v != Info.isZygiskEnabled) _messages.tryEmit(
            AppContext.getString(
                CoreR.string.reboot_apply_change
            )
        )
    }

    fun setDenyList(v: Boolean) {
        viewModelScope.launch {
            val cmd = if (v) "enable" else "disable";
            val ok = withContext(Dispatchers.IO) {
                Shell.cmd("magisk --denylist $cmd").exec().isSuccess
            }; state = if (ok) {
            Config.denyList = v; snapshotState()
        } else {
            _messages.emit(AppContext.getString(CoreR.string.failure)); snapshotState()
        }
        }
    }

    fun setRootMode(v: Int) {
        Config.rootMode = v; state = snapshotState()
    }

    fun setSuMultiuserMode(v: Int) {
        Config.suMultiuserMode = v; state = snapshotState()
    }

    fun setSuMntNamespaceMode(v: Int) {
        Config.suMntNamespaceMode = v; state = snapshotState()
    }

    fun setSuAuth(v: Boolean) {
        Config.suAuth = v; state = snapshotState()
    }

    fun setSuAutoResponse(v: Int) {
        Config.suAutoResponse = v; state = snapshotState()
    }

    fun setSuTimeoutIndex(index: Int) {
        val safe = index.coerceIn(0, SU_TIMEOUT_VALUES.lastIndex); Config.suDefaultTimeout =
            SU_TIMEOUT_VALUES[safe]; state = snapshotState()
    }

    fun setSuNotification(v: Int) {
        Config.suNotification = v; state = snapshotState()
    }

    fun setSuReAuth(v: Boolean) {
        Config.suReAuth = v; state = snapshotState()
    }

    fun setSuTapjack(v: Boolean) {
        Config.suTapjack = v; state = snapshotState()
    }

    fun setSuRestrict(v: Boolean) {
        Config.suRestrict = v; state = snapshotState()
    }

    fun setMessageRes(res: Int) {
        _messages.tryEmit(AppContext.getString(res))
    }

    private fun snapshotState(): SettingsUiState {
        return SettingsUiState()
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return SettingsComposeViewModel() as T
            }
        }
    }
}

private val SU_TIMEOUT_VALUES = listOf(10, 15, 20, 30, 45, 60)
