package com.mmr.core.parser.template

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsTemplateRegistryTest {

    @Test
    fun normalizeSenderId_keepsMultiPartSuffix() {
        assertEquals("HDFCBK", SmsTemplateRegistry.normalizeSenderId("VM-HDFCBK"))
        assertEquals("BOBCRD-S", SmsTemplateRegistry.normalizeSenderId("AD-BOBCRD-S"))
    }
}
