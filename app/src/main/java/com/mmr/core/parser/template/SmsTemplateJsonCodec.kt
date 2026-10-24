package com.mmr.core.parser.template

import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.SmsInstrument
import com.mmr.core.parser.model.SmsParseIntent
import com.mmr.core.parser.model.SmsPaymentChannel
import com.mmr.core.parser.model.SmsTemplate
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

object SmsTemplateJsonCodec {
    const val MAX_OVERLAY_TEMPLATES = 120
    const val MAX_BODY_REGEX_LENGTH = 4_000

    data class TemplateParseError(
        val index: Int,
        val templateId: String?,
        val message: String,
    )

    data class ParseOverlayResult(
        val templates: List<SmsTemplate>,
        val errors: List<TemplateParseError>,
    ) {
        val ok: Boolean get() = errors.isEmpty()
    }

    fun mergeBuiltInWithOverlay(
        builtIn: List<SmsTemplate>,
        overlay: List<SmsTemplate>,
    ): List<SmsTemplate> {
        val byId = LinkedHashMap<String, SmsTemplate>()
        builtIn.forEach { byId[it.id] = it }
        overlay.forEach { byId[it.id] = it }
        return byId.values.sortedByDescending { it.priority }
    }

    fun parseTemplatesArray(array: JSONArray): List<SmsTemplate> {
        val parsed = mutableListOf<SmsTemplate>()
        for (i in 0 until array.length()) {
            parsed.add(parseTemplate(array.getJSONObject(i)))
        }
        return parsed.sortedByDescending { it.priority }
    }

    fun parseTemplatesRoot(root: JSONObject): List<SmsTemplate> {
        val array = root.optJSONArray("templates") ?: return emptyList()
        return parseTemplatesArray(array)
    }

    /**
     * Validates overlay JSON without mutating storage. Overlay entries with the same [SmsTemplate.id]
     * replace built-in templates after merge; duplicate ids inside the overlay file are rejected.
     */
    fun parseOverlayJson(json: String): ParseOverlayResult {
        val errors = mutableListOf<TemplateParseError>()
        val root =
            runCatching { JSONObject(json.trim()) }
                .getOrElse {
                    return ParseOverlayResult(
                        emptyList(),
                        listOf(TemplateParseError(-1, null, "Invalid JSON: ${it.message}")),
                    )
                }
        val array = root.optJSONArray("templates")
        if (array == null) {
            return ParseOverlayResult(
                emptyList(),
                listOf(TemplateParseError(-1, null, """Root object must contain a "templates" array.""")),
            )
        }
        if (array.length() > MAX_OVERLAY_TEMPLATES) {
            return ParseOverlayResult(
                emptyList(),
                listOf(
                    TemplateParseError(
                        -1,
                        null,
                        "At most $MAX_OVERLAY_TEMPLATES overlay templates allowed (file has ${array.length()}).",
                    ),
                ),
            )
        }
        val parsed = mutableListOf<SmsTemplate>()
        val seenIds = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)
            if (item == null) {
                errors += TemplateParseError(i, null, "Template entry must be an object.")
                continue
            }
            when (val result = parseTemplateOrError(item)) {
                is TemplateItemResult.Ok -> {
                    if (!seenIds.add(result.template.id)) {
                        errors += TemplateParseError(i, result.template.id, "Duplicate template id in overlay file.")
                    } else {
                        parsed.add(result.template)
                    }
                }
                is TemplateItemResult.Err -> errors += TemplateParseError(i, result.id, result.message)
            }
        }
        return if (errors.isNotEmpty()) {
            ParseOverlayResult(emptyList(), errors)
        } else {
            ParseOverlayResult(parsed.sortedByDescending { it.priority }, emptyList())
        }
    }

    fun overlayJsonFromTemplates(templates: List<SmsTemplate>): String {
        val array = JSONArray()
        templates.sortedByDescending { it.priority }.forEach { template ->
            array.put(templateToJson(template))
        }
        return JSONObject().put("templates", array).toString(2)
    }

    private sealed class TemplateItemResult {
        data class Ok(
            val template: SmsTemplate,
        ) : TemplateItemResult()

        data class Err(
            val id: String?,
            val message: String,
        ) : TemplateItemResult()
    }

    private fun parseTemplateOrError(item: JSONObject): TemplateItemResult {
        val id = item.optString("id", "").trim()
        if (id.isEmpty()) {
            return TemplateItemResult.Err(null, """Missing or empty "id".""")
        }
        val bodyRegex = item.optString("bodyRegex", "").trim()
        if (bodyRegex.isEmpty()) {
            return TemplateItemResult.Err(id, """Missing or empty "bodyRegex".""")
        }
        if (bodyRegex.length > MAX_BODY_REGEX_LENGTH) {
            return TemplateItemResult.Err(id, "bodyRegex exceeds $MAX_BODY_REGEX_LENGTH characters.")
        }
        val pattern =
            try {
                Pattern.compile(bodyRegex, Pattern.CASE_INSENSITIVE)
            } catch (e: PatternSyntaxException) {
                return TemplateItemResult.Err(id, "Invalid regex: ${e.description}")
            }
        val transactionType =
            runCatching { TransactionType.valueOf(item.getString("transactionType")) }
                .getOrElse {
                    return TemplateItemResult.Err(id, "Invalid transactionType: ${item.optString("transactionType")}")
                }
        val suffixes = mutableListOf<String>()
        val suffixArray = item.optJSONArray("senderSuffixes")
        if (suffixArray != null) {
            for (i in 0 until suffixArray.length()) {
                suffixArray.optString(i)?.takeIf { it.isNotBlank() }?.let { suffixes.add(it.uppercase()) }
            }
        }
        val channel =
            item
                .optString("paymentChannel", "")
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { SmsPaymentChannel.valueOf(it) }.getOrNull() }
        val instrument =
            item
                .optString("instrument", "")
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { SmsInstrument.valueOf(it) }.getOrNull() }
        val intent =
            runCatching {
                SmsParseIntent.valueOf(item.optString("intent", "SETTLED"))
            }.getOrDefault(SmsParseIntent.SETTLED)
        return TemplateItemResult.Ok(
            SmsTemplate(
                id = id,
                senderSuffixes = suffixes,
                priority = item.optInt("priority", 50),
                bodyPattern = pattern,
                transactionType = transactionType,
                paymentChannel = channel,
                instrument = instrument,
                intent = intent,
                amountGroup = item.optString("amountGroup", "amount"),
                merchantGroup = item.optString("merchantGroup", "merchant").ifBlank { null },
                last4Group = item.optString("last4Group", "last4").ifBlank { null },
                refGroup = item.optString("refGroup", "ref").ifBlank { null },
                balanceGroup = item.optString("balanceGroup", "balance").ifBlank { null },
            ),
        )
    }

    private fun parseTemplate(item: JSONObject): SmsTemplate =
        when (val result = parseTemplateOrError(item)) {
            is TemplateItemResult.Ok -> result.template
            is TemplateItemResult.Err -> throw IllegalArgumentException(result.message)
        }

    private fun templateToJson(template: SmsTemplate): JSONObject =
        JSONObject()
            .put("id", template.id)
            .put(
                "senderSuffixes",
                JSONArray().apply { template.senderSuffixes.forEach { put(it) } },
            ).put("priority", template.priority)
            .put("bodyRegex", template.bodyPattern.pattern())
            .put("transactionType", template.transactionType.name)
            .put("paymentChannel", template.paymentChannel?.name ?: "")
            .put("instrument", template.instrument?.name ?: "")
            .put("intent", template.intent.name)
            .put("amountGroup", template.amountGroup)
            .put("merchantGroup", template.merchantGroup ?: "")
            .put("last4Group", template.last4Group ?: "")
            .put("refGroup", template.refGroup ?: "")
            .put("balanceGroup", template.balanceGroup ?: "")
}
