package com.rrparallelstudio.miapp.core.backup

import com.rrparallelstudio.miapp.core.backup.model.MmrAppSettingsBackup
import com.rrparallelstudio.miapp.core.database.entity.TrustedLanDeviceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupPayloadSanitizerTest {
    @Test
    fun forImport_rejectsTrustedLanDevices() {
        val payload =
            com.rrparallelstudio.miapp.core.backup.model.MmrBackupPayload(
                formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
                appId = "mmr",
                exportedAtEpochMillis = 0L,
                databaseVersion = 1,
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
                trustedLanDevices =
                    listOf(
                        TrustedLanDeviceEntity(
                            remoteDeviceId = "peer-1",
                            displayName = "Peer",
                            theirAuthToken = "secret",
                            pairedAtMillis = 0L,
                        ),
                    ),
                appSettings = MmrAppSettingsBackup(smsScanMonths = 3),
            )

        assertThrows(IllegalArgumentException::class.java) {
            BackupPayloadSanitizer.forImport(payload)
        }
    }

    @Test
    fun forImport_passesCleanPayload() {
        val payload =
            com.rrparallelstudio.miapp.core.backup.model.MmrBackupPayload(
                formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
                appId = "mmr",
                exportedAtEpochMillis = 0L,
                databaseVersion = 1,
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
                appSettings = MmrAppSettingsBackup(smsScanMonths = 3),
            )

        val sanitized = BackupPayloadSanitizer.forImport(payload)
        assertEquals(3, sanitized.appSettings?.smsScanMonths)
    }
}
