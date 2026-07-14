package com.topjohnwu.magisk.viewmodel.install

import android.net.Uri
import com.topjohnwu.magisk.arch.BaseViewModel
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.topjohnwu.magisk.core.R as CoreR

class InstallViewModel : BaseViewModel() {

    enum class Method { NONE, PATCH, DIRECT, INACTIVE_SLOT }

    data class UiState(
        val step: Int = 0,
        val method: Method = Method.NONE,
        val patchUri: Uri? = null,
        val requestFilePicker: Boolean = false,
        val showConfirm: Boolean = false,
    )

    private val runtime = MagiskRuntimeEngine.snapshot()
    val isRooted get() = runtime.isRooted
    val skipOptions = runtime.shouldSkipInstallOptions
    val noSecondSlot = !runtime.canInstallToInactiveSlot

    private val _uiState = MutableStateFlow(UiState(step = if (skipOptions) 1 else 0))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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

            Method.DIRECT, Method.INACTIVE_SLOT -> {
                _uiState.update { it.copy(showConfirm = true) }
            }

            else -> {}
        }
    }

    fun onFilePickerConsumed() {
        _uiState.update { it.copy(requestFilePicker = false) }
    }

    fun onConfirmDismissed() {
        _uiState.update { it.copy(showConfirm = false) }
    }

    fun onPatchFileSelected(uri: Uri) {
        _uiState.update { it.copy(patchUri = uri, showConfirm = true) }
    }

    fun install() {
        _uiState.update { it.copy(showConfirm = true) }
    }

    fun executeInstall() {
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

}
