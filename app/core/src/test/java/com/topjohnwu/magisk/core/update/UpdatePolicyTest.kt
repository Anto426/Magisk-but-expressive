package com.topjohnwu.magisk.core.update

import com.topjohnwu.magisk.core.model.ModuleJson
import com.topjohnwu.magisk.core.model.UpdateInfo
import com.topjohnwu.magisk.core.model.module.OnlineModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {

    @Test
    fun `MBE counter exclusively controls client updates`() {
        val update = UpdateInfo(
            version = "next",
            versionCode = 1,
            clientVersionCode = 30701,
            link = "https://example.com/app.apk"
        )

        assertFalse(UpdatePolicy.isAppUpdateAvailable(update, 99))
        assertTrue(UpdatePolicy.isAppUpdateAvailable(update.copy(versionCode = 100), 99))
    }

    @Test
    fun `invalid app URL never exposes an update`() {
        val update = UpdateInfo(version = "next", versionCode = 2, link = "http://example.com/app.apk")

        assertFalse(UpdatePolicy.isAppUpdateAvailable(update, 1))
    }

    @Test
    fun `module fingerprint is stable independent of map order`() {
        val alpha = OnlineModule("alpha", "Alpha", "2", 2, "https://example.com/a.zip", "")
        val beta = OnlineModule("beta", "Beta", "3", 3, "https://example.com/b.zip", "")
        val first = linkedMapOf("alpha" to alpha, "beta" to beta)
        val second = linkedMapOf("beta" to beta, "alpha" to alpha)

        assertEquals(UpdatePolicy.modulesFingerprint(first), UpdatePolicy.modulesFingerprint(second))
    }

    @Test
    fun `app fingerprint changes with the artifact`() {
        val first = UpdateInfo("next", 2, 10, "https://example.com/a.apk", "notes")
        val second = first.copy(link = "https://example.com/b.apk")

        assertNotEquals(UpdatePolicy.appFingerprint(first), UpdatePolicy.appFingerprint(second))
    }

    @Test
    fun `app fingerprint ignores corrected descriptive metadata`() {
        val first = UpdateInfo("old label", 2, 30_700, "https://example.com/app.apk", "old")
        val corrected = first.copy(
            version = "corrected label",
            magiskVersionCode = 30_701,
            note = "corrected notes"
        )

        assertEquals(UpdatePolicy.appFingerprint(first), UpdatePolicy.appFingerprint(corrected))
    }
}
