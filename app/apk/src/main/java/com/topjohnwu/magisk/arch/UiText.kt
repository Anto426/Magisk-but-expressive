package com.topjohnwu.magisk.arch

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    data class Plain(val value: String) : UiText
    data class Resource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText
}

fun UiText.resolve(context: Context): String {
    return when (this) {
        is UiText.Plain -> value
        is UiText.Resource -> context.getString(resId, *args.toTypedArray())
    }
}

fun uiText(value: String): UiText = UiText.Plain(value)

fun uiText(@StringRes resId: Int, vararg args: Any): UiText =
    UiText.Resource(resId, args.toList())
