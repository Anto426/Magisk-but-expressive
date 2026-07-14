package com.topjohnwu.magisk.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class MediaStoreUtilsTest {

    @Test
    fun `download directory stays relative`() {
        assertEquals(
            listOf("Magisk", "patched").joinToString(File.separator),
            MediaStoreUtils.normalizeDownloadDirectory("/Magisk\\patched/")
        )
    }

    @Test
    fun `download directory rejects traversal and drive paths`() {
        assertNull(MediaStoreUtils.normalizeDownloadDirectory("../outside"))
        assertNull(MediaStoreUtils.normalizeDownloadDirectory("safe/../../outside"))
        assertNull(MediaStoreUtils.normalizeDownloadDirectory("C:\\outside"))
    }

    @Test
    fun `empty download directory selects Downloads root`() {
        assertEquals("", MediaStoreUtils.normalizeDownloadDirectory(" / "))
    }
}
