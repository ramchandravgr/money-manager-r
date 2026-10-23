package com.mmr.core.categorization

import com.mmr.core.database.dao.CategorizationRuleDao
import com.mmr.core.model.enums.CategorizationMatchType
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CategorizationRuleMatch(
    val categoryName: String,
    val subcategoryName: String?,
)

@Singleton
class CategorizationRuleEngine
    @Inject
    constructor(
        private val categorizationRuleDao: CategorizationRuleDao,
    ) {
        suspend fun resolveCategoryName(
            merchant: String?,
            note: String?,
            sender: String?,
            smsBody: String?,
        ): String? = resolveMatch(merchant, note, sender, smsBody)?.categoryName

        suspend fun resolveMatch(
            merchant: String?,
            note: String?,
            sender: String?,
            smsBody: String?,
        ): CategorizationRuleMatch? {
            val rules = categorizationRuleDao.getEnabledOrdered()
            val merchantLower = merchant?.lowercase(Locale.getDefault()) ?: ""
            val noteLower = note?.lowercase(Locale.getDefault()) ?: ""
            val senderLower = sender?.lowercase(Locale.getDefault()) ?: ""
            val bodyLower = smsBody?.lowercase(Locale.getDefault()) ?: ""

            for (rule in rules) {
                val pattern = rule.pattern.lowercase(Locale.getDefault()).trim()
                if (pattern.isEmpty()) continue
                val matched =
                    when (rule.matchType) {
                        CategorizationMatchType.MERCHANT_OR_NOTE_CONTAINS ->
                            merchantLower.contains(pattern) || noteLower.contains(pattern)
                        CategorizationMatchType.SMS_BODY_CONTAINS ->
                            bodyLower.contains(pattern)
                        CategorizationMatchType.SENDER_CONTAINS ->
                            senderLower.contains(pattern)
                    }
                if (matched) {
                    return CategorizationRuleMatch(
                        categoryName = rule.categoryName,
                        subcategoryName = rule.subcategoryName?.trim()?.takeIf { it.isNotEmpty() },
                    )
                }
            }
            return null
        }
    }
