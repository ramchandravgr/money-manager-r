package com.rrparallelstudio.miapp.core.backup

import com.rrparallelstudio.miapp.core.backup.model.MmrBackupPayload

object BackupPayloadSanitizer {
    fun forImport(payload: MmrBackupPayload): MmrBackupPayload {
        if (payload.trustedLanDevices.isNotEmpty()) {
            throw IllegalArgumentException("Backup must not include trusted LAN devices.")
        }
        return payload
    }
}
