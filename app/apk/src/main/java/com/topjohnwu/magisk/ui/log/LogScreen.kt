package com.topjohnwu.magisk.ui.log

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UIActivity
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.ktx.timeFormatStandard
import com.topjohnwu.magisk.core.ktx.toTime
import com.topjohnwu.magisk.core.repository.LogRepository
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.outputStream
import com.topjohnwu.magisk.ui.RefreshOnResume
import com.topjohnwu.magisk.ui.animation.MagiskMotion
import com.topjohnwu.magisk.ui.component.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun LogsScreen(
    viewModel: MagiskLogViewModel = viewModel(factory = MagiskLogViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as? UIActivity<*>
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clearDialog = rememberConfirmDialog()

    var filter by remember { mutableStateOf(LogDisplayFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(state.visibleLogs, filter, searchQuery) {
        val base = when (filter) {
            LogDisplayFilter.ALL -> state.visibleLogs
            LogDisplayFilter.ISSUES -> state.visibleLogs.filter { it.isIssue }
            LogDisplayFilter.MAGISK -> state.visibleLogs.filter { it.isMagisk }
            LogDisplayFilter.SU -> state.visibleLogs.filter { it.isSu }
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter { it.contains(searchQuery) }
        }
    }

    val stats = remember(state.visibleLogs) { LogStats.from(state.visibleLogs) }

    RefreshOnResume { viewModel.refresh() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = MagiskUiDefaults.screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.ListItemSpacing)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.SectionSpacing)) {
                    LogHeroCard(
                        stats = stats,
                        loading = state.loading,
                        onRefresh = viewModel::refresh,
                        onSave = {
                            activity?.withPermission(WRITE_EXTERNAL_STORAGE) { granted ->
                                if (granted) viewModel.saveMagiskLog()
                            }
                        },
                        onClear = {
                            scope.launch {
                                val result = clearDialog.awaitConfirm(
                                    title = AppContext.getString(CoreR.string.log_clear_confirm_title),
                                    content = AppContext.getString(CoreR.string.log_clear_confirm_msg),
                                    confirm = AppContext.getString(CoreR.string.menuClearLog)
                                )
                                if (result == ConfirmResult.Confirmed) {
                                    viewModel.clearMagiskLogs()
                                }
                            }
                        }
                    )

                    LogFilterSection(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        activeFilter = filter,
                        onFilterChange = { filter = it }
                    )
                }
            }

            if (state.loading && state.visibleLogs.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeCap = StrokeCap.Round)
                    }
                }
            } else if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxWidth().fillParentMaxHeight(0.5f),
                        contentAlignment = Alignment.Center
                    ) {
                        MagiskEmptyState(
                            icon = Icons.Rounded.Terminal,
                            title = stringResource(id = CoreR.string.log_data_magisk_none)
                        )
                    }
                }
            } else {
                items(items = filteredLogs, key = { it.id }) { item ->
                    LogEventCard(item = item)
                }
            }
        }

        MagiskSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MagiskUiDefaults.SnackbarBottomPaddingWithBar)
        )

        AnimatedVisibility(
            visible = listState.canScrollForward && filteredLogs.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(
                    end = MagiskUiDefaults.ScreenHorizontalPadding,
                    bottom = MagiskUiDefaults.FloatingActionBottomPaddingWithBar
                ),
            enter = MagiskMotion.fabEnter(),
            exit = MagiskMotion.fabExit()
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        MagiskMotion.scrollToItem(
                            listState,
                            filteredLogs.size
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MagiskUiDefaults.PillShape
            ) {
                Icon(imageVector = Icons.Rounded.KeyboardDoubleArrowDown, contentDescription = null)
            }
        }
    }
}

@Immutable
data class MagiskLogScreenUiState(
    val loading: Boolean = true,
    val visibleLogs: List<MagiskLogUiItem> = emptyList()
)

@Immutable
data class LogStats(
    val total: Int,
    val issues: Int,
    val sources: Int
) {
    companion object {
        fun from(items: List<MagiskLogUiItem>): LogStats {
            return LogStats(
                total = items.size,
                issues = items.count { it.isIssue },
                sources = items.map { it.sourceLabel }.distinct().size
            )
        }
    }
}

enum class LogDisplayFilter(@param:StringRes val labelRes: Int) {
    ALL(CoreR.string.log_filter_all),
    MAGISK(CoreR.string.log_filter_magisk),
    SU(CoreR.string.log_filter_su),
    ISSUES(CoreR.string.log_filter_issues)
}

enum class MagiskLogLevel(val code: Char, val shortLabel: String) {
    VERBOSE('V', "V"),
    DEBUG('D', "D"),
    INFO('I', "I"),
    WARN('W', "W"),
    ERROR('E', "E"),
    FATAL('F', "F"),
    UNKNOWN('?', "?");

    @Composable
    fun color(): Color = when (this) {
        WARN -> Color(0xFFF4B400)
        ERROR, FATAL -> MaterialTheme.colorScheme.error
        DEBUG -> MaterialTheme.colorScheme.primary
        INFO -> MaterialTheme.colorScheme.tertiary
        VERBOSE -> MaterialTheme.colorScheme.outline
        UNKNOWN -> MaterialTheme.colorScheme.outline
    }

    fun icon(): ImageVector = when (this) {
        WARN -> Icons.Rounded.Warning
        ERROR, FATAL -> Icons.Rounded.Dangerous
        DEBUG -> Icons.Rounded.BugReport
        else -> Icons.Rounded.Info
    }

    companion object {
        fun from(code: Char): MagiskLogLevel {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}

@Immutable
data class MagiskLogUiItem(
    val id: Int,
    val timestamp: String,
    val tag: String,
    val level: MagiskLogLevel,
    val message: String,
    val raw: String,
    val pid: Int = 0,
    val tid: Int = 0
) {
    val isIssue: Boolean
        get() = level == MagiskLogLevel.WARN ||
                level == MagiskLogLevel.ERROR ||
                level == MagiskLogLevel.FATAL

    val isMagisk: Boolean
        get() = tag.contains("magisk", ignoreCase = true) ||
                message.contains("magisk", ignoreCase = true)

    val isSu: Boolean
        get() = message.contains("su:", ignoreCase = true) ||
                raw.contains("su:", ignoreCase = true) ||
                tag.equals("su", ignoreCase = true)

    val sourceLabel: String
        get() = when {
            isMagisk -> AppContext.getString(CoreR.string.log_source_magisk)
            isSu -> AppContext.getString(CoreR.string.log_source_su)
            tag.isNotBlank() -> tag
            else -> AppContext.getString(CoreR.string.log_source_system)
        }

    fun contains(query: String): Boolean {
        return tag.contains(query, ignoreCase = true) ||
                message.contains(query, ignoreCase = true) ||
                raw.contains(query, ignoreCase = true) ||
                timestamp.contains(query, ignoreCase = true)
    }
}

class MagiskLogViewModel(private val repo: LogRepository) : ViewModel() {
    private val _state = MutableStateFlow(MagiskLogScreenUiState())
    val state = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val raw = withContext(Dispatchers.IO) { repo.fetchMagiskLogs() }
            val items = withContext(Dispatchers.Default) {
                MagiskLogParser.parse(raw).mapIndexed { index, entry ->
                    MagiskLogUiItem(
                        id = index,
                        timestamp = entry.timestamp,
                        tag = entry.tag,
                        level = MagiskLogLevel.from(entry.level),
                        message = entry.message,
                        raw = entry.message,
                        pid = entry.pid,
                        tid = entry.tid
                    )
                }
            }
            _state.update { it.copy(loading = false, visibleLogs = items) }
        }
    }

    fun clearMagiskLogs() {
        repo.clearMagiskLogs {
            _messages.tryEmit(AppContext.getString(CoreR.string.logs_cleared))
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
                result
                    .onSuccess { _messages.emit(AppContext.getString(CoreR.string.saved_to_path, it)) }
                    .onFailure { _messages.emit(AppContext.getString(CoreR.string.failure)) }
            }
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MagiskLogViewModel(ServiceLocator.logRepo) as T
            }
        }
    }
}
