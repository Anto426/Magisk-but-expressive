package com.topjohnwu.magisk.ui.flash

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.ktx.reboot
import com.topjohnwu.magisk.core.ktx.timeFormatStandard
import com.topjohnwu.magisk.core.ktx.toTime
import com.topjohnwu.magisk.core.ktx.writeTo
import com.topjohnwu.magisk.core.tasks.MagiskInstaller
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.displayName
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.inputStream
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.outputStream
import com.topjohnwu.magisk.ui.RouteProcessTopBarState
import com.topjohnwu.magisk.ui.component.TerminalCloseButton
import com.topjohnwu.magisk.ui.component.TerminalRebootButton
import com.topjohnwu.magisk.ui.component.TerminalRunningActionSlot
import com.topjohnwu.magisk.ui.component.TerminalSaveLogButton
import com.topjohnwu.magisk.ui.component.TerminalScreenScaffold
import com.topjohnwu.magisk.ui.terminal.TerminalLogBuffer
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun FlashScreen(
    action: String,
    uriArg: String?,
    onTitleStateChange: (String?, String?, RouteProcessTopBarState) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    val viewModel: FlashComposeViewModel = viewModel(factory = FlashComposeViewModel.Factory)
    val state by viewModel.state.collectAsState()
    val lines = viewModel.lines
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val hasLogs by remember { derivedStateOf { lines.isNotEmpty() } }
    val parsedUri = uriArg
        ?.takeIf { it.isNotBlank() }
        ?.let(::parseNavigationUriArg)
    var hasStarted by remember(action, uriArg) { mutableStateOf(false) }

    BackHandler(enabled = state.running) { }

    LaunchedEffect(action, parsedUri) {
        viewModel.start(action, parsedUri)
    }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(state.running, state.success, lines.size) {
        if (state.running || hasLogs) {
            hasStarted = true
        }
        val title = AppContext.getString(CoreR.string.flash_screen_title)
        val subtitle = when {
            state.running -> AppContext.getString(CoreR.string.flashing)
            !hasStarted -> null
            state.success -> AppContext.getString(CoreR.string.done)
            else -> AppContext.getString(CoreR.string.failure)
        }
        onTitleStateChange(
            title,
            subtitle,
            RouteProcessTopBarState(
                running = state.running,
                success = state.success,
                hasResult = hasStarted && !state.running
            )
        )
    }
    DisposableEffect(Unit) {
        onDispose { onTitleStateChange(null, null, RouteProcessTopBarState()) }
    }

    TerminalScreenScaffold(
        lines = lines,
        listState = listState,
        snackbarHostState = snackbarHostState,
        emptyText = AppContext.getString(CoreR.string.waiting_for_logs)
    ) {
        TerminalSaveLogButton(
            hasLogs = hasLogs,
            onClick = viewModel::saveLog,
            modifier = Modifier.weight(1f)
        )
        TerminalRunningActionSlot(
            isRunning = state.running,
            modifier = Modifier.weight(1f)
        ) {
            if (state.showReboot) {
                TerminalRebootButton(onClick = viewModel::rebootNow)
            } else {
                TerminalCloseButton(onClick = onBack)
            }
        }
    }
}

private data class FlashUiState(
    val running: Boolean = false,
    val success: Boolean = false,
    val showReboot: Boolean = Info.isRooted
)

private class FlashComposeViewModel : ViewModel() {
    private val _state = MutableStateFlow(FlashUiState())
    val state: StateFlow<FlashUiState> = _state
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private val terminal = TerminalLogBuffer()
    val lines = terminal.lines
    private val logs = terminal.logs
    private val outItems = terminal.console
    private var started = false

    init {
        terminal.bind(viewModelScope)
    }

    fun start(action: String, uri: Uri?) {
        if (started) return
        started = true
        viewModelScope.launch {
            _state.update { it.copy(running = true, success = false, showReboot = Info.isRooted) }
            if (requiresRoot(action)) {
                val hasRoot = withContext(Dispatchers.IO) { Shell.getShell().isRoot }
                if (!hasRoot) {
                    outItems.add("! ${AppContext.getString(CoreR.string.root_required_operation)}")
                    _state.update { it.copy(running = false, success = false, showReboot = false) }
                    _messages.emit(AppContext.getString(CoreR.string.failure))
                    return@launch
                }
            }
            val result = when (action) {
                Const.Value.FLASH_ZIP -> if (uri == null) false else flashZipWithLogs(uri)

                Const.Value.UNINSTALL -> {
                    _state.update { it.copy(showReboot = false) }; MagiskInstaller.Uninstall(
                        outItems,
                        logs
                    ).exec()
                }

                Const.Value.FLASH_MAGISK -> if (Info.isEmulator) MagiskInstaller.Emulator(
                    outItems,
                    logs
                ).exec() else MagiskInstaller.Direct(outItems, logs).exec()

                Const.Value.FLASH_INACTIVE_SLOT -> {
                    _state.update { it.copy(showReboot = false) }; MagiskInstaller.SecondSlot(
                        outItems,
                        logs
                    ).exec()
                }

                Const.Value.PATCH_FILE -> if (uri == null) false else {
                    _state.update { it.copy(showReboot = false) }; MagiskInstaller.Patch(
                        uri,
                        outItems,
                        logs
                    ).exec()
                }

                else -> false
            }
            _state.update { it.copy(running = false, success = result) }
        }
    }
    
    private suspend fun flashZipWithLogs(uri: Uri): Boolean {
        val installDir = File(AppContext.cacheDir, "flash")
        val prep = withContext(Dispatchers.IO) {
            try {
                installDir.deleteRecursively()
                installDir.mkdirs()

                val zipFile = if (uri.scheme == "file") {
                    uri.toFile()
                } else {
                    File(installDir, "install.zip").also {
                        try {
                            uri.inputStream().writeTo(it)
                        } catch (e: IOException) {
                            val msg = if (e is FileNotFoundException) "Invalid Uri" else "Cannot copy to cache"
                            return@withContext msg to null
                        }
                    }
                }

                val binary = File(installDir, "update-binary")
                AppContext.assets.open("module_installer.sh").use { it.writeTo(binary) }

                val name = uri.displayName
                null to Triple(installDir, zipFile, name)
            } catch (e: Exception) {
                Timber.e(e)
                "Unable to extract files" to null
            }
        }

        val (error, prepResult) = prep
        if (prepResult == null) {
            outItems.add("! ${error ?: "Installation failed"}")
            return false
        }

        val (dir, zipFile, displayName) = prepResult
        outItems.add("- Installing $displayName")

        val success = withContext(Dispatchers.IO) {
            Shell.cmd(
                "sh $dir/update-binary dummy 1 '${zipFile.absolutePath}'"
            ).to(outItems, logs).exec().isSuccess
        }
        if (!success) outItems.add("! Installation failed")

        Shell.cmd("cd /", "rm -rf $dir ${Const.TMPDIR}").submit()
        return success
    }

    fun saveLog() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val name = "magisk_install_log_%s.log".format(
                    System.currentTimeMillis().toTime(timeFormatStandard)
                )
                val file = MediaStoreUtils.getFile(name)
                file.uri.outputStream().bufferedWriter()
                    .use(terminal::writeTo)
                file.toString()
            }.onSuccess { path -> _messages.emit(path) }
                .onFailure { _messages.emit(AppContext.getString(CoreR.string.failure)) }
        }
    }

    fun rebootNow() {
        reboot()
    }

    override fun onCleared() {
        terminal.close()
        super.onCleared()
    }

    private fun requiresRoot(action: String): Boolean =
        action == Const.Value.FLASH_ZIP || action == Const.Value.UNINSTALL || action == Const.Value.FLASH_MAGISK || action == Const.Value.FLASH_INACTIVE_SLOT

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return FlashComposeViewModel() as T
            }
        }
    }
}

private fun parseNavigationUriArg(rawArg: String): Uri? {
    val rawParsed = runCatching { rawArg.toUri() }.getOrNull()
    if (rawParsed?.scheme == "content" || rawParsed?.scheme == "file") {
        return rawParsed
    }
    val decoded = runCatching { Uri.decode(rawArg) }.getOrNull() ?: return rawParsed
    val decodedParsed = runCatching { decoded.toUri() }.getOrNull()
    return when (decodedParsed?.scheme) {
        "content", "file" -> decodedParsed
        else -> rawParsed
    }
}
