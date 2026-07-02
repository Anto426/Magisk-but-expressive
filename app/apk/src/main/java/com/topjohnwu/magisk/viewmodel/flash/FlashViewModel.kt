package com.topjohnwu.magisk.viewmodel.flash

import android.Manifest
import android.app.Notification.Builder as NotificationBuilder
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.TerminalLogStore
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.ktx.timeFormatStandard
import com.topjohnwu.magisk.core.ktx.toTime
import com.topjohnwu.magisk.core.ktx.writeTo
import com.topjohnwu.magisk.core.tasks.MagiskInstaller
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.displayName
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.inputStream
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.outputStream
import com.topjohnwu.magisk.core.utils.ProgressInputStream
import com.topjohnwu.magisk.navigation.FlashPayloadStore
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import com.topjohnwu.magisk.view.Notifications
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import com.topjohnwu.magisk.core.R as CoreR

data class FlashUiState(
    val running: Boolean = false,
    val success: Boolean = false,
    val showReboot: Boolean = MagiskRuntimeEngine.snapshot().isRooted,
    val title: String = AppContext.getString(CoreR.string.flash_screen_title)
)

class FlashViewModel : ViewModel() {
    private val _state = MutableStateFlow(FlashUiState())
    val state: StateFlow<FlashUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    private val terminal = TerminalLogStore()
    val lines = terminal.lines
    private var started = false

    fun start(action: String, uri: Uri?) {
        if (started) return
        started = true
        viewModelScope.launch {
            _state.update {
                it.copy(
                    running = true,
                    success = false,
                    showReboot = MagiskRuntimeEngine.snapshot().isRooted,
                    title = titleFor(action, uri)
                )
            }
            if (MagiskRuntimeEngine.requiresRoot(action)) {
                val hasRoot = MagiskRuntimeEngine.hasRootShell()
                if (!hasRoot) {
                    terminal.addLine("! ${AppContext.getString(CoreR.string.root_required_operation)}")
                    _state.update { it.copy(running = false, success = false, showReboot = false) }
                    _messages.emit(uiText(CoreR.string.failure))
                    return@launch
                }
            }
            val result = when (action) {
                Const.Value.FLASH_ZIP -> if (uri == null) false else flashZipWithLogs(uri)
                Const.Value.FLASH_MULTIPLE_ZIPS -> if (uri == null) false else {
                    flashMultipleZipsWithLogs(uri.toString())
                }
                Const.Value.UNINSTALL -> {
                    _state.update { it.copy(showReboot = false) }
                    MagiskInstaller.Uninstall(terminal.console, terminal.logs).exec()
                }

                Const.Value.FLASH_MAGISK -> if (MagiskRuntimeEngine.snapshot().isEmulator) {
                    MagiskInstaller.Emulator(terminal.console, terminal.logs).exec()
                } else {
                    MagiskInstaller.Direct(terminal.console, terminal.logs).exec()
                }

                Const.Value.FLASH_INACTIVE_SLOT -> {
                    _state.update { it.copy(showReboot = false) }
                    MagiskInstaller.SecondSlot(terminal.console, terminal.logs).exec()
                }

                Const.Value.PATCH_FILE -> if (uri == null) false else {
                    _state.update { it.copy(showReboot = false) }
                    MagiskInstaller.Patch(uri, terminal.console, terminal.logs).exec()
                }

                else -> false
            }
            terminal.sync()
            _state.update { it.copy(running = false, success = result) }
        }
    }

    private suspend fun flashMultipleZipsWithLogs(urisString: String): Boolean {
        val rawUrls = FlashPayloadStore.takeUrls(urisString) ?: urisString
            .lineSequence()
            .flatMap { it.split(",") }
            .filter { it.isNotBlank() }
            .toList()
        val uris = rawUrls.map { Uri.parse(it) }
        var allSuccess = uris.isNotEmpty()
        uris.forEachIndexed { index, uri ->
            terminal.addLine("--- ${AppContext.getString(CoreR.string.download)}: ${index + 1} / ${uris.size} ---")
            val success = flashZipWithLogs(uri)
            if (!success) {
                terminal.addLine(
                    errorLine(CoreR.string.module_update_failed, uri.flashDisplayName())
                )
                allSuccess = false
            }
        }
        return allSuccess
    }

    private fun titleFor(action: String, uri: Uri?): String {
        return when (action) {
            Const.Value.FLASH_ZIP,
            Const.Value.PATCH_FILE -> uri?.flashDisplayName()?.takeIf { it.isNotBlank() }
                ?: AppContext.getString(CoreR.string.flash_screen_title)

            Const.Value.FLASH_MULTIPLE_ZIPS -> AppContext.getString(CoreR.string.module_updates_title)
            Const.Value.FLASH_MAGISK -> AppContext.getString(CoreR.string.home_core_title)
            Const.Value.FLASH_INACTIVE_SLOT -> AppContext.getString(CoreR.string.install_inactive_slot)
            Const.Value.UNINSTALL -> AppContext.getString(CoreR.string.uninstall)
            else -> AppContext.getString(CoreR.string.flash_screen_title)
        }
    }

    private suspend fun flashZipWithLogs(uri: Uri): Boolean {
        val installDir = File(AppContext.cacheDir, "flash")
        val prep: Pair<Int?, Triple<File, File, String>?> = withContext(Dispatchers.IO) {
            try {
                installDir.deleteRecursively()
                installDir.mkdirs()

                val zipFile = when (uri.scheme) {
                    "file" -> uri.toFile()
                    "http", "https" -> {
                        File(installDir, "install.zip").also {
                            try {
                                downloadUrlToFile(uri, it, uri.flashDisplayName())
                            } catch (e: IOException) {
                                return@withContext CoreR.string.flash_copy_to_cache_failed to null
                            }
                        }
                    }

                    else -> {
                        File(installDir, "install.zip").also {
                            try {
                                uri.inputStream().writeTo(it)
                            } catch (e: IOException) {
                                val message = if (e is FileNotFoundException) {
                                    CoreR.string.flash_invalid_uri
                                } else {
                                    CoreR.string.flash_copy_to_cache_failed
                                }
                                return@withContext message to null
                            }
                        }
                    }
                }

                val binary = File(installDir, "update-binary")
                AppContext.assets.open("module_installer.sh").use { it.writeTo(binary) }

                val name = uri.flashDisplayName()
                null to Triple(installDir, zipFile, name)
            } catch (e: Exception) {
                Timber.e(e)
                CoreR.string.flash_extract_failed to null
            }
        }

        val (errorRes, prepResult) = prep
        if (prepResult == null) {
            terminal.addLine(errorLine(errorRes ?: CoreR.string.flash_installation_failed))
            return false
        }

        val (dir, zipFile, displayName) = prepResult
        terminal.addLine(AppContext.getString(CoreR.string.flash_installing_file, displayName))

        val success = withContext(Dispatchers.IO) {
            Shell.cmd("sh $dir/update-binary dummy 1 '${zipFile.absolutePath}'")
                .to(terminal.console, terminal.logs).exec().isSuccess
        }
        if (!success) terminal.addLine(errorLine(CoreR.string.flash_installation_failed))

        Shell.cmd("cd /", "rm -rf $dir ${Const.TMPDIR}").submit()
        return success
    }

    private suspend fun downloadUrlToFile(uri: Uri, file: File, title: String) {
        val postNotification = canPostProgressNotifications()
        val notificationId = if (postNotification) Notifications.nextId() else -1
        val notification = if (postNotification) Notifications.startProgress(title) else null

        fun post(editor: (NotificationBuilder) -> Unit = {}) {
            val builder = notification ?: return
            editor(builder)
            runCatching {
                Notifications.mgr.notify(notificationId, builder.build())
            }
        }

        post()
        try {
            val connection = URL(uri.toString()).openConnection()
            val max = connection.contentLengthLong
            connection.getInputStream().use { raw ->
                ProgressInputStream(raw) { downloaded ->
                    post { builder ->
                        updateDownloadProgress(builder, downloaded, max)
                    }
                }.use { input ->
                    input.writeTo(file)
                }
            }
            post { builder ->
                builder
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentText(AppContext.getString(CoreR.string.download_complete))
                    .setProgress(100, 100, false)
                    .setOngoing(false)
                    .setAutoCancel(true)
                Notifications.run { builder.applyAndroid16ProgressStyle(100) }
            }
        } catch (e: IOException) {
            post { builder ->
                builder
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentText(AppContext.getString(CoreR.string.download_file_error))
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(true)
                Notifications.run { builder.clearAndroid16ProgressStyle() }
            }
            throw e
        }
    }

    private fun updateDownloadProgress(
        builder: NotificationBuilder,
        downloaded: Long,
        max: Long
    ) {
        val progress = downloaded.toFloat() / 1048576
        if (max > 0) {
            val total = max.toFloat() / 1048576
            val percent = ((downloaded.toDouble() / max) * 100).toInt().coerceIn(0, 100)
            builder
                .setProgress(100, percent, false)
                .setContentText("%.2f / %.2f MB".format(progress, total))
            Notifications.run { builder.applyAndroid16ProgressStyle(percent) }
        } else {
            builder
                .setProgress(0, 0, true)
                .setContentText("%.2f MB / ??".format(progress))
            Notifications.run { builder.applyAndroid16ProgressStyle(null) }
        }
    }

    private fun canPostProgressNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                AppContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun saveLog() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val name = "magisk_install_log_%s.log".format(
                    System.currentTimeMillis().toTime(timeFormatStandard)
                )
                val file = MediaStoreUtils.getFile(name)
                file.uri.outputStream().bufferedWriter().use(terminal::writeTo)
                file.toString()
            }.onSuccess { path ->
                _messages.emit(uiText(CoreR.string.saved_to_path, path))
            }.onFailure {
                _messages.emit(uiText(CoreR.string.failure))
            }
        }
    }

    fun rebootNow() {
        if (_state.value.running) return
        viewModelScope.launch {
            val runtime = MagiskRuntimeEngine.snapshot()
            if (!runtime.isRooted || !MagiskRuntimeEngine.hasRootShell()) {
                _messages.emit(uiText(CoreR.string.root_required_operation))
                return@launch
            }
            _effects.emit(UiEffect.Reboot())
        }
    }

    private fun errorLine(@StringRes res: Int, vararg args: Any): String {
        return "! ${AppContext.getString(res, *args)}"
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return FlashViewModel() as T
            }
        }
    }
}

internal fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

private fun Uri.flashDisplayName(): String {
    return when (scheme) {
        "file", "content" -> runCatching { displayName }.getOrDefault(toString())
        "http", "https" -> lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: host
            ?: toString()

        else -> toString()
    }
}
