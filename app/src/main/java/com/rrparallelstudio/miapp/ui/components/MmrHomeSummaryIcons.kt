package com.rrparallelstudio.miapp.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Calendar with a filled left-to-right arrow (carry-over from prior period).
 * Matches [docs/design/home-summary-strip-icons](file:../../../../../../../docs/design/home-summary-strip-icons/index.html) option 13.
 */
val MmrCarryOverCalendarArrow: ImageVector
    get() {
        if (_carryOverCalendarArrow != null) {
            return _carryOverCalendarArrow!!
        }
        return ImageVector
            .Builder(
                name = "MmrCarryOverCalendarArrow",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    stroke = null,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(4.4f, 13.6f)
                    horizontalLineToRelative(8.35f)
                    verticalLineToRelative(-2.45f)
                    lineTo(19.6f, 16.4f)
                    lineTo(12.75f, 21.65f)
                    verticalLineToRelative(-2.55f)
                    horizontalLineToRelative(-8.35f)
                    close()
                }
                path(
                    fill = SolidColor(Color.Transparent),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(7.5f, 4.5f)
                    verticalLineToRelative(3.5f)
                    moveTo(16.5f, 4.5f)
                    verticalLineToRelative(3.5f)
                    moveTo(3.5f, 11f)
                    horizontalLineToRelative(17f)
                    moveTo(5.5f, 7f)
                    horizontalLineToRelative(13f)
                    verticalLineToRelative(14f)
                    horizontalLineToRelative(-13f)
                    close()
                }
            }.build()
            .also { _carryOverCalendarArrow = it }
    }

private var _carryOverCalendarArrow: ImageVector? = null

/** Receipt with lines (mockup total option 12 — period net / sum). */
val MmrTotalReceiptSum: ImageVector
    get() {
        if (_totalReceiptSum != null) {
            return _totalReceiptSum!!
        }
        return ImageVector
            .Builder(
                name = "MmrTotalReceiptSum",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                // Filled receipt + even-odd line slots (chip/background shows through) — readable at 16–22 dp.
                path(
                    fill = SolidColor(Color.Black),
                    stroke = null,
                    pathFillType = PathFillType.EvenOdd,
                ) {
                    moveTo(7.2f, 5f)
                    horizontalLineToRelative(9.6f)
                    verticalLineToRelative(13.2f)
                    lineToRelative(-1.6f, -0.85f)
                    lineToRelative(-1.6f, 0.85f)
                    lineToRelative(-1.6f, -0.85f)
                    lineToRelative(-1.6f, 0.85f)
                    lineToRelative(-1.6f, -0.85f)
                    lineToRelative(-1.6f, 0.85f)
                    close()
                    moveTo(9f, 9.15f)
                    horizontalLineToRelative(6f)
                    verticalLineToRelative(1.05f)
                    horizontalLineToRelative(-6f)
                    close()
                    moveTo(9f, 12.1f)
                    horizontalLineToRelative(6f)
                    verticalLineToRelative(1.05f)
                    horizontalLineToRelative(-6f)
                    close()
                    moveTo(9f, 15.05f)
                    horizontalLineToRelative(4f)
                    verticalLineToRelative(1.05f)
                    horizontalLineToRelative(-4f)
                    close()
                }
            }.build()
            .also { _totalReceiptSum = it }
    }

private var _totalReceiptSum: ImageVector? = null
