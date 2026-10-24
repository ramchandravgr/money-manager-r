package com.mmr.core.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupSignedZipCodecInstrumentedTest {
    @Test
    fun encodeDecode_roundTrip_preservesPayload() {
        val payloadJson =
            """
            {
              "formatVersion": 4,
              "appId": "${BackupJsonCodec.EXPECTED_APP_ID}",
              "exportedAtEpochMillis": 1,
              "databaseVersion": 17,
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
        val attachments =
            mapOf(
                "sync-attachment-1" to byteArrayOf(1, 2, 3, 4),
            )

        val archive = BackupSignedZipCodec.encode(payloadJson, attachments)
        assertTrue(BackupSignedZipCodec.isZipArchive(archive))

        val parsed = BackupSignedZipCodec.decode(archive)
        assertEquals(payloadJson, parsed.payloadJson)
        assertEquals(
            attachments["sync-attachment-1"]!!.toList(),
            parsed.attachmentBytesBySyncId["sync-attachment-1"]!!.toList(),
        )
    }

    @Test
    fun integrity_verify_detectsTampering() {
        val valid = BackupIntegrity.sign("abc", emptyList())
        assertTrue(BackupIntegrity.verify("abc", emptyList(), valid))
        assertFalse(BackupIntegrity.verify("abc", emptyList(), valid + "x"))
        assertFalse(BackupIntegrity.verify("tampered", emptyList(), valid))
    }
}
