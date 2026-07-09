package com.topjohnwu.magisk.viewmodel.install

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.BaseViewModel
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.repository.NetworkService
import com.topjohnwu.magisk.core.update.AppVersion
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import com.topjohnwu.magisk.core.R as CoreR

class InstallViewModel(svc: NetworkService) : BaseViewModel() {

    enum class Method { NONE, PATCH, DIRECT, INACTIVE_SLOT }

    data class UiState(
        val step: Int = 0,
        val method: Method = Method.NONE,
        val notes: String = "",
        val patchUri: Uri? = null,
        val requestFilePicker: Boolean = false,
        val showSecondSlotWarning: Boolean = false,
    )

    private val runtime = MagiskRuntimeEngine.snapshot()
    val isRooted get() = runtime.isRooted
    val skipOptions = runtime.shouldSkipInstallOptions
    val noSecondSlot = !runtime.canInstallToInactiveSlot

    private val _uiState = MutableStateFlow(UiState(step = if (skipOptions) 1 else 0))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val noteFile = File(AppContext.cacheDir, "mbe-${AppVersion.installedCode}.md")
                val noteText = when {
                    noteFile.exists() -> noteFile.readText()
                    else -> {
                        val note = svc.fetchUpdate(AppVersion.installedCode)?.note.orEmpty()
                        if (note.isEmpty()) return@launch
                        noteFile.writeText(note)
                        note
                    }
                }
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(notes = normalizeMarkdown(noteText)) }
                }
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(step = 1) }
    }

    fun selectMethod(method: Method) {
        _uiState.update { it.copy(method = method) }
        when (method) {
            Method.PATCH -> {
                showMessage(CoreR.string.patch_file_msg)
                _uiState.update { it.copy(requestFilePicker = true) }
                sendEffect(UiEffect.RequestFilePicker)
            }

            Method.INACTIVE_SLOT -> {
                _uiState.update { it.copy(showSecondSlotWarning = true) }
            }

            else -> {}
        }
    }

    fun onFilePickerConsumed() {
        _uiState.update { it.copy(requestFilePicker = false) }
    }

    fun onSecondSlotWarningConsumed() {
        _uiState.update { it.copy(showSecondSlotWarning = false) }
    }

    fun onPatchFileSelected(uri: Uri) {
        _uiState.update { it.copy(patchUri = uri) }
        if (_uiState.value.method == Method.PATCH) {
            install()
        }
    }

    fun install() {
        val state = _uiState.value
        val route = when (state.method) {
            Method.PATCH -> AppRoute.Flash(
                action = Const.Value.PATCH_FILE,
                additionalData = state.patchUri?.toString() ?: return
            )

            Method.DIRECT -> AppRoute.Flash(action = Const.Value.FLASH_MAGISK)
            Method.INACTIVE_SLOT -> AppRoute.Flash(action = Const.Value.FLASH_INACTIVE_SLOT)
            Method.NONE -> return
        }
        navigateTo(route)
    }

    val canInstall: Boolean
        get() {
            val state = _uiState.value
            return when (state.method) {
                Method.PATCH -> state.patchUri != null
                Method.DIRECT, Method.INACTIVE_SLOT -> true
                Method.NONE -> false
            }
        }

    private fun normalizeMarkdown(input: String): String {
        return input.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\r\n", "\n").trim()
    }
}
