package com.mmr.core.data.repository

import com.mmr.core.categorization.CategorizationRuleEngine
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.dao.ApprovalLedgerDao
import com.mmr.core.database.dao.CategoryDao
import com.mmr.core.database.dao.TransactionApprovalDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SmsMessageRecordEntity
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.domain.model.ManualSmsDuplicateMatch
import com.mmr.core.domain.model.ManualSmsDuplicateResolution
import com.mmr.core.domain.model.ManualSmsSubmitResult
import com.mmr.core.domain.model.PendingApprovalWithSms
import com.mmr.core.domain.model.SmsMergeCandidate
import com.mmr.core.domain.model.toManualPriorActionLabel
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.loan.LoanTransactionCoordinator
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.SmsParsedStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.account.SmsAccountResolver
import com.mmr.core.parser.catalog.CardMetadataExtractor
import com.mmr.core.parser.duplicate.SmsMessageHasher
import com.mmr.core.parser.engine.SmsParserEngine
import com.mmr.core.parser.model.ParsedSmsCandidate
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsParseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApprovalRepositoryImpl
    @Inject
    constructor(
        private val transactionApprovalDao: TransactionApprovalDao,
        private val approvalLedgerDao: ApprovalLedgerDao,
        private val categoryDao: CategoryDao,
        private val transactionDao: TransactionDao,
        private val smsAccountResolver: SmsAccountResolver,
        private val smsParserEngine: SmsParserEngine,
        private val loanTransactionCoordinator: LoanTransactionCoordinator,
        private val categorizationRuleEngine: CategorizationRuleEngine,
    ) : ApprovalRepository {
        private suspend fun resolveCategoryId(
            candidate: ParsedSmsCandidate,
            activeCategories: List<CategoryEntity>,
        ): Long? {
            val ruleCategory =
                categorizationRuleEngine.resolveCategoryName(
                    merchant = candidate.merchant,
                    note = candidate.merchant ?: candidate.sender,
                    sender = candidate.sender,
                    smsBody = candidate.rawBody,
                )
            val hint = ruleCategory ?: candidate.categoryHint
            return hint?.let { name ->
                activeCategories.find { it.name.equals(name, ignoreCase = true) }?.id
            }
        }

        private suspend fun resolveSubcategoryIdFromRules(
            approval: TransactionApprovalEntity,
            categoryId: Long?,
            smsBody: String,
        ): Long? {
            if (categoryId == null || categoryId <= 0L) return null
            val match =
                categorizationRuleEngine.resolveMatch(
                    merchant = approval.extractedMerchant,
                    note = approval.extractedNote,
                    sender = approval.extractedSender,
                    smsBody = smsBody,
                ) ?: return null
            val subName = match.subcategoryName ?: return null
            val subs = categoryDao.getSubcategoriesForCategoryOnce(categoryId)
            return subs.find { it.name.equals(subName, ignoreCase = true) }?.id
        }

        override fun getApprovalsByState(state: SmsApprovalState): Flow<List<TransactionApprovalEntity>> =
            transactionApprovalDao.getApprovalsByState(state)

        override fun getApprovalsByStateWithSms(state: SmsApprovalState): Flow<List<PendingApprovalWithSms>> =
            transactionApprovalDao.getApprovalsByStateWithSmsBody(state).map { rows ->
                rows.map { row ->
                    val body =
                        row.smsFullBody?.trim()?.takeIf { it.isNotEmpty() }
                            ?: row.approval.rawBodySnippet.trim()
                    PendingApprovalWithSms(approval = row.approval, smsBody = body)
                }
            }

        override fun getPendingCount(): Flow<Int> = transactionApprovalDao.getPendingCount()

        override suspend fun getApprovalById(id: Long): TransactionApprovalEntity? =
            if (id <= 0L) null else transactionApprovalDao.getById(id)

        override suspend fun insertCandidates(candidates: List<ParsedSmsCandidate>): Int {
            var insertedCount = 0
            val activeCategories = categoryDao.getActiveCategories().firstOrNull() ?: emptyList()

            for (candidate in candidates) {
                if (candidate.messageHash.isBlank()) continue
                if (!MmrValidation.isValidEpochMillis(candidate.timestamp)) continue
                if (candidate.amount <= 0.0 || !candidate.amount.isFinite()) continue
                if (candidate.sender.isBlank()) continue

                val recordEntity =
                    SmsMessageRecordEntity(
                        sender = candidate.sender,
                        messageBodyHash = candidate.messageHash,
                        messageBodySnippet = candidate.rawBody.take(150),
                        messageBodyFull = candidate.rawBody,
                        receivedTime = candidate.timestamp,
                        parsedStatus = SmsParsedStatus.MATCHED,
                        parsedAmount = candidate.amount,
                        parsedDateTime = candidate.timestamp,
                        approvalState = SmsApprovalState.PENDING,
                        rawMessageId = candidate.rawMessageId,
                    )

                val categoryId = resolveCategoryId(candidate, activeCategories)

                val resolvedAccountId = smsAccountResolver.resolveAccountId(candidate)
                val cardNetwork = CardMetadataExtractor.detectNetwork(candidate.rawBody)

                val approvalEntity =
                    TransactionApprovalEntity(
                        smsMessageRecordId = 0,
                        extractedAmount = candidate.amount,
                        extractedType = candidate.type,
                        extractedDateTime = candidate.timestamp,
                        extractedSender = candidate.sender,
                        extractedMerchant = candidate.merchant,
                        extractedCategoryId = categoryId,
                        extractedAccountId = resolvedAccountId,
                        extractedNote = candidate.merchant ?: candidate.sender,
                        confidenceScore = candidate.confidenceScore,
                        userEdited = false,
                        approvalState = SmsApprovalState.PENDING,
                        rawBodySnippet = candidate.rawBody.take(200),
                        extractedTemplateId = candidate.templateId,
                        extractedPaymentChannel = candidate.paymentChannel?.name,
                        extractedInstrument = candidate.instrument?.name,
                        extractedCardLastFour = candidate.accountLastDigits,
                        extractedCardNetwork = cardNetwork,
                    )

                if (approvalLedgerDao.insertPendingCandidate(recordEntity, approvalEntity) != null) {
                    insertedCount++
                }
            }

            return insertedCount
        }

        override suspend fun reconcilePendingWithCurrentParser(): Int {
            val pending =
                transactionApprovalDao
                    .getApprovalsByState(SmsApprovalState.PENDING)
                    .first()
            var cleared = 0
            val now = System.currentTimeMillis()
            for (approval in pending) {
                val sms = approvalLedgerDao.getSmsRecordById(approval.smsMessageRecordId) ?: continue
                val body =
                    sms.messageBodyFull?.trim()?.takeIf { it.isNotEmpty() }
                        ?: sms.messageBodySnippet.trim().takeIf { it.isNotEmpty() }
                        ?: approval.rawBodySnippet.trim()
                if (body.isEmpty()) continue
                val raw =
                    RawSmsMessage(
                        id = sms.rawMessageId ?: approval.id.toString(),
                        sender = approval.extractedSender.ifBlank { sms.sender },
                        body = body,
                        timestamp = approval.extractedDateTime,
                    )
                val stillTransaction = smsParserEngine.parseResult(raw) is SmsParseResult.Transaction
                if (stillTransaction) continue
                val updated =
                    approvalLedgerDao.markApprovalStateIfPending(
                        id = approval.id,
                        state = SmsApprovalState.IGNORED,
                        now = now,
                    )
                if (updated > 0) {
                    approvalLedgerDao.updateSmsRecord(
                        sms.copy(
                            approvalState = SmsApprovalState.IGNORED,
                            parsedStatus = SmsParsedStatus.NON_FINANCIAL,
                        ),
                    )
                    cleared++
                }
            }
            return cleared
        }

        override suspend fun updateApproval(
            id: Long,
            amount: Double,
            type: TransactionType,
            categoryId: Long?,
            accountId: Long?,
            note: String?,
            dateTimeMillis: Long?,
        ) {
            if (id <= 0L) return
            val current = transactionApprovalDao.getById(id) ?: return
            if (current.approvalState != SmsApprovalState.PENDING) return
            MmrValidation.requirePositiveAmount(amount)
            val updated =
                current.copy(
                    extractedAmount = amount,
                    extractedType = type,
                    extractedDateTime = dateTimeMillis ?: current.extractedDateTime,
                    extractedCategoryId = categoryId,
                    extractedAccountId = accountId?.takeIf { it > 0L },
                    extractedNote = note?.trim()?.takeIf { it.isNotEmpty() },
                    userEdited = true,
                    updatedAt = System.currentTimeMillis(),
                )
            transactionApprovalDao.update(updated)
        }

        override suspend fun findMergeCandidatesForApproval(approvalId: Long): List<SmsMergeCandidate> {
            if (approvalId <= 0L) return emptyList()
            val approval = transactionApprovalDao.getById(approvalId) ?: return emptyList()
            if (approval.approvalState != SmsApprovalState.PENDING) return emptyList()
            if (approval.extractedType == TransactionType.TRANSFER) return emptyList()
            val accountId = approval.extractedAccountId ?: return emptyList()
            val windowMillis = 3L * 24L * 60L * 60L * 1000L
            val center = MmrValidation.normalizeEpochMillis(approval.extractedDateTime)
            val candidates =
                transactionDao.findSmsMergeCandidates(
                    type = approval.extractedType,
                    accountId = accountId,
                    amount = approval.extractedAmount,
                    startMillis = center - windowMillis,
                    endMillis = center + windowMillis,
                )
            return candidates.map { txn ->
                SmsMergeCandidate(
                    transactionId = txn.id,
                    amount = txn.amount,
                    dateTimeMillis = txn.dateTime,
                    note = txn.note,
                )
            }
        }

        override suspend fun mergeCandidateWithTransaction(
            approvalId: Long,
            transactionId: Long,
            categoryId: Long?,
            subcategoryId: Long?,
            useLedgerCategory: Boolean,
        ): Long {
            if (approvalId <= 0L || transactionId <= 0L) {
                throw IllegalArgumentException("Pending entry not found.")
            }
            val approval =
                transactionApprovalDao.getById(approvalId)
                    ?: throw IllegalArgumentException("Pending entry not found.")
            if (approval.approvalState != SmsApprovalState.PENDING) {
                throw IllegalStateException("This entry is no longer pending.")
            }
            val existing =
                transactionDao.getById(transactionId)
                    ?: throw IllegalArgumentException("Ledger transaction not found.")
            if (existing.type != approval.extractedType) {
                throw IllegalStateException("Transaction type does not match this SMS.")
            }
            MmrValidation.requirePositiveAmount(approval.extractedAmount)

            val smsRecord =
                approvalLedgerDao.getSmsRecordById(approval.smsMessageRecordId)
                    ?: throw IllegalStateException("SMS record missing for this approval.")

            val smsNote = approval.extractedNote ?: approval.extractedMerchant ?: approval.extractedSender
            val mergedNote =
                buildString {
                    existing.note
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { append(it) }
                    if (isNotEmpty()) append(" | ")
                    append("SMS: ")
                    append(smsNote)
                }
            val now = System.currentTimeMillis()
            val resolvedCategoryId =
                when {
                    useLedgerCategory -> existing.categoryId
                    else -> categoryId ?: approval.extractedCategoryId ?: existing.categoryId
                }
            val merged =
                existing.copy(
                    amount = existing.amount + approval.extractedAmount,
                    categoryId = resolvedCategoryId,
                    subcategoryId =
                        if (useLedgerCategory) {
                            existing.subcategoryId
                        } else {
                            subcategoryId
                                ?: existing.subcategoryId
                        },
                    note = mergedNote,
                    updatedAt = now,
                    isEdited = true,
                )
            RepositoryGuards.requireValidTransactionEntity(merged)

            val txnId =
                approvalLedgerDao.mergeApprovalWithExistingTransaction(
                    approvalId = approvalId,
                    mergedTransaction = merged,
                    smsRecordId = approval.smsMessageRecordId,
                    approvedSms = smsRecord,
                )
            loanTransactionCoordinator.syncPaymentFromTransaction(merged.copy(id = txnId))
            return txnId
        }

        override suspend fun approveCandidate(
            id: Long,
            fallbackAccountId: Long,
            fallbackCategoryId: Long?,
        ): Long {
            if (id <= 0L) {
                throw IllegalArgumentException("Pending entry not found.")
            }
            val approval =
                transactionApprovalDao.getById(id)
                    ?: throw IllegalArgumentException("Pending entry not found.")
            if (approval.approvalState != SmsApprovalState.PENDING) {
                throw IllegalStateException("This entry is no longer pending.")
            }
            MmrValidation.requirePositiveAmount(approval.extractedAmount)
            val accountId = approval.extractedAccountId ?: fallbackAccountId
            if (accountId <= 0L) {
                throw IllegalStateException("No valid account available for approval.")
            }
            val categoryId = approval.extractedCategoryId ?: fallbackCategoryId
            val dateTime = MmrValidation.normalizeEpochMillis(approval.extractedDateTime)

            val smsRecord =
                approvalLedgerDao.getSmsRecordById(approval.smsMessageRecordId)
                    ?: throw IllegalStateException("SMS record missing for this approval.")

            val subcategoryId =
                resolveSubcategoryIdFromRules(
                    approval = approval,
                    categoryId = categoryId,
                    smsBody =
                        smsRecord.messageBodyFull?.trim()?.takeIf { it.isNotEmpty() }
                            ?: smsRecord.messageBodySnippet,
                )

            val transaction =
                TransactionEntity(
                    amount = approval.extractedAmount,
                    type = approval.extractedType,
                    dateTime = dateTime,
                    accountId = accountId,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    note = approval.extractedNote ?: approval.extractedMerchant ?: "Approved SMS",
                    sourceType = TransactionSourceType.SMS,
                    sourceReferenceId = approval.id.toString(),
                    status = RecordStatus.APPROVED,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    paymentChannel = approval.extractedPaymentChannel,
                    paymentInstrument = approval.extractedInstrument,
                    cardLastFour = approval.extractedCardLastFour,
                    cardNetwork = approval.extractedCardNetwork,
                )
            RepositoryGuards.requireValidTransactionEntity(transaction)

            return approvalLedgerDao.approvePendingAndCreateTransaction(
                approvalId = id,
                transaction = transaction,
                smsRecordId = approval.smsMessageRecordId,
                approvedSms = smsRecord,
            )
        }

        override suspend fun rejectCandidate(id: Long) {
            if (id <= 0L) return
            val approval = transactionApprovalDao.getById(id) ?: return
            if (approval.approvalState != SmsApprovalState.PENDING) return
            approvalLedgerDao.updatePendingApprovalState(
                approvalId = id,
                smsRecordId = approval.smsMessageRecordId,
                newState = SmsApprovalState.REJECTED,
            )
        }

        override suspend fun markDuplicate(id: Long) {
            if (id <= 0L) return
            val approval = transactionApprovalDao.getById(id) ?: return
            if (approval.approvalState != SmsApprovalState.PENDING) return
            approvalLedgerDao.updatePendingApprovalState(
                approvalId = id,
                smsRecordId = approval.smsMessageRecordId,
                newState = SmsApprovalState.DUPLICATE,
            )
        }

        override suspend fun submitManualSms(
            sender: String,
            body: String,
            timestampMillis: Long,
            resolution: ManualSmsDuplicateResolution?,
            existingApprovalIdForRevert: Long?,
        ): ManualSmsSubmitResult {
            if (body.isEmpty()) {
                return ManualSmsSubmitResult.ParseFailed("SMS message cannot be empty.")
            }
            val trimmedSender = sender.trim().ifEmpty { "Manual" }
            if (!MmrValidation.isValidEpochMillis(timestampMillis)) {
                return ManualSmsSubmitResult.ParseFailed("Choose a valid date and time.")
            }

            val rawSms =
                RawSmsMessage(
                    id = "manual-${UUID.randomUUID()}",
                    sender = trimmedSender,
                    body = body,
                    timestamp = timestampMillis,
                )
            val parseResult = smsParserEngine.parseResult(rawSms)
            val candidate =
                when (parseResult) {
                    is SmsParseResult.Transaction -> parseResult.candidate
                    is SmsParseResult.Informational ->
                        return ManualSmsSubmitResult.ParseFailed(
                            parseResult.reason ?: "This SMS is informational, not a transaction.",
                        )
                    is SmsParseResult.Ignored ->
                        return ManualSmsSubmitResult.ParseFailed("Could not parse a transaction from this SMS.")
                }

            val duplicate = approvalLedgerDao.findLatestApprovalByExactBody(body)
            if (duplicate != null && resolution == null) {
                return ManualSmsSubmitResult.NeedsDuplicateDecision(
                    ManualSmsDuplicateMatch(
                        approvalId = duplicate.approvalId,
                        priorState = duplicate.approvalState,
                        priorActionLabel = duplicate.approvalState.toManualPriorActionLabel(),
                    ),
                )
            }

            val hashNonce =
                when {
                    resolution == ManualSmsDuplicateResolution.ADD_AS_NEW_TRANSACTION -> ":${UUID.randomUUID()}"
                    else -> ""
                }
            val messageHash =
                SmsMessageHasher.generateHash(
                    sender = trimmedSender,
                    body = body,
                    timestamp = timestampMillis,
                    uniquenessNonce = hashNonce,
                )
            val manualCandidate =
                candidate.copy(
                    sender = trimmedSender,
                    rawBody = body,
                    timestamp = timestampMillis,
                    messageHash = messageHash,
                    rawMessageId = rawSms.id,
                )

            if (resolution == ManualSmsDuplicateResolution.REVERT_PRIOR_AND_USE_NEW) {
                val approvalId =
                    existingApprovalIdForRevert ?: duplicate?.approvalId
                        ?: return ManualSmsSubmitResult.ParseFailed("Could not find the prior entry to revert.")
                return revertAndRequeueManual(approvalId, manualCandidate, body)
            }

            val approvalId =
                insertSingleCandidate(manualCandidate, body)
                    ?: return ManualSmsSubmitResult.ParseFailed(
                        "This SMS was already imported with the same sender, time, and text.",
                    )
            return ManualSmsSubmitResult.Added(approvalId)
        }

        private suspend fun revertAndRequeueManual(
            approvalId: Long,
            candidate: ParsedSmsCandidate,
            fullBody: String,
        ): ManualSmsSubmitResult {
            val approval =
                transactionApprovalDao.getById(approvalId)
                    ?: return ManualSmsSubmitResult.ParseFailed("Prior approval entry not found.")
            val sms =
                approvalLedgerDao.getSmsRecordById(approval.smsMessageRecordId)
                    ?: return ManualSmsSubmitResult.ParseFailed("Prior SMS record not found.")

            if (approval.approvalState == SmsApprovalState.APPROVED) {
                val txnId = sms.matchedTransactionId
                if (txnId != null) {
                    transactionDao.getById(txnId)?.let { transactionDao.delete(it) }
                }
            }

            val (recordEntity, approvalEntity) = buildEntitiesFromCandidate(candidate, fullBody)
            val now = System.currentTimeMillis()
            approvalLedgerDao.replaceApprovalWithPendingParse(
                approvalId = approvalId,
                smsRecordId = approval.smsMessageRecordId,
                updatedSms =
                    recordEntity.copy(
                        id = sms.id,
                        approvalState = SmsApprovalState.PENDING,
                        matchedTransactionId = null,
                        processedAt = now,
                    ),
                updatedApproval =
                    approvalEntity.copy(
                        id = approvalId,
                        smsMessageRecordId = approval.smsMessageRecordId,
                        userEdited = false,
                        approvalState = SmsApprovalState.PENDING,
                        updatedAt = now,
                    ),
            )
            return ManualSmsSubmitResult.Added(approvalId)
        }

        private suspend fun insertSingleCandidate(
            candidate: ParsedSmsCandidate,
            fullBody: String,
        ): Long? {
            val (recordEntity, approvalEntity) = buildEntitiesFromCandidate(candidate, fullBody)
            return approvalLedgerDao.insertPendingCandidate(recordEntity, approvalEntity)
        }

        private suspend fun buildEntitiesFromCandidate(
            candidate: ParsedSmsCandidate,
            fullBody: String,
        ): Pair<SmsMessageRecordEntity, TransactionApprovalEntity> {
            val activeCategories = categoryDao.getActiveCategories().firstOrNull() ?: emptyList()
            val categoryId = resolveCategoryId(candidate, activeCategories)
            val resolvedAccountId = smsAccountResolver.resolveAccountId(candidate)
            val cardNetwork = CardMetadataExtractor.detectNetwork(candidate.rawBody)

            val recordEntity =
                SmsMessageRecordEntity(
                    sender = candidate.sender,
                    messageBodyHash = candidate.messageHash,
                    messageBodySnippet = candidate.rawBody.take(150),
                    messageBodyFull = fullBody,
                    receivedTime = candidate.timestamp,
                    parsedStatus = SmsParsedStatus.MATCHED,
                    parsedAmount = candidate.amount,
                    parsedDateTime = candidate.timestamp,
                    approvalState = SmsApprovalState.PENDING,
                    rawMessageId = candidate.rawMessageId,
                )
            val approvalEntity =
                TransactionApprovalEntity(
                    smsMessageRecordId = 0,
                    extractedAmount = candidate.amount,
                    extractedType = candidate.type,
                    extractedDateTime = candidate.timestamp,
                    extractedSender = candidate.sender,
                    extractedMerchant = candidate.merchant,
                    extractedCategoryId = categoryId,
                    extractedAccountId = resolvedAccountId,
                    extractedNote = candidate.merchant ?: candidate.sender,
                    confidenceScore = candidate.confidenceScore,
                    userEdited = false,
                    approvalState = SmsApprovalState.PENDING,
                    rawBodySnippet = candidate.rawBody.take(200),
                    extractedTemplateId = candidate.templateId,
                    extractedPaymentChannel = candidate.paymentChannel?.name,
                    extractedInstrument = candidate.instrument?.name,
                    extractedCardLastFour = candidate.accountLastDigits,
                    extractedCardNetwork = cardNetwork,
                )
            return recordEntity to approvalEntity
        }
    }
