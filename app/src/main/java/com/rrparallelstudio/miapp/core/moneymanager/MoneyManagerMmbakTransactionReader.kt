package com.rrparallelstudio.miapp.core.moneymanager

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

internal object MoneyManagerMmbakTransactionReader {
    data class ColumnSet(
        val uidCol: String,
        val dateCol: String,
        val moneyCol: String,
        val typeCol: String,
        val assetCol: String?,
        val ctgCol: String?,
        val noteCol: String?,
        val toAssetCol: String?,
        val transferGroupCol: String?,
    )

    fun readTransactions(database: SQLiteDatabase): List<MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow> {
        val inoutTable =
            MoneyManagerMmbakSqlite.resolveTable(
                database,
                listOf("INOUTCOME", "ZINOUTCOME", "inoutcome"),
            ) ?: return emptyList()
        val columns = MoneyManagerMmbakSqlite.columnNames(database, inoutTable)
        val cols = resolveColumns(columns) ?: return emptyList()
        val isDelCol =
            MoneyManagerMmbakSqlite.resolveColumn(
                columns,
                listOf("IS_DEL", "C_IS_DEL", "ZIS_DEL", "isDel", "ZISDEL", "ZISDELETED"),
            )
        val sql = buildSelectSql(inoutTable, cols, isDelCol)
        val rows = ArrayList<MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow>()
        database.rawQuery(sql, null).use { cursor ->
            val indices = cursorIndices(cols)
            while (cursor.moveToNext()) {
                mapRow(cursor, indices)?.let { rows.add(it) }
            }
        }
        return rows
    }

    private fun resolveColumns(columns: List<String>): ColumnSet? {
        val uidCol = MoneyManagerMmbakSqlite.resolveColumn(columns, listOf("UID", "ZUID", "C_UID", "ID", "ZID"))
        val dateCol =
            MoneyManagerMmbakSqlite.resolveColumn(
                columns,
                listOf("WDATE", "ZWDATE", "wdate", "ZDATE", "DATE", "date", "ZTIME"),
            )
        val moneyCol =
            MoneyManagerMmbakSqlite.resolveColumn(
                columns,
                listOf("ZMONEY", "ZZMONEY", "AMOUNT", "ZAMOUNT", "MONEY", "money"),
            )
        val typeCol =
            MoneyManagerMmbakSqlite.resolveColumn(
                columns,
                listOf("DO_TYPE", "ZDO_TYPE", "TYPE", "ZTYPE", "inoutType"),
            )
        if (uidCol == null || dateCol == null || moneyCol == null || typeCol == null) return null
        return ColumnSet(
            uidCol = uidCol,
            dateCol = dateCol,
            moneyCol = moneyCol,
            typeCol = typeCol,
            assetCol =
                MoneyManagerMmbakSqlite.resolveColumn(
                    columns,
                    listOf("ASSETUID", "ZASSETUID", "ASSET_UID", "assetUid", "asset_uid"),
                ),
            ctgCol =
                MoneyManagerMmbakSqlite.resolveColumn(
                    columns,
                    listOf("CTGUID", "ZCTGUID", "CTG_UID", "CATEGORYUID", "ctgUid", "ctg_uid", "ZCATEGORY"),
                ),
            noteCol =
                MoneyManagerMmbakSqlite.resolveColumn(
                    columns,
                    listOf("ZCONTENT", "ZZCONTENT", "CONTENT", "NOTE", "MEMO", "memo", "ZCOMMENT"),
                ),
            toAssetCol =
                MoneyManagerMmbakSqlite.resolveColumn(
                    columns,
                    listOf("TOASSETUID", "ZTOASSETUID", "TO_ASSET_UID", "toAssetUid", "to_asset_uid"),
                ),
            transferGroupCol =
                MoneyManagerMmbakSqlite.resolveColumn(
                    columns,
                    listOf("txUidTrans", "TX_UID_TRANS", "TXUIDTRANS", "ZTXUIDTRANS"),
                ),
        )
    }

    private fun buildSelectSql(
        table: String,
        cols: ColumnSet,
        isDelCol: String?,
    ): String {
        val q = MoneyManagerMmbakSqlite::quote
        val select =
            buildList {
                add(q(cols.uidCol))
                add(q(cols.dateCol))
                add(q(cols.moneyCol))
                add(q(cols.typeCol))
                cols.assetCol?.let { add(q(it)) }
                cols.ctgCol?.let { add(q(it)) }
                cols.noteCol?.let { add(q(it)) }
                cols.toAssetCol?.let { add(q(it)) }
                cols.transferGroupCol?.let { add(q(it)) }
            }
        val where =
            if (isDelCol != null) {
                "WHERE (${q(isDelCol)} IS NULL OR ${q(isDelCol)} = 0)"
            } else {
                ""
            }
        return "SELECT ${select.joinToString(", ")} FROM ${q(table)} $where"
    }

    private data class CursorIndices(
        val dateIdx: Int,
        val moneyIdx: Int,
        val typeIdx: Int,
        val assetIdx: Int,
        val ctgIdx: Int,
        val noteIdx: Int,
        val toAssetIdx: Int,
        val transferGroupIdx: Int,
    )

    private fun cursorIndices(cols: ColumnSet): CursorIndices {
        var next = 4
        val assetIdx = if (cols.assetCol != null) next++ else -1
        val ctgIdx = if (cols.ctgCol != null) next++ else -1
        val noteIdx = if (cols.noteCol != null) next++ else -1
        val toAssetIdx = if (cols.toAssetCol != null) next++ else -1
        val transferGroupIdx = if (cols.transferGroupCol != null) next else -1
        return CursorIndices(1, 2, 3, assetIdx, ctgIdx, noteIdx, toAssetIdx, transferGroupIdx)
    }

    private fun mapRow(
        cursor: Cursor,
        idx: CursorIndices,
    ): MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow? {
        val rowKey = MoneyManagerMmbakStorageKeys.readStorageKey(cursor, 0)
        if (rowKey.isEmpty()) return null
        return MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow(
            rowKey = rowKey,
            dateTimeMillis = MoneyManagerMmbakDate.readDateMillis(cursor, idx.dateIdx),
            amount = kotlin.math.abs(cursor.getDouble(idx.moneyIdx)),
            doType = readDoType(cursor, idx.typeIdx),
            assetUid = readOptionalKey(cursor, idx.assetIdx),
            categoryUid = readOptionalLong(cursor, idx.ctgIdx),
            note = readNote(cursor, idx.noteIdx),
            toAssetUid = readOptionalKey(cursor, idx.toAssetIdx),
            transferGroupId = readOptionalKey(cursor, idx.transferGroupIdx),
        )
    }

    private fun readOptionalKey(
        cursor: Cursor,
        index: Int,
    ): String? =
        if (index >= 0 && !cursor.isNull(index)) {
            MoneyManagerMmbakStorageKeys.readStorageKey(cursor, index).takeIf { it.isNotEmpty() }
        } else {
            null
        }

    private fun readOptionalLong(
        cursor: Cursor,
        index: Int,
    ): Long? =
        if (index >= 0 && !cursor.isNull(index)) {
            MoneyManagerMmbakStorageKeys.readOptionalLongKey(cursor, index)
        } else {
            null
        }

    private fun readNote(
        cursor: Cursor,
        index: Int,
    ): String? =
        if (index >= 0 && !cursor.isNull(index)) {
            cursor.getString(index)?.takeIf { it.isNotBlank() }
        } else {
            null
        }

    private fun readDoType(
        cursor: Cursor,
        typeIdx: Int,
    ): Int =
        when (cursor.getType(typeIdx)) {
            Cursor.FIELD_TYPE_STRING -> cursor.getString(typeIdx).trim().toIntOrNull() ?: -1
            Cursor.FIELD_TYPE_NULL -> -1
            else -> cursor.getInt(typeIdx)
        }
}
