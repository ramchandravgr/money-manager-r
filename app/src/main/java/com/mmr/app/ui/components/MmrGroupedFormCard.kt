package com.mmr.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrBrandColors

/** Corner radius for floating white field cards (transaction entry + settings sub-screens). */
val MmrGroupedFormCardCorner = 14.dp

private val groupedFormCardShape = RoundedCornerShape(MmrGroupedFormCardCorner)

/**
 * How inset-grouped settings rows are placed in cards:
 * - [Grouped] — one card per section (Settings hub).
 * - [Separated] — one elevated card per row/control (sub-settings and entity forms).
 */
enum class MmrSettingsFieldGrouping {
    Grouped,
    Separated,
}

val LocalMmrSettingsFieldGrouping =
    compositionLocalOf { MmrSettingsFieldGrouping.Separated }

@Composable
fun MmrSettingsFieldGrouping.separatedFieldCards(): Boolean = this == MmrSettingsFieldGrouping.Separated

/** Single white elevated card (transaction fields, settings rows). */
@Composable
fun MmrGroupedFormCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = groupedFormCardShape,
        color = MmrBrandColors.SurfaceWhite,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/** Caption above a separated field card or transaction form group. */
@Composable
fun MmrFormSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MmrBrandColors.TextMedium,
        fontWeight = FontWeight.Medium,
    )
}
