package com.topjohnwu.magisk.core.tasks

import java.io.File
import java.io.IOException

internal enum class MagiskTarImageKind {
    BOOT,
    INIT_BOOT,
    RECOVERY
}

internal data class MagiskTarImageEntry(
    val kind: MagiskTarImageKind,
    val outputName: String
)

/**
 * Restricts image extraction from user-provided TAR archives to known, flat file names.
 */
internal object MagiskTarEntryPolicy {

    fun resolveImage(
        extractionDir: File,
        entryName: String,
        recoveryMode: Boolean
    ): MagiskTarImageEntry? {
        val entry = when {
            isExactImage(entryName, "boot.img") ->
                MagiskTarImageEntry(MagiskTarImageKind.BOOT, "boot.img")
            isExactImage(entryName, "init_boot.img") ->
                MagiskTarImageEntry(MagiskTarImageKind.INIT_BOOT, "init_boot.img")
            isExactImage(entryName, "recovery.img") ->
                MagiskTarImageEntry(MagiskTarImageKind.RECOVERY, "recovery.img")
                    .takeIf { recoveryMode }
            else -> null
        } ?: return null

        return entry.takeIf { isDirectChild(extractionDir, it.outputName) }
    }

    fun isExactImage(entryName: String, imageName: String): Boolean =
        entryName == imageName || entryName == "$imageName.lz4"

    private fun isDirectChild(directory: File, childName: String): Boolean = try {
        val canonicalDirectory = directory.canonicalFile
        val canonicalChild = File(canonicalDirectory, childName).canonicalFile
        canonicalChild.parentFile == canonicalDirectory
    } catch (_: IOException) {
        false
    }
}
