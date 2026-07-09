package com.topjohnwu.magisk.viewmodel.settings

import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.isRunningAsStub
import com.topjohnwu.magisk.core.tasks.AppMigration
import com.topjohnwu.magisk.core.update.UpdateManager
import com.topjohnwu.magisk.core.utils.LocaleSetting
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.core.utils.RootUtils
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import com.topjohnwu.magisk.runtime.MagiskRuntimeState
import com.topjohnwu.magisk.ui.theme.MagiskThemeController
import com.topjohnwu.magisk.ui.theme.ThemeCustomColors
import com.topjohnwu.magisk.ui.theme.ThemeOption
import com.topjohnwu.magisk.view.Shortcuts
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.topjohnwu.magisk.core.R as CoreR

private val updateChannelArray by lazy { AppContext.resources.getStringArray(CoreR.array.update_channel) }
private val suAccessArray by lazy { AppContext.resources.getStringArray(CoreR.array.su_access) }
private val multiuserModeArray by lazy { AppContext.resources.getStringArray(CoreR.array.multiuser_mode) }
private val multiuserSummaryArray by lazy { AppContext.resources.getStringArray(CoreR.array.multiuser_summary) }
private val namespaceArray by lazy { AppContext.resources.getStringArray(CoreR.array.namespace) }
private val namespaceSummaryArray by lazy { AppContext.resources.getStringArray(CoreR.array.namespace_summary) }
private val autoResponseArray by lazy { AppContext.resources.getStringArray(CoreR.array.auto_response) }
private val requestTimeoutArray by lazy { AppContext.resources.getStringArray(CoreR.array.request_timeout) }
private val suNotificationArray by lazy { AppContext.resources.getStringArray(CoreR.array.su_notification) }

data class SettingsUiState(
    val runtime: MagiskRuntimeState = MagiskRuntimeEngine.snapshot(),
    val darkThemeMode: Int = Config.darkTheme,
    val bottomBarStyle: Int = Config.bottomBarStyle,
    val themeOrdinal: Int = Config.themeOrdinal,
    val selectedThemeIndex: Int = ThemeOption.displayOrder.indexOf(ThemeOption.selected)
        .coerceAtLeast(0),
    @param:StringRes val themeNameRes: Int = ThemeOption.selected.labelRes,
    val useLocaleManager: Boolean = LocaleSetting.useLocaleManager,
    val languageSystemName: String = LocaleSetting.instance.appLocale?.let { it.getDisplayName(it) }
        ?: AppContext.getString(CoreR.string.system_default),
    val languageIndex: Int = LocaleSetting.available.tags.indexOf(currentLanguageTag())
        .let { if (it < 0) 0 else it },
    val languageName: String = LocaleSetting.available.names.getOrElse(
        LocaleSetting.available.tags.indexOf(currentLanguageTag())
            .let { if (it < 0) 0 else it }) { AppContext.getString(CoreR.string.system_default) },
    val canAddShortcut: Boolean = isRunningAsStub && ShortcutManagerCompat.isRequestPinShortcutSupported(
        AppContext
    ),
    val canMigrateApp: Boolean = runtime.canMigrateApp,
    val isHiddenApp: Boolean = isHiddenMagiskApp(),
    val canRestoreApp: Boolean = canMigrateApp && isHiddenApp,
    val canHideApp: Boolean = canMigrateApp && !isHiddenApp,
    val checkUpdate: Boolean = Config.checkUpdate,
    val updateChannel: Int = Config.updateChannel.coerceIn(
        Config.Value.MBE_CHANNEL,
        Config.Value.CUSTOM_CHANNEL
    ),
    val updateChannelName: String = updateChannelArray
        .getOrElse(updateChannel) { "-" },
    val isCustomChannel: Boolean = updateChannel == Config.Value.CUSTOM_CHANNEL,
    val customChannelUrl: String = Config.customChannelUrl,
    val doh: Boolean = Config.doh,
    val downloadDir: String = Config.downloadDir,
    val downloadDirPath: String = MediaStoreUtils.fullPath(Config.downloadDir),
    val randName: Boolean = Config.randName,
    val zygisk: Boolean = Config.zygisk,
    val zygiskMismatch: Boolean = Config.zygisk != runtime.isZygiskEnabled,
    val denyList: Boolean = Config.denyList,
    val showMagisk: Boolean = runtime.canShowMagiskSettings,
    val showMagiskAdvanced: Boolean = runtime.canShowMagiskAdvancedSettings,
    val showDenyListConfig: Boolean = runtime.canShowDenyListConfig,
    val showSuperuser: Boolean = runtime.canShowSuperuser,
    val deviceSecure: Boolean = Info.isDeviceSecure,
    val suTapjack: Boolean = Config.suTapjack,
    val suAuth: Boolean = Config.suAuth,
    val hideTapjackOnSPlus: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val rootMode: Int = Config.rootMode,
    val accessModeName: String = suAccessArray
        .getOrElse(Config.rootMode) { "-" },
    val suMultiuserMode: Int = Config.suMultiuserMode,
    val multiuserModeName: String = multiuserModeArray
        .getOrElse(Config.suMultiuserMode) { "-" },
    val multiuserModeEnabled: Boolean = Const.USER_ID == 0,
    val multiuserSummary: String = multiuserSummaryArray
        .getOrElse(Config.suMultiuserMode) { "-" },
    val suMntNamespaceMode: Int = Config.suMntNamespaceMode,
    val mountNamespaceName: String = namespaceArray
        .getOrElse(Config.suMntNamespaceMode) { "-" },
    val mountNamespaceSummary: String = namespaceSummaryArray
        .getOrElse(Config.suMntNamespaceMode) { "-" },
    val suAutoResponse: Int = Config.suAutoResponse,
    val autoResponseName: String = autoResponseArray
        .getOrElse(Config.suAutoResponse) { "-" },
    val suTimeoutIndex: Int = SU_TIMEOUT_VALUES.indexOf(Config.suDefaultTimeout)
        .let { if (it < 0) 0 else it },
    val requestTimeoutName: String = requestTimeoutArray
        .getOrElse(
            SU_TIMEOUT_VALUES.indexOf(Config.suDefaultTimeout)
                .let { if (it < 0) 0 else it }) { "-" },
    val suNotification: Int = Config.suNotification,
    val suNotificationName: String = suNotificationArray
        .getOrElse(Config.suNotification) { "-" },
    val suReAuth: Boolean = Config.suReAuth,
    val showReauthenticate: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O,
    val suRestrict: Boolean = Config.suRestrict,
    val showRestrict: Boolean = Const.Version.atLeast_30_1(),
    val languageSearchQuery: String = "",
    val languageSearchVisible: Boolean = false,
    val settingsSearchQuery: String = "",
    val settingsSearchVisible: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    private var refreshJob: Job? = null

    fun refreshState() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { updateSnapshot() }
    }

    fun setDarkMode(mode: Int) {
        MagiskThemeController.setDarkMode(mode)
        updateSnapshot()
    }

    fun setThemeIndex(index: Int) {
        MagiskThemeController.setThemeIndex(index)
        updateSnapshot()
    }

    fun setCustomTheme(colors: ThemeCustomColors) {
        MagiskThemeController.setCustomColors(colors)
        updateSnapshot()
    }

    fun setBottomBarStyle(style: Int) {
        MagiskThemeController.setBottomBarStyle(style)
        updateSnapshot()
    }

    fun setLanguageByIndex(index: Int) {
        val tags = LocaleSetting.available.tags
        if (tags.isEmpty()) return
        Config.locale = tags[index.coerceIn(0, tags.lastIndex)]
        updateSnapshot()
    }

    fun addShortcut() {
        runCatching {
            Shortcuts.addHomeIcon(AppContext)
            updateSnapshot()
        }.onFailure {
            _messages.tryEmit(uiText(CoreR.string.failure))
        }
    }

    fun requestHideApp(label: String) {
        if (!_state.value.canHideApp) {
            _messages.tryEmit(uiText(CoreR.string.failure))
            return
        }
        val safeLabel = label.trim()
        if (safeLabel.isBlank() || safeLabel.length > AppMigration.MAX_LABEL_LENGTH) {
            _messages.tryEmit(uiText(CoreR.string.failure))
            return
        }
        _effects.tryEmit(UiEffect.RequestHideApp(safeLabel))
    }

    fun requestRestoreApp() {
        if (!_state.value.canRestoreApp) {
            _messages.tryEmit(uiText(CoreR.string.failure))
            return
        }
        _effects.tryEmit(UiEffect.RequestRestoreApp)
    }

    fun onAppMigrationResult(success: Boolean) {
        if (!success) {
            _messages.tryEmit(uiText(CoreR.string.failure))
        }
        updateSnapshot()
    }

    fun setCheckUpdate(value: Boolean) {
        Config.checkUpdate = value
        updateSnapshot()
    }

    fun setUpdateChannel(value: Int) {
        Config.updateChannel = value.coerceIn(Config.Value.MBE_CHANNEL, Config.Value.CUSTOM_CHANNEL)
        UpdateManager.resetAppUpdate()
        updateSnapshot()
    }

    fun setCustomChannelUrl(url: String) {
        Config.customChannelUrl = url.trim()
        UpdateManager.resetAppUpdate()
        updateSnapshot()
    }

    fun setDoH(value: Boolean) {
        Config.doh = value
        updateSnapshot()
    }

    fun setDownloadDir(value: String) {
        Config.downloadDir = value
        updateSnapshot()
    }

    fun setRandName(value: Boolean) {
        Config.randName = value
        updateSnapshot()
    }

    fun createSystemlessHosts() {
        viewModelScope.launch {
            val ok = RootUtils.addSystemlessHosts()
            _messages.tryEmit(uiText(if (ok) CoreR.string.settings_hosts_toast else CoreR.string.failure))
        }
    }

    fun setZygisk(value: Boolean) {
        Config.zygisk = value
        updateSnapshot()
        if (value != MagiskRuntimeEngine.snapshot().isZygiskEnabled) {
            _messages.tryEmit(uiText(CoreR.string.reboot_apply_change))
        }
    }

    fun setDenyList(value: Boolean) {
        viewModelScope.launch {
            val cmd = if (value) "enable" else "disable"
            val ok = withContext(Dispatchers.IO) {
                Shell.cmd("magisk --denylist $cmd").exec().isSuccess
            }
            if (ok) {
                Config.denyList = value
            } else {
                _messages.emit(uiText(CoreR.string.failure))
            }
            updateSnapshot()
        }
    }

    fun setRootMode(value: Int) {
        Config.rootMode = value
        updateSnapshot()
    }

    fun setSuMultiuserMode(value: Int) {
        Config.suMultiuserMode = value
        updateSnapshot()
    }

    fun setSuMntNamespaceMode(value: Int) {
        Config.suMntNamespaceMode = value
        updateSnapshot()
    }

    fun setSuAuth(value: Boolean) {
        Config.suAuth = value
        updateSnapshot()
    }

    fun setSuAutoResponse(value: Int) {
        Config.suAutoResponse = value
        updateSnapshot()
    }

    fun setSuTimeoutIndex(index: Int) {
        Config.suDefaultTimeout = SU_TIMEOUT_VALUES[index.coerceIn(0, SU_TIMEOUT_VALUES.lastIndex)]
        updateSnapshot()
    }

    fun setSuNotification(value: Int) {
        Config.suNotification = value
        updateSnapshot()
    }

    fun setSuReAuth(value: Boolean) {
        Config.suReAuth = value
        updateSnapshot()
    }

    fun setSuTapjack(value: Boolean) {
        Config.suTapjack = value
        updateSnapshot()
    }

    fun setSuRestrict(value: Boolean) {
        Config.suRestrict = value
        updateSnapshot()
    }

    fun toggleLanguageSearch() {
        _state.update { it.copy(languageSearchVisible = !it.languageSearchVisible, languageSearchQuery = "") }
    }

    fun setLanguageSearchQuery(query: String) {
        _state.update { it.copy(languageSearchQuery = query) }
    }

    fun toggleSettingsSearch() {
        _state.update { it.copy(settingsSearchVisible = !it.settingsSearchVisible, settingsSearchQuery = "") }
    }

    fun setSettingsSearchQuery(query: String) {
        _state.update { it.copy(settingsSearchQuery = query) }
    }

    fun setMessageRes(res: Int) {
        _messages.tryEmit(uiText(res))
    }

    private fun updateSnapshot() {
        val runtime = MagiskRuntimeEngine.snapshot()
        val updateChannel = Config.updateChannel.coerceIn(
            Config.Value.MBE_CHANNEL,
            Config.Value.CUSTOM_CHANNEL
        )
        val suTimeoutIndex = SU_TIMEOUT_VALUES.indexOf(Config.suDefaultTimeout).let { if (it < 0) 0 else it }
        val currentLang = currentLanguageTag()
        val languageIndex = LocaleSetting.available.tags.indexOf(currentLang).let { if (it < 0) 0 else it }
        val isHiddenApp = isHiddenMagiskApp()
        val canMigrateApp = runtime.canMigrateApp

        _state.update {
            it.copy(
                runtime = runtime,
                darkThemeMode = Config.darkTheme,
                bottomBarStyle = Config.bottomBarStyle,
                themeOrdinal = Config.themeOrdinal,
                selectedThemeIndex = ThemeOption.displayOrder.indexOf(ThemeOption.selected).coerceAtLeast(0),
                themeNameRes = ThemeOption.selected.labelRes,
                useLocaleManager = LocaleSetting.useLocaleManager,
                languageSystemName = LocaleSetting.instance.appLocale?.let { locale -> locale.getDisplayName(locale) } ?: AppContext.getString(CoreR.string.system_default),
                languageIndex = languageIndex,
                languageName = LocaleSetting.available.names.getOrElse(languageIndex) { AppContext.getString(CoreR.string.system_default) },
                canMigrateApp = canMigrateApp,
                isHiddenApp = isHiddenApp,
                canRestoreApp = canMigrateApp && isHiddenApp,
                canHideApp = canMigrateApp && !isHiddenApp,
                updateChannel = updateChannel,
                updateChannelName = updateChannelArray.getOrElse(updateChannel) { "-" },
                isCustomChannel = updateChannel == Config.Value.CUSTOM_CHANNEL,
                customChannelUrl = Config.customChannelUrl,
                doh = Config.doh,
                downloadDir = Config.downloadDir,
                downloadDirPath = MediaStoreUtils.fullPath(Config.downloadDir),
                randName = Config.randName,
                zygisk = Config.zygisk,
                zygiskMismatch = Config.zygisk != runtime.isZygiskEnabled,
                denyList = Config.denyList,
                showMagisk = runtime.canShowMagiskSettings,
                showMagiskAdvanced = runtime.canShowMagiskAdvancedSettings,
                showDenyListConfig = runtime.canShowDenyListConfig,
                showSuperuser = runtime.canShowSuperuser,
                suTapjack = Config.suTapjack,
                suAuth = Config.suAuth,
                rootMode = Config.rootMode,
                accessModeName = suAccessArray.getOrElse(Config.rootMode) { "-" },
                suMultiuserMode = Config.suMultiuserMode,
                multiuserModeName = multiuserModeArray.getOrElse(Config.suMultiuserMode) { "-" },
                multiuserSummary = multiuserSummaryArray.getOrElse(Config.suMultiuserMode) { "-" },
                suMntNamespaceMode = Config.suMntNamespaceMode,
                mountNamespaceName = namespaceArray.getOrElse(Config.suMntNamespaceMode) { "-" },
                mountNamespaceSummary = namespaceSummaryArray.getOrElse(Config.suMntNamespaceMode) { "-" },
                suAutoResponse = Config.suAutoResponse,
                autoResponseName = autoResponseArray.getOrElse(Config.suAutoResponse) { "-" },
                suTimeoutIndex = suTimeoutIndex,
                requestTimeoutName = requestTimeoutArray.getOrElse(suTimeoutIndex) { "-" },
                suNotification = Config.suNotification,
                suNotificationName = suNotificationArray.getOrElse(Config.suNotification) { "-" },
                suReAuth = Config.suReAuth,
                suRestrict = Config.suRestrict
            )
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return SettingsViewModel() as T
            }
        }
    }
}

val SU_TIMEOUT_VALUES = listOf(10, 15, 20, 30, 45, 60)

private fun currentLanguageTag(): String {
    return LocaleSetting.instance.appLocale?.toLanguageTag() ?: Config.locale
}

private fun isHiddenMagiskApp(): Boolean {
    return isRunningAsStub && Config.suManager == AppContext.packageName
}
