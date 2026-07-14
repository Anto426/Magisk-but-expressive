package com.topjohnwu.magisk.core.repository

import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.data.RawUrl
import com.topjohnwu.magisk.core.model.UpdateInfo
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.HttpException
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

internal const val MAX_CHANGELOG_BYTES = 512 * 1024

class NetworkService(
    private val raw: RawUrl,
) {
    suspend fun fetchUpdate(): UpdateInfo? = try {
        fetchCustomUpdate(Config.updateChannelUrl)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Unable to fetch app update metadata")
        null
    }

    private suspend fun fetchCustomUpdate(url: String): UpdateInfo {
        val info = raw.fetchUpdateJson(url).resolve()
        if (info.note.isBlank()) return info
        val changelog = try {
            fetchString(info.note)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Unable to fetch update changelog")
            ""
        }
        return info.copy(note = changelog)
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
    suspend fun fetchString(url: String) = wrap {
        raw.fetchString(url).use(ResponseBody::readChangelogUtf8)
    }
    suspend fun fetchModuleJson(url: String) = wrap { raw.fetchModuleJson(url) }

    private val Config.updateChannelUrl: String
        get() = when (updateChannel) {
            Config.Value.CUSTOM_CHANNEL -> customChannelUrl
            else -> Config.MBE_CHANNEL_URL
        }

}

private fun ResponseBody.readChangelogUtf8(): String {
    val declaredLength = contentLength()
    if (declaredLength > MAX_CHANGELOG_BYTES) {
        throw IOException("Changelog exceeds $MAX_CHANGELOG_BYTES bytes")
    }

    val initialSize = declaredLength
        .takeIf { it in 1..MAX_CHANGELOG_BYTES.toLong() }
        ?.toInt()
        ?: 8 * 1024
    val output = ByteArrayOutputStream(initialSize)
    byteStream().use { input ->
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_CHANGELOG_BYTES) {
                throw IOException("Changelog exceeds $MAX_CHANGELOG_BYTES bytes")
            }
            output.write(buffer, 0, read)
        }
    }
    return output.toString(StandardCharsets.UTF_8.name())
}
