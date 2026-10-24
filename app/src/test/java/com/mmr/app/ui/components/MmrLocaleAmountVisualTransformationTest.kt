package com.mmr.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MmrLocaleAmountVisualTransformationTest {
    @Test
    fun formatSanitizedAmountForDisplay_groupsIntegerPart() {
        val formatted =
            formatSanitizedAmountForDisplay(
                "230898.50",
                Locale("en", "IN"),
            )
        assertEquals(true, formatted.contains("230,898") || formatted.contains("2,30,898"))
    }
}
