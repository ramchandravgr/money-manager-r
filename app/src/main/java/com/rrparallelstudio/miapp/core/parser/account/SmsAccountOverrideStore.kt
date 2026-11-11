package com.rrparallelstudio.miapp.core.parser.account

import com.rrparallelstudio.miapp.core.database.dao.AccountDao
import com.rrparallelstudio.miapp.core.database.dao.SmsAccountOverrideDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionApprovalDao
import com.rrparallelstudio.miapp.core.database.entity.SmsAccountOverrideEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsAccountOverrideStore
    @Inject
    constructor(
        private val overrideDao: SmsAccountOverrideDao,
        private val accountDao: AccountDao,
        private val transactionApprovalDao: TransactionApprovalDao,
    ) {
        suspend fun resolve(
            templateId: String?,
            instrument: String?,
            smsLastFour: String?,
        ): Long? {
            val last4 = normalizeLastFour(smsLastFour) ?: return null
            val template = templateId?.trim().orEmpty()
            val instrumentKey = instrument?.trim().orEmpty()
            val row =
                if (template.isNotEmpty()) {
                    overrideDao.findByTemplate(template, last4)
                } else {
                    null
                } ?: if (instrumentKey.isNotEmpty()) {
                    overrideDao.findByInstrument(instrumentKey, last4)
                } else {
                    null
                } ?: return null
            val account = accountDao.getById(row.accountId) ?: return null
            if (!account.isActive) return null
            return account.id
        }

        suspend fun remember(
            templateId: String?,
            instrument: String?,
            smsLastFour: String?,
            accountId: Long,
        ) {
            if (accountId <= 0L) return
            val last4 = normalizeLastFour(smsLastFour) ?: return
            val template = templateId?.trim().orEmpty()
            val instrumentKey = instrument?.trim().orEmpty()
            if (template.isEmpty() && instrumentKey.isEmpty()) return
            val now = System.currentTimeMillis()
            val existing = overrideDao.findByTemplate(template, last4)
            if (existing == null) {
                overrideDao.insert(
                    SmsAccountOverrideEntity(
                        templateId = template,
                        instrument = instrumentKey,
                        smsLastFour = last4,
                        accountId = accountId,
                        updatedAt = now,
                    ),
                )
            } else if (existing.accountId != accountId || existing.instrument != instrumentKey) {
                overrideDao.update(
                    existing.copy(
                        instrument = instrumentKey.ifEmpty { existing.instrument },
                        accountId = accountId,
                        updatedAt = now,
                    ),
                )
            }
        }

        suspend fun rememberFromEditedSmsTransaction(
            previous: TransactionEntity,
            newAccountId: Long,
        ) {
            if (previous.sourceType != TransactionSourceType.SMS) return
            if (previous.accountId == newAccountId) return
            val approvalId = previous.sourceReferenceId?.toLongOrNull()
            val approval = approvalId?.let { transactionApprovalDao.getById(it) }
            val template = approval?.extractedTemplateId ?: ""
            val instrument = previous.paymentInstrument ?: approval?.extractedInstrument
            val last4 = previous.cardLastFour ?: approval?.extractedCardLastFour
            remember(
                templateId = template,
                instrument = instrument,
                smsLastFour = last4,
                accountId = newAccountId,
            )
        }

        private fun normalizeLastFour(raw: String?): String? =
            raw?.trim()?.takeIf { it.length == 4 && it.all(Char::isDigit) }
    }
