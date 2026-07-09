package com.topjohnwu.magisk.core.repository

import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.data.GithubApiServices
import com.topjohnwu.magisk.core.data.RawUrl
import com.topjohnwu.magisk.core.ktx.dateFormat
import com.topjohnwu.magisk.core.model.Release
import com.topjohnwu.magisk.core.model.ReleaseAssets
import com.topjohnwu.magisk.core.model.UpdateInfo
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

class NetworkService(
    private val raw: RawUrl,
    private val api: GithubApiServices,
) {
    suspend fun fetchUpdate() = safe {
        fetchCustomUpdate(Config.updateChannelUrl)
    }

    suspend fun fetchUpdate(version: Int) = safe {
        val info = try {
            fetchCustomUpdate(Config.updateChannelUrl)
        } catch (e: Exception) {
            null
        }
        if (info != null && info.versionCode == version) {
            info
        } else {
            findRelease { it.versionCode == version }.asInfo()
        }
    }

    // Keep going through all release pages until we find a match
    private suspend inline fun findRelease(predicate: (Release) -> Boolean): Release? {
        var page = 1
        while (true) {
            val response = api.fetchReleases(page = page)
            val releases = response.body() ?: throw HttpException(response)
            // Remove all non Magisk releases
            releases.removeAll { !it.isMagiskReleaseTag }
            // Make sure it's sorted correctly
            releases.sortByDescending { it.createdTime }
            releases.find(predicate)?.let { return it }
            if (response.headers()["link"]?.contains("rel=\"next\"", ignoreCase = true) == true) {
                page += 1
            } else {
                return null
            }
        }
    }

    private inline fun Release?.asInfo(
        selector: (ReleaseAssets) -> Boolean = {
            // Default selector picks the non-debug APK
            it.name.run { endsWith(".apk") && !contains("debug") }
        }): UpdateInfo {
        return if (this == null) UpdateInfo()
        else if (tag.startsWith("v")) asPublicInfo(selector)
        else asCanaryInfo(selector)
    }

    private inline fun Release.asPublicInfo(selector: (ReleaseAssets) -> Boolean): UpdateInfo {
        val version = tag.drop(1)
        val date = dateFormat.format(createdTime)
        return UpdateInfo(
            version = version,
            versionCode = versionCode,
            link = assets.find(selector)!!.url,
            note = "## $date $name\n\n$body"
        )
    }

    private inline fun Release.asCanaryInfo(selector: (ReleaseAssets) -> Boolean): UpdateInfo {
        return UpdateInfo(
            version = name.substring(8, 16),
            versionCode = versionCode,
            link = assets.find(selector)!!.url,
            note = "## $name\n\n$body"
        )
    }

    private suspend fun fetchCustomUpdate(url: String): UpdateInfo {
        val info = raw.fetchUpdateJson(url).magisk
        return info.let { it.copy(note = raw.fetchString(it.note)) }
    }

    private inline fun <T> safe(factory: () -> T): T? {
        return try {
            if (Info.isConnected.value == true)
                factory()
            else
                null
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private inline fun <T> wrap(factory: () -> T): T {
        return try {
            factory()
        } catch (e: HttpException) {
            throw IOException(e)
        }
    }

    // Fetch files
    suspend fun fetchFile(url: String) = wrap { raw.fetchFile(url) }
    suspend fun fetchString(url: String) = wrap { raw.fetchString(url) }
    suspend fun fetchModuleJson(url: String) = wrap { raw.fetchModuleJson(url) }

    private val Config.updateChannelUrl: String
        get() = if (updateChannel == Config.Value.CUSTOM_CHANNEL && customChannelUrl.isNotBlank()) {
            customChannelUrl
        } else {
            Config.MBE_CHANNEL_URL
        }

    private val Release.isMagiskReleaseTag: Boolean
        get() = tag.startsWith("v") || tag.startsWith("canary")
}
