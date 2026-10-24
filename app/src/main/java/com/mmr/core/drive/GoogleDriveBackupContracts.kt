package com.mmr.core.drive

object GoogleDriveBackupContracts {
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    const val USERINFO_EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"
    const val FOLDER_MONEY_INBOX = "MoneyInbox"
    const val FOLDER_BACKUPS = "Backups"
    const val FILE_CURRENT_NAME = "MoneyInbox-backup.mmrbackup"
    const val FILE_DATED_PREFIX = "MoneyInbox-backup-"
    const val FILE_EXTENSION = ".mmrbackup"
    const val MAX_RETAINED_FILES = 6
    const val AUTO_FAILURE_NOTIFY_THRESHOLD = 5
    const val WORK_NAME_AUTO = "mmr_google_drive_auto_backup"
}

enum class GoogleDriveAutoSchedule {
    OFF,
    DAILY,
    WEEKLY,
    ;

    companion object {
        fun fromStored(value: String?): GoogleDriveAutoSchedule =
            when (value) {
                DAILY.name -> DAILY
                WEEKLY.name -> WEEKLY
                else -> OFF
            }
    }
}
