package com.mmr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrBrandColors

/**
 * Design 12 footer band — shared by [MmrContentDialog] and [MmrDetailScreenScaffold].
 */
@Composable
fun MmrFooterCtaBar(
    modifier: Modifier = Modifier,
    bandBackground: Color = MmrBrandColors.ListBackground,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
    primaryIsDestructive: Boolean = false,
    primaryTestTag: String? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryEnabled: Boolean = true,
    secondaryTestTag: String? = null,
    textLabel: String? = null,
    onText: (() -> Unit)? = null,
    textEnabled: Boolean = true,
    textIsDestructive: Boolean = false,
    textTestTag: String? = null,
) {
    val hasPrimary = primaryLabel != null && onPrimary != null
    val hasSecondary = secondaryLabel != null && onSecondary != null
    val hasText = textLabel != null && onText != null
    if (!hasPrimary && !hasSecondary && !hasText) return

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(bandBackground)
                    .padding(MmrDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasPrimary) {
                val primaryModifier =
                    Modifier
                        .fillMaxWidth()
                        .then(primaryTestTag?.let { Modifier.testTag(it) } ?: Modifier)
                if (primaryIsDestructive) {
                    MmrDestructivePrimaryActionButton(
                        text = primaryLabel!!,
                        onClick = onPrimary!!,
                        enabled = primaryEnabled,
                        modifier = primaryModifier,
                    )
                } else {
                    MmrPrimaryActionButton(
                        text = primaryLabel!!,
                        onClick = onPrimary!!,
                        enabled = primaryEnabled,
                        modifier = primaryModifier,
                    )
                }
            }
            if (hasSecondary) {
                MmrSecondaryActionButton(
                    text = secondaryLabel!!,
                    onClick = onSecondary!!,
                    enabled = secondaryEnabled,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(secondaryTestTag?.let { Modifier.testTag(it) } ?: Modifier),
                )
            }
            if (hasText) {
                TextButton(
                    onClick = onText!!,
                    enabled = textEnabled,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(textTestTag?.let { Modifier.testTag(it) } ?: Modifier),
                ) {
                    Text(
                        text = textLabel!!,
                        color =
                            if (textIsDestructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MmrBrandColors.TextMedium
                            },
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
