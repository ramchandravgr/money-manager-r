package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.backup.model.MmrAppSettingsBackup
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
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
import com.rrparallelstudio.miapp.core.lansync.model.LanOwnerProfileRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanRecurringRuleRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanSmsAccountOverrideRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanSubcategoryRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanSyncTombstoneRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanTransactionRecord
import com.rrparallelstudio.miapp.core.lansync.model.MmrLanSyncPayload
import org.json.JSONArray
import org.json.JSONObject

object LanSyncJsonCodec {
    const val CURRENT_FORMAT_VERSION = 7

    fun encode(payload: MmrLanSyncPayload): String =
        JSONObject()
            .apply {
                put("formatVersion", payload.formatVersion)
                put("exportedAtEpochMillis", payload.exportedAtEpochMillis)
                put("sourceDeviceId", payload.sourceDeviceId)
                putNullableString("sourceDeviceName", payload.sourceDeviceName)
                payload.sourceListenPort?.takeIf { it in 1..65535 }?.let { put("sourceListenPort", it) }
                putNullableString("sourceProfileId", payload.sourceProfileId)
                putNullableString("sourceProfileName", payload.sourceProfileName)
                if (payload.knownProfiles.isNotEmpty()) {
                    put("knownProfiles", payload.knownProfiles.toJsonArray { knownProfileToJson(it) })
                }
                put("categories", payload.categories.toJsonArray { categoryToJson(it) })
                put("subcategories", payload.subcategories.toJsonArray { subcategoryToJson(it) })
                put("accounts", payload.accounts.toJsonArray { accountToJson(it) })
                put("loans", payload.loans.toJsonArray { loanToJson(it) })
                put("transactions", payload.transactions.toJsonArray { transactionToJson(it) })
                put("loanPayments", payload.loanPayments.toJsonArray { loanPaymentToJson(it) })
                put("loanInterestRevisions", payload.loanInterestRevisions.toJsonArray { loanRevisionToJson(it) })
                put("loanScheduleEntries", payload.loanScheduleEntries.toJsonArray { loanScheduleToJson(it) })
                put("recurringRules", payload.recurringRules.toJsonArray { recurringToJson(it) })
                put("budgets", payload.budgets.toJsonArray { budgetToJson(it) })
                put("assets", payload.assets.toJsonArray { assetToJson(it) })
                put("assetValuations", payload.assetValuations.toJsonArray { assetValuationToJson(it) })
                put("categorizationRules", payload.categorizationRules.toJsonArray { categorizationToJson(it) })
                put("smsAccountOverrides", payload.smsAccountOverrides.toJsonArray { smsAccountOverrideToJson(it) })
                put("notificationRules", payload.notificationRules.toJsonArray { notificationToJson(it) })
                payload.appSettings?.let { put("appSettings", appSettingsToJson(it)) }
                payload.displaySettings?.let { put("displaySettings", displaySettingsToJson(it)) }
                if (payload.attachments.isNotEmpty()) {
                    put("attachments", payload.attachments.toJsonArray { attachmentToJson(it) })
                }
                if (payload.assetAttachments.isNotEmpty()) {
                    put("assetAttachments", payload.assetAttachments.toJsonArray { assetAttachmentToJson(it) })
                }
                if (payload.tombstones.isNotEmpty()) {
                    put("tombstones", payload.tombstones.toJsonArray { tombstoneToJson(it) })
                }
            }.toString()

    fun decode(json: String): MmrLanSyncPayload {
        val root = JSONObject(json)
        val version = root.getInt("formatVersion")
        if (version != CURRENT_FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported LAN sync format version: $version")
        }
        return MmrLanSyncPayload(
            formatVersion = CURRENT_FORMAT_VERSION,
            exportedAtEpochMillis = root.getLong("exportedAtEpochMillis"),
            sourceDeviceId = root.getString("sourceDeviceId"),
            sourceDeviceName = root.optNullableString("sourceDeviceName"),
            sourceListenPort = root.optInt("sourceListenPort", 0).takeIf { it in 1..65535 },
            sourceProfileId = root.optNullableString("sourceProfileId"),
            sourceProfileName = root.optNullableString("sourceProfileName"),
            knownProfiles = root.readArray("knownProfiles").mapObjectsLenient { knownProfileFromJson(it) },
            categories = root.readArray("categories").mapObjectsLenient { categoryFromJson(it) },
            subcategories = root.readArray("subcategories").mapObjectsLenient { subcategoryFromJson(it) },
            accounts = root.readArray("accounts").mapObjectsLenient { accountFromJson(it) },
            loans = root.readArray("loans").mapObjectsLenient { loanFromJson(it) },
            transactions = root.readArray("transactions").mapObjectsLenient { transactionFromJson(it) },
            loanPayments = root.readArray("loanPayments").mapObjectsLenient { loanPaymentFromJson(it) },
            loanInterestRevisions =
                root
                    .readArray(
                        "loanInterestRevisions",
                    ).mapObjectsLenient { loanRevisionFromJson(it) },
            loanScheduleEntries =
                root
                    .readArray(
                        "loanScheduleEntries",
                    ).mapObjectsLenient { loanScheduleFromJson(it) },
            recurringRules = root.readArray("recurringRules").mapObjectsLenient { recurringFromJson(it) },
            budgets = root.readArray("budgets").mapObjectsLenient { budgetFromJson(it) },
            assets = root.readArray("assets").mapObjectsLenient { assetFromJson(it) },
            assetValuations = root.readArray("assetValuations").mapObjectsLenient { assetValuationFromJson(it) },
            categorizationRules =
                root
                    .readArray(
                        "categorizationRules",
                    ).mapObjectsLenient { categorizationFromJson(it) },
            smsAccountOverrides =
                root
                    .readArray(
                        "smsAccountOverrides",
                    ).mapObjectsLenient { smsAccountOverrideFromJson(it) },
            notificationRules = root.readArray("notificationRules").mapObjectsLenient { notificationFromJson(it) },
            appSettings = root.optJSONObject("appSettings")?.let { appSettingsFromJson(it) },
            displaySettings = root.optJSONObject("displaySettings")?.let { displaySettingsFromJson(it) },
            attachments = root.readArray("attachments").mapObjectsLenient { attachmentFromJson(it) },
            assetAttachments = root.readArray("assetAttachments").mapObjectsLenient { assetAttachmentFromJson(it) },
            tombstones = root.readArray("tombstones").mapObjectsLenient { tombstoneFromJson(it) },
        )
    }

    private fun tombstoneToJson(record: LanSyncTombstoneRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("entityType", record.entityType)
            put("deletedAt", record.deletedAt)
            put("sourceDeviceId", record.sourceDeviceId)
        }

    private fun tombstoneFromJson(json: JSONObject) =
        LanSyncTombstoneRecord(
            syncId = json.getString("syncId"),
            entityType = json.getString("entityType"),
            deletedAt = json.getLong("deletedAt"),
            sourceDeviceId = json.getString("sourceDeviceId"),
        )

    private fun attachmentToJson(record: LanAttachmentRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("transactionSyncId", record.transactionSyncId)
            put("mimeType", record.mimeType)
            put("fileSizeBytes", record.fileSizeBytes)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
        }

    private fun attachmentFromJson(json: JSONObject) =
        LanAttachmentRecord(
            syncId = json.getString("syncId"),
            transactionSyncId = json.getString("transactionSyncId"),
            mimeType = json.getString("mimeType"),
            fileSizeBytes = json.getLong("fileSizeBytes"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )

    private fun categoryToJson(record: LanCategoryRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("name", record.name)
            put("iconName", record.iconName)
            put("colorHex", record.colorHex)
            put("type", record.type)
            put("isDefault", record.isDefault)
            put("isActive", record.isActive)
            put("orderIndex", record.orderIndex)
        }

    private fun categoryFromJson(json: JSONObject) =
        LanCategoryRecord(
            syncId = json.getString("syncId"),
            name = json.getString("name"),
            iconName = json.getString("iconName"),
            colorHex = json.getString("colorHex"),
            type = json.getString("type"),
            isDefault = json.getBoolean("isDefault"),
            isActive = json.getBoolean("isActive"),
            orderIndex = json.getInt("orderIndex"),
        )

    private fun subcategoryToJson(record: LanSubcategoryRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("categorySyncId", record.categorySyncId)
            put("name", record.name)
            put("isActive", record.isActive)
            put("orderIndex", record.orderIndex)
        }

    private fun subcategoryFromJson(json: JSONObject) =
        LanSubcategoryRecord(
            syncId = json.getString("syncId"),
            categorySyncId = json.getString("categorySyncId"),
            name = json.getString("name"),
            isActive = json.getBoolean("isActive"),
            orderIndex = json.getInt("orderIndex"),
        )

    private fun accountToJson(record: LanAccountRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("name", record.name)
            put("type", record.type)
            put("openingBalance", record.openingBalance)
            put("currentBalance", record.currentBalance)
            put("currencyCode", record.currencyCode)
            put("isActive", record.isActive)
            put("isDefault", record.isDefault)
            putNullableString("cardLastFour", record.cardLastFour)
            put("smsAutoCreated", record.smsAutoCreated)
            put("smsAutoCreatedUserModified", record.smsAutoCreatedUserModified)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
        }

    private fun accountFromJson(json: JSONObject) =
        LanAccountRecord(
            syncId = json.getString("syncId"),
            name = json.getString("name"),
            type = json.getString("type"),
            openingBalance = json.getDouble("openingBalance"),
            currentBalance = json.getDouble("currentBalance"),
            currencyCode = json.getString("currencyCode"),
            isActive = json.getBoolean("isActive"),
            isDefault = json.getBoolean("isDefault"),
            cardLastFour = json.optNullableString("cardLastFour"),
            smsAutoCreated = json.optBoolean("smsAutoCreated", false),
            smsAutoCreatedUserModified = json.optBoolean("smsAutoCreatedUserModified", false),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )

    private fun loanToJson(record: LanLoanRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("name", record.name)
            put("lenderName", record.lenderName)
            put("principalAmount", record.principalAmount)
            put("interestRate", record.interestRate)
            put("outstandingAmount", record.outstandingAmount)
            put("openingDate", record.openingDate)
            putNullableLong("dueDate", record.dueDate)
            putNullableLong("autoTransferDate", record.autoTransferDate)
            putNullableString("linkedDebitAccountSyncId", record.linkedDebitAccountSyncId)
            putNullableString("linkedLoanAccountSyncId", record.linkedLoanAccountSyncId)
            put("loanType", record.loanType)
            put("status", record.status)
            putNullableString("repaymentMode", record.repaymentMode)
            putNullableInt("tenureMonths", record.tenureMonths)
            putNullableInt("remainingTenureMonths", record.remainingTenureMonths)
            putNullableDouble("fixedEmiAmount", record.fixedEmiAmount)
            putNullableInt("emiDayOfMonth", record.emiDayOfMonth)
            putNullableLong("firstEmiDueMillis", record.firstEmiDueMillis)
            put("emiReminderDaysBefore", record.emiReminderDaysBefore)
            putNullableString("fixedEmiInputMode", record.fixedEmiInputMode)
            putNullableString("notes", record.notes)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
            putNullableString("createdByProfileId", record.createdByProfileId)
            putNullableString("createdByDisplayName", record.createdByDisplayName)
        }

    private fun loanFromJson(json: JSONObject) =
        LanLoanRecord(
            syncId = json.getString("syncId"),
            name = json.getString("name"),
            lenderName = json.getString("lenderName"),
            principalAmount = json.getDouble("principalAmount"),
            interestRate = json.getDouble("interestRate"),
            outstandingAmount = json.getDouble("outstandingAmount"),
            openingDate = json.getLong("openingDate"),
            dueDate = json.optNullableLong("dueDate"),
            autoTransferDate = json.optNullableLong("autoTransferDate"),
            linkedDebitAccountSyncId = json.optNullableString("linkedDebitAccountSyncId"),
            linkedLoanAccountSyncId = json.optNullableString("linkedLoanAccountSyncId"),
            loanType = json.getString("loanType"),
            status = json.getString("status"),
            repaymentMode = json.optNullableString("repaymentMode"),
            tenureMonths = json.optNullableInt("tenureMonths"),
            remainingTenureMonths = json.optNullableInt("remainingTenureMonths"),
            fixedEmiAmount = json.optNullableDouble("fixedEmiAmount"),
            emiDayOfMonth = json.optNullableInt("emiDayOfMonth"),
            firstEmiDueMillis = json.optNullableLong("firstEmiDueMillis"),
            emiReminderDaysBefore = json.optInt("emiReminderDaysBefore", 1),
            fixedEmiInputMode = json.optNullableString("fixedEmiInputMode"),
            notes = json.optNullableString("notes"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            createdByProfileId = json.optNullableString("createdByProfileId"),
            createdByDisplayName = json.optNullableString("createdByDisplayName"),
        )

    private fun transactionToJson(record: LanTransactionRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("dateTime", record.dateTime)
            put("amount", record.amount)
            put("type", record.type)
            putNullableString("categorySyncId", record.categorySyncId)
            putNullableString("subcategorySyncId", record.subcategorySyncId)
            putNullableString("note", record.note)
            put("accountSyncId", record.accountSyncId)
            putNullableString("relatedAccountSyncId", record.relatedAccountSyncId)
            putNullableString("loanSyncId", record.loanSyncId)
            put("sourceType", record.sourceType)
            putNullableString("sourceReferenceId", record.sourceReferenceId)
            put("attachmentCount", record.attachmentCount)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
            put("isEdited", record.isEdited)
            putNullableString("paymentChannel", record.paymentChannel)
            putNullableString("paymentInstrument", record.paymentInstrument)
            putNullableString("cardLastFour", record.cardLastFour)
            putNullableString("cardNetwork", record.cardNetwork)
            putNullableString("createdByProfileId", record.createdByProfileId)
            putNullableString("createdByDisplayName", record.createdByDisplayName)
            putNullableDouble("originalAmount", record.originalAmount)
            put("refundedTotal", record.refundedTotal)
        }

    private fun transactionFromJson(json: JSONObject) =
        LanTransactionRecord(
            syncId = json.getString("syncId"),
            dateTime = json.getLong("dateTime"),
            amount = json.getDouble("amount"),
            type = json.getString("type"),
            categorySyncId = json.optNullableString("categorySyncId"),
            subcategorySyncId = json.optNullableString("subcategorySyncId"),
            note = json.optNullableString("note"),
            accountSyncId = json.getString("accountSyncId"),
            relatedAccountSyncId = json.optNullableString("relatedAccountSyncId"),
            loanSyncId = json.optNullableString("loanSyncId"),
            sourceType = json.getString("sourceType"),
            sourceReferenceId = json.optNullableString("sourceReferenceId"),
            attachmentCount = json.getInt("attachmentCount"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            isEdited = json.getBoolean("isEdited"),
            paymentChannel = json.optNullableString("paymentChannel"),
            paymentInstrument = json.optNullableString("paymentInstrument"),
            cardLastFour = json.optNullableString("cardLastFour"),
            cardNetwork = json.optNullableString("cardNetwork"),
            createdByProfileId =
                json.optNullableString("createdByProfileId")
                    ?: json.optNullableString("syncedFromDeviceId"),
            createdByDisplayName =
                json.optNullableString("createdByDisplayName")
                    ?: json.optNullableString("syncedFromDeviceName"),
            originalAmount = json.optNullableDouble("originalAmount"),
            refundedTotal = json.optDouble("refundedTotal", 0.0),
        )

    private fun loanPaymentToJson(record: LanLoanPaymentRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("loanSyncId", record.loanSyncId)
            putNullableString("transactionSyncId", record.transactionSyncId)
            put("paymentAmount", record.paymentAmount)
            put("paymentDate", record.paymentDate)
            put("paymentType", record.paymentType)
            putNullableDouble("principalComponent", record.principalComponent)
            putNullableDouble("interestComponent", record.interestComponent)
            put("outstandingAfterPayment", record.outstandingAfterPayment)
            putNullableString("notes", record.notes)
        }

    private fun loanPaymentFromJson(json: JSONObject) =
        LanLoanPaymentRecord(
            syncId = json.getString("syncId"),
            loanSyncId = json.getString("loanSyncId"),
            transactionSyncId = json.optNullableString("transactionSyncId"),
            paymentAmount = json.getDouble("paymentAmount"),
            paymentDate = json.getLong("paymentDate"),
            paymentType = json.getString("paymentType"),
            principalComponent = json.optNullableDouble("principalComponent"),
            interestComponent = json.optNullableDouble("interestComponent"),
            outstandingAfterPayment = json.getDouble("outstandingAfterPayment"),
            notes = json.optNullableString("notes"),
        )

    private fun loanRevisionToJson(record: LanLoanInterestRevisionRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("loanSyncId", record.loanSyncId)
            put("effectiveFromMillis", record.effectiveFromMillis)
            put("annualRatePercent", record.annualRatePercent)
            putNullableString("note", record.note)
            put("createdAt", record.createdAt)
        }

    private fun loanRevisionFromJson(json: JSONObject) =
        LanLoanInterestRevisionRecord(
            syncId = json.getString("syncId"),
            loanSyncId = json.getString("loanSyncId"),
            effectiveFromMillis = json.getLong("effectiveFromMillis"),
            annualRatePercent = json.getDouble("annualRatePercent"),
            note = json.optNullableString("note"),
            createdAt = json.getLong("createdAt"),
        )

    private fun loanScheduleToJson(record: LanLoanScheduleEntryRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("loanSyncId", record.loanSyncId)
            put("sequenceNumber", record.sequenceNumber)
            put("dueDateMillis", record.dueDateMillis)
            put("emiAmount", record.emiAmount)
            put("principalPlanned", record.principalPlanned)
            put("interestPlanned", record.interestPlanned)
            put("status", record.status)
            putNullableString("linkedPaymentSyncId", record.linkedPaymentSyncId)
            put("annualRatePercentAtPlan", record.annualRatePercentAtPlan)
        }

    private fun loanScheduleFromJson(json: JSONObject) =
        LanLoanScheduleEntryRecord(
            syncId = json.getString("syncId"),
            loanSyncId = json.getString("loanSyncId"),
            sequenceNumber = json.getInt("sequenceNumber"),
            dueDateMillis = json.getLong("dueDateMillis"),
            emiAmount = json.getDouble("emiAmount"),
            principalPlanned = json.getDouble("principalPlanned"),
            interestPlanned = json.getDouble("interestPlanned"),
            status = json.getString("status"),
            linkedPaymentSyncId = json.optNullableString("linkedPaymentSyncId"),
            annualRatePercentAtPlan = json.getDouble("annualRatePercentAtPlan"),
        )

    private fun recurringToJson(record: LanRecurringRuleRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("name", record.name)
            put("transactionTemplateType", record.transactionTemplateType)
            put("amount", record.amount)
            putNullableString("categorySyncId", record.categorySyncId)
            putNullableString("subcategorySyncId", record.subcategorySyncId)
            put("accountSyncId", record.accountSyncId)
            putNullableString("relatedAccountSyncId", record.relatedAccountSyncId)
            putNullableString("loanSyncId", record.loanSyncId)
            putNullableInt("dayOfMonth", record.dayOfMonth)
            put("recurrenceInterval", record.recurrenceInterval)
            put("nextRunDate", record.nextRunDate)
            put("isEnabled", record.isEnabled)
            putNullableLong("lastGeneratedDate", record.lastGeneratedDate)
            putNullableLong("endDate", record.endDate)
            putNullableString("noteTemplate", record.noteTemplate)
            put("reminderMinutesBefore", record.reminderMinutesBefore)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
            putNullableString("createdByProfileId", record.createdByProfileId)
            putNullableString("createdByDisplayName", record.createdByDisplayName)
        }

    private fun recurringFromJson(json: JSONObject) =
        LanRecurringRuleRecord(
            syncId = json.getString("syncId"),
            name = json.getString("name"),
            transactionTemplateType = json.getString("transactionTemplateType"),
            amount = json.getDouble("amount"),
            categorySyncId = json.optNullableString("categorySyncId"),
            subcategorySyncId = json.optNullableString("subcategorySyncId"),
            accountSyncId = json.getString("accountSyncId"),
            relatedAccountSyncId = json.optNullableString("relatedAccountSyncId"),
            loanSyncId = json.optNullableString("loanSyncId"),
            dayOfMonth = json.optNullableInt("dayOfMonth"),
            recurrenceInterval = json.getString("recurrenceInterval"),
            nextRunDate = json.getLong("nextRunDate"),
            isEnabled = json.getBoolean("isEnabled"),
            lastGeneratedDate = json.optNullableLong("lastGeneratedDate"),
            endDate = json.optNullableLong("endDate"),
            noteTemplate = json.optNullableString("noteTemplate"),
            reminderMinutesBefore = json.getInt("reminderMinutesBefore"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            createdByProfileId = json.optNullableString("createdByProfileId"),
            createdByDisplayName = json.optNullableString("createdByDisplayName"),
        )

    private fun budgetToJson(record: LanBudgetRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("categorySyncId", record.categorySyncId)
            putNullableString("subcategorySyncId", record.subcategorySyncId)
            put("amountLimit", record.amountLimit)
            put("periodType", record.periodType)
            putNullableLong("customPeriodStartMillis", record.customPeriodStartMillis)
            putNullableLong("customPeriodEndMillis", record.customPeriodEndMillis)
            put("alertOnExceeded", record.alertOnExceeded)
            putNullableString("lastAlertPeriodKey", record.lastAlertPeriodKey)
            put("isActive", record.isActive)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
            putNullableString("createdByProfileId", record.createdByProfileId)
            putNullableString("createdByDisplayName", record.createdByDisplayName)
        }

    private fun budgetFromJson(json: JSONObject) =
        LanBudgetRecord(
            syncId = json.getString("syncId"),
            categorySyncId = json.getString("categorySyncId"),
            subcategorySyncId = json.optNullableString("subcategorySyncId"),
            amountLimit = json.getDouble("amountLimit"),
            periodType = json.getString("periodType"),
            customPeriodStartMillis = json.optNullableLong("customPeriodStartMillis"),
            customPeriodEndMillis = json.optNullableLong("customPeriodEndMillis"),
            alertOnExceeded = json.getBoolean("alertOnExceeded"),
            lastAlertPeriodKey = json.optNullableString("lastAlertPeriodKey"),
            isActive = json.getBoolean("isActive"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            createdByProfileId = json.optNullableString("createdByProfileId"),
            createdByDisplayName = json.optNullableString("createdByDisplayName"),
        )

    private fun assetToJson(record: LanAssetRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("name", record.name)
            put("assetType", record.assetType)
            put("status", record.status)
            put("isInherited", record.isInherited)
            putNullableLong("purchaseDateMillis", record.purchaseDateMillis)
            putNullableDouble("purchasePrice", record.purchasePrice)
            putNullableString("purchasePaymentMode", record.purchasePaymentMode)
            putNullableDouble("purchaseDownPayment", record.purchaseDownPayment)
            putNullableDouble("purchaseFinancedAmount", record.purchaseFinancedAmount)
            put("currentValue", record.currentValue)
            putNullableString("ownedByName", record.ownedByName)
            putNullableString("location", record.location)
            putNullableString("identifier", record.identifier)
            putNullableString("linkedLoanSyncId", record.linkedLoanSyncId)
            putNullableLong("nextValuationDueMillis", record.nextValuationDueMillis)
            put("valuationReminderDaysBefore", record.valuationReminderDaysBefore)
            putNullableLong("saleDateMillis", record.saleDateMillis)
            putNullableDouble("salePrice", record.salePrice)
            putNullableString("notes", record.notes)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
            putNullableString("createdByProfileId", record.createdByProfileId)
            putNullableString("createdByDisplayName", record.createdByDisplayName)
        }

    private fun assetFromJson(json: JSONObject) =
        LanAssetRecord(
            syncId = json.getString("syncId"),
            name = json.getString("name"),
            assetType = json.getString("assetType"),
            status = json.getString("status"),
            isInherited = json.getBoolean("isInherited"),
            purchaseDateMillis = json.optNullableLong("purchaseDateMillis"),
            purchasePrice = json.optNullableDouble("purchasePrice"),
            purchasePaymentMode = json.optNullableString("purchasePaymentMode"),
            purchaseDownPayment = json.optNullableDouble("purchaseDownPayment"),
            purchaseFinancedAmount = json.optNullableDouble("purchaseFinancedAmount"),
            currentValue = json.getDouble("currentValue"),
            ownedByName = json.optNullableString("ownedByName"),
            location = json.optNullableString("location"),
            identifier = json.optNullableString("identifier"),
            linkedLoanSyncId = json.optNullableString("linkedLoanSyncId"),
            nextValuationDueMillis = json.optNullableLong("nextValuationDueMillis"),
            valuationReminderDaysBefore = json.optInt("valuationReminderDaysBefore", 0),
            saleDateMillis = json.optNullableLong("saleDateMillis"),
            salePrice = json.optNullableDouble("salePrice"),
            notes = json.optNullableString("notes"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            createdByProfileId = json.optNullableString("createdByProfileId"),
            createdByDisplayName = json.optNullableString("createdByDisplayName"),
        )

    private fun assetValuationToJson(record: LanAssetValuationRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("assetSyncId", record.assetSyncId)
            put("amount", record.amount)
            put("recordedAt", record.recordedAt)
            putNullableString("notes", record.notes)
            put("createdAt", record.createdAt)
        }

    private fun assetValuationFromJson(json: JSONObject) =
        LanAssetValuationRecord(
            syncId = json.getString("syncId"),
            assetSyncId = json.getString("assetSyncId"),
            amount = json.getDouble("amount"),
            recordedAt = json.getLong("recordedAt"),
            notes = json.optNullableString("notes"),
            createdAt = json.getLong("createdAt"),
        )

    private fun assetAttachmentToJson(record: LanAssetAttachmentRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("assetSyncId", record.assetSyncId)
            put("mimeType", record.mimeType)
            put("fileSizeBytes", record.fileSizeBytes)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
        }

    private fun assetAttachmentFromJson(json: JSONObject) =
        LanAssetAttachmentRecord(
            syncId = json.getString("syncId"),
            assetSyncId = json.getString("assetSyncId"),
            mimeType = json.getString("mimeType"),
            fileSizeBytes = json.getLong("fileSizeBytes"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )

    private fun smsAccountOverrideToJson(record: LanSmsAccountOverrideRecord) =
        JSONObject().apply {
            put("templateId", record.templateId)
            put("instrument", record.instrument)
            put("smsLastFour", record.smsLastFour)
            put("accountSyncId", record.accountSyncId)
            put("updatedAt", record.updatedAt)
        }

    private fun smsAccountOverrideFromJson(json: JSONObject) =
        LanSmsAccountOverrideRecord(
            templateId = json.optString("templateId", ""),
            instrument = json.optString("instrument", ""),
            smsLastFour = json.getString("smsLastFour"),
            accountSyncId = json.getString("accountSyncId"),
            updatedAt = json.getLong("updatedAt"),
        )

    private fun categorizationToJson(record: LanCategorizationRuleRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("priority", record.priority)
            put("matchType", record.matchType)
            put("pattern", record.pattern)
            put("categoryName", record.categoryName)
            putNullableString("subcategoryName", record.subcategoryName)
            put("isEnabled", record.isEnabled)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
        }

    private fun categorizationFromJson(json: JSONObject) =
        LanCategorizationRuleRecord(
            syncId = json.getString("syncId"),
            priority = json.getInt("priority"),
            matchType = json.getString("matchType"),
            pattern = json.getString("pattern"),
            categoryName = json.getString("categoryName"),
            subcategoryName = json.optNullableString("subcategoryName"),
            isEnabled = json.getBoolean("isEnabled"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )

    private fun notificationToJson(record: LanNotificationRuleRecord) =
        JSONObject().apply {
            put("syncId", record.syncId)
            put("type", record.type)
            put("enabled", record.enabled)
            putNullableLong("scheduleTime", record.scheduleTime)
            putNullableString("targetLoanSyncId", record.targetLoanSyncId)
            putNullableString("targetRecurringRuleSyncId", record.targetRecurringRuleSyncId)
            putNullableString("targetAssetSyncId", record.targetAssetSyncId)
            putNullableDouble("thresholdValue", record.thresholdValue)
            putNullableString("title", record.title)
            putNullableString("message", record.message)
            put("isFired", record.isFired)
            put("createdAt", record.createdAt)
        }

    private fun notificationFromJson(json: JSONObject) =
        LanNotificationRuleRecord(
            syncId = json.getString("syncId"),
            type = json.getString("type"),
            enabled = json.getBoolean("enabled"),
            scheduleTime = json.optNullableLong("scheduleTime"),
            targetLoanSyncId = json.optNullableString("targetLoanSyncId"),
            targetRecurringRuleSyncId = json.optNullableString("targetRecurringRuleSyncId"),
            targetAssetSyncId = json.optNullableString("targetAssetSyncId"),
            thresholdValue = json.optNullableDouble("thresholdValue"),
            title = json.optNullableString("title"),
            message = json.optNullableString("message"),
            isFired = json.getBoolean("isFired"),
            createdAt = json.getLong("createdAt"),
        )

    private fun appSettingsToJson(settings: MmrAppSettingsBackup) =
        JSONObject().apply {
            put("smsScanMonths", settings.smsScanMonths)
        }

    private fun appSettingsFromJson(json: JSONObject) =
        MmrAppSettingsBackup(smsScanMonths = json.optInt("smsScanMonths", 3))

    private fun displaySettingsToJson(snapshot: AppDisplaySettingsSnapshot) =
        JSONObject().apply {
            put("mainCurrencyCode", snapshot.mainCurrencyCode)
            putNullableString("subCurrencyCode", snapshot.subCurrencyCode)
            put("startScreenTab", snapshot.startScreenTab.name)
            put("monthStartDay", snapshot.monthStartDay)
            put("weekStartDay", snapshot.weekStartDay.name)
            put("budgetCarryOver", snapshot.budgetCarryOver)
            put("swipeBehavior", "CHANGE_DATE")
            put("amountColorScheme", snapshot.amountColorScheme.name)
            put("timeDisplay", snapshot.timeDisplay.name)
            put("showDescription", snapshot.showDescription)
            put("autocompleteEnabled", snapshot.autocompleteEnabled)
            put("inputOrder", snapshot.inputOrder.name)
            put("appColorTheme", snapshot.appColorTheme.name)
        }

    private fun displaySettingsFromJson(json: JSONObject): AppDisplaySettingsSnapshot {
        val mainCode = json.optString("mainCurrencyCode", "INR")
        return AppDisplaySettingsSnapshot(
            mainCurrencyCode = mainCode,
            mainCurrencySymbol = currencySymbol(mainCode),
            subCurrencyCode = json.optNullableString("subCurrencyCode"),
            startScreenTab =
                safeEnum(
                    json.optString("startScreenTab"),
                    com.rrparallelstudio.miapp.core.model.settings.MmrStartScreenTab.DAILY,
                ),
            monthStartDay = json.optInt("monthStartDay", 1),
            weekStartDay =
                safeEnum(
                    json.optString("weekStartDay"),
                    com.rrparallelstudio.miapp.core.model.settings.MmrWeekStartDay.MONDAY,
                ),
            budgetCarryOver = json.optBoolean("budgetCarryOver", true),
            amountColorScheme =
                safeEnum(
                    json.optString("amountColorScheme"),
                    com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme.SET_B,
                ),
            timeDisplay =
                safeEnum(
                    json.optString("timeDisplay"),
                    com.rrparallelstudio.miapp.core.model.settings.MmrTimeDisplay.NONE_ASC,
                ),
            showDescription = json.optBoolean("showDescription", true),
            autocompleteEnabled = json.optBoolean("autocompleteEnabled", true),
            inputOrder =
                safeEnum(
                    json.optString("inputOrder"),
                    com.rrparallelstudio.miapp.core.model.settings.MmrInputOrder.FROM_AMOUNT,
                ),
            appColorTheme =
                safeEnum(
                    json.optString("appColorTheme"),
                    com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme.ROSE_GOLD,
                ),
        )
    }

    private fun currencySymbol(code: String): String =
        when (code.uppercase()) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> code
        }

    private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray =
        JSONArray().apply {
            forEach { put(mapper(it)) }
        }

    private fun JSONObject.readArray(key: String): JSONArray =
        if (has(key) && !isNull(key)) getJSONArray(key) else JSONArray()

    private fun <T> JSONArray.mapObjectsLenient(mapper: (JSONObject) -> T): List<T> {
        val items = mutableListOf<T>()
        for (index in 0 until length()) {
            runCatching { mapper(getJSONObject(index)) }.onSuccess { items.add(it) }
        }
        return items
    }

    private inline fun <reified E : Enum<E>> safeEnum(
        name: String,
        default: E,
    ): E {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return default
        return runCatching { enumValueOf<E>(trimmed) }.getOrDefault(default)
    }

    private fun JSONObject.putNullableString(
        key: String,
        value: String?,
    ) {
        if (value != null) put(key, value)
    }

    private fun JSONObject.putNullableLong(
        key: String,
        value: Long?,
    ) {
        if (value != null) put(key, value)
    }

    private fun JSONObject.putNullableInt(
        key: String,
        value: Int?,
    ) {
        if (value != null) put(key, value)
    }

    private fun JSONObject.putNullableDouble(
        key: String,
        value: Double?,
    ) {
        if (value != null) put(key, value)
    }

    private fun knownProfileToJson(record: LanOwnerProfileRecord) =
        JSONObject().apply {
            put("profileId", record.profileId)
            put("displayName", record.displayName)
            put("updatedAt", record.updatedAt)
        }

    private fun knownProfileFromJson(json: JSONObject) =
        LanOwnerProfileRecord(
            profileId = json.getString("profileId"),
            displayName = json.getString("displayName"),
            updatedAt = json.optLong("updatedAt", 0L),
        )

    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.optNullableLong(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.optNullableInt(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null
}
