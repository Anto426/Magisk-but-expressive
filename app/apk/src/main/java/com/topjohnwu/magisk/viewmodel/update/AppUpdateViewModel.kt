package com.topjohnwu.magisk.viewmodel.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.update.UpdateManager
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.model.UpdateInfo
import com.topjohnwu.magisk.core.repository.NetworkService
import com.topjohnwu.magisk.core.update.AppVersion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.topjohnwu.magisk.core.R as CoreR

data class AppUpdateUiState(
    val loading: Boolean = true,
    val update: UpdateInfo = UpdateInfo(),
    val changelogAvailable: Boolean = false,
    val downloadProgress: Float? = null,
    val downloadFailed: Boolean = false,
    val refreshFailed: Boolean = false
) {
    val hasUpdateInfo: Boolean get() = update.versionCode > 0 && update.link.isNotBlank()
    val updateAvailable: Boolean get() = hasUpdateInfo && AppVersion.isUpdateAvailable(update)
    val installedVersion: String get() = AppVersion.installedDisplay
    val installedVersionCode: String get() = AppVersion.installedCodeText
    val latestVersion: String get() = if (hasUpdateInfo) AppVersion.remoteDisplay(update) else ""
    val latestVersionCode: String get() = if (hasUpdateInfo) AppVersion.remoteCodeText(update) else ""
    val downloadInProgress: Boolean
        get() = downloadProgress?.let { it < 1f } == true && !downloadFailed
}

class AppUpdateViewModel(
    private val svc: NetworkService
) : ViewModel() {
    private val _state = MutableStateFlow(AppUpdateUiState())
    val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            UpdateManager.state
                .map { it.app }
                .distinctUntilChanged()
                .collect { appUpdate ->
                _state.update { current ->
                    val nextUpdate = appUpdate.updateInfo ?: UpdateInfo()
                    val sameArtifact = current.update.link == nextUpdate.link &&
                        current.update.versionCode == nextUpdate.versionCode
                    current.copy(
                        loading = appUpdate.isChecking,
                        update = nextUpdate,
                        changelogAvailable = appUpdate.changelog.isNotBlank(),
                        downloadProgress = current.downloadProgress.takeIf { sameArtifact },
                        downloadFailed = current.downloadFailed && sameArtifact,
                        refreshFailed = appUpdate.hasFailed
                    )
                }
            }
        }
        refresh()
    }

    fun refresh(force: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val update = UpdateManager.refreshApp(
                service = svc,
                force = force,
                publishNotification = true
            )
            if (update == null) {
                _messages.emit(uiText(CoreR.string.no_connection))
            }
        }
    }

    fun onDownloadProgress(progress: Float, url: String, mbeVersionCode: Int) {
        _state.update {
            if (url != it.update.link || mbeVersionCode != it.update.versionCode) {
                return@update it
            }
            when {
                progress == -2f -> it.copy(downloadProgress = null, downloadFailed = true)
                progress >= 1f -> it.copy(downloadProgress = 1f, downloadFailed = false)
                progress >= 0f -> it.copy(downloadProgress = progress, downloadFailed = false)
                else -> it.copy(downloadProgress = -1f, downloadFailed = false)
            }
        }
    }

    @Synchronized
    fun onDownloadStarted(url: String, mbeVersionCode: Int): Boolean {
        val current = _state.value
        if (
            current.downloadInProgress ||
            url != current.update.link ||
            mbeVersionCode != current.update.versionCode
        ) {
            return false
        }
        _state.value = current.copy(downloadProgress = 0f, downloadFailed = false)
        return true
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AppUpdateViewModel(ServiceLocator.networkService) as T
            }
        }
    }
}
