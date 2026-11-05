package com.mmr.core.backup

import com.mmr.core.backup.model.MmrBackupPayload

object BackupPayloadSanitizer {
    fun forImport(payload: MmrBackupPayload): MmrBackupPayload {
        if (payload.trustedLanDevices.isNotEmpty()) {
            throw IllegalArgumentException("Backup must not include trusted LAN devices.")
        }
        return payload
    }
}
