package com.mmr.app.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.app.ui.theme.MmrFontFamily

@Composable
fun MmrMainNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    iconPair: MmrNavIconPair,
    label: String,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        if (selected) {
            MmrBrandColors.NavIconSelected
        } else {
            MmrBrandColors.NavIcon
        }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            modifier
                .semantics {
                    role = Role.Tab
                    this.selected = selected
                }.defaultMinSize(minHeight = MmrDimens.touchTargetMin)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, color = contentColor),
                    onClick = onClick,
                ).padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) iconPair.filled else iconPair.outlined,
            contentDescription = label,
            modifier = Modifier.size(MmrDimens.bottomNavIconSize),
            tint = contentColor,
        )
        if (showLabel) {
            Text(
                text = label,
                color = contentColor,
                fontFamily = MmrFontFamily,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                letterSpacing = 0.2.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
