package com.topjohnwu.magisk.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleSettingTest {

    @Test
    fun `legacy Hebrew locale tag is normalized`() {
        assertEquals("he", normalizeLocaleTag("iw"))
    }

    @Test
    fun `legacy Indonesian locale tag is normalized`() {
        assertEquals("id", normalizeLocaleTag("in"))
    }

    @Test
    fun `regional locale tag is preserved`() {
        assertEquals("pt-BR", normalizeLocaleTag("pt-BR"))
    }
}
