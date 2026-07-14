package com.topjohnwu.magisk.core.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
class UpdateJson(
    val magisk: UpdateInfo? = null,
    val channels: Map<String, UpdateChannel> = emptyMap(),
) {
    /** Legacy/custom feeds use `magisk`; the official upstream uses a channel. */
    fun resolve(channel: String = STABLE_CHANNEL): UpdateInfo {
        return magisk ?: channels[channel]?.release ?: UpdateInfo()
    }

    companion object {
        const val STABLE_CHANNEL = "stable"
    }
}

@JsonClass(generateAdapter = true)
data class UpdateChannel(
    val release: UpdateInfo = UpdateInfo(),
)

@Parcelize
@JsonClass(generateAdapter = true)
data class UpdateInfo(
    val version: String = "",
    val versionCode: Int = -1,
    val clientVersionCode: Int = -1,
    val link: String = "",
    val note: String = "",
    val magiskVersionCode: Int = -1,
) : Parcelable {
    /** Alpha/core code bundled by this client release; not an app-update gate. */
    val bundledMagiskVersionCode: Int
        get() = magiskVersionCode.takeIf { it > 0 } ?: clientVersionCode
}

@JsonClass(generateAdapter = true)
data class ModuleJson(
    val version: String,
    val versionCode: Int,
    val zipUrl: String,
    val changelog: String,
)
