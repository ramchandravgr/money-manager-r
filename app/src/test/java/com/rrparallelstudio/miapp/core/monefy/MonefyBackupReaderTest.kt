package com.rrparallelstudio.miapp.core.monefy

import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
class MonefyBackupReaderTest {
    @Test
    fun decryptsAndReadsAccountsCategoriesTransactionsAndTransfers() {
        val plain = buildSqlite()
        val encrypted = encrypt(plain)
        val snapshot = MonefyBackupReader.read(encrypted, File(System.getProperty("java.io.tmpdir")))

        assertEquals(listOf("Cash", "Payment card"), snapshot.accounts.map { it.title })
        assertEquals(AccountTypeCheck.CASH, MonefyAccountTypeMapper.fromTitle("Cash").name)
        assertEquals(AccountTypeCheck.CREDIT, MonefyAccountTypeMapper.fromTitle("Payment card").name)
        assertTrue(snapshot.categories.any { it.title == "Salary" && it.isIncome })
        assertTrue(snapshot.categories.any { it.title == "Food" && !it.isIncome })
        assertEquals(1, snapshot.transactions.size)
        assertEquals(50000.0, snapshot.transactions[0].amount, 0.001)
        assertEquals("Salary", snapshot.transactions[0].note)
        assertEquals(1, snapshot.transfers.size)
        assertEquals(1258.0, snapshot.transfers[0].amount, 0.001)
        assertTrue(MonefyBackupFormat.isProbablyMonefyBackup(encrypted, "monefy_backup_sample"))
    }

    private fun buildSqlite(): ByteArray {
        val file = File.createTempFile("monefy-plain", ".db")
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        db.execSQL(
            """
            CREATE TABLE accounts (
                _id TEXT PRIMARY KEY,
                title TEXT,
                initialAmountCents INTEGER,
                deletedOn INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE categories (
                _id TEXT PRIMARY KEY,
                title TEXT,
                categoryType TEXT,
                deletedOn INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE transactions (
                _id TEXT PRIMARY KEY,
                account_id TEXT,
                category_id TEXT,
                amountCents INTEGER,
                createdOn INTEGER,
                note TEXT,
                deletedOn INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE transfers (
                _id TEXT PRIMARY KEY,
                accountFrom TEXT,
                accountTo TEXT,
                amountCents INTEGER,
                createdOn INTEGER,
                note TEXT,
                deletedOn INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL("INSERT INTO accounts VALUES ('a1', 'Cash', 0, NULL)")
        db.execSQL("INSERT INTO accounts VALUES ('a2', 'Payment card', 0, NULL)")
        db.execSQL("INSERT INTO accounts VALUES ('a3', 'Old', 0, 1)")
        db.execSQL("INSERT INTO categories VALUES ('c1', 'Salary', 'Income', NULL)")
        db.execSQL("INSERT INTO categories VALUES ('c2', 'Food', 'Expense', NULL)")
        db.execSQL(
            "INSERT INTO transactions VALUES ('t1', 'a1', 'c1', 50000000, 1790793000000, 'Salary', NULL)",
        )
        db.execSQL("INSERT INTO transactions VALUES ('t2', 'a1', 'c2', 0, 1790793000000, 'Empty', NULL)")
        db.execSQL(
            "INSERT INTO transfers VALUES ('x1', 'a1', 'a2', 1258000, 1794447448291, NULL, NULL)",
        )
        db.close()
        val bytes = file.readBytes()
        file.delete()
        val padded = bytes.size % 16
        return if (padded == 0) bytes else bytes + ByteArray(16 - padded)
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec("MyDifficultPassw".toByteArray(Charsets.UTF_8), "AES"),
        )
        return cipher.doFinal(plain)
    }

    private object AccountTypeCheck {
        const val CASH = "CASH"
        const val CREDIT = "CREDIT"
    }
}
