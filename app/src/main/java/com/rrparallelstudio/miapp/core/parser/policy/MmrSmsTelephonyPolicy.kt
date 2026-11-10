package com.rrparallelstudio.miapp.core.parser.policy

/**
 * Play Store: finance apps cannot declare [android.Manifest.permission.READ_SMS] or
 * [android.Manifest.permission.RECEIVE_SMS]. Inbox scan, live receive, and the pending-approvals
 * widget stay in the tree so a sideload / default-SMS build can turn them back on.
 *
 * Planned Play-safe intake: import XML exports from SMS Backup & Restore (SyncTech). Until then,
 * Approval Center uses manual paste only.
 */
object MmrSmsTelephonyPolicy {
    const val INBOX_SCAN_AND_RECEIVE_ENABLED = false
    const val PENDING_APPROVALS_WIDGET_ENABLED = false

    /**
     * Legacy Settings "SMS approvals" section (duplicate entry to template overlay).
     * Play uses **Settings → Transaction scan** ([Screen.TransactionScan]) for scan months + backup folder.
     */
    const val SETTINGS_SMS_APPROVALS_SECTION_VISIBLE = false
}
