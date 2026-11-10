package com.rrparallelstudio.miapp.core.backup

/** User-facing rejection when a backup file is not a signed `.mibxbac` archive. */
object BackupImportGuard {
    const val UNSIGNED_FILE_MESSAGE =
        "Import a signed .mibxbac file. Plain JSON or other unsigned backups are not accepted."
}
