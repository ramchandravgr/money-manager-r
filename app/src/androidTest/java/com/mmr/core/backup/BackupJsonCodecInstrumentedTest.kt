package com.mmr.core.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.backup.model.MmrBackupPayload
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupJsonCodecInstrumentedTest {

    @Test
    fun encodeDecode_roundTrip_preservesCoreRecords() {
        val payload = MmrBackupPayload(
            formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
            appId = BackupJsonCodec.EXPECTED_APP_ID,
            exportedAtEpochMillis = 1_700_000_000_000L,
            databaseVersion = 2,
            categories = listOf(
                CategoryEntity(id = 1L, name = "Food", type = CategoryType.EXPENSE, isDefault = true)
            ),
            subcategories = emptyList(),
            accounts = listOf(
                AccountEntity(id = 2L, name = "Cash", type = AccountType.CASH, isDefault = true)
            ),
            loans = emptyList(),
            transactions = listOf(
                TransactionEntity(
                    id = 3L,
                    dateTime = 1_700_000_000_000L,
                    amount = 42.5,
                    type = TransactionType.EXPENSE,
                    categoryId = 1L,
                    accountId = 2L,
                    note = "Lunch",
                    status = RecordStatus.APPROVED,
                    sourceType = TransactionSourceType.MANUAL
                )
            ),
            loanPayments = emptyList(),
            recurringRules = emptyList(),
            notificationRules = emptyList(),
            smsMessageRecords = emptyList(),
            transactionApprovals = emptyList()
        )

        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(payload))

        assertEquals(1, decoded.categories.size)
        assertEquals("Food", decoded.categories.first().name)
        assertEquals(42.5, decoded.transactions.first().amount, 0.001)
    }

    @Test
    fun decode_rejectsUnknownAppId() {
        val json = BackupJsonCodec.encode(
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
                transactionApprovals = emptyList()
            )
        )
        try {
            BackupJsonCodec.decode(json)
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Money Manager R"))
        }
    }
}
