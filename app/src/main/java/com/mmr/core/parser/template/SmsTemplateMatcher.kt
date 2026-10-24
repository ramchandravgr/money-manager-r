package com.mmr.core.parser.template

import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsParseIntent
import com.mmr.core.parser.model.SmsTemplate
import com.mmr.core.parser.model.SmsTemplateMatch

internal class SmsTemplateMatcher(
    initialTemplates: List<SmsTemplate>,
) {
    @Volatile
    private var templates: List<SmsTemplate> = initialTemplates.sortedByDescending { it.priority }

    fun setTemplates(next: List<SmsTemplate>) {
        templates = next.sortedByDescending { it.priority }
    }

    fun templateCount(): Int = templates.size

    fun matchSettled(
        sms: RawSmsMessage,
        normalizedBody: String,
    ): SmsTemplateMatch? = matchByIntent(sms, normalizedBody, SmsParseIntent.SETTLED)

    fun matchInformational(
        sms: RawSmsMessage,
        normalizedBody: String,
    ): SmsTemplateMatch? {
        val senderKey = SmsTemplateRegistry.normalizeSenderId(sms.sender)
        for (template in templates) {
            if (template.intent == SmsParseIntent.SETTLED) continue
            if (!senderMatches(senderKey, template.senderSuffixes)) continue
            val matcher = template.bodyPattern.matcher(normalizedBody)
            if (!matcher.find()) continue
            buildMatch(template, matcher)?.let { return it }
        }
        return null
    }

    fun matchWithIntent(
        sms: RawSmsMessage,
        normalizedBody: String,
        intent: SmsParseIntent,
    ): SmsTemplateMatch? = matchByIntent(sms, normalizedBody, intent)

    private fun matchByIntent(
        sms: RawSmsMessage,
        normalizedBody: String,
        intent: SmsParseIntent,
    ): SmsTemplateMatch? {
        val senderKey = SmsTemplateRegistry.normalizeSenderId(sms.sender)
        for (template in templates) {
            if (template.intent != intent) continue
            if (!senderMatches(senderKey, template.senderSuffixes)) continue
            val matcher = template.bodyPattern.matcher(normalizedBody)
            if (!matcher.find()) continue
            buildMatch(template, matcher)?.let { return it }
        }
        return null
    }

    private fun buildMatch(
        template: SmsTemplate,
        matcher: java.util.regex.Matcher,
    ): SmsTemplateMatch? {
        val amount =
            SmsTemplateFieldMapper.parseAmount(
                SmsTemplateFieldMapper.groupValue(matcher, template.amountGroup),
            ) ?: return null
        val merchant =
            SmsTemplateFieldMapper.prettifyMerchant(
                SmsTemplateFieldMapper.groupValue(matcher, template.merchantGroup),
            )
        val last4 = SmsTemplateFieldMapper.groupValue(matcher, template.last4Group)
        val ref = SmsTemplateFieldMapper.groupValue(matcher, template.refGroup)
        val balance =
            SmsTemplateFieldMapper.parseAmount(
                SmsTemplateFieldMapper.groupValue(matcher, template.balanceGroup),
            )
        return SmsTemplateMatch(
            template = template,
            amount = amount,
            merchant = merchant,
            accountLastDigits = last4?.takeIf { it.length == 4 },
            referenceNumber = ref,
            balanceAfter = balance,
        )
    }

    companion object {
        fun senderMatches(
            senderKey: String,
            suffixes: List<String>,
        ): Boolean {
            if (suffixes.isEmpty()) return true
            return suffixes.any { suffix ->
                senderKey == suffix || senderKey.endsWith(suffix) || senderKey.contains(suffix)
            }
        }
    }
}
