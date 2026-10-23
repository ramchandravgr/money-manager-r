package com.mmr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrMmColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackNavigation: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    /** When set with back navigation, shows a text action (e.g. "Cancel") instead of the back icon. */
    navigationLabel: String? = null,
    /** @deprecated Use [useMmHeaderStyle] — kept for call sites; enables MM indigo header when true. */
    usePrimaryContainerColors: Boolean = false,
    /** Indigo header that extends under the status bar (matches Transactions / Home). */
    useMmHeaderStyle: Boolean = false,
    /** Lighter MM header title (15sp, normal weight). Default true when [useMmHeaderStyle]. */
    useMmCompactTitle: Boolean? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val mmStyle = useMmHeaderStyle || usePrimaryContainerColors
    val compactTitle = useMmCompactTitle ?: mmStyle

    val colors: TopAppBarColors =
        if (mmStyle) {
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MmrMmColors.HeaderIcon,
                navigationIconContentColor = MmrMmColors.HeaderIcon,
                actionIconContentColor = MmrMmColors.HeaderIcon,
            )
        } else {
            TopAppBarDefaults.topAppBarColors()
        }

    val appBar: @Composable () -> Unit = {
        TopAppBar(
            modifier = Modifier.then(modifier),
            title = {
                val compact = mmStyle && compactTitle
                Text(
                    text = title,
                    style =
                        if (compact) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                    fontWeight = FontWeight.Normal,
                    color = if (mmStyle) MmrMmColors.HeaderIcon else Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                if (showBackNavigation && onNavigateBack != null) {
                    if (navigationLabel != null) {
                        TextButton(onClick = onNavigateBack) {
                            Text(
                                text = navigationLabel,
                                color =
                                    if (mmStyle) {
                                        MmrMmColors.HeaderIcon
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(MmrDimens.topBarIconButton),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(MmrDimens.iconMd),
                                tint =
                                    if (mmStyle) {
                                        MmrMmColors.HeaderIcon
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                    }
                }
            },
            actions = actions,
            colors = colors,
            windowInsets = if (mmStyle) WindowInsets(0) else TopAppBarDefaults.windowInsets,
        )
    }

    if (mmStyle) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MmrMmColors.Header)
                    .statusBarsPadding(),
        ) {
            appBar()
            HorizontalDivider(color = Color.Black.copy(alpha = 0.12f), thickness = 0.5.dp)
        }
    } else {
        appBar()
    }
}
