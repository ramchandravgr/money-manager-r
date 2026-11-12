package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.backup.model.MmrAppSettingsBackup
import com.rrparallelstudio.miapp.core.common.sync.MmrSyncId
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import com.rrparallelstudio.miapp.core.database.dao.BackupDao
import com.rrparallelstudio.miapp.core.database.dao.CategorizationRuleDao
import com.rrparallelstudio.miapp.core.database.dao.SmsAccountOverrideDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionAttachmentDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionDao
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetValuationEntity
import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.database.entity.CategorizationRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanInterestRevisionEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanPaymentEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanScheduleEntryEntity
import com.rrparallelstudio.miapp.core.database.entity.NotificationRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.RecurringRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.SmsAccountOverrideEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.lansync.model.LanAccountRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanAssetAttachmentRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanAssetRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanAssetValuationRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanAttachmentRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanBudgetRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanCategorizationRuleRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanCategoryRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanLoanInterestRevisionRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanLoanPaymentRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanLoanRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanLoanScheduleEntryRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanNotificationRuleRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanRecurringRuleRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanSmsAccountOverrideRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanSubcategoryRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanSyncTombstoneRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanTransactionRecord
import com.rrparallelstudio.miapp.core.lansync.model.MmrLanSyncPayload
import com.rrparallelstudio.miapp.core.owner.MmrOwnerProfileCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncExporter
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val backupDao: BackupDao,
        private val categorizationRuleDao: CategorizationRuleDao,
        private val smsAccountOverrideDao: SmsAccountOverrideDao,
        private val attachmentDao: TransactionAttachmentDao,
        private val tombstoneCoordinator: LanSyncTombstoneCoordinator,
        private val ownerProfileCoordinator: MmrOwnerProfileCoordinator,
    ) {
        suspend fun buildPayload(
            sourceDeviceId: String,
            sourceDeviceName: String,
            appSettings: MmrAppSettingsBackup?,
            displaySettings: AppDisplaySettingsSnapshot?,
            includeAttachments: Boolean = false,
            peerDeviceId: String? = null,
            sourceListenPort: Int? = null,
        ): MmrLanSyncPayload {
            val categories = backupDao.getAllCategories().map { it.withSyncId() }
            val categorySyncMap = categories.associate { it.id to it.syncId }
            val subcategories = backupDao.getAllSubcategories().map { it.withSyncId() }
            val subcategorySyncMap = subcategories.associate { it.id to it.syncId }
            val accounts = backupDao.getAllAccounts().map { it.withSyncId() }
            val accountSyncMap = accounts.associate { it.id to it.syncId }
            val loans = backupDao.getAllLoans().map { it.withSyncId() }
            val loanSyncMap = loans.associate { it.id to it.syncId }
            val payments = backupDao.getAllLoanPayments()
            val paymentSyncMap = payments.associate { it.id to MmrSyncId.normalize(it.syncId) }
            val txnSyncById =
                backupDao.getAllTransactions().associate { it.id to MmrSyncId.normalize(it.syncId) }
            val recurring = backupDao.getAllRecurringRules().map { it.withSyncId() }
            val recurringSyncMap = recurring.associate { it.id to it.syncId }
            val assets = backupDao.getAllAssets().map { it.withSyncId() }
            val assetSyncMap = assets.associate { it.id to it.syncId }
            val assetValuations = backupDao.getAllAssetValuations()
            val approved = transactionDao.getAllApprovedTransactions().map { it.withSyncId() }
            ownerProfileCoordinator.registerLocalProfile()
            val knownProfiles = ownerProfileCoordinator.knownProfilesForExport()

            return MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = System.currentTimeMillis(),
                sourceDeviceId = sourceDeviceId,
                sourceDeviceName = sourceDeviceName,
                sourceListenPort = sourceListenPort?.takeIf { it in 1..65535 },
                sourceProfileId = ownerProfileCoordinator.localProfileId,
                sourceProfileName = sourceDeviceName,
                knownProfiles = knownProfiles,
                categories = categories.map { it.toLanRecord() },
                subcategories = subcategories.map { it.toLanRecord(categorySyncMap) },
                accounts = accounts.map { it.toLanRecord() },
                loans = loans.map { it.toLanRecord(accountSyncMap) },
                transactions =
                    approved.map {
                        it.toLanRecord(categorySyncMap, subcategorySyncMap, accountSyncMap, loanSyncMap)
                    },
                loanPayments = payments.map { it.toLanRecord(loanSyncMap, txnSyncById) },
                loanInterestRevisions =
                    backupDao.getAllLoanInterestRevisions().map { it.toLanRecord(loanSyncMap) },
                loanScheduleEntries =
                    backupDao.getAllLoanScheduleEntries().map {
                        it.toLanRecord(loanSyncMap, paymentSyncMap)
                    },
                recurringRules =
                    recurring.map {
                        it.toLanRecord(categorySyncMap, subcategorySyncMap, accountSyncMap, loanSyncMap)
                    },
                budgets =
                    backupDao.getAllBudgets().map { budget ->
                        budget.withSyncId().toLanRecord(categorySyncMap, subcategorySyncMap)
                    },
                assets = assets.map { it.toLanRecord(loanSyncMap) },
                assetValuations =
                    assetValuations.mapNotNull { valuation ->
                        val assetSyncId = assetSyncMap[valuation.assetId] ?: return@mapNotNull null
                        valuation.toLanRecord(assetSyncId)
                    },
                categorizationRules = categorizationRuleDao.getAll().map { it.withSyncId().toLanRecord() },
                smsAccountOverrides =
                    smsAccountOverrideDao.getAll().mapNotNull { override ->
                        val accountSyncId = accountSyncMap[override.accountId] ?: return@mapNotNull null
                        override.toLanRecord(accountSyncId)
                    },
                notificationRules =
                    backupDao.getAllNotificationRules().map {
                        it.withSyncId().toLanRecord(loanSyncMap, recurringSyncMap, assetSyncMap)
                    },
                appSettings = appSettings,
                displaySettings = displaySettings,
                attachments =
                    if (includeAttachments) {
                        buildAttachmentRecords(txnSyncById)
                    } else {
                        emptyList()
                    },
                assetAttachments =
                    if (includeAttachments) {
                        buildAssetAttachmentRecords(assetSyncMap)
                    } else {
                        emptyList()
                    },
                tombstones =
                    tombstoneCoordinator.tombstonesForExport(peerDeviceId).map {
                        LanSyncTombstoneRecord(
                            syncId = it.syncId,
                            entityType = it.entityType,
                            deletedAt = it.deletedAt,
                            sourceDeviceId = it.sourceDeviceId,
                        )
                    },
            )
        }

        private suspend fun buildAssetAttachmentRecords(
            assetSyncMap: Map<Long, String>,
        ): List<LanAssetAttachmentRecord> {
            val all = backupDao.getAllAssetAttachments()
            return all.mapNotNull { entity ->
                val assetSyncId = assetSyncMap[entity.assetId] ?: return@mapNotNull null
                LanAssetAttachmentRecord(
                    syncId = entity.syncId,
                    assetSyncId = assetSyncId,
                    mimeType = entity.mimeType,
                    fileSizeBytes = entity.fileSizeBytes,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
        }

        private suspend fun buildAttachmentRecords(txnSyncById: Map<Long, String>): List<LanAttachmentRecord> {
            val all = attachmentDao.getAll()
            return all.mapNotNull { entity ->
                val txnSyncId = txnSyncById[entity.transactionId] ?: return@mapNotNull null
                LanAttachmentRecord(
                    syncId = entity.syncId,
                    transactionSyncId = txnSyncId,
                    mimeType = entity.mimeType,
                    fileSizeBytes = entity.fileSizeBytes,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
        }

        suspend fun buildEmptyPayload(
            sourceDeviceId: String,
            sourceDeviceName: String,
            sourceListenPort: Int? = null,
        ): MmrLanSyncPayload {
            ownerProfileCoordinator.registerLocalProfile()
            return MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = System.currentTimeMillis(),
                sourceDeviceId = sourceDeviceId,
                sourceDeviceName = sourceDeviceName,
                sourceListenPort = sourceListenPort?.takeIf { it in 1..65535 },
                sourceProfileId = ownerProfileCoordinator.localProfileId,
                sourceProfileName = sourceDeviceName,
                knownProfiles = ownerProfileCoordinator.knownProfilesForExport(),
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
            )
        }

        private fun CategoryEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun SubcategoryEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun AccountEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun LoanEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun TransactionEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun RecurringRuleEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun CategorizationRuleEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun BudgetEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun NotificationRuleEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun AssetEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun CategoryEntity.toLanRecord() =
            LanCategoryRecord(
                syncId = syncId,
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                type = type.name,
                isDefault = isDefault,
                isActive = isActive,
                orderIndex = orderIndex,
            )

        private fun SubcategoryEntity.toLanRecord(categorySyncMap: Map<Long, String>) =
            LanSubcategoryRecord(
                syncId = syncId,
                categorySyncId = categorySyncMap[categoryId] ?: error("Missing category sync id"),
                name = name,
                iconName = iconName,
                isActive = isActive,
                orderIndex = orderIndex,
            )

        private fun AccountEntity.toLanRecord() =
            LanAccountRecord(
                syncId = syncId,
                name = name,
                type = type.name,
                openingBalance = openingBalance,
                currentBalance = currentBalance,
                currencyCode = currencyCode,
                isActive = isActive,
                isDefault = isDefault,
                cardLastFour = cardLastFour,
                smsAutoCreated = smsAutoCreated,
                smsAutoCreatedUserModified = smsAutoCreatedUserModified,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun LoanEntity.toLanRecord(accountSyncMap: Map<Long, String>) =
            LanLoanRecord(
                syncId = syncId,
                name = name,
                lenderName = lenderName,
                principalAmount = principalAmount,
                interestRate = interestRate,
                outstandingAmount = outstandingAmount,
                openingDate = openingDate,
                dueDate = dueDate,
                autoTransferDate = autoTransferDate,
                linkedDebitAccountSyncId = linkedDebitAccountId?.let { accountSyncMap[it] },
                linkedLoanAccountSyncId = linkedLoanAccountId?.let { accountSyncMap[it] },
                loanType = loanType.name,
                status = status.name,
                repaymentMode = repaymentMode,
                tenureMonths = tenureMonths,
                remainingTenureMonths = remainingTenureMonths,
                fixedEmiAmount = fixedEmiAmount,
                emiDayOfMonth = emiDayOfMonth,
                firstEmiDueMillis = firstEmiDueMillis,
                emiReminderDaysBefore = emiReminderDaysBefore,
                fixedEmiInputMode = fixedEmiInputMode,
                notes = notes,
                createdAt = createdAt,
                updatedAt = updatedAt,
                createdByProfileId = createdByProfileId,
                createdByDisplayName = createdByDisplayName,
            )

        private fun TransactionEntity.toLanRecord(
            categorySyncMap: Map<Long, String>,
            subcategorySyncMap: Map<Long, String>,
            accountSyncMap: Map<Long, String>,
            loanSyncMap: Map<Long, String>,
        ) = LanTransactionRecord(
            syncId = syncId,
            dateTime = dateTime,
            amount = amount,
            type = type.name,
            categorySyncId = categoryId?.let { categorySyncMap[it] },
            subcategorySyncId = subcategoryId?.let { subcategorySyncMap[it] },
            note = note,
            accountSyncId = accountSyncMap[accountId] ?: error("Missing account sync id"),
            relatedAccountSyncId = relatedAccountId?.let { accountSyncMap[it] },
            loanSyncId = loanId?.let { loanSyncMap[it] },
            sourceType = sourceType.name,
            sourceReferenceId = sourceReferenceId,
            attachmentCount = attachmentCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isEdited = isEdited,
            paymentChannel = paymentChannel,
            paymentInstrument = paymentInstrument,
            cardLastFour = cardLastFour,
            cardNetwork = cardNetwork?.name,
            createdByProfileId = createdByProfileId,
            createdByDisplayName = createdByDisplayName,
            originalAmount = originalAmount,
            refundedTotal = refundedTotal,
        )

        private fun LoanPaymentEntity.toLanRecord(
            loanSyncMap: Map<Long, String>,
            txnSyncByTxnId: Map<Long, String>,
        ): LanLoanPaymentRecord =
            LanLoanPaymentRecord(
                syncId = MmrSyncId.normalize(syncId),
                loanSyncId = loanSyncMap[loanId] ?: error("Missing loan sync id"),
                transactionSyncId = transactionId?.let { txnSyncByTxnId[it] },
                paymentAmount = paymentAmount,
                paymentDate = paymentDate,
                paymentType = paymentType.name,
                principalComponent = principalComponent,
                interestComponent = interestComponent,
                outstandingAfterPayment = outstandingAfterPayment,
                notes = notes,
            )

        private fun LoanInterestRevisionEntity.toLanRecord(loanSyncMap: Map<Long, String>) =
            LanLoanInterestRevisionRecord(
                syncId = MmrSyncId.normalize(syncId),
                loanSyncId = loanSyncMap[loanId] ?: error("Missing loan sync id"),
                effectiveFromMillis = effectiveFromMillis,
                annualRatePercent = annualRatePercent,
                note = note,
                createdAt = createdAt,
            )

        private fun LoanScheduleEntryEntity.toLanRecord(
            loanSyncMap: Map<Long, String>,
            paymentSyncMap: Map<Long, String>,
        ) = LanLoanScheduleEntryRecord(
            syncId = MmrSyncId.normalize(syncId),
            loanSyncId = loanSyncMap[loanId] ?: error("Missing loan sync id"),
            sequenceNumber = sequenceNumber,
            dueDateMillis = dueDateMillis,
            emiAmount = emiAmount,
            principalPlanned = principalPlanned,
            interestPlanned = interestPlanned,
            status = status.name,
            linkedPaymentSyncId = linkedPaymentId?.let { paymentSyncMap[it] },
            annualRatePercentAtPlan = annualRatePercentAtPlan,
        )

        private fun RecurringRuleEntity.toLanRecord(
            categorySyncMap: Map<Long, String>,
            subcategorySyncMap: Map<Long, String>,
            accountSyncMap: Map<Long, String>,
            loanSyncMap: Map<Long, String>,
        ) = LanRecurringRuleRecord(
            syncId = syncId,
            name = name,
            transactionTemplateType = transactionTemplateType.name,
            amount = amount,
            categorySyncId = categoryId?.let { categorySyncMap[it] },
            subcategorySyncId = subcategoryId?.let { subcategorySyncMap[it] },
            accountSyncId = accountSyncMap[accountId] ?: error("Missing account sync id"),
            relatedAccountSyncId = relatedAccountId?.let { accountSyncMap[it] },
            loanSyncId = loanId?.let { loanSyncMap[it] },
            dayOfMonth = dayOfMonth,
            recurrenceInterval = recurrenceInterval.name,
            nextRunDate = nextRunDate,
            isEnabled = isEnabled,
            lastGeneratedDate = lastGeneratedDate,
            endDate = endDate,
            noteTemplate = noteTemplate,
            reminderMinutesBefore = reminderMinutesBefore,
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdByProfileId = createdByProfileId,
            createdByDisplayName = createdByDisplayName,
        )

        private fun BudgetEntity.toLanRecord(
            categorySyncMap: Map<Long, String>,
            subcategorySyncMap: Map<Long, String>,
        ) = LanBudgetRecord(
            syncId = syncId,
            categorySyncId = categorySyncMap[categoryId] ?: error("Missing category sync id"),
            subcategorySyncId = subcategoryId?.let { subcategorySyncMap[it] },
            amountLimit = amountLimit,
            periodType = periodType.name,
            customPeriodStartMillis = customPeriodStartMillis,
            customPeriodEndMillis = customPeriodEndMillis,
            alertOnExceeded = alertOnExceeded,
            lastAlertPeriodKey = lastAlertPeriodKey,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdByProfileId = createdByProfileId,
            createdByDisplayName = createdByDisplayName,
        )

        private fun SmsAccountOverrideEntity.toLanRecord(accountSyncId: String) =
            LanSmsAccountOverrideRecord(
                templateId = templateId,
                instrument = instrument,
                smsLastFour = smsLastFour,
                accountSyncId = accountSyncId,
                updatedAt = updatedAt,
            )

        private fun CategorizationRuleEntity.toLanRecord() =
            LanCategorizationRuleRecord(
                syncId = syncId,
                priority = priority,
                matchType = matchType.name,
                pattern = pattern,
                categoryName = categoryName,
                subcategoryName = subcategoryName,
                isEnabled = isEnabled,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun AssetEntity.toLanRecord(loanSyncMap: Map<Long, String>) =
            LanAssetRecord(
                syncId = syncId,
                name = name,
                assetType = assetType,
                status = status.name,
                isInherited = isInherited,
                purchaseDateMillis = purchaseDateMillis,
                purchasePrice = purchasePrice,
                purchasePaymentMode = purchasePaymentMode,
                purchaseDownPayment = purchaseDownPayment,
                purchaseFinancedAmount = purchaseFinancedAmount,
                currentValue = currentValue,
                ownedByName = ownedByName,
                location = location,
                identifier = identifier,
                linkedLoanSyncId = linkedLoanId?.let { loanSyncMap[it] },
                nextValuationDueMillis = nextValuationDueMillis,
                valuationReminderDaysBefore = valuationReminderDaysBefore,
                saleDateMillis = saleDateMillis,
                salePrice = salePrice,
                notes = notes,
                createdAt = createdAt,
                updatedAt = updatedAt,
                createdByProfileId = createdByProfileId,
                createdByDisplayName = createdByDisplayName,
            )

        private fun AssetValuationEntity.toLanRecord(assetSyncId: String) =
            LanAssetValuationRecord(
                syncId = MmrSyncId.normalize(syncId),
                assetSyncId = assetSyncId,
                amount = amount,
                recordedAt = recordedAt,
                notes = notes,
                createdAt = createdAt,
            )

        private fun NotificationRuleEntity.toLanRecord(
            loanSyncMap: Map<Long, String>,
            recurringSyncMap: Map<Long, String>,
            assetSyncMap: Map<Long, String>,
        ) = LanNotificationRuleRecord(
            syncId = syncId,
            type = type.name,
            enabled = enabled,
            scheduleTime = scheduleTime,
            targetLoanSyncId = targetLoanId?.let { loanSyncMap[it] },
            targetRecurringRuleSyncId = targetRecurringRuleId?.let { recurringSyncMap[it] },
            targetAssetSyncId = targetAssetId?.let { assetSyncMap[it] },
            thresholdValue = thresholdValue,
            title = title,
            message = message,
            isFired = isFired,
            createdAt = createdAt,
        )
    }
