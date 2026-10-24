package com.mmr.core.lansync

import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot
import com.mmr.core.lansync.model.LanAccountRecord
import com.mmr.core.lansync.model.LanAttachmentRecord
import com.mmr.core.lansync.model.LanCategorizationRuleRecord
import com.mmr.core.lansync.model.LanCategoryRecord
import com.mmr.core.lansync.model.LanLoanInterestRevisionRecord
import com.mmr.core.lansync.model.LanLoanPaymentRecord
import com.mmr.core.lansync.model.LanLoanRecord
import com.mmr.core.lansync.model.LanLoanScheduleEntryRecord
import com.mmr.core.lansync.model.LanNotificationRuleRecord
import com.mmr.core.lansync.model.LanRecurringRuleRecord
import com.mmr.core.lansync.model.LanSubcategoryRecord
import com.mmr.core.lansync.model.LanSyncTombstoneRecord
import com.mmr.core.lansync.model.LanTransactionRecord
import com.mmr.core.lansync.model.MmrLanSyncPayload
import org.json.JSONArray
import org.json.JSONObject

object LanSyncJsonCodec {
    const val CURRENT_FORMAT_VERSION = 3

    fun encode(payload: MmrLanSyncPayload): String =
        JSONObject()
            .apply {
                put("formatVersion", payload.formatVersion)
                put("exportedAtEpochMillis", payload.exportedAtEpochMillis)
                put("sourceDeviceId", payload.sourceDeviceId)
                putNullableString("sourceDeviceName", payload.sourceDeviceName)
                put("categories", payload.categories.toJsonArray { categoryToJson(it) })
                put("subcategories", payload.subcategories.toJsonArray { subcategoryToJson(it) })
                put("accounts", payload.accounts.toJsonArray { accountToJson(it) })
                put("loans", payload.loans.toJsonArray { loanToJson(it) })
                put("transactions", payload.transactions.toJsonArray { transactionToJson(it) })
                put("loanPayments", payload.loanPayments.toJsonArray { loanPaymentToJson(it) })
                put("loanInterestRevisions", payload.loanInterestRevisions.toJsonArray { loanRevisionToJson(it) })
                put("loanScheduleEntries", payload.loanScheduleEntries.toJsonArray { loanScheduleToJson(it) })
                put("recurringRules", payload.recurringRules.toJsonArray { recurringToJson(it) })
                put("categorizationRules", payload.categorizationRules.toJsonArray { categorizationToJson(it) })
                put("notificationRules", payload.notificationRules.toJsonArray { notificationToJson(it) })
                payload.appSettings?.let { put("appSettings", appSettingsToJson(it)) }
                payload.displaySettings?.let { put("displaySettings", displaySettingsToJson(it)) }
                if (payload.attachments.isNotEmpty()) {
                    put("attachments", payload.attachments.toJsonArray { attachmentToJson(it) })
                }
                if (payload.tombstones.isNotEmpty()) {
                    put("tombstones", payload.tombstones.toJsonArray { tombstoneToJson(it) })
                }
            }.toString()

    fun decode(json: String): MmrLanSyncPayload {
        val root = JSONObject(json)
        val version = root.getInt("formatVersion")
        if (version !in 1..CURRENT_FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported LAN sync format version: $version")
        }
        return MmrLanSyncPayload(
            formatVersion = version,
            exportedAtEpochMillis = root.getLong("exportedAtEpochMillis"),
            sourceDeviceId = root.getString("sourceDeviceId"),
            sourceDeviceName = root.optNullableString("sourceDeviceName"),
            categories = root.readArray("categories").mapObjects { categoryFromJson(it) },
            subcategories = root.readArray("subcategories").mapObjects { subcategoryFromJson(it) },
            accounts = root.readArray("accounts").mapObjects { accountFromJson(it) },
            loans = root.readArray("loans").mapObjects { loanFromJson(it) },
            transactions = root.readArray("transactions").mapObjects { transactionFromJson(it) },
            loanPayments = root.readArray("loanPayments").mapObjects { loanPaymentFromJson(it) },
            loanInterestRevisions = root.readArray("loanInterestRevisions").mapObjects { loanRevisionFromJson(it) },
            loanScheduleEntries = root.readArray("loanScheduleEntries").mapObjects { loanScheduleFromJson(it) },
            recurringRules = root.readArray("recurringRules").mapObjects { recurringFromJson(it) },
            categorizationRules = root.readArray("categorizationRules").mapObjects { categorizationFromJson(it) },
            notificationRules = root.readArray("notificationRules").mapObjects { notificationFromJson(it) },
            appSettings = root.optJSONObject("appSettings")?.let { appSettingsFromJson(it) },
            displaySettings = root.optJSONObject("displaySettings")?.let { displaySettingsFromJson(it) },
            attachments = root.readArray("attachments").mapObjects { attachmentFromJson(it) },
            tombstones = root.readArray("tombstones").mapObjects { tombstoneFromJson(it) },
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
            putNullableString("fixedEmiInputMode", record.fixedEmiInputMode)
            putNullableString("notes", record.notes)
            put("createdAt", record.createdAt)
            put("updatedAt", record.updatedAt)
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
            fixedEmiInputMode = json.optNullableString("fixedEmiInputMode"),
            notes = json.optNullableString("notes"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
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
            putNullableString("syncedFromDeviceId", record.syncedFromDeviceId)
            putNullableString("syncedFromDeviceName", record.syncedFromDeviceName)
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
            syncedFromDeviceId = json.optNullableString("syncedFromDeviceId"),
            syncedFromDeviceName = json.optNullableString("syncedFromDeviceName"),
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
        MmrAppSettingsBackup(smsScanMonths = json.getInt("smsScanMonths"))

    private fun displaySettingsToJson(snapshot: AppDisplaySettingsSnapshot) =
        JSONObject().apply {
            put("mainCurrencyCode", snapshot.mainCurrencyCode)
            putNullableString("subCurrencyCode", snapshot.subCurrencyCode)
            put("startScreenTab", snapshot.startScreenTab.name)
            put("monthStartDay", snapshot.monthStartDay)
            put("weekStartDay", snapshot.weekStartDay.name)
            put("budgetCarryOver", snapshot.budgetCarryOver)
            put("swipeBehavior", snapshot.swipeBehavior.name)
            put("amountColorScheme", snapshot.amountColorScheme.name)
            put("timeDisplay", snapshot.timeDisplay.name)
            put("showDescription", snapshot.showDescription)
            put("autocompleteEnabled", snapshot.autocompleteEnabled)
            put("inputOrder", snapshot.inputOrder.name)
            put("appColorTheme", snapshot.appColorTheme.name)
        }

    private fun displaySettingsFromJson(json: JSONObject): AppDisplaySettingsSnapshot {
        val mainCode = json.getString("mainCurrencyCode")
        return AppDisplaySettingsSnapshot(
            mainCurrencyCode = mainCode,
            mainCurrencySymbol = currencySymbol(mainCode),
            subCurrencyCode = json.optNullableString("subCurrencyCode"),
            startScreenTab = enumValueOf(json.getString("startScreenTab")),
            monthStartDay = json.getInt("monthStartDay"),
            weekStartDay = enumValueOf(json.getString("weekStartDay")),
            budgetCarryOver = json.getBoolean("budgetCarryOver"),
            swipeBehavior = enumValueOf(json.getString("swipeBehavior")),
            amountColorScheme = enumValueOf(json.getString("amountColorScheme")),
            timeDisplay = enumValueOf(json.getString("timeDisplay")),
            showDescription = json.getBoolean("showDescription"),
            autocompleteEnabled = json.getBoolean("autocompleteEnabled"),
            inputOrder = enumValueOf(json.getString("inputOrder")),
            appColorTheme =
                json.optString("appColorTheme").takeIf { it.isNotBlank() }?.let {
                    runCatching {
                        com.mmr.core.model.settings.MmrAppColorTheme
                            .valueOf(it)
                    }.getOrNull()
                } ?: com.mmr.core.model.settings.MmrAppColorTheme.CLASSIC_CORAL,
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

    private fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> {
        val items = mutableListOf<T>()
        for (index in 0 until length()) {
            items.add(mapper(getJSONObject(index)))
        }
        return items
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

    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.optNullableLong(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.optNullableInt(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null
}
