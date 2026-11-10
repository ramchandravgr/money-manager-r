package com.rrparallelstudio.miapp.core.backup

import org.junit.Assert.assertTrue
import org.junit.Test

class BackupImportGuardTest {
    @Test
    fun unsignedFileMessage_namesSignedArchive() {
        assertTrue(BackupImportGuard.UNSIGNED_FILE_MESSAGE.contains(".mibxbac"))
    }
}
