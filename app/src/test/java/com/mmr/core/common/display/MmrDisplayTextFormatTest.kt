package com.mmr.core.common.display

import org.junit.Assert.assertEquals
import org.junit.Test

class MmrDisplayTextFormatTest {
    @Test
    fun capitalizeAfterWordBoundaries_capitalizesAfterSpaceCommaPeriod() {
        assertEquals("Hello, World. Next", MmrDisplayTextFormat.capitalizeAfterWordBoundaries("hello, world. next"))
    }
}
