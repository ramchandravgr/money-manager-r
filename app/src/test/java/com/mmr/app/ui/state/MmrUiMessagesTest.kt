package com.mmr.app.ui.state

import org.junit.Assert.assertEquals
import org.junit.Test

class MmrUiMessagesTest {

    @Test
    fun friendly_usesFallbackWhenMessageBlank() {
        assertEquals(
            MmrUiMessages.GENERIC_LOAD,
            MmrUiMessages.friendly(null, MmrUiMessages.GENERIC_LOAD)
        )
        assertEquals(
            MmrUiMessages.GENERIC_LOAD,
            MmrUiMessages.friendly("   ", MmrUiMessages.GENERIC_LOAD)
        )
    }

    @Test
    fun friendly_trimsAndKeepsTechnicalMessage() {
        assertEquals(
            "Database locked",
            MmrUiMessages.friendly("  Database locked  ", MmrUiMessages.GENERIC_ACTION)
        )
    }
}
