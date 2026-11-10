package com.rrparallelstudio.miapp.core.moneymanager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MoneyManagerMmbakFormatTest {
    @Test
    fun detectsByFileName() {
        assertTrue(
            MoneyManagerMmbakFormat.isProbablyMoneyManagerBackup(
                byteArrayOf(0x50, 0x4B),
                "backup-2026.mmbak",
            ),
        )
    }

    @Test
    fun mibxbacZipIsNotMoneyManager() {
        val archive =
            ByteArrayOutputStream().use { buffer ->
                ZipOutputStream(buffer).use { zip ->
                    zip.putNextEntry(ZipEntry("manifest.json"))
                    zip.write("{}".toByteArray())
                    zip.closeEntry()
                }
                buffer.toByteArray()
            }
        assertFalse(
            MoneyManagerMmbakFormat.isProbablyMoneyManagerBackup(
                archive,
                "money-inbox-backup.mibxbac",
            ),
        )
    }
}
