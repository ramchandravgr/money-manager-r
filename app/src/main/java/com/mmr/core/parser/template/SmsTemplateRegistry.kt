package com.mmr.core.parser.template

import android.content.Context
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsInstrument
import com.mmr.core.parser.model.SmsParseIntent
import com.mmr.core.parser.model.SmsPaymentChannel
import com.mmr.core.parser.model.SmsTemplate
import com.mmr.core.parser.model.SmsTemplateMatch
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsTemplateRegistry private constructor(
    private val templates: List<SmsTemplate>
) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(loadFromAssets(context))

    fun matchSettled(sms: RawSmsMessage, normalizedBody: String): SmsTemplateMatch? =
        matchByIntent(sms, normalizedBody, SmsParseIntent.SETTLED)

    fun matchInformational(sms: RawSmsMessage, normalizedBody: String): SmsTemplateMatch? {
        val senderKey = normalizeSenderId(sms.sender)
        for (template in templates) {
            if (template.intent == SmsParseIntent.SETTLED) continue
            if (!senderMatches(senderKey, template.senderSuffixes)) continue
            val matcher = template.bodyPattern.matcher(normalizedBody)
            if (!matcher.find()) continue
            buildMatch(template, matcher)?.let { return it }
        }
        return null
    }

    /** Settled-transaction templates only (phase 2 API). */
    fun match(sms: RawSmsMessage, normalizedBody: String): SmsTemplateMatch? =
        matchSettled(sms, normalizedBody)

    fun matchWithIntent(
        sms: RawSmsMessage,
        normalizedBody: String,
        intent: SmsParseIntent
    ): SmsTemplateMatch? = matchByIntent(sms, normalizedBody, intent)

    private fun matchByIntent(
        sms: RawSmsMessage,
        normalizedBody: String,
        intent: SmsParseIntent
    ): SmsTemplateMatch? {
        val senderKey = normalizeSenderId(sms.sender)
        for (template in templates) {
            if (template.intent != intent) continue
            if (!senderMatches(senderKey, template.senderSuffixes)) continue
            val matcher = template.bodyPattern.matcher(normalizedBody)
            if (!matcher.find()) continue
            buildMatch(template, matcher)?.let { return it }
        }
        return null
    }

    private fun buildMatch(template: SmsTemplate, matcher: java.util.regex.Matcher): SmsTemplateMatch? {
        val amount = SmsTemplateFieldMapper.parseAmount(
            SmsTemplateFieldMapper.groupValue(matcher, template.amountGroup)
        ) ?: return null
        val merchant = SmsTemplateFieldMapper.prettifyMerchant(
            SmsTemplateFieldMapper.groupValue(matcher, template.merchantGroup)
        )
        val last4 = SmsTemplateFieldMapper.groupValue(matcher, template.last4Group)
        val ref = SmsTemplateFieldMapper.groupValue(matcher, template.refGroup)
        val balance = SmsTemplateFieldMapper.parseAmount(
            SmsTemplateFieldMapper.groupValue(matcher, template.balanceGroup)
        )
        return SmsTemplateMatch(
            template = template,
            amount = amount,
            merchant = merchant,
            accountLastDigits = last4?.takeIf { it.length == 4 },
            referenceNumber = ref,
            balanceAfter = balance
        )
    }

    companion object {
        private const val ASSET_FILE = "sms_templates.json"

        fun empty(): SmsTemplateRegistry = SmsTemplateRegistry(emptyList())

        fun fromTemplates(templates: List<SmsTemplate>): SmsTemplateRegistry =
            SmsTemplateRegistry(templates.sortedByDescending { it.priority })

        fun fromJson(json: String): SmsTemplateRegistry =
            SmsTemplateRegistry(parseTemplates(JSONObject(json)))

        fun normalizeSenderId(sender: String): String {
            val upper = sender.trim().uppercase()
            val firstDash = upper.indexOf('-')
            return if (firstDash >= 0 && firstDash < upper.length - 1) {
                upper.substring(firstDash + 1)
            } else {
                upper
            }
        }

        fun senderMatches(senderKey: String, suffixes: List<String>): Boolean {
            if (suffixes.isEmpty()) return true
            return suffixes.any { suffix ->
                senderKey == suffix || senderKey.endsWith(suffix) || senderKey.contains(suffix)
            }
        }

        fun loadFromAssets(context: Context): List<SmsTemplate> {
            val text = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
            return parseTemplates(JSONObject(text))
        }

        fun parseTemplates(root: JSONObject): List<SmsTemplate> {
            val array = root.optJSONArray("templates") ?: return emptyList()
            val parsed = mutableListOf<SmsTemplate>()
            for (i in 0 until array.length()) {
                parsed.add(parseTemplate(array.getJSONObject(i)))
            }
            return parsed.sortedByDescending { it.priority }
        }

        private fun parseTemplate(item: JSONObject): SmsTemplate {
            val suffixes = mutableListOf<String>()
            val suffixArray = item.optJSONArray("senderSuffixes")
            if (suffixArray != null) {
                for (i in 0 until suffixArray.length()) {
                    suffixArray.optString(i)?.takeIf { it.isNotBlank() }?.let { suffixes.add(it.uppercase()) }
                }
            }
            val type = TransactionType.valueOf(item.getString("transactionType"))
            val channel = item.optString("paymentChannel", "")
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { SmsPaymentChannel.valueOf(it) }.getOrNull() }
            val instrument = item.optString("instrument", "")
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { SmsInstrument.valueOf(it) }.getOrNull() }
            val intent = runCatching {
                SmsParseIntent.valueOf(item.optString("intent", "SETTLED"))
            }.getOrDefault(SmsParseIntent.SETTLED)
            return SmsTemplate(
                id = item.getString("id"),
                senderSuffixes = suffixes,
                priority = item.optInt("priority", 50),
                bodyPattern = Pattern.compile(item.getString("bodyRegex"), Pattern.CASE_INSENSITIVE),
                transactionType = type,
                paymentChannel = channel,
                instrument = instrument,
                intent = intent,
                amountGroup = item.optString("amountGroup", "amount"),
                merchantGroup = item.optString("merchantGroup", "merchant").ifBlank { null },
                last4Group = item.optString("last4Group", "last4").ifBlank { null },
                refGroup = item.optString("refGroup", "ref").ifBlank { null },
                balanceGroup = item.optString("balanceGroup", "balance").ifBlank { null }
            )
        }
    }
}
