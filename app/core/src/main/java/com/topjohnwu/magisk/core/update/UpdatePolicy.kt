package com.topjohnwu.magisk.core.update

import com.topjohnwu.magisk.core.model.ModuleJson
import com.topjohnwu.magisk.core.model.UpdateInfo
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.core.model.module.OnlineModule
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

internal object UpdatePolicy {

    fun isValidAppMetadata(info: UpdateInfo): Boolean {
        return info.version.isNotBlank() && info.versionCode > 0 && isHttpsUrl(info.link)
    }

    fun isAppUpdateAvailable(
        info: UpdateInfo,
        installedMbeVersionCode: Int
    ): Boolean {
        if (!isValidAppMetadata(info)) return false
        return info.versionCode > installedMbeVersionCode
    }

    fun resolveModuleUpdate(module: LocalModule, metadata: ModuleJson): OnlineModule? {
        if (metadata.versionCode <= module.versionCode || !isHttpsUrl(metadata.zipUrl)) return null
        return OnlineModule(module, metadata)
    }

    fun appFingerprint(info: UpdateInfo): String = fingerprint(
        "app",
        info.versionCode.toString(),
        info.link
    )

    fun modulesFingerprint(updates: Map<String, OnlineModule>): String = fingerprint(
        "modules",
        *updates.toSortedMap().map { (id, update) -> "$id:${update.versionCode}:${update.zipUrl}" }.toTypedArray()
    )

    fun isHttpsUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private fun fingerprint(prefix: String, vararg fields: String): String {
        val input = (listOf(prefix) + fields).joinToString("\u0000")
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(Locale.ROOT, it) }
    }
}
