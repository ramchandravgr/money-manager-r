package com.mmr.core.parser.template

import android.content.Context
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsParseIntent
import com.mmr.core.parser.model.SmsTemplate
import com.mmr.core.parser.model.SmsTemplateMatch
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsTemplateRegistry private constructor(
    private val matcher: SmsTemplateMatcher,
    @ApplicationContext private val context: Context?,
    private val overlayStore: SmsTemplateOverlayStore?,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        overlayStore: SmsTemplateOverlayStore,
    ) : this(
        matcher = SmsTemplateMatcher(loadMerged(context, overlayStore)),
        context = context,
        overlayStore = overlayStore,
    )

    fun reload() {
        val ctx = context ?: return
        val store = overlayStore ?: return
        matcher.setTemplates(loadMerged(ctx, store))
    }

    fun templateCount(): Int = matcher.templateCount()

    fun matchSettled(
        sms: RawSmsMessage,
        normalizedBody: String,
    ): SmsTemplateMatch? = matcher.matchSettled(sms, normalizedBody)

    fun matchInformational(
        sms: RawSmsMessage,
        normalizedBody: String,
    ): SmsTemplateMatch? = matcher.matchInformational(sms, normalizedBody)

    fun match(
        sms: RawSmsMessage,
        normalizedBody: String,
    ): SmsTemplateMatch? = matcher.matchSettled(sms, normalizedBody)

    fun matchWithIntent(
        sms: RawSmsMessage,
        normalizedBody: String,
        intent: SmsParseIntent,
    ): SmsTemplateMatch? = matcher.matchWithIntent(sms, normalizedBody, intent)

    companion object {
        private const val ASSET_FILE = "sms_templates.json"

        fun empty(): SmsTemplateRegistry =
            SmsTemplateRegistry(SmsTemplateMatcher(emptyList()), context = null, overlayStore = null)

        fun fromTemplates(templates: List<SmsTemplate>): SmsTemplateRegistry =
            SmsTemplateRegistry(SmsTemplateMatcher(templates), context = null, overlayStore = null)

        fun fromJson(json: String): SmsTemplateRegistry =
            fromTemplates(SmsTemplateJsonCodec.parseTemplatesRoot(JSONObject(json)))

        fun normalizeSenderId(sender: String): String {
            val upper = sender.trim().uppercase()
            val firstDash = upper.indexOf('-')
            return if (firstDash >= 0 && firstDash < upper.length - 1) {
                upper.substring(firstDash + 1)
            } else {
                upper
            }
        }

        fun senderMatches(
            senderKey: String,
            suffixes: List<String>,
        ): Boolean = SmsTemplateMatcher.senderMatches(senderKey, suffixes)

        fun loadBuiltInFromAssets(context: Context): List<SmsTemplate> {
            val text =
                context.assets
                    .open(ASSET_FILE)
                    .bufferedReader()
                    .use { it.readText() }
            return SmsTemplateJsonCodec.parseTemplatesRoot(JSONObject(text))
        }

        fun loadMerged(
            context: Context,
            overlayStore: SmsTemplateOverlayStore,
        ): List<SmsTemplate> {
            val builtIn = loadBuiltInFromAssets(context)
            val overlayJson = overlayStore.readJsonOrNull()
            val overlay =
                if (overlayJson.isNullOrBlank()) {
                    emptyList()
                } else {
                    val parsed = SmsTemplateJsonCodec.parseOverlayJson(overlayJson)
                    if (parsed.ok) parsed.templates else emptyList()
                }
            return SmsTemplateJsonCodec.mergeBuiltInWithOverlay(builtIn, overlay)
        }
    }
}
