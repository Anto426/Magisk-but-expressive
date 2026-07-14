package com.topjohnwu.magisk.core.model

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UpdateJsonParsingTest {

    @Test
    fun `manifest-only deploy uses the installed client schema`() {
        val json = """
            {
              "magisk": {
                "version": "e8a58776-alpha-mbe.4",
                "versionCode": 4,
                "magiskVersionCode": 30700,
                "clientVersionCode": 30700,
                "link": "https://example.com/Magisk.apk",
                "note": "https://example.com/release.md",
                "publishedAt": "2026-07-14",
                "releaseKey": "123456",
                "telegramNotified": false
              }
            }
        """.trimIndent()

        val parsed = Moshi.Builder().build()
            .adapter(UpdateJson::class.java)
            .fromJson(json)

        assertNotNull(parsed)
        val update = parsed!!.resolve()
        assertEquals("e8a58776-alpha-mbe.4", update.version)
        assertEquals(4, update.versionCode)
        assertEquals(30700, update.bundledMagiskVersionCode)
        assertEquals("https://example.com/Magisk.apk", update.link)
    }
}
