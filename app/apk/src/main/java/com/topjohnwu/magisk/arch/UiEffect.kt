package com.topjohnwu.magisk.arch

import android.net.Uri
import com.topjohnwu.magisk.navigation.AppRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface UiEffect {
    data class Message(val text: UiText) : UiEffect
    data class Navigate(val route: AppRoute) : UiEffect
    data object NavigateBack : UiEffect
    data object RequestAuthentication : UiEffect
    data object RequestFilePicker : UiEffect
    data object Finish : UiEffect
    data class Reboot(val reason: String = "") : UiEffect
    data class OpenUri(val uri: Uri) : UiEffect
    data class RequestHideApp(val label: String) : UiEffect
    data object RequestRestoreApp : UiEffect
}

class UiEffectEmitter {
    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 64)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    fun tryEmit(effect: UiEffect) {
        _effects.tryEmit(effect)
    }

    suspend fun emit(effect: UiEffect) {
        _effects.emit(effect)
    }
}
