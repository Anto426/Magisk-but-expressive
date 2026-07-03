package com.topjohnwu.magisk.viewmodel.superuser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.ktx.timeDateFormat
import com.topjohnwu.magisk.core.ktx.timeFormatStandard
import com.topjohnwu.magisk.core.ktx.toTime
import com.topjohnwu.magisk.core.model.su.SuLog
import com.topjohnwu.magisk.core.model.su.SuPolicy
import com.topjohnwu.magisk.core.repository.LogRepository
import com.topjohnwu.magisk.core.su.SuEvents
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.outputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.topjohnwu.magisk.core.R as CoreR

data class SuLogUiItem(
    val id: Int,
    val appName: String,
    val iconPackageName: String?,
    val allowed: Boolean,
    val infoLines: List<String>,
    val command: String
)

data class SuperuserLogsUiState(
    val loading: Boolean = true, val items: List<SuLogUiItem> = emptyList()
)

class SuperuserLogsViewModel(private val repo: LogRepository) : ViewModel() {
    private val _state = MutableStateFlow(SuperuserLogsUiState())
    val state: StateFlow<SuperuserLogsUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            SuEvents.logUpdated.collectLatest {
                refresh()
            }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        val hadItems = _state.value.items.isNotEmpty()
        refreshJob = viewModelScope.launch {
            if (!hadItems) {
                _state.update { it.copy(loading = true) }
            }
            val items = withContext(Dispatchers.IO) {
                repo.fetchSuLogs().map { it.toUiItem() }
            }
            _state.update { it.copy(loading = false, items = items) }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.clearLogs() }
            _messages.emit(uiText(CoreR.string.logs_cleared))
            refresh()
        }
    }

    fun saveLogs() {
        val items = state.value.items
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val name = "superuser_log_%s.log".format(
                    System.currentTimeMillis().toTime(timeFormatStandard)
                )
                val logFile = MediaStoreUtils.getFile(name)
                logFile.uri.outputStream().bufferedWriter().use { writer ->
                    items.forEach { item ->
                        writer.write("${item.appName}\n")
                        item.infoLines.forEach { line -> writer.write("$line\n") }
                        if (item.command.isNotBlank()) {
                            writer.write("${item.command}\n")
                        }
                        writer.write("\n")
                    }
                }
                logFile.uri.toString()
            }
            withContext(Dispatchers.Main) {
                result.onSuccess { path ->
                        _messages.emit(
                            uiText(
                                CoreR.string.saved_to_path,
                                path
                            )
                        )
                    }.onFailure { _messages.emit(uiText(CoreR.string.failure)) }
            }
        }
    }

    fun postExternalRwDenied() {
        _messages.tryEmit(uiText(CoreR.string.external_rw_permission_denied))
    }

    private fun SuLog.toUiItem(): SuLogUiItem {
        val res = AppContext.resources
        val infoLines = mutableListOf<String>()
        infoLines += time.toTime(timeDateFormat)
        infoLines += buildString {
            append(res.getString(CoreR.string.target_uid, toUid))
            append("  ")
            append(res.getString(CoreR.string.pid, fromPid))
            if (target != -1) {
                val pid = if (target == 0) "magiskd" else target.toString()
                append("  ")
                append(res.getString(CoreR.string.target_pid, pid))
            }
        }
        if (context.isNotEmpty()) {
            infoLines += res.getString(CoreR.string.selinux_context, context)
        }
        if (gids.isNotEmpty()) {
            infoLines += res.getString(CoreR.string.supp_group, gids)
        }

        return SuLogUiItem(
            id = id,
            appName = appName,
            iconPackageName = packageName.takeUnless { it.isBlank() },
            allowed = action >= SuPolicy.ALLOW,
            infoLines = infoLines,
            command = command
        )
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return SuperuserLogsViewModel(ServiceLocator.logRepo) as T
            }
        }
    }
}
