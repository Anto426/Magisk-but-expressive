package com.topjohnwu.magisk

import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class StubApkTest {

    @Test
    fun `complete APK zip is accepted`() {
        val apk = apkWith("AndroidManifest.xml", "classes.dex")
        try {
            StubApk.verifyZip(apk)
        } finally {
            apk.delete()
        }
    }

    @Test
    fun `APK zip without primary dex is rejected`() {
        val apk = apkWith("AndroidManifest.xml")
        try {
            assertThrows(IOException::class.java) {
                StubApk.verifyZip(apk)
            }
        } finally {
            apk.delete()
        }
    }

    @Test
    fun `matching forward MBE version is accepted`() {
        StubApk.verifyMbeVersionCodes(4, 3, 4)
    }

    @Test
    fun `artifact MBE version must match deployment metadata`() {
        assertThrows(IOException::class.java) {
            StubApk.verifyMbeVersionCodes(3, 2, 4)
        }
    }

    @Test
    fun `older signed artifact is rejected as rollback`() {
        assertThrows(IOException::class.java) {
            StubApk.verifyMbeVersionCodes(2, 3, -1)
        }
    }

    private fun apkWith(vararg entries: String): File {
        val file = File.createTempFile("stub-update-", ".apk")
        ZipOutputStream(file.outputStream()).use { output ->
            entries.forEach { name ->
                output.putNextEntry(ZipEntry(name))
                output.write(byteArrayOf(1, 2, 3, 4))
                output.closeEntry()
            }
        }
        return file
    }
}
