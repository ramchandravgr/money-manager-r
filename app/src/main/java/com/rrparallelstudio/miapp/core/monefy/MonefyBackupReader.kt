package com.rrparallelstudio.miapp.core.monefy

import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Reads accounts, categories, transactions, and transfers from a decrypted Monefy SQLite file.
 * Amounts in Monefy are stored in [amountCents] where display units = cents / 1000.
 */
internal object MonefyBackupReader {
    data class AccountRow(
        val id: String,
        val title: String,
        val openingBalance: Double,
    )

    data class CategoryRow(
        val id: String,
        val title: String,
        val isIncome: Boolean,
    )

    data class TransactionRow(
        val id: String,
        val accountId: String,
        val categoryId: String?,
        val amount: Double,
        val createdOnMillis: Long,
        val note: String?,
    )

    data class TransferRow(
        val id: String,
        val fromAccountId: String,
        val toAccountId: String,
        val amount: Double,
        val createdOnMillis: Long,
        val note: String?,
    )

    data class Snapshot(
        val accounts: List<AccountRow>,
        val categories: List<CategoryRow>,
        val transactions: List<TransactionRow>,
        val transfers: List<TransferRow>,
    )

    fun read(
        encrypted: ByteArray,
        cacheDir: File,
    ): Snapshot {
        val plain = MonefyBackupCrypto.decrypt(encrypted)
        cacheDir.mkdirs()
        val file = File(cacheDir, "monefy-${System.currentTimeMillis()}.db")
        file.writeBytes(plain)
        return try {
            val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            try {
                Snapshot(
                    accounts = readAccounts(db),
                    categories = readCategories(db),
                    transactions = readTransactions(db),
                    transfers = readTransfers(db),
                )
            } finally {
                db.close()
            }
        } finally {
            file.delete()
        }
    }

    private fun readAccounts(db: SQLiteDatabase): List<AccountRow> {
        val rows = ArrayList<AccountRow>()
        db
            .rawQuery(
                """
                SELECT _id, title, initialAmountCents
                FROM accounts
                WHERE deletedOn IS NULL
                """.trimIndent(),
                null,
            ).use { cursor ->
                val id = cursor.getColumnIndexOrThrow("_id")
                val title = cursor.getColumnIndexOrThrow("title")
                val cents = cursor.getColumnIndexOrThrow("initialAmountCents")
                while (cursor.moveToNext()) {
                    val name = cursor.getString(title)?.trim().orEmpty()
                    if (name.isEmpty()) continue
                    rows +=
                        AccountRow(
                            id = cursor.getString(id),
                            title = name,
                            openingBalance = centsToAmount(cursor.getLong(cents)),
                        )
                }
            }
        return rows
    }

    private fun readCategories(db: SQLiteDatabase): List<CategoryRow> {
        val rows = ArrayList<CategoryRow>()
        db
            .rawQuery(
                """
                SELECT _id, title, categoryType
                FROM categories
                WHERE deletedOn IS NULL
                """.trimIndent(),
                null,
            ).use { cursor ->
                val id = cursor.getColumnIndexOrThrow("_id")
                val title = cursor.getColumnIndexOrThrow("title")
                val type = cursor.getColumnIndexOrThrow("categoryType")
                while (cursor.moveToNext()) {
                    val name = cursor.getString(title)?.trim().orEmpty()
                    if (name.isEmpty()) continue
                    rows +=
                        CategoryRow(
                            id = cursor.getString(id),
                            title = name,
                            isIncome = cursor.getString(type).equals("Income", ignoreCase = true),
                        )
                }
            }
        return rows
    }

    private fun readTransactions(db: SQLiteDatabase): List<TransactionRow> {
        val rows = ArrayList<TransactionRow>()
        db
            .rawQuery(
                """
                SELECT _id, account_id, category_id, amountCents, createdOn, note
                FROM transactions
                WHERE deletedOn IS NULL
                """.trimIndent(),
                null,
            ).use { cursor ->
                val id = cursor.getColumnIndexOrThrow("_id")
                val account = cursor.getColumnIndexOrThrow("account_id")
                val category = cursor.getColumnIndexOrThrow("category_id")
                val cents = cursor.getColumnIndexOrThrow("amountCents")
                val created = cursor.getColumnIndexOrThrow("createdOn")
                val note = cursor.getColumnIndexOrThrow("note")
                while (cursor.moveToNext()) {
                    val amount = centsToAmount(cursor.getLong(cents))
                    if (amount == 0.0) continue
                    rows +=
                        TransactionRow(
                            id = cursor.getString(id),
                            accountId = cursor.getString(account).orEmpty(),
                            categoryId = cursor.getString(category),
                            amount = amount,
                            createdOnMillis = cursor.getLong(created),
                            note = cursor.getString(note)?.trim()?.ifEmpty { null },
                        )
                }
            }
        return rows
    }

    private fun readTransfers(db: SQLiteDatabase): List<TransferRow> {
        val rows = ArrayList<TransferRow>()
        db
            .rawQuery(
                """
                SELECT _id, accountFrom, accountTo, amountCents, createdOn, note
                FROM transfers
                WHERE deletedOn IS NULL
                """.trimIndent(),
                null,
            ).use { cursor ->
                val id = cursor.getColumnIndexOrThrow("_id")
                val from = cursor.getColumnIndexOrThrow("accountFrom")
                val to = cursor.getColumnIndexOrThrow("accountTo")
                val cents = cursor.getColumnIndexOrThrow("amountCents")
                val created = cursor.getColumnIndexOrThrow("createdOn")
                val note = cursor.getColumnIndexOrThrow("note")
                while (cursor.moveToNext()) {
                    val amount = centsToAmount(cursor.getLong(cents))
                    if (amount == 0.0) continue
                    rows +=
                        TransferRow(
                            id = cursor.getString(id),
                            fromAccountId = cursor.getString(from).orEmpty(),
                            toAccountId = cursor.getString(to).orEmpty(),
                            amount = amount,
                            createdOnMillis = cursor.getLong(created),
                            note = cursor.getString(note)?.trim()?.ifEmpty { null },
                        )
                }
            }
        return rows
    }

    private fun centsToAmount(cents: Long): Double = cents / AMOUNT_SCALE

    private const val AMOUNT_SCALE = 1000.0
}
