package com.topjohnwu.magisk.core.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MagiskTarEntryPolicyTest {

    private val extractionDir = File("build/test-tar-extraction")

    @Test
    fun `accepts only exact supported image basenames`() {
        assertImage("boot.img", MagiskTarImageKind.BOOT, "boot.img")
        assertImage("boot.img.lz4", MagiskTarImageKind.BOOT, "boot.img")
        assertImage("init_boot.img", MagiskTarImageKind.INIT_BOOT, "init_boot.img")
        assertImage("init_boot.img.lz4", MagiskTarImageKind.INIT_BOOT, "init_boot.img")
        assertImage("recovery.img", MagiskTarImageKind.RECOVERY, "recovery.img")
        assertImage("recovery.img.lz4", MagiskTarImageKind.RECOVERY, "recovery.img")
    }

    @Test
    fun `rejects archive paths and deceptive image names`() {
        val invalidNames = listOf(
            "../boot.img",
            "images/boot.img",
            "images\\boot.img",
            "/boot.img",
            "boot.img/../../outside",
            "boot.img.backup",
            "prefix-boot.img",
            "recovery.img/../boot.img"
        )

        invalidNames.forEach { name ->
            assertNull(
                name,
                MagiskTarEntryPolicy.resolveImage(extractionDir, name, recoveryMode = true)
            )
        }
    }

    @Test
    fun `accepts recovery image only in recovery mode`() {
        assertNull(
            MagiskTarEntryPolicy.resolveImage(
                extractionDir,
                "recovery.img.lz4",
                recoveryMode = false
            )
        )
    }

    @Test
    fun `matches metadata images only as flat exact names`() {
        assertTrue(MagiskTarEntryPolicy.isExactImage("vbmeta.img", "vbmeta.img"))
        assertTrue(MagiskTarEntryPolicy.isExactImage("vbmeta.img.lz4", "vbmeta.img"))
        assertFalse(MagiskTarEntryPolicy.isExactImage("../vbmeta.img", "vbmeta.img"))
        assertFalse(MagiskTarEntryPolicy.isExactImage("prefix-vbmeta.img", "vbmeta.img"))
        assertFalse(MagiskTarEntryPolicy.isExactImage("userdata.img/../boot.img", "userdata.img"))
    }

    private fun assertImage(
        entryName: String,
        expectedKind: MagiskTarImageKind,
        expectedOutputName: String
    ) {
        val result = MagiskTarEntryPolicy.resolveImage(
            extractionDir,
            entryName,
            recoveryMode = true
        )

        assertEquals(expectedKind, result?.kind)
        assertEquals(expectedOutputName, result?.outputName)
    }
}
