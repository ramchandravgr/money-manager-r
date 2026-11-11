package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.backup.model.MmrAppSettingsBackup
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import com.rrparallelstudio.miapp.core.lansync.model.LanAttachmentRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanRecurringRuleRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanSmsAccountOverrideRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanSyncTombstoneRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanTransactionRecord
import com.rrparallelstudio.miapp.core.lansync.model.MmrLanSyncPayload
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme
import com.rrparallelstudio.miapp.core.model.settings.MmrInputOrder
import com.rrparallelstudio.miapp.core.model.settings.MmrStartScreenTab
import com.rrparallelstudio.miapp.core.model.settings.MmrTimeDisplay
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class LanSyncJsonCodecTest {
    @Test
    fun roundTrip_preservesTransactionSyncId() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 1L,
                sourceDeviceId = "device-a",
                sourceDeviceName = "Phone A",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions =
                    listOf(
                        LanTransactionRecord(
                            syncId = "txn-1",
                            dateTime = 100L,
                            amount = 50.0,
                            type = "EXPENSE",
                            categorySyncId = null,
                            subcategorySyncId = null,
                            note = "tea",
                            accountSyncId = "acct-1",
                            relatedAccountSyncId = null,
                            loanSyncId = null,
                            sourceType = "MANUAL",
                            sourceReferenceId = null,
                            attachmentCount = 0,
                            createdAt = 100L,
                            updatedAt = 100L,
                            isEdited = false,
                            paymentChannel = null,
                            paymentInstrument = null,
                            cardLastFour = null,
                            cardNetwork = null,
                        ),
                    ),
            )
        val decoded = LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(payload))
        assertEquals("txn-1", decoded.transactions.single().syncId)
    }

    @Test
    fun roundTrip_preservesCardRefundFields() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 1L,
                sourceDeviceId = "device-a",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions =
                    listOf(
                        LanTransactionRecord(
                            syncId = "txn-refund",
                            dateTime = 100L,
                            amount = 80.0,
                            type = "EXPENSE",
                            categorySyncId = null,
                            subcategorySyncId = null,
                            note = null,
                            accountSyncId = "acct-1",
                            relatedAccountSyncId = null,
                            loanSyncId = null,
                            sourceType = "MONEY_MANAGER",
                            sourceReferenceId = "mmbak:42",
                            attachmentCount = 0,
                            createdAt = 100L,
                            updatedAt = 100L,
                            isEdited = false,
                            paymentChannel = null,
                            paymentInstrument = null,
                            cardLastFour = null,
                            cardNetwork = null,
                            originalAmount = 100.0,
                            refundedTotal = 20.0,
                        ),
                    ),
            )
        val decoded = LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(payload)).transactions.single()
        assertEquals(100.0, decoded.originalAmount!!, 0.001)
        assertEquals(20.0, decoded.refundedTotal, 0.001)
        assertEquals("MONEY_MANAGER", decoded.sourceType)
    }

    @Test
    fun decode_omittedRefundFields_defaultSafely() {
        val json =
            """
            {
              "formatVersion": ${LanSyncJsonCodec.CURRENT_FORMAT_VERSION},
              "exportedAtEpochMillis": 1,
              "sourceDeviceId": "a",
              "categories": [],
              "subcategories": [],
              "accounts": [],
              "loans": [],
              "transactions": [{
                "syncId": "t1",
                "dateTime": 1,
                "amount": 1.0,
                "type": "EXPENSE",
                "accountSyncId": "a1",
                "sourceType": "MANUAL",
                "attachmentCount": 0,
                "createdAt": 1,
                "updatedAt": 1,
                "isEdited": false,
                "refundedTotal": 0.0
              }]
            }
            """.trimIndent()
        val decoded = LanSyncJsonCodec.decode(json).transactions.single()
        assertEquals(null, decoded.originalAmount)
        assertEquals(0.0, decoded.refundedTotal, 0.001)
    }

    @Test
    fun roundTrip_preservesAppAndDisplaySettings() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 2L,
                sourceDeviceId = "device-b",
                sourceDeviceName = "Tablet",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                appSettings = MmrAppSettingsBackup(smsScanMonths = 6),
                displaySettings =
                    AppDisplaySettingsSnapshot(
                        mainCurrencyCode = "USD",
                        mainCurrencySymbol = "$",
                        startScreenTab = MmrStartScreenTab.CALENDAR,
                    ),
            )
        val decoded = LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(payload))
        assertEquals(6, decoded.appSettings?.smsScanMonths)
        assertEquals("USD", decoded.displaySettings?.mainCurrencyCode)
        assertEquals(MmrStartScreenTab.CALENDAR, decoded.displaySettings?.startScreenTab)
    }

    @Test
    fun roundTrip_preservesRecurringTransferTargetsOnRecurringRule() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 4L,
                sourceDeviceId = "device-d",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                recurringRules =
                    listOf(
                        LanRecurringRuleRecord(
                            syncId = "rule-transfer",
                            name = "Loan EMI xfer",
                            transactionTemplateType = "TRANSFER",
                            amount = 2000.0,
                            categorySyncId = null,
                            subcategorySyncId = null,
                            accountSyncId = "acct-from",
                            relatedAccountSyncId = "acct-to",
                            loanSyncId = "loan-1",
                            dayOfMonth = 5,
                            recurrenceInterval = "MONTHLY",
                            nextRunDate = 200L,
                            isEnabled = true,
                            lastGeneratedDate = null,
                            endDate = null,
                            noteTemplate = null,
                            reminderMinutesBefore = 0,
                            createdAt = 50L,
                            updatedAt = 60L,
                        ),
                    ),
            )
        val decoded = LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(payload)).recurringRules.single()
        assertEquals("acct-to", decoded.relatedAccountSyncId)
        assertEquals("loan-1", decoded.loanSyncId)
    }

    @Test
    fun roundTrip_preservesRecurringRuleSyncId() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 3L,
                sourceDeviceId = "device-c",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                recurringRules =
                    listOf(
                        LanRecurringRuleRecord(
                            syncId = "rule-1",
                            name = "Rent",
                            transactionTemplateType = "EXPENSE",
                            amount = 15_000.0,
                            categorySyncId = null,
                            subcategorySyncId = null,
                            accountSyncId = "acct-1",
                            dayOfMonth = 1,
                            recurrenceInterval = "MONTHLY",
                            nextRunDate = 100L,
                            isEnabled = true,
                            lastGeneratedDate = null,
                            endDate = null,
                            noteTemplate = null,
                            reminderMinutesBefore = 0,
                            createdAt = 50L,
                            updatedAt = 60L,
                        ),
                    ),
            )
        val decoded = LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(payload))
        assertEquals("rule-1", decoded.recurringRules.single().syncId)
        assertEquals(60L, decoded.recurringRules.single().updatedAt)
    }

    @Test
    fun roundTrip_preservesAttachmentMetadata() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 3L,
                sourceDeviceId = "device-c",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                attachments =
                    listOf(
                        LanAttachmentRecord(
                            syncId = "att-1",
                            transactionSyncId = "txn-1",
                            mimeType = "image/jpeg",
                            fileSizeBytes = 1024L,
                            createdAt = 1L,
                            updatedAt = 2L,
                        ),
                    ),
            )
        val decoded = LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(payload))
        assertEquals("att-1", decoded.attachments.single().syncId)
        assertEquals("txn-1", decoded.attachments.single().transactionSyncId)
    }

    @Test
    fun roundTrip_preservesTombstones() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 4L,
                sourceDeviceId = "device-d",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                tombstones =
                    listOf(
                        LanSyncTombstoneRecord(
                            syncId = "txn-gone",
                            entityType = SyncEntityType.TRANSACTION,
                            deletedAt = 99L,
                            sourceDeviceId = "device-d",
                        ),
                    ),
            )
        val decoded = LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(payload)).tombstones.single()
        assertEquals("txn-gone", decoded.syncId)
        assertEquals(99L, decoded.deletedAt)
    }

    @Test
    fun decode_skipsMalformedTransactionRows() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 1L,
                sourceDeviceId = "device-a",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions =
                    listOf(
                        LanTransactionRecord(
                            syncId = "txn-ok",
                            dateTime = 100L,
                            amount = 50.0,
                            type = "EXPENSE",
                            categorySyncId = null,
                            subcategorySyncId = null,
                            note = null,
                            accountSyncId = "acct-1",
                            relatedAccountSyncId = null,
                            loanSyncId = null,
                            sourceType = "MANUAL",
                            sourceReferenceId = null,
                            attachmentCount = 0,
                            createdAt = 100L,
                            updatedAt = 100L,
                            isEdited = false,
                            paymentChannel = null,
                            paymentInstrument = null,
                            cardLastFour = null,
                            cardNetwork = null,
                        ),
                    ),
            )
        val root = JSONObject(LanSyncJsonCodec.encode(payload))
        val transactions = root.getJSONArray("transactions")
        transactions.put(JSONObject().put("amount", 1.0))
        val decoded = LanSyncJsonCodec.decode(root.toString())
        assertEquals("txn-ok", decoded.transactions.single().syncId)
    }

    @Test
    fun decode_displaySettings_unknownEnumsUseDefaults() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 1L,
                sourceDeviceId = "device-a",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                displaySettings =
                    AppDisplaySettingsSnapshot(
                        startScreenTab = MmrStartScreenTab.CALENDAR,
                        amountColorScheme = MmrAmountColorScheme.SET_C,
                        timeDisplay = MmrTimeDisplay.SHOW_DESC,
                        inputOrder = MmrInputOrder.FROM_CATEGORY,
                    ),
            )
        val root = JSONObject(LanSyncJsonCodec.encode(payload))
        val display = root.getJSONObject("displaySettings")
        display.put("startScreenTab", "NOT_A_TAB")
        display.put("timeDisplay", "NOT_TIME")
        display.put("inputOrder", "NOT_ORDER")
        display.put("amountColorScheme", "NOT_SCHEME")
        val decoded = LanSyncJsonCodec.decode(root.toString()).displaySettings!!
        assertEquals(MmrStartScreenTab.DAILY, decoded.startScreenTab)
        assertEquals(MmrTimeDisplay.NONE_ASC, decoded.timeDisplay)
        assertEquals(MmrInputOrder.FROM_AMOUNT, decoded.inputOrder)
        assertEquals(MmrAmountColorScheme.SET_B, decoded.amountColorScheme)
    }

    @Test
    fun roundTrip_preservesSmsAccountOverrides() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 1L,
                sourceDeviceId = "device-a",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                smsAccountOverrides =
                    listOf(
                        LanSmsAccountOverrideRecord(
                            templateId = "hdfc_cc_spent",
                            instrument = "CREDIT_CARD",
                            smsLastFour = "3903",
                            accountSyncId = "acct-savings",
                            updatedAt = 99L,
                        ),
                    ),
            )
        val decoded = LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(payload))
        assertEquals(payload.smsAccountOverrides, decoded.smsAccountOverrides)
    }

    @Test
    fun roundTrip_preservesSourceListenPort() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 1L,
                sourceDeviceId = "device-a",
                sourceListenPort = 39403,
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
            )
        val decoded = LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(payload))
        assertEquals(39403, decoded.sourceListenPort)
        val withoutPort = JSONObject(LanSyncJsonCodec.encode(payload)).apply { remove("sourceListenPort") }
        assertEquals(null, LanSyncJsonCodec.decode(withoutPort.toString()).sourceListenPort)
    }
}
