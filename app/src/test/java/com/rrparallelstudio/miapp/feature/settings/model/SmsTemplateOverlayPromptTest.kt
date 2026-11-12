package com.rrparallelstudio.miapp.feature.settings.model

import org.junit.Assert.assertTrue
import org.junit.Test

class SmsTemplateOverlayPromptTest {
    @Test
    fun prompt_matchesImportSchema() {
        val text = SmsTemplateOverlayPrompt.text
        assertTrue(text.contains("---JSON---"))
        assertTrue(text.contains("\"templates\""))
        assertTrue(text.contains("bodyRegex"))
        assertTrue(text.contains("EXPENSE"))
        assertTrue(text.contains("SETTLED"))
        assertTrue(text.contains("120"))
        assertTrue(text.contains("(?<amount>"))
        assertTrue(text.contains("inr\\\\s"))
        assertTrue(text.contains("Do not use Python"))
    }
}
