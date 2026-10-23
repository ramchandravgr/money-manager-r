package com.mmr.core.backup

import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.backup.model.MmrBackupPayload
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanInterestRevisionEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.LoanScheduleEntryEntity
import com.mmr.core.database.entity.NotificationRuleEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.entity.SmsMessageRecordEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.*
import org.json.JSONArray
import org.json.JSONObject

object BackupJsonCodec {
    const val CURRENT_FORMAT_VERSION = 2
    const val MIN_SUPPORTED_FORMAT_VERSION = 1
    const val EXPECTED_APP_ID = "com.mmr.app"

    fun encode(payload: MmrBackupPayload): String =
        JSONObject()
            .apply {
                put("formatVersion", payload.formatVersion)
                put("appId", payload.appId)
                put("exportedAtEpochMillis", payload.exportedAtEpochMillis)
                put("databaseVersion", payload.databaseVersion)
                put("categories", payload.categories.toJsonArray { categoryToJson(it) })
                put("subcategories", payload.subcategories.toJsonArray { subcategoryToJson(it) })
                put("accounts", payload.accounts.toJsonArray { accountToJson(it) })
                put("loans", payload.loans.toJsonArray { loanToJson(it) })
                put(
                    "loanInterestRevisions",
                    payload.loanInterestRevisions.toJsonArray { loanInterestRevisionToJson(it) },
                )
                put("loanScheduleEntries", payload.loanScheduleEntries.toJsonArray { loanScheduleEntryToJson(it) })
                put("transactions", payload.transactions.toJsonArray { transactionToJson(it) })
                put("loanPayments", payload.loanPayments.toJsonArray { loanPaymentToJson(it) })
                put("recurringRules", payload.recurringRules.toJsonArray { recurringRuleToJson(it) })
                put("notificationRules", payload.notificationRules.toJsonArray { notificationRuleToJson(it) })
                put("smsMessageRecords", payload.smsMessageRecords.toJsonArray { smsRecordToJson(it) })
                put("transactionApprovals", payload.transactionApprovals.toJsonArray { approvalToJson(it) })
                payload.appSettings?.let { settings ->
                    put("appSettings", appSettingsToJson(settings))
                }
            }.toString(2)

    fun decode(json: String): MmrBackupPayload {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Backup file is empty.")
        }
        val root =
            try {
                JSONObject(trimmed)
            } catch (e: Exception) {
                throw IllegalArgumentException("Backup file is not valid JSON.", e)
            }
        val formatVersion = root.optInt("formatVersion", -1)
        if (formatVersion < MIN_SUPPORTED_FORMAT_VERSION || formatVersion > CURRENT_FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported backup format version: $formatVersion")
        }
        val appId = root.optString("appId", "")
        if (appId != EXPECTED_APP_ID) {
            throw IllegalArgumentException("This backup file is not for Money Manager R.")
        }

        return MmrBackupPayload(
            formatVersion = formatVersion,
            appId = appId,
            exportedAtEpochMillis = root.optLong("exportedAtEpochMillis", 0L),
            databaseVersion = root.optInt("databaseVersion", 0),
            categories = root.readArray("categories").mapObjects { categoryFromJson(it) },
            subcategories = root.readArray("subcategories").mapObjects { subcategoryFromJson(it) },
            accounts = root.readArray("accounts").mapObjects { accountFromJson(it) },
            loans = root.readArray("loans").mapObjects { loanFromJson(it) },
            loanInterestRevisions =
                root
                    .readArray(
                        "loanInterestRevisions",
                    ).mapObjects { loanInterestRevisionFromJson(it) },
            loanScheduleEntries =
                root
                    .readArray(
                        "loanScheduleEntries",
                    ).mapObjects { loanScheduleEntryFromJson(it) },
            transactions = root.readArray("transactions").mapObjects { transactionFromJson(it) },
            loanPayments = root.readArray("loanPayments").mapObjects { loanPaymentFromJson(it) },
            recurringRules = root.readArray("recurringRules").mapObjects { recurringRuleFromJson(it) },
            notificationRules = root.readArray("notificationRules").mapObjects { notificationRuleFromJson(it) },
            smsMessageRecords = root.readArray("smsMessageRecords").mapObjects { smsRecordFromJson(it) },
            transactionApprovals = root.readArray("transactionApprovals").mapObjects { approvalFromJson(it) },
            appSettings =
                if (root.has("appSettings") && !root.isNull("appSettings")) {
                    appSettingsFromJson(root.getJSONObject("appSettings"))
                } else {
                    null
                },
        )
    }

    private fun appSettingsToJson(settings: MmrAppSettingsBackup) =
        JSONObject().apply {
            put("smsScanMonths", settings.smsScanMonths)
        }

    private fun appSettingsFromJson(json: JSONObject) =
        MmrAppSettingsBackup(
            smsScanMonths = json.getInt("smsScanMonths"),
        )

    private fun loanInterestRevisionToJson(entity: LoanInterestRevisionEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("loanId", entity.loanId)
            put("effectiveFromMillis", entity.effectiveFromMillis)
            put("annualRatePercent", entity.annualRatePercent)
            putNullableString("note", entity.note)
            put("createdAt", entity.createdAt)
        }

    private fun loanInterestRevisionFromJson(json: JSONObject) =
        LoanInterestRevisionEntity(
            id = json.getLong("id"),
            loanId = json.getLong("loanId"),
            effectiveFromMillis = json.getLong("effectiveFromMillis"),
            annualRatePercent = json.getDouble("annualRatePercent"),
            note = json.optNullableString("note"),
            createdAt = json.getLong("createdAt"),
        )

    private fun loanScheduleEntryToJson(entity: LoanScheduleEntryEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("loanId", entity.loanId)
            put("sequenceNumber", entity.sequenceNumber)
            put("dueDateMillis", entity.dueDateMillis)
            put("emiAmount", entity.emiAmount)
            put("principalPlanned", entity.principalPlanned)
            put("interestPlanned", entity.interestPlanned)
            put("status", entity.status.name)
            putNullableLong("linkedPaymentId", entity.linkedPaymentId)
            put("annualRatePercentAtPlan", entity.annualRatePercentAtPlan)
        }

    private fun loanScheduleEntryFromJson(json: JSONObject) =
        LoanScheduleEntryEntity(
            id = json.getLong("id"),
            loanId = json.getLong("loanId"),
            sequenceNumber = json.getInt("sequenceNumber"),
            dueDateMillis = json.getLong("dueDateMillis"),
            emiAmount = json.getDouble("emiAmount"),
            principalPlanned = json.getDouble("principalPlanned"),
            interestPlanned = json.getDouble("interestPlanned"),
            status = LoanScheduleStatus.valueOf(json.getString("status")),
            linkedPaymentId = json.optNullableLong("linkedPaymentId"),
            annualRatePercentAtPlan = json.getDouble("annualRatePercentAtPlan"),
        )

    private fun JSONObject.readArray(name: String): JSONArray =
        if (has(name) && !isNull(name)) getJSONArray(name) else JSONArray()

    private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray =
        JSONArray().also { array ->
            forEach { array.put(mapper(it)) }
        }

    private fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> {
        val items = mutableListOf<T>()
        for (index in 0 until length()) {
            items.add(mapper(getJSONObject(index)))
        }
        return items
    }

    private fun categoryToJson(entity: CategoryEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("syncId", entity.syncId)
            put("name", entity.name)
            put("iconName", entity.iconName)
            put("colorHex", entity.colorHex)
            put("type", entity.type.name)
            put("isDefault", entity.isDefault)
            put("isActive", entity.isActive)
            put("orderIndex", entity.orderIndex)
        }

    private fun categoryFromJson(json: JSONObject) =
        CategoryEntity(
            id = json.getLong("id"),
            syncId = syncIdFromJson(json),
            name = json.getString("name"),
            iconName = json.getString("iconName"),
            colorHex = json.getString("colorHex"),
            type = CategoryType.valueOf(json.getString("type")),
            isDefault = json.getBoolean("isDefault"),
            isActive = json.getBoolean("isActive"),
            orderIndex = json.getInt("orderIndex"),
        )

    private fun subcategoryToJson(entity: SubcategoryEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("syncId", entity.syncId)
            put("categoryId", entity.categoryId)
            put("name", entity.name)
            put("isActive", entity.isActive)
            put("orderIndex", entity.orderIndex)
        }

    private fun subcategoryFromJson(json: JSONObject) =
        SubcategoryEntity(
            id = json.getLong("id"),
            syncId = syncIdFromJson(json),
            categoryId = json.getLong("categoryId"),
            name = json.getString("name"),
            isActive = json.getBoolean("isActive"),
            orderIndex = json.getInt("orderIndex"),
        )

    private fun accountToJson(entity: AccountEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("syncId", entity.syncId)
            put("name", entity.name)
            put("type", entity.type.name)
            put("openingBalance", entity.openingBalance)
            put("currentBalance", entity.currentBalance)
            put("currencyCode", entity.currencyCode)
            put("isActive", entity.isActive)
            put("isDefault", entity.isDefault)
            putNullableString("cardLastFour", entity.cardLastFour)
            put("createdAt", entity.createdAt)
            put("updatedAt", entity.updatedAt)
        }

    private fun accountFromJson(json: JSONObject) =
        AccountEntity(
            id = json.getLong("id"),
            syncId = syncIdFromJson(json),
            name = json.getString("name"),
            type = AccountType.valueOf(json.getString("type")),
            openingBalance = json.getDouble("openingBalance"),
            currentBalance = json.getDouble("currentBalance"),
            currencyCode = json.getString("currencyCode"),
            isActive = json.getBoolean("isActive"),
            isDefault = json.getBoolean("isDefault"),
            cardLastFour = json.optNullableString("cardLastFour"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )

    private fun loanToJson(entity: LoanEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("syncId", entity.syncId)
            put("name", entity.name)
            put("lenderName", entity.lenderName)
            put("principalAmount", entity.principalAmount)
            put("interestRate", entity.interestRate)
            put("outstandingAmount", entity.outstandingAmount)
            put("openingDate", entity.openingDate)
            putNullableLong("dueDate", entity.dueDate)
            putNullableLong("autoTransferDate", entity.autoTransferDate)
            putNullableLong("linkedDebitAccountId", entity.linkedDebitAccountId)
            putNullableLong("linkedLoanAccountId", entity.linkedLoanAccountId)
            put("loanType", entity.loanType.name)
            put("status", entity.status.name)
            putNullableString("repaymentMode", entity.repaymentMode)
            putNullableInt("tenureMonths", entity.tenureMonths)
            putNullableInt("remainingTenureMonths", entity.remainingTenureMonths)
            putNullableDouble("fixedEmiAmount", entity.fixedEmiAmount)
            putNullableInt("emiDayOfMonth", entity.emiDayOfMonth)
            putNullableString("fixedEmiInputMode", entity.fixedEmiInputMode)
            putNullableString("notes", entity.notes)
            put("createdAt", entity.createdAt)
            put("updatedAt", entity.updatedAt)
        }

    private fun loanFromJson(json: JSONObject) =
        LoanEntity(
            id = json.getLong("id"),
            syncId = syncIdFromJson(json),
            name = json.getString("name"),
            lenderName = json.getString("lenderName"),
            principalAmount = json.getDouble("principalAmount"),
            interestRate = json.getDouble("interestRate"),
            outstandingAmount = json.getDouble("outstandingAmount"),
            openingDate = json.getLong("openingDate"),
            dueDate = json.optNullableLong("dueDate"),
            autoTransferDate = json.optNullableLong("autoTransferDate"),
            linkedDebitAccountId = json.optNullableLong("linkedDebitAccountId"),
            linkedLoanAccountId = json.optNullableLong("linkedLoanAccountId"),
            loanType = LoanType.valueOf(json.getString("loanType")),
            status = LoanStatus.valueOf(json.getString("status")),
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

    private fun transactionToJson(entity: TransactionEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("syncId", entity.syncId)
            put("dateTime", entity.dateTime)
            put("amount", entity.amount)
            put("type", entity.type.name)
            putNullableLong("categoryId", entity.categoryId)
            putNullableLong("subcategoryId", entity.subcategoryId)
            putNullableString("note", entity.note)
            put("accountId", entity.accountId)
            putNullableLong("relatedAccountId", entity.relatedAccountId)
            putNullableLong("loanId", entity.loanId)
            put("sourceType", entity.sourceType.name)
            putNullableString("sourceReferenceId", entity.sourceReferenceId)
            put("status", entity.status.name)
            put("attachmentCount", entity.attachmentCount)
            put("createdAt", entity.createdAt)
            put("updatedAt", entity.updatedAt)
            put("isEdited", entity.isEdited)
            putNullableString("paymentChannel", entity.paymentChannel)
            putNullableString("paymentInstrument", entity.paymentInstrument)
            putNullableString("cardLastFour", entity.cardLastFour)
            putNullableString("cardNetwork", entity.cardNetwork?.name)
        }

    private fun syncIdFromJson(json: JSONObject): String =
        json.optString("syncId").ifBlank {
            com.mmr.core.common.sync.MmrSyncId
                .newId()
        }

    private fun transactionFromJson(json: JSONObject) =
        TransactionEntity(
            id = json.getLong("id"),
            syncId = syncIdFromJson(json),
            dateTime = json.getLong("dateTime"),
            amount = json.getDouble("amount"),
            type = TransactionType.valueOf(json.getString("type")),
            categoryId = json.optNullableLong("categoryId"),
            subcategoryId = json.optNullableLong("subcategoryId"),
            note = json.optNullableString("note"),
            accountId = json.getLong("accountId"),
            relatedAccountId = json.optNullableLong("relatedAccountId"),
            loanId = json.optNullableLong("loanId"),
            sourceType = TransactionSourceType.valueOf(json.getString("sourceType")),
            sourceReferenceId = json.optNullableString("sourceReferenceId"),
            status = RecordStatus.valueOf(json.getString("status")),
            attachmentCount = json.getInt("attachmentCount"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            isEdited = json.getBoolean("isEdited"),
            paymentChannel = json.optNullableString("paymentChannel"),
            paymentInstrument = json.optNullableString("paymentInstrument"),
            cardLastFour = json.optNullableString("cardLastFour"),
            cardNetwork = json.optNullableString("cardNetwork")?.let { CardNetwork.valueOf(it) },
        )

    private fun loanPaymentToJson(entity: LoanPaymentEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("loanId", entity.loanId)
            putNullableLong("transactionId", entity.transactionId)
            put("paymentAmount", entity.paymentAmount)
            put("paymentDate", entity.paymentDate)
            put("paymentType", entity.paymentType.name)
            putNullableDouble("principalComponent", entity.principalComponent)
            putNullableDouble("interestComponent", entity.interestComponent)
            put("outstandingAfterPayment", entity.outstandingAfterPayment)
            putNullableString("notes", entity.notes)
        }

    private fun loanPaymentFromJson(json: JSONObject) =
        LoanPaymentEntity(
            id = json.getLong("id"),
            loanId = json.getLong("loanId"),
            transactionId = json.optNullableLong("transactionId"),
            paymentAmount = json.getDouble("paymentAmount"),
            paymentDate = json.getLong("paymentDate"),
            paymentType = LoanPaymentType.valueOf(json.getString("paymentType")),
            principalComponent = json.optNullableDouble("principalComponent"),
            interestComponent = json.optNullableDouble("interestComponent"),
            outstandingAfterPayment = json.getDouble("outstandingAfterPayment"),
            notes = json.optNullableString("notes"),
        )

    private fun recurringRuleToJson(entity: RecurringRuleEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("name", entity.name)
            put("transactionTemplateType", entity.transactionTemplateType.name)
            put("amount", entity.amount)
            putNullableLong("categoryId", entity.categoryId)
            putNullableLong("subcategoryId", entity.subcategoryId)
            put("accountId", entity.accountId)
            putNullableInt("dayOfMonth", entity.dayOfMonth)
            put("recurrenceInterval", entity.recurrenceInterval.name)
            put("nextRunDate", entity.nextRunDate)
            put("isEnabled", entity.isEnabled)
            putNullableLong("lastGeneratedDate", entity.lastGeneratedDate)
            putNullableLong("endDate", entity.endDate)
            putNullableString("noteTemplate", entity.noteTemplate)
            put("reminderMinutesBefore", entity.reminderMinutesBefore)
            put("createdAt", entity.createdAt)
            put("updatedAt", entity.updatedAt)
        }

    private fun recurringRuleFromJson(json: JSONObject) =
        RecurringRuleEntity(
            id = json.getLong("id"),
            name = json.getString("name"),
            transactionTemplateType = TransactionType.valueOf(json.getString("transactionTemplateType")),
            amount = json.getDouble("amount"),
            categoryId = json.optNullableLong("categoryId"),
            subcategoryId = json.optNullableLong("subcategoryId"),
            accountId = json.getLong("accountId"),
            dayOfMonth = json.optNullableInt("dayOfMonth"),
            recurrenceInterval = RecurrenceInterval.valueOf(json.getString("recurrenceInterval")),
            nextRunDate = json.getLong("nextRunDate"),
            isEnabled = json.getBoolean("isEnabled"),
            lastGeneratedDate = json.optNullableLong("lastGeneratedDate"),
            endDate = json.optNullableLong("endDate"),
            noteTemplate = json.optNullableString("noteTemplate"),
            reminderMinutesBefore = json.getInt("reminderMinutesBefore"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )

    private fun notificationRuleToJson(entity: NotificationRuleEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("type", entity.type.name)
            put("enabled", entity.enabled)
            putNullableLong("scheduleTime", entity.scheduleTime)
            putNullableLong("targetLoanId", entity.targetLoanId)
            putNullableLong("targetRecurringRuleId", entity.targetRecurringRuleId)
            putNullableDouble("thresholdValue", entity.thresholdValue)
            putNullableString("title", entity.title)
            putNullableString("message", entity.message)
            put("isFired", entity.isFired)
            put("createdAt", entity.createdAt)
        }

    private fun notificationRuleFromJson(json: JSONObject) =
        NotificationRuleEntity(
            id = json.getLong("id"),
            type = NotificationRuleType.valueOf(json.getString("type")),
            enabled = json.getBoolean("enabled"),
            scheduleTime = json.optNullableLong("scheduleTime"),
            targetLoanId = json.optNullableLong("targetLoanId"),
            targetRecurringRuleId = json.optNullableLong("targetRecurringRuleId"),
            thresholdValue = json.optNullableDouble("thresholdValue"),
            title = json.optNullableString("title"),
            message = json.optNullableString("message"),
            isFired = json.getBoolean("isFired"),
            createdAt = json.getLong("createdAt"),
        )

    private fun smsRecordToJson(entity: SmsMessageRecordEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("sender", entity.sender)
            put("messageBodyHash", entity.messageBodyHash)
            put("messageBodySnippet", entity.messageBodySnippet)
            putNullableString("messageBodyFull", entity.messageBodyFull)
            put("receivedTime", entity.receivedTime)
            put("parsedStatus", entity.parsedStatus.name)
            putNullableDouble("parsedAmount", entity.parsedAmount)
            putNullableLong("parsedDateTime", entity.parsedDateTime)
            putNullableLong("matchedTransactionId", entity.matchedTransactionId)
            put("approvalState", entity.approvalState.name)
            putNullableString("rawMessageId", entity.rawMessageId)
            putNullableLong("processedAt", entity.processedAt)
        }

    private fun smsRecordFromJson(json: JSONObject) =
        SmsMessageRecordEntity(
            id = json.getLong("id"),
            sender = json.getString("sender"),
            messageBodyHash = json.getString("messageBodyHash"),
            messageBodySnippet = json.getString("messageBodySnippet"),
            messageBodyFull = json.optNullableString("messageBodyFull"),
            receivedTime = json.getLong("receivedTime"),
            parsedStatus = SmsParsedStatus.valueOf(json.getString("parsedStatus")),
            parsedAmount = json.optNullableDouble("parsedAmount"),
            parsedDateTime = json.optNullableLong("parsedDateTime"),
            matchedTransactionId = json.optNullableLong("matchedTransactionId"),
            approvalState = SmsApprovalState.valueOf(json.getString("approvalState")),
            rawMessageId = json.optNullableString("rawMessageId"),
            processedAt = json.optNullableLong("processedAt"),
        )

    private fun approvalToJson(entity: TransactionApprovalEntity) =
        JSONObject().apply {
            put("id", entity.id)
            put("smsMessageRecordId", entity.smsMessageRecordId)
            put("extractedAmount", entity.extractedAmount)
            put("extractedType", entity.extractedType.name)
            put("extractedDateTime", entity.extractedDateTime)
            put("extractedSender", entity.extractedSender)
            putNullableString("extractedMerchant", entity.extractedMerchant)
            putNullableLong("extractedCategoryId", entity.extractedCategoryId)
            putNullableLong("extractedAccountId", entity.extractedAccountId)
            putNullableString("extractedNote", entity.extractedNote)
            put("confidenceScore", entity.confidenceScore)
            put("userEdited", entity.userEdited)
            put("approvalState", entity.approvalState.name)
            put("rawBodySnippet", entity.rawBodySnippet)
            putNullableString("extractedTemplateId", entity.extractedTemplateId)
            putNullableString("extractedPaymentChannel", entity.extractedPaymentChannel)
            putNullableString("extractedInstrument", entity.extractedInstrument)
            putNullableString("extractedCardLastFour", entity.extractedCardLastFour)
            putNullableString("extractedCardNetwork", entity.extractedCardNetwork?.name)
            put("createdAt", entity.createdAt)
            put("updatedAt", entity.updatedAt)
        }

    private fun approvalFromJson(json: JSONObject) =
        TransactionApprovalEntity(
            id = json.getLong("id"),
            smsMessageRecordId = json.getLong("smsMessageRecordId"),
            extractedAmount = json.getDouble("extractedAmount"),
            extractedType = TransactionType.valueOf(json.getString("extractedType")),
            extractedDateTime = json.getLong("extractedDateTime"),
            extractedSender = json.getString("extractedSender"),
            extractedMerchant = json.optNullableString("extractedMerchant"),
            extractedCategoryId = json.optNullableLong("extractedCategoryId"),
            extractedAccountId = json.optNullableLong("extractedAccountId"),
            extractedNote = json.optNullableString("extractedNote"),
            confidenceScore = json.getInt("confidenceScore"),
            userEdited = json.getBoolean("userEdited"),
            approvalState = SmsApprovalState.valueOf(json.getString("approvalState")),
            rawBodySnippet = json.getString("rawBodySnippet"),
            extractedTemplateId = json.optNullableString("extractedTemplateId"),
            extractedPaymentChannel = json.optNullableString("extractedPaymentChannel"),
            extractedInstrument = json.optNullableString("extractedInstrument"),
            extractedCardLastFour = json.optNullableString("extractedCardLastFour"),
            extractedCardNetwork = json.optNullableString("extractedCardNetwork")?.let { CardNetwork.valueOf(it) },
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )

    private fun JSONObject.putNullableString(
        key: String,
        value: String?,
    ) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.putNullableLong(
        key: String,
        value: Long?,
    ) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.putNullableInt(
        key: String,
        value: Int?,
    ) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.putNullableDouble(
        key: String,
        value: Double?,
    ) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return getString(key)
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return getLong(key)
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return getInt(key)
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return getDouble(key)
    }
}
