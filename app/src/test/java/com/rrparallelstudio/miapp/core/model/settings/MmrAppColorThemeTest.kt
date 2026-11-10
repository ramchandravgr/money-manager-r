package com.rrparallelstudio.miapp.core.model.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MmrAppColorThemeTest {
    @Test
    fun classicCoralMapsToOceanForUi() {
        assertEquals(MmrAppColorTheme.OCEAN, MmrAppColorTheme.CLASSIC_CORAL.forUi())
        assertEquals(MmrAppColorTheme.FOREST, MmrAppColorTheme.FOREST.forUi())
    }

    @Test
    fun pickerOmitsClassicCoral() {
        assertFalse(MmrAppColorTheme.pickerEntries.contains(MmrAppColorTheme.CLASSIC_CORAL))
        assertEquals(9, MmrAppColorTheme.pickerEntries.size)
    }
}
