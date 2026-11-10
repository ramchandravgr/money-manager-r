package com.rrparallelstudio.miapp.ui.theme

import androidx.compose.ui.graphics.Color

/** Fill, stroke, and label for a selected/unselected chip or compact control. */
data class MmrChipPaint(
    val fill: Color,
    val border: Color,
    val content: Color,
)

object MmrChipPaints {
    /** Solid accent when selected (Home filters, form chips). */
    fun accentFilled(selected: Boolean): MmrChipPaint {
        val active = MmrBrandColors.Active
        return if (selected) {
            MmrChipPaint(fill = active, border = active, content = active.mmrContrastingContent())
        } else {
            MmrChipPaint(
                fill = MmrBrandColors.filterChipUnselectedFill(),
                border = MmrBrandColors.filterChipUnselectedBorder(),
                content = MmrBrandColors.filterChipUnselectedText(),
            )
        }
    }

    /** Card-style chip: white unselected, accent selected (transaction form). */
    fun accentOnCard(selected: Boolean): MmrChipPaint {
        val active = MmrBrandColors.Active
        return if (selected) {
            MmrChipPaint(fill = active, border = active, content = active.mmrContrastingContent())
        } else {
            MmrChipPaint(
                fill = MmrBrandColors.SurfaceWhite,
                border = MmrBrandColors.TransactionListBorder,
                content = MmrBrandColors.Reversal,
            )
        }
    }

    /**
     * Same as [accentOnCard], but unselected label uses [MmrBrandColors.formCardPrimaryText] so chips read on white
     * [MmrBrandColors.SurfaceWhite] when the active palette is a **dark appearance** (where [MmrBrandColors.Reversal]
     * is a light tint).
     */
    fun accentOnWhiteGroupedCard(selected: Boolean): MmrChipPaint {
        val active = MmrBrandColors.Active
        val unselFill = MmrBrandColors.SurfaceWhite
        return if (selected) {
            MmrChipPaint(fill = active, border = active, content = active.mmrContrastingContent())
        } else {
            MmrChipPaint(
                fill = unselFill,
                border = MmrBrandColors.TransactionListBorder,
                content = MmrBrandColors.formCardPrimaryText(),
            )
        }
    }

    /** Selected fill is a category/user color; label contrast follows that fill. */
    fun colorFilled(
        selected: Boolean,
        selectedFill: Color,
    ): MmrChipPaint =
        if (selected) {
            MmrChipPaint(
                fill = selectedFill,
                border = selectedFill,
                content = selectedFill.mmrContrastingContent(),
            )
        } else {
            MmrChipPaint(
                fill = MmrBrandColors.ListBackground,
                border = MmrBrandColors.ListBackground,
                content = MmrBrandColors.TextStrong,
            )
        }

    /** Light accent wash when selected (search period quick picks). */
    fun accentTinted(selected: Boolean): MmrChipPaint {
        val active = MmrBrandColors.Active
        return if (selected) {
            MmrChipPaint(
                fill = active.copy(alpha = 0.12f),
                border = active,
                content = active,
            )
        } else if (MmrBrandColors.canvasIsDark()) {
            accentFilled(false)
        } else {
            MmrChipPaint(
                fill = MmrBrandColors.SurfaceWhite,
                border = MmrBrandColors.BorderMain,
                content = MmrBrandColors.TextStrong,
            )
        }
    }

    fun rowBackground(
        selected: Boolean,
        unselected: Color = MmrBrandColors.SurfaceWhite,
    ): Color = if (selected) MmrBrandColors.RowSelectionFill else unselected
}
