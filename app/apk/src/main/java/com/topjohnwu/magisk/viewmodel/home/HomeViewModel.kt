package com.topjohnwu.magisk.viewmodel.home

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.update.UpdateManager
import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.repository.NetworkService
import com.topjohnwu.magisk.core.tasks.MagiskInstaller
import com.topjohnwu.magisk.runtime.MagiskInstallState
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import com.topjohnwu.magisk.runtime.MagiskRuntimeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.topjohnwu.magisk.core.R as CoreR

data class HomeUiState(
    val runtime: MagiskRuntimeState = MagiskRuntimeEngine.snapshot(),
    val magiskState: HomeViewModel.State = HomeViewModel.State.INVALID,
    val appState: HomeViewModel.State = HomeViewModel.State.LOADING,
    val managerRemoteVersion: String = "",
    val managerRemoteVersionCode: String = "",
    val managerReleaseNotes: String = "",
    val managerInstalledVersion: String = "",
    val managerInstalledVersionCode: String = "",
    val packageName: String = "",
    val envActive: Boolean = runtime.isInstalled,
    val showHideRestore: Boolean = false,
    val showManagerInstall: Boolean = false,
    val envFixCode: Int = 0,
    val noticeVisible: Boolean = Config.safetyNotice
)

class HomeViewModel(private val svc: NetworkService) : ViewModel() {
    enum class State {
        LOADING, INVALID, OUTDATED, UP_TO_DATE
    }

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    private var refreshJob: Job? = null
    private var lastRefreshAt = 0L

    init {
        viewModelScope.launch {
            UpdateManager.appUpdateState.collect { appUpdate ->
                val appState = when {
                    appUpdate.isChecking -> HomeViewModel.State.LOADING
                    appUpdate.hasFailed -> HomeViewModel.State.INVALID
                    appUpdate.isUpdateAvailable -> HomeViewModel.State.OUTDATED
                    else -> if (appUpdate.updateInfo != null) HomeViewModel.State.UP_TO_DATE else HomeViewModel.State.LOADING
                }
                val remote = appUpdate.updateInfo
                _state.update {
                    it.copy(
                        appState = appState,
                        managerInstalledVersion = "${BuildConfig.MBE_VERSION_NAME} (${BuildConfig.APP_VERSION_CODE})",
                        managerInstalledVersionCode = String.format("%05d", BuildConfig.MBE_VERSION_CODE),
                        managerRemoteVersion = remote?.let { r -> "${r.version} (${if (r.clientVersionCode > 0) r.clientVersionCode else BuildConfig.APP_VERSION_CODE})" }.orEmpty(),
                        managerRemoteVersionCode = remote?.versionCode?.takeIf { code -> code > 0 }?.let { String.format("%05d", it) }.orEmpty(),
                        managerReleaseNotes = remote?.note.orEmpty(),
                    )
                }
            }
        }
    }

    fun refresh(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && _state.value.appState != HomeViewModel.State.LOADING && now - lastRefreshAt < MIN_REFRESH_INTERVAL_MS) {
            return
        }
        lastRefreshAt = now
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val update = async { UpdateManager.checkForAppUpdate(svc, force = force) }
            update.await()
            val runtime = MagiskRuntimeEngine.snapshot()
            val magiskState = runtime.toHomeState()
            _state.update {
                it.copy(
                    runtime = runtime,
                    magiskState = magiskState,
                    packageName = AppContext.packageName,
                    envActive = runtime.isInstalled,
                    noticeVisible = Config.safetyNotice
                )
            }
            ensureEnv(runtime)
        }
    }

    fun hideNotice() {
        Config.safetyNotice = false
        _state.update { it.copy(noticeVisible = false) }
    }

    fun checkForMagiskUpdates() {
        refresh(force = true)
    }

    fun onHideRestorePressed() {
        _state.update { it.copy(showHideRestore = true) }
    }

    fun onHideRestoreConsumed() {
        _state.update { it.copy(showHideRestore = false) }
    }

    fun onEnvFixConsumed() {
        _state.update { it.copy(envFixCode = 0) }
    }

    fun onManagerPressed() {
        when (_state.value.appState) {
            HomeViewModel.State.LOADING -> _messages.tryEmit(uiText(CoreR.string.loading))
            HomeViewModel.State.INVALID -> _messages.tryEmit(uiText(CoreR.string.no_connection))
            else -> _state.update { it.copy(showManagerInstall = true) }
        }
    }

    fun onManagerInstallConsumed() {
        _state.update { it.copy(showManagerInstall = false) }
    }

    fun restoreImages() {
        viewModelScope.launch {
            _messages.tryEmit(uiText(CoreR.string.restore_img_msg))
            val success = MagiskInstaller.Restore().exec { }
            _messages.emit(uiText(if (success) CoreR.string.restore_done else CoreR.string.restore_fail))
        }
    }

    fun requestReboot(reason: String = "") {
        viewModelScope.launch {
            val runtime = MagiskRuntimeEngine.snapshot()
            if (!runtime.isRooted || !MagiskRuntimeEngine.hasRootShell()) {
                _messages.emit(uiText(CoreR.string.root_required_operation))
                return@launch
            }
            _effects.emit(UiEffect.Reboot(reason))
        }
    }

    fun openLink(link: String) {
        _effects.tryEmit(UiEffect.OpenUri(link.toUri()))
    }

    private suspend fun ensureEnv(runtime: MagiskRuntimeState) {
        if (!runtime.isInstalled || runtime.isUnsupported || checkedEnv) return
        val code = MagiskRuntimeEngine.checkEnvironment(runtime)
        if (code != 0) {
            _state.update { it.copy(envFixCode = code) }
        }
        checkedEnv = true
    }

    companion object {
        internal const val MIN_REFRESH_INTERVAL_MS = 1200L
        private var checkedEnv = false

        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return HomeViewModel(ServiceLocator.networkService) as T
            }
        }
    }
}

private fun MagiskRuntimeState.toHomeState(): HomeViewModel.State = when (installState) {
    MagiskInstallState.NotInstalled -> HomeViewModel.State.INVALID
    MagiskInstallState.Installed -> HomeViewModel.State.UP_TO_DATE
    MagiskInstallState.Outdated,
    MagiskInstallState.Unsupported -> HomeViewModel.State.OUTDATED
}
