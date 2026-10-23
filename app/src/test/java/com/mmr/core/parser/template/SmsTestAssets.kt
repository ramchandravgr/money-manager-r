package com.mmr.core.parser.template

import java.io.File

internal object SmsTestAssets {
    fun loadTemplatesJson(): String {
        val candidates =
            listOf(
                File("app/src/main/assets/sms_templates.json"),
                File("src/main/assets/sms_templates.json"),
            )
        val file =
            candidates.firstOrNull { it.exists() }
                ?: error("sms_templates.json not found (cwd=${File(".").absolutePath})")
        return file.readText()
    }
}
