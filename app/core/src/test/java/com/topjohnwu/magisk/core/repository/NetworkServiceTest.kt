package com.topjohnwu.magisk.core.repository

import com.topjohnwu.magisk.core.data.RawUrl
import com.topjohnwu.magisk.core.model.ModuleJson
import com.topjohnwu.magisk.core.model.UpdateJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class NetworkServiceTest {

    @Test
    fun `changelog at limit is accepted`() = runBlocking {
        val markdown = "a".repeat(MAX_CHANGELOG_BYTES)
        val service = NetworkService(FakeRaw { markdown.toResponseBody() })

        assertEquals(markdown, service.fetchString("https://example.com/changelog.md"))
    }

    @Test
    fun `oversized changelog is rejected even without content length`() {
        val bytes = ByteArray(MAX_CHANGELOG_BYTES + 1) { 'a'.code.toByte() }
        val body = object : ResponseBody() {
            override fun contentType() = null
            override fun contentLength() = -1L
            override fun source() = okio.Buffer().write(bytes)
        }
        val service = NetworkService(FakeRaw { body })

        assertThrows(IOException::class.java) {
            runBlocking { service.fetchString("https://example.com/changelog.md") }
        }
    }

    @Test
    fun `cancellation is never converted to an empty changelog`() {
        val service = NetworkService(FakeRaw { throw CancellationException("cancelled") })

        assertThrows(CancellationException::class.java) {
            runBlocking { service.fetchString("https://example.com/changelog.md") }
        }
    }

    private class FakeRaw(
        private val response: suspend () -> ResponseBody
    ) : RawUrl {
        override suspend fun fetchString(url: String): ResponseBody = response()
        override suspend fun fetchFile(url: String): ResponseBody = error("unused")
        override suspend fun fetchModuleJson(url: String): ModuleJson = error("unused")
        override suspend fun fetchUpdateJson(url: String): UpdateJson = error("unused")
    }
}
