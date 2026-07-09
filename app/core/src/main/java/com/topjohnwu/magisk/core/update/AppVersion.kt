package com.topjohnwu.magisk.core.update

import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.model.UpdateInfo

object AppVersion {
    val installedName: String get() = BuildConfig.MBE_VERSION_NAME
    val installedCode: Int get() = BuildConfig.MBE_VERSION_CODE

    val installedDisplay: String
        get() = display(installedName, installedCode)

    val installedCodeText: String
        get() = formatCode(installedCode)

    fun isUpdateAvailable(update: UpdateInfo): Boolean {
        return update.versionCode > installedCode
    }

    fun remoteDisplay(update: UpdateInfo): String {
        return display(update.version, update.versionCode)
    }

    fun remoteCodeText(update: UpdateInfo): String {
        return formatCode(update.versionCode)
    }

    private fun display(version: String, code: Int): String {
        return if (code > 0) "$version (${formatCode(code)})" else version
    }

    private fun formatCode(code: Int): String {
        return String.format("%05d", code)
    }
}
