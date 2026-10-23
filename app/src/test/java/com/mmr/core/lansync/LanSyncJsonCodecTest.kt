package com.mmr.core.lansync

import com.mmr.core.lansync.model.LanTransactionRecord
import com.mmr.core.lansync.model.MmrLanSyncPayload
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
}
