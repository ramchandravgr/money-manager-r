package com.mmr.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Rounded-rectangle corners (not pill/cylinder). Used by buttons, cards, etc. */
val MmrShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(10.dp),
    )
