package com.mmr.app.testing

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText

/** Match formatted currency amounts without hard-coding the locale symbol. */
fun hasAmountText(amount: String): SemanticsMatcher = hasText(amount, substring = true)
