package com.rrparallelstudio.miapp.core.moneymanager

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
        val uid: Long,
        val dateTimeMillis: Long,
        val amount: Double,
        val doType: Int,
        val assetUid: Long?,
        val categoryUid: Long?,
        val note: String?,
        val toAssetUid: Long? = null,
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
                extractSqliteFromZip(input, temp)
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

    fun readTransactions(database: SQLiteDatabase): List<MoneyManagerTransactionRow> {
        val inoutTable =
            resolveTable(
                database,
                listOf("INOUTCOME", "ZINOUTCOME"),
            ) ?: return emptyList()
        val columns = columnNames(database, inoutTable)
        val uidCol = resolveColumn(columns, listOf("UID", "ZUID"))
        val dateCol = resolveColumn(columns, listOf("WDATE", "ZWDATE", "ZDATE", "DATE"))
        val moneyCol = resolveColumn(columns, listOf("ZMONEY", "ZZMONEY", "AMOUNT", "ZAMOUNT"))
        val typeCol = resolveColumn(columns, listOf("DO_TYPE", "ZDO_TYPE", "TYPE", "ZTYPE"))
        val assetCol = resolveColumn(columns, listOf("ASSETUID", "ZASSETUID", "ASSET_UID"))
        val ctgCol = resolveColumn(columns, listOf("CTGUID", "ZCTGUID", "CTG_UID", "CATEGORYUID"))
        val noteCol = resolveColumn(columns, listOf("ZCONTENT", "ZZCONTENT", "CONTENT", "NOTE"))
        val toAssetCol = resolveColumn(columns, listOf("TOASSETUID", "ZTOASSETUID", "TO_ASSET_UID"))
        val isDelCol = resolveColumn(columns, listOf("IS_DEL", "C_IS_DEL", "ZIS_DEL"))

        if (uidCol == null || dateCol == null || moneyCol == null || typeCol == null) {
            return emptyList()
        }

        val select =
            buildList {
                add(quote(uidCol))
                add(quote(dateCol))
                add(quote(moneyCol))
                add(quote(typeCol))
                if (assetCol != null) add(quote(assetCol))
                if (ctgCol != null) add(quote(ctgCol))
                if (noteCol != null) add(quote(noteCol))
                if (toAssetCol != null) add(quote(toAssetCol))
            }
        val where =
            if (isDelCol != null) {
                "WHERE (${quote(isDelCol)} IS NULL OR ${quote(isDelCol)} = 0)"
            } else {
                ""
            }
        val sql = "SELECT ${select.joinToString(", ")} FROM ${quote(inoutTable)} $where"

        val rows = ArrayList<MoneyManagerTransactionRow>()
        database.rawQuery(sql, null).use { cursor ->
            val uidIdx = 0
            val dateIdx = 1
            val moneyIdx = 2
            val typeIdx = 3
            var nextIdx = 4
            val assetIdx = if (assetCol != null) nextIdx++ else -1
            val ctgIdx = if (ctgCol != null) nextIdx++ else -1
            val noteIdx = if (noteCol != null) nextIdx++ else -1
            val toAssetIdx = if (toAssetCol != null) nextIdx else -1
            while (cursor.moveToNext()) {
                val uid = cursor.getLong(uidIdx)
                val date = cursor.getLong(dateIdx)
                val amount = cursor.getDouble(moneyIdx)
                val doType = cursor.getInt(typeIdx)
                val assetUid = if (assetIdx >= 0 && !cursor.isNull(assetIdx)) cursor.getLong(assetIdx) else null
                val ctgUid = if (ctgIdx >= 0 && !cursor.isNull(ctgIdx)) cursor.getLong(ctgIdx) else null
                val note = if (noteIdx >= 0 && !cursor.isNull(noteIdx)) cursor.getString(noteIdx) else null
                val toAsset =
                    if (toAssetIdx >= 0 && !cursor.isNull(toAssetIdx)) cursor.getLong(toAssetIdx) else null
                rows.add(
                    MoneyManagerTransactionRow(
                        uid = uid,
                        dateTimeMillis = date,
                        amount = kotlin.math.abs(amount),
                        doType = doType,
                        assetUid = assetUid,
                        categoryUid = ctgUid,
                        note = note?.takeIf { it.isNotBlank() },
                        toAssetUid = toAsset,
                    ),
                )
            }
        }
        return rows
    }

    fun readAssetNames(database: SQLiteDatabase): Map<Long, String> {
        val table =
            resolveTable(
                database,
                listOf("ASSETS", "ZASSET"),
            ) ?: return emptyMap()
        val columns = columnNames(database, table)
        val uidCol = resolveColumn(columns, listOf("UID", "ZUID")) ?: return emptyMap()
        val nameCol =
            resolveColumn(
                columns,
                listOf("NIC_NAME", "ZNIC_NAME", "NAME", "ZNAME", "TITLE", "ZTITLE"),
            ) ?: return emptyMap()
        val isDelCol = resolveColumn(columns, listOf("IS_DEL", "C_IS_DEL", "ZIS_DEL"))
        val where =
            if (isDelCol != null) {
                "WHERE (${quote(isDelCol)} IS NULL OR ${quote(isDelCol)} = 0)"
            } else {
                ""
            }
        val sql = "SELECT ${quote(uidCol)}, ${quote(nameCol)} FROM ${quote(table)} $where"
        val map = LinkedHashMap<Long, String>()
        database.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                val uid = cursor.getLong(0)
                val name = cursor.getString(1)?.trim().orEmpty()
                if (name.isNotEmpty()) map[uid] = name
            }
        }
        return map
    }

    fun readCategoryNames(database: SQLiteDatabase): Map<Long, CategoryRef> {
        val table =
            resolveTable(
                database,
                listOf("ZCATEGORY", "ZCATEGORY2"),
            ) ?: return emptyMap()
        val columns = columnNames(database, table)
        val uidCol = resolveColumn(columns, listOf("UID", "ZUID")) ?: return emptyMap()
        val nameCol = resolveColumn(columns, listOf("NAME", "ZNAME")) ?: return emptyMap()
        val typeCol = resolveColumn(columns, listOf("TYPE", "ZTYPE"))
        val parentCol = resolveColumn(columns, listOf("PUID", "ZPUID", "P_UID", "PARENTUID"))
        val isDelCol = resolveColumn(columns, listOf("IS_DEL", "C_IS_DEL", "ZIS_DEL"))
        val where =
            if (isDelCol != null) {
                "WHERE (${quote(isDelCol)} IS NULL OR ${quote(isDelCol)} = 0)"
            } else {
                ""
            }
        val select =
            buildList {
                add(quote(uidCol))
                add(quote(nameCol))
                if (typeCol != null) add(quote(typeCol))
                if (parentCol != null) add(quote(parentCol))
            }
        val sql = "SELECT ${select.joinToString(", ")} FROM ${quote(table)} $where"
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

    private fun extractSqliteFromZip(
        input: InputStream,
        dest: File,
    ) {
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry?
            var total = 0L
            while (zip.nextEntry.also { entry = it } != null) {
                val e = entry ?: continue
                if (e.isDirectory) continue
                val name = e.name.lowercase()
                if (!name.endsWith(".db") && !name.endsWith(".sqlite") && !name.endsWith(".sqlite3")) {
                    continue
                }
                val size = e.size
                if (size > MAX_UNCOMPRESSED_BYTES) return
                dest.outputStream().use { out ->
                    val buffer = ByteArray(16_384)
                    while (true) {
                        val read = zip.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_UNCOMPRESSED_BYTES) return
                        out.write(buffer, 0, read)
                    }
                }
                return
            }
        }
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

    private fun resolveTable(
        database: SQLiteDatabase,
        candidates: List<String>,
    ): String? {
        for (name in candidates) {
            database
                .rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=? COLLATE NOCASE LIMIT 1",
                    arrayOf(name),
                ).use { cursor ->
                    if (cursor.moveToFirst()) return cursor.getString(0)
                }
        }
        return null
    }

    private fun columnNames(
        database: SQLiteDatabase,
        table: String,
    ): List<String> {
        val names = ArrayList<String>()
        database.rawQuery("PRAGMA table_info(${quote(table)})", null).use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIdx >= 0) names.add(cursor.getString(nameIdx))
            }
        }
        return names
    }

    private fun resolveColumn(
        columns: List<String>,
        candidates: List<String>,
    ): String? {
        val upperToActual = columns.associateBy { it.uppercase() }
        for (key in candidates) {
            upperToActual[key]?.let { return it }
        }
        return null
    }

    private fun quote(identifier: String): String = "\"${identifier.replace("\"", "\"\"")}\""
}
