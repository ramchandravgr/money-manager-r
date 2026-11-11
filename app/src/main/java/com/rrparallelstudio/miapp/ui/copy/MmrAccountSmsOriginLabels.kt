package com.rrparallelstudio.miapp.ui.copy

import com.rrparallelstudio.miapp.core.database.entity.AccountEntity

/** Title-cased labels for accounts created via SMS provisioning (quick approve). */
object MmrAccountSmsOriginLabels {
    const val AUTO_CREATED = "Auto Created"
    const val AUTO_CREATED_MODIFIED = "Auto Created - Modified"

    fun labelFor(entity: AccountEntity): String? =
        when {
            !entity.smsAutoCreated -> null
            entity.smsAutoCreatedUserModified -> AUTO_CREATED_MODIFIED
            else -> AUTO_CREATED
        }
}
