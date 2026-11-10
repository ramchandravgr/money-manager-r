package com.rrparallelstudio.miapp.ui.theme

import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme
import org.junit.Assert.assertTrue
import org.junit.Test

class MmrThemeContrastTest {
    @Test
    fun midnightDarkSelectedNavIsNotNearBlack() {
        val palette = MmrThemePresets.resolve(MmrAppColorTheme.MIDNIGHT, MmrThemeAppearance.DARK)
        assertTrue(
            "selected nav should read on a dark bar, luminance=${palette.navIconSelected.mmrRelativeLuminance()}",
            palette.navIconSelected.mmrRelativeLuminance() > 0.2f,
        )
    }

    @Test
    fun lightAccentsUseDarkChipLabel() {
        val ocean = MmrThemePresets.resolve(MmrAppColorTheme.OCEAN, MmrThemeAppearance.LIGHT)
        val mint = MmrThemePresets.resolve(MmrAppColorTheme.MINT, MmrThemeAppearance.LIGHT)
        assertTrue(ocean.active.mmrRelativeLuminance() > 0.45f)
        assertTrue(mint.active.mmrRelativeLuminance() > 0.45f)
        assertTrue(ocean.active.mmrContrastingContent().mmrRelativeLuminance() < 0.2f)
        assertTrue(mint.active.mmrContrastingContent().mmrRelativeLuminance() < 0.2f)
    }

    @Test
    fun midnightAndOceanLightUseDarkCanvasChrome() {
        assertTrue(
            MmrExpressiveThemeProfiles.canvasIsDark(
                MmrAppColorTheme.MIDNIGHT,
                MmrThemeAppearance.LIGHT,
            ),
        )
        assertTrue(
            MmrExpressiveThemeProfiles.canvasIsDark(
                MmrAppColorTheme.OCEAN,
                MmrThemeAppearance.LIGHT,
            ),
        )
        assertTrue(
            MmrExpressiveThemeProfiles.canvasIsDark(
                MmrAppColorTheme.OCEAN,
                MmrThemeAppearance.DARK,
            ),
        )
        assertTrue(
            !MmrExpressiveThemeProfiles.canvasIsDark(
                MmrAppColorTheme.FOREST,
                MmrThemeAppearance.LIGHT,
            ),
        )
        assertTrue(MmrExpressiveThemeProfiles.of(MmrAppColorTheme.OCEAN).photoFillResId != null)
        assertTrue(MmrExpressiveThemeProfiles.of(MmrAppColorTheme.MIDNIGHT).photoFillResId == null)
    }

    @Test
    fun accentOnCardSelectedLabelContrastsLightFill() {
        MmrBrandColorState.palette = MmrThemePresets.resolve(MmrAppColorTheme.MINT, MmrThemeAppearance.LIGHT)
        val selected = MmrChipPaints.accentOnCard(selected = true)
        assertTrue(selected.fill.mmrRelativeLuminance() > 0.45f)
        assertTrue(selected.content.mmrRelativeLuminance() < 0.2f)
        val colorChip = MmrChipPaints.colorFilled(selected = true, selectedFill = selected.fill)
        assertTrue(colorChip.content.mmrRelativeLuminance() < 0.2f)
    }

    @Test
    fun formCardPrimaryTextIsDarkWhenPaletteSurfaceIsLight() {
        MmrBrandColorState.palette = MmrThemePresets.resolve(MmrAppColorTheme.OCEAN, MmrThemeAppearance.LIGHT)
        assertTrue(MmrBrandColorState.palette.surface.mmrRelativeLuminance() > 0.45f)
        val onCard = MmrBrandColors.formCardPrimaryText()
        assertTrue(
            "label on light grouped card should be dark, got luminance=${onCard.mmrRelativeLuminance()}",
            onCard.mmrRelativeLuminance() < 0.2f,
        )
    }

    @Test
    fun accentOnWhiteGroupedCardUnselectedLabelMatchesFormCardPrimaryText() {
        MmrBrandColorState.palette = MmrThemePresets.resolve(MmrAppColorTheme.OCEAN, MmrThemeAppearance.LIGHT)
        val unsel = MmrChipPaints.accentOnWhiteGroupedCard(selected = false)
        assertTrue(unsel.fill.mmrRelativeLuminance() > 0.45f)
        assertTrue(unsel.content.mmrRelativeLuminance() < 0.2f)
    }

    @Test
    fun listAreaCaptionIsReadableOnExpressiveDarkCanvas() {
        val prevTheme = MmrExpressiveThemeState.appColorTheme
        val prevAppearance = MmrExpressiveThemeState.appearance
        try {
            MmrBrandColorState.palette = MmrThemePresets.resolve(MmrAppColorTheme.OCEAN, MmrThemeAppearance.LIGHT)
            MmrExpressiveThemeState.appColorTheme = MmrAppColorTheme.OCEAN
            MmrExpressiveThemeState.appearance = MmrThemeAppearance.LIGHT
            assertTrue(
                "footer/helper on transparent list should be light on dark painted canvas",
                MmrBrandColors.listAreaCaptionText().mmrRelativeLuminance() > 0.45f,
            )
            MmrExpressiveThemeState.appColorTheme = MmrAppColorTheme.FOREST
            assertTrue(MmrBrandColors.listAreaCaptionText() == MmrBrandColors.TextMedium)
        } finally {
            MmrExpressiveThemeState.appColorTheme = prevTheme
            MmrExpressiveThemeState.appearance = prevAppearance
        }
    }
}
