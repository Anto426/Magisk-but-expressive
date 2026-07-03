package com.topjohnwu.magisk.viewmodel.log

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.ktx.timeFormatStandard
import com.topjohnwu.magisk.core.ktx.toTime
import com.topjohnwu.magisk.core.repository.LogRepository
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.outputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

data class MagiskLogScreenUiState(
    val loading: Boolean = true,
    val logs: List<MagiskLogUiItem> = emptyList(),
    val filteredLogs: List<MagiskLogUiItem> = emptyList(),
    val stats: LogStats = LogStats.Empty,
    val filter: LogDisplayFilter = LogDisplayFilter.ALL,
    val searchQuery: String = "",
    val searchVisible: Boolean = false
)

data class LogStats(
    val total: Int, val issues: Int, val sources: Int
) {
    companion object {
        val Empty = LogStats(total = 0, issues = 0, sources = 0)

        fun from(items: List<MagiskLogUiItem>): LogStats {
            return LogStats(
                total = items.size,
                issues = items.count { it.isIssue },
                sources = items.asSequence().map { it.sourceLabel }.toSet().size
            )
        }
    }
}

enum class LogDisplayFilter(@StringRes val labelRes: Int) {
    ALL(CoreR.string.log_filter_all), MAGISK(CoreR.string.log_filter_magisk), SU(CoreR.string.log_filter_su), ISSUES(
        CoreR.string.log_filter_issues
    )
}

enum class MagiskLogLevel(val code: Char, val shortLabel: String) {
    VERBOSE('V', "V"), DEBUG('D', "D"), INFO('I', "I"), WARN('W', "W"), ERROR('E', "E"), FATAL(
        'F',
        "F"
    ),
    UNKNOWN('?', "?");

    companion object {
        fun from(code: Char): MagiskLogLevel {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}

data class MagiskLogUiItem(
    val id: Int,
    val timestamp: String,
    val tag: String,
    val level: MagiskLogLevel,
    val message: String,
    val raw: String,
    val pid: Int = 0,
    val tid: Int = 0,
    val isIssue: Boolean,
    val isMagisk: Boolean,
    val isSu: Boolean,
    val sourceLabel: String,
    val searchKey: String
) {
    fun contains(query: String): Boolean {
        return searchKey.contains(query)
    }
}

class MagiskLogViewModel(private val repo: LogRepository) : ViewModel() {
    private val _state = MutableStateFlow(MagiskLogScreenUiState())
    val state = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val raw = withContext(Dispatchers.IO) { repo.fetchMagiskLogs() }
            val items = withContext(Dispatchers.Default) {
                val magiskLabel = AppContext.getString(CoreR.string.log_source_magisk)
                val suLabel = AppContext.getString(CoreR.string.log_source_su)
                val systemLabel = AppContext.getString(CoreR.string.log_source_system)
                MagiskLogParser.parse(raw).mapIndexed { index, entry ->
                    val isMagisk = entry.tag.contains("magisk", ignoreCase = true) || entry.message.contains("magisk", ignoreCase = true)
                    val isSu = entry.message.contains("su:", ignoreCase = true) || entry.tag.equals("su", ignoreCase = true)
                    val isIssue = entry.level == 'W' || entry.level == 'E' || entry.level == 'F'
                    val sourceLabel = when {
                        isMagisk -> magiskLabel
                        isSu -> suLabel
                        entry.tag.isNotBlank() -> entry.tag
                        else -> systemLabel
                    }
                    MagiskLogUiItem(
                        id = index,
                        timestamp = entry.timestamp,
                        tag = entry.tag,
                        level = MagiskLogLevel.from(entry.level),
                        message = entry.message,
                        raw = entry.message,
                        pid = entry.pid,
                        tid = entry.tid,
                        isIssue = isIssue,
                        isMagisk = isMagisk,
                        isSu = isSu,
                        sourceLabel = sourceLabel,
                        searchKey = buildLogSearchKey(entry, sourceLabel)
                    )
                }
            }
            _state.update { it.withLogs(items, loading = false) }
        }
    }

    fun setFilter(filter: LogDisplayFilter) {
        _state.update { it.withFilter(filter) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.withSearchQuery(query) }
    }

    fun clearSearch() {
        setSearchQuery("")
    }

    fun toggleSearch() {
        _state.update {
            if (it.searchVisible) {
                it.withSearchQuery("").copy(searchVisible = false)
            } else {
                it.copy(searchVisible = true)
            }
        }
    }

    fun clearMagiskLogs() {
        repo.clearMagiskLogs {
            _messages.tryEmit(uiText(CoreR.string.logs_cleared))
            refresh()
        }
    }

    fun saveMagiskLog() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val filename = "magisk_log_%s.log".format(
                    System.currentTimeMillis().toTime(timeFormatStandard)
                )
                val logFile = MediaStoreUtils.getFile(filename)
                val raw = repo.fetchMagiskLogs()
                logFile.uri.outputStream().bufferedWriter().use {
                    it.write("---Magisk Logs---\n${Info.env.versionString}\n\n$raw")
                }
                logFile.toString()
            }
            withContext(Dispatchers.Main) {
                result.onSuccess { _messages.emit(uiText(CoreR.string.saved_to_path, it)) }
                    .onFailure { _messages.emit(uiText(CoreR.string.failure)) }
            }
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return MagiskLogViewModel(ServiceLocator.logRepo) as T
            }
        }
    }
}

private fun MagiskLogScreenUiState.withLogs(
    logs: List<MagiskLogUiItem>, loading: Boolean
): MagiskLogScreenUiState {
    return copy(
        loading = loading,
        logs = logs,
        filteredLogs = logs.filteredBy(filter, searchQuery),
        stats = LogStats.from(logs)
    )
}

private fun MagiskLogScreenUiState.withFilter(filter: LogDisplayFilter): MagiskLogScreenUiState {
    return copy(
        filter = filter, filteredLogs = logs.filteredBy(filter, searchQuery)
    )
}

private fun MagiskLogScreenUiState.withSearchQuery(query: String): MagiskLogScreenUiState {
    return copy(
        searchQuery = query, filteredLogs = logs.filteredBy(filter, query)
    )
}

private fun List<MagiskLogUiItem>.filteredBy(
    filter: LogDisplayFilter, query: String
): List<MagiskLogUiItem> {
    val normalized = query.trim().lowercase(Locale.ROOT)
    val base = when (filter) {
        LogDisplayFilter.ALL -> this
        LogDisplayFilter.ISSUES -> filter { it.isIssue }
        LogDisplayFilter.MAGISK -> filter { it.isMagisk }
        LogDisplayFilter.SU -> filter { it.isSu }
    }
    return if (normalized.isEmpty()) base else base.filter { it.contains(normalized) }
}

private fun buildLogSearchKey(entry: MagiskLogEntry, sourceLabel: String): String {
    return buildString {
        append(entry.timestamp)
        append('\n')
        append(entry.tag)
        append('\n')
        append(sourceLabel)
        append('\n')
        append(entry.message)
    }.lowercase(Locale.ROOT)
}
