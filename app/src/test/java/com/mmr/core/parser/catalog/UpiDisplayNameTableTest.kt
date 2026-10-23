package com.mmr.core.parser.catalog

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UpiDisplayNameTableTest {
    @Before
    fun setUp() {
        val root =
            JSONObject(
                """
                {
                  "exactVpa": { "zepto@ybl": "Zepto" },
                  "pspHandles": { "ybl": "PhonePe" }
                }
                """.trimIndent(),
            )
        UpiDisplayNameTable.loadFromJsonRoot(root)
    }

    @Test
    fun resolvesKnownVpaAlias() {
        assertEquals("Zepto", UpiDisplayNameTable.resolve("zepto@ybl"))
    }

    @Test
    fun resolvesPspHandleToFriendlyLabel() {
        assertEquals("Merchant", UpiDisplayNameTable.resolve("merchant@ybl"))
    }
}
