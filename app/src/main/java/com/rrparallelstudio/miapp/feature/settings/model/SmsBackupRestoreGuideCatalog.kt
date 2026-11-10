package com.rrparallelstudio.miapp.feature.settings.model

/**
 * In-app setup for [SMS Backup & Restore](https://play.google.com/store/apps/details?id=com.riteshsahu.SMSBackupRestore)
 * (SyncTech). Play builds of Money Inbox import bank SMS from XML files in a folder you choose — not live inbox read.
 */
object SmsBackupRestoreGuideCatalog {
    const val PLAY_STORE_PACKAGE = "com.riteshsahu.SMSBackupRestore"
    const val PLAY_STORE_APP_NAME = "SMS Backup & Restore"

    data class Step(
        val title: String,
        val body: String,
    )

    val intro: String =
        "Money Inbox on Play cannot read SMS directly. Use ${PLAY_STORE_APP_NAME} to export " +
            "messages as XML, then point Transaction scan at that folder."

    val steps: List<Step> =
        listOf(
            Step(
                title = "Install the backup app",
                body = "Get ${PLAY_STORE_APP_NAME} from the Play Store (SyncTech).",
            ),
            Step(
                title = "Back up SMS as XML",
                body =
                    "In ${PLAY_STORE_APP_NAME}: open Back up → choose XML (not .db). " +
                        "Pick a folder you can find later, for example Documents or Download. " +
                        "You can use one fixed file name; Money Inbox re-reads it when the file is updated.",
            ),
            Step(
                title = "Schedule automatic backups (recommended)",
                body =
                    "In ${PLAY_STORE_APP_NAME} → schedule / automatic backup: choose hourly " +
                        "(or as often as you like). Enable overwrite the same XML file on each run " +
                        "so the folder stays tidy—Refresh picks up the latest version.",
            ),
            Step(
                title = "Link the folder in Money Inbox",
                body =
                    "Here: Settings → Approvals → Transaction scan → Backup folder. " +
                        "Grant access to the same folder where XML files are saved.",
            ),
            Step(
                title = "Refresh approvals",
                body =
                    "Open Approval Center and tap Refresh. The latest sms-*.xml in that folder is imported " +
                        "(only bank-like messages in your scan range). Tap Refresh again after the next hourly backup.",
            ),
        )

    val footnote: String =
        "You can change how far back we look under Look back on this screen. Clear the folder anytime to stop imports."

    /** Shown on Transaction scan settings (matches [com.rrparallelstudio.miapp.R.string.transaction_scan_sms_data_declaration]). */
    const val DATA_USE_DECLARATION =
        "Money Inbox only accesses the SMS backup XML files you choose. We use them on this device " +
            "to find bank transactions for Approval Center. SMS content is not uploaded to our servers."
}
