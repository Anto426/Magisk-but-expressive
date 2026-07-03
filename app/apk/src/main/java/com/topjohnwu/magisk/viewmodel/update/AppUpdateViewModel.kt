package com.topjohnwu.magisk.viewmodel.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.model.UpdateInfo
import com.topjohnwu.magisk.core.repository.NetworkService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.topjohnwu.magisk.core.R as CoreR

data class AppUpdateUiState(
    val loading: Boolean = true,
    val update: UpdateInfo = UpdateInfo(),
    val downloadProgress: Float? = null,
    val downloadFailed: Boolean = false
) {
    val hasUpdateInfo: Boolean get() = update.versionCode > 0 && update.link.isNotBlank()
    val updateAvailable: Boolean get() = hasUpdateInfo && BuildConfig.MBE_VERSION_CODE < update.versionCode
    val installedVersion: String get() = BuildConfig.MBE_VERSION_NAME
    val installedVersionCode: String get() = BuildConfig.MBE_VERSION_CODE.toString()
    val latestVersion: String get() = if (hasUpdateInfo) update.version else ""
    val latestVersionCode: String get() = if (hasUpdateInfo) update.versionCode.toString() else ""
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
        refresh()
    }

    fun refresh(force: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (force) {
                Info.resetUpdate()
            }
            _state.update { it.copy(loading = true, downloadFailed = false) }
            val update = Info.fetchUpdate(svc)
            if (update == null) {
                _state.update { it.copy(loading = false, update = UpdateInfo()) }
                _messages.emit(uiText(CoreR.string.no_connection))
            } else {
                _state.update { it.copy(loading = false, update = update) }
            }
        }
    }

    fun onDownloadProgress(progress: Float) {
        _state.update {
            when {
                progress == -2f -> it.copy(downloadProgress = null, downloadFailed = true)
                progress >= 1f -> it.copy(downloadProgress = 1f, downloadFailed = false)
                progress >= 0f -> it.copy(downloadProgress = progress, downloadFailed = false)
                else -> it.copy(downloadProgress = -1f, downloadFailed = false)
            }
        }
    }

    fun onDownloadStarted() {
        _state.update { it.copy(downloadProgress = 0f, downloadFailed = false) }
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
