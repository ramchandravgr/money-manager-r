package com.rrparallelstudio.miapp.core.moneymanager

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.InputStream

/**
 * Opens a read-only SQLite database from a Realbyte Money Manager `.mmbak` export.
 * Android exports are usually ZIP-wrapped; some installs use a raw SQLite file.
 */
internal object MoneyManagerMmbakDatabaseOpener {
    private const val MAX_UNCOMPRESSED_BYTES = 64L * 1024L * 1024L

    data class OpenedDatabase(
        val database: SQLiteDatabase,
        val tempFile: File,
    )

    data class MoneyManagerTransactionRow(
        val rowKey: String,
        val dateTimeMillis: Long,
        val amount: Double,
        val doType: Int,
        val assetUid: String?,
        val categoryUid: Long?,
        val note: String?,
        val toAssetUid: String? = null,
        val transferGroupId: String? = null,
    )

    fun openFromStream(
        input: InputStream,
        cacheDir: File,
    ): OpenedDatabase? {
        val peek = ByteArray(4)
        val read =
            input.markSupported().let { marked ->
                if (marked) input.mark(4)
                val n = input.read(peek)
                if (marked) input.reset()
                n
            }
        if (read < 2) return null
        val isZip = peek[0] == 'P'.code.toByte() && peek[1] == 'K'.code.toByte()
        val temp = File(cacheDir, "mmbak-${System.currentTimeMillis()}.db")
        return try {
            if (isZip) {
                MoneyManagerMmbakZip.extractDatabaseFromZip(input.readBytes(), temp)
            } else {
                input.use { copyToFile(it, temp) }
            }
            if (!temp.exists() || temp.length() <= 0L) return null
            val db =
                SQLiteDatabase.openDatabase(
                    temp.path,
                    null,
                    SQLiteDatabase.OPEN_READONLY,
                )
            OpenedDatabase(database = db, tempFile = temp)
        } catch (_: Exception) {
            temp.delete()
            null
        }
    }

    fun readTransactions(database: SQLiteDatabase): List<MoneyManagerTransactionRow> =
        MoneyManagerMmbakTransactionReader.readTransactions(database)

    fun readAssetNames(database: SQLiteDatabase): Map<String, String> {
        val table =
            MoneyManagerMmbakSqlite.resolveTable(
                database,
                listOf("ASSETS", "ZASSET", "ZASSETS", "ASSET", "assets"),
            ) ?: return emptyMap()
        val columns = MoneyManagerMmbakSqlite.columnNames(database, table)
        val uidCol = MoneyManagerMmbakSqlite.resolveColumn(columns, listOf("UID", "ZUID")) ?: return emptyMap()
        val nameCol =
            MoneyManagerMmbakSqlite.resolveColumn(
                columns,
                listOf("NIC_NAME", "ZNIC_NAME", "NAME", "ZNAME", "TITLE", "ZTITLE"),
            ) ?: return emptyMap()
        val isDelCol = MoneyManagerMmbakSqlite.resolveColumn(columns, listOf("IS_DEL", "C_IS_DEL", "ZIS_DEL"))
        val q = MoneyManagerMmbakSqlite::quote
        val where =
            if (isDelCol != null) {
                "WHERE (${q(isDelCol)} IS NULL OR ${q(isDelCol)} = 0)"
            } else {
                ""
            }
        val sql = "SELECT ${q(uidCol)}, ${q(nameCol)} FROM ${q(table)} $where"
        val map = LinkedHashMap<String, String>()
        database.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                val uid = MoneyManagerMmbakStorageKeys.readStorageKey(cursor, 0)
                val name = cursor.getString(1)?.trim().orEmpty()
                if (uid.isNotEmpty() && name.isNotEmpty()) map[uid] = name
            }
        }
        return map
    }

    fun readCategoryNames(database: SQLiteDatabase): Map<Long, CategoryRef> {
        val table =
            MoneyManagerMmbakSqlite.resolveTable(
                database,
                listOf("ZCATEGORY", "ZCATEGORY2", "CATEGORY", "category"),
            ) ?: return emptyMap()
        val columns = MoneyManagerMmbakSqlite.columnNames(database, table)
        val uidCol = MoneyManagerMmbakSqlite.resolveColumn(columns, listOf("UID", "ZUID")) ?: return emptyMap()
        val nameCol = MoneyManagerMmbakSqlite.resolveColumn(columns, listOf("NAME", "ZNAME")) ?: return emptyMap()
        val typeCol = MoneyManagerMmbakSqlite.resolveColumn(columns, listOf("TYPE", "ZTYPE"))
        val parentCol = MoneyManagerMmbakSqlite.resolveColumn(columns, listOf("PUID", "ZPUID", "P_UID", "PARENTUID"))
        val isDelCol = MoneyManagerMmbakSqlite.resolveColumn(columns, listOf("IS_DEL", "C_IS_DEL", "ZIS_DEL"))
        val q = MoneyManagerMmbakSqlite::quote
        val where =
            if (isDelCol != null) {
                "WHERE (${q(isDelCol)} IS NULL OR ${q(isDelCol)} = 0)"
            } else {
                ""
            }
        val select =
            buildList {
                add(q(uidCol))
                add(q(nameCol))
                typeCol?.let { add(q(it)) }
                parentCol?.let { add(q(it)) }
            }
        val sql = "SELECT ${select.joinToString(", ")} FROM ${q(table)} $where"
        val map = LinkedHashMap<Long, CategoryRef>()
        database.rawQuery(sql, null).use { cursor ->
            var idx = 0
            val uidIdx = idx++
            val nameIdx = idx++
            val typeIdx = if (typeCol != null) idx++ else -1
            val parentIdx = if (parentCol != null) idx else -1
            while (cursor.moveToNext()) {
                val uid = cursor.getLong(uidIdx)
                val name = cursor.getString(nameIdx)?.trim().orEmpty()
                if (name.isEmpty()) continue
                val type = if (typeIdx >= 0 && !cursor.isNull(typeIdx)) cursor.getInt(typeIdx) else null
                val parent =
                    if (parentIdx >= 0 && !cursor.isNull(parentIdx)) {
                        val p = cursor.getLong(parentIdx)
                        if (p > 0L) p else null
                    } else {
                        null
                    }
                map[uid] = CategoryRef(uid = uid, name = name, parentUid = parent, type = type)
            }
        }
        return map
    }

    data class CategoryRef(
        val uid: Long,
        val name: String,
        val parentUid: Long?,
        val type: Int?,
    )

    fun closeQuietly(opened: OpenedDatabase?) {
        if (opened == null) return
        runCatching { opened.database.close() }
        runCatching { opened.tempFile.delete() }
    }

    private fun copyToFile(
        input: InputStream,
        dest: File,
    ) {
        dest.outputStream().use { out ->
            var total = 0L
            val buffer = ByteArray(16_384)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > MAX_UNCOMPRESSED_BYTES) error("mmbak too large")
                out.write(buffer, 0, read)
            }
        }
    }
}
