package com.rrparallelstudio.miapp.feature.settings.model

/**
 * In-app setup for SMS Backup & Restore (SyncTech).
 * Play builds import bank SMS from XML in a folder you choose.
 */
object SmsBackupRestoreGuideCatalog {
    const val PLAY_STORE_PACKAGE = "com.riteshsahu.SMSBackupRestore"
    const val PLAY_STORE_APP_NAME = "SMS Backup & Restore"

    data class Step(
        val title: String,
        val body: String,
    )

    val intro: String =
        "This app does not read your message inbox. Save messages with $PLAY_STORE_APP_NAME, " +
            "then choose that folder here."

    val steps: List<Step> =
        listOf(
            Step(
                title = "Get $PLAY_STORE_APP_NAME",
                body = "Opens the SyncTech app on the Play Store. Install it, then come back here.",
            ),
            Step(
                title = "Save Messages As A File",
                body =
                    "In that app, save messages as an XML file, not a database. " +
                        "Pick a folder you can find again. One file name is enough.",
            ),
            Step(
                title = "Repeat That Save",
                body = "An hourly schedule is enough. Let it replace the same file each time.",
            ),
            Step(
                title = "Choose That Folder Here",
                body = "Go back to Transaction Scan and tap Backup Folder. Allow access to the same folder.",
            ),
            Step(
                title = "Refresh Approval Center",
                body =
                    "Open Approval Center and tap Refresh. " +
                        "New bank messages from that file are offered for you to accept.",
            ),
        )

    val footnote: String =
        "Look Back sets how many months of those messages are offered. " +
            "Forget Folder stops reading the folder. It does not delete the files."
}
