package com.mmr.core.lansync

import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot
import com.mmr.core.lansync.model.LanAttachmentRecord
import com.mmr.core.lansync.model.LanRecurringRuleRecord
import com.mmr.core.lansync.model.LanSyncTombstoneRecord
import com.mmr.core.lansync.model.LanTransactionRecord
import com.mmr.core.lansync.model.MmrLanSyncPayload
import com.mmr.core.model.settings.MmrStartScreenTab
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
}
