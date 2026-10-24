package com.mmr.core.parser.template

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsTemplateOverlayStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val overlayFile = File(context.filesDir, OVERLAY_FILE_NAME)

        fun readJsonOrNull(): String? {
            if (!overlayFile.exists()) return null
            return runCatching { overlayFile.readText() }.getOrNull()
        }

        fun writeJson(json: String) {
            overlayFile.parentFile?.mkdirs()
            overlayFile.writeText(json)
        }

        fun clear() {
            if (overlayFile.exists()) {
                overlayFile.delete()
            }
        }

        fun hasOverlay(): Boolean = overlayFile.exists() && overlayFile.length() > 0L

        companion object {
            const val OVERLAY_FILE_NAME = "sms_templates_overlay.json"
        }
    }
