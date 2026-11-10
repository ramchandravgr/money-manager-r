package com.rrparallelstudio.miapp.core.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.backup.model.MmrAppSettingsBackup
import com.rrparallelstudio.miapp.core.backup.model.MmrBackupPayload
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.SyncTombstoneEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.lansync.SyncEntityType
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.BudgetPeriodType
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.core.model.enums.RecordStatus
import com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupJsonCodecInstrumentedTest {
    @Test
    fun encodeDecode_roundTrip_preservesCoreRecords() {
        val payload =
            MmrBackupPayload(
                formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
                appId = BackupJsonCodec.EXPECTED_APP_ID,
                exportedAtEpochMillis = 1_700_000_000_000L,
                databaseVersion = 2,
                categories =
                    listOf(
                        CategoryEntity(id = 1L, name = "Food", type = CategoryType.EXPENSE, isDefault = true),
                    ),
                subcategories = emptyList(),
                accounts =
                    listOf(
                        AccountEntity(id = 2L, name = "Cash", type = AccountType.CASH, isDefault = true),
                    ),
                loans = emptyList(),
                transactions =
                    listOf(
                        TransactionEntity(
                            id = 3L,
                            dateTime = 1_700_000_000_000L,
                            amount = 42.5,
                            type = TransactionType.EXPENSE,
                            categoryId = 1L,
                            accountId = 2L,
                            note = "Lunch",
                            status = RecordStatus.APPROVED,
                            sourceType = TransactionSourceType.MANUAL,
                        ),
                    ),
                loanPayments = emptyList(),
                recurringRules = emptyList(),
                notificationRules = emptyList(),
                smsMessageRecords = emptyList(),
                transactionApprovals = emptyList(),
            )

        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(payload))

        assertEquals(1, decoded.categories.size)
        assertEquals("Food", decoded.categories.first().name)
        assertEquals(42.5, decoded.transactions.first().amount, 0.001)
    }

    @Test
    fun encodeDecode_roundTrip_preservesBudgets() {
        val now = 1_700_000_000_000L
        val payload =
            MmrBackupPayload(
                formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
                appId = BackupJsonCodec.EXPECTED_APP_ID,
                exportedAtEpochMillis = now,
                databaseVersion = 19,
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                loanPayments = emptyList(),
                recurringRules = emptyList(),
                budgets =
                    listOf(
                        BudgetEntity(
                            id = 9L,
                            syncId = "budget-sync-1",
                            categoryId = 1L,
                            amountLimit = 5000.0,
                            periodType = BudgetPeriodType.MONTHLY,
                            alertOnExceeded = true,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    ),
                notificationRules = emptyList(),
                smsMessageRecords = emptyList(),
                transactionApprovals = emptyList(),
            )
        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(payload))
        assertEquals(1, decoded.budgets.size)
        assertEquals(5000.0, decoded.budgets.first().amountLimit, 0.001)
        assertTrue(decoded.budgets.first().alertOnExceeded)
    }

    @Test
    fun encodeDecode_roundTrip_preservesAppSettings() {
        val payload =
            MmrBackupPayload(
                formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
                appId = BackupJsonCodec.EXPECTED_APP_ID,
                exportedAtEpochMillis = 1_700_000_000_000L,
                databaseVersion = 2,
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                loanPayments = emptyList(),
                recurringRules = emptyList(),
                notificationRules = emptyList(),
                smsMessageRecords = emptyList(),
                transactionApprovals = emptyList(),
                appSettings = MmrAppSettingsBackup(smsScanMonths = 9),
            )
        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(payload))
        assertEquals(9, decoded.appSettings?.smsScanMonths)
    }

    @Test
    fun decode_rejectsOldFormatVersion() {
        val v1Json =
            """
            {
              "formatVersion": 1,
              "appId": "${BackupJsonCodec.EXPECTED_APP_ID}",
              "exportedAtEpochMillis": 0,
              "databaseVersion": 2,
              "categories": [],
              "subcategories": [],
              "accounts": [],
              "loans": [],
              "transactions": [],
              "loanPayments": [],
              "recurringRules": [],
              "notificationRules": [],
              "smsMessageRecords": [],
              "transactionApprovals": []
            }
            """.trimIndent()
        try {
            BackupJsonCodec.decode(v1Json)
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unsupported backup format version"))
        }
    }

    @Test
    fun encodeDecode_roundTrip_preservesTombstones() {
        val payload =
            MmrBackupPayload(
                formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
                appId = BackupJsonCodec.EXPECTED_APP_ID,
                exportedAtEpochMillis = 1L,
                databaseVersion = 17,
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                loanPayments = emptyList(),
                recurringRules = emptyList(),
                notificationRules = emptyList(),
                smsMessageRecords = emptyList(),
                transactionApprovals = emptyList(),
                tombstones =
                    listOf(
                        SyncTombstoneEntity(
                            syncId = "txn-del",
                            entityType = SyncEntityType.TRANSACTION,
                            deletedAt = 500L,
                            sourceDeviceId = "device-a",
                        ),
                    ),
            )
        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(payload)).tombstones.single()
        assertEquals("txn-del", decoded.syncId)
        assertEquals(500L, decoded.deletedAt)
    }

    @Test
    fun decode_rejectsUnknownAppId() {
        val json =
            BackupJsonCodec.encode(
                MmrBackupPayload(
                    formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
                    appId = "other.app",
                    exportedAtEpochMillis = 0L,
                    databaseVersion = 2,
                    categories = emptyList(),
                    subcategories = emptyList(),
                    accounts = emptyList(),
                    loans = emptyList(),
                    transactions = emptyList(),
                    loanPayments = emptyList(),
                    recurringRules = emptyList(),
                    notificationRules = emptyList(),
                    smsMessageRecords = emptyList(),
                    transactionApprovals = emptyList(),
                ),
            )
        try {
            BackupJsonCodec.decode(json)
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Money Inbox"))
        }
    }
}
