package com.rrparallelstudio.miapp.core.moneymanager

import android.database.sqlite.SQLiteDatabase

internal object MoneyManagerMmbakSqlite {
    fun resolveTable(
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

    fun columnNames(
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

    fun resolveColumn(
        columns: List<String>,
        candidates: List<String>,
    ): String? {
        val upperToActual = columns.associateBy { it.uppercase() }
        for (key in candidates) {
            upperToActual[key]?.let { return it }
        }
        return null
    }

    fun quote(identifier: String): String = "\"${identifier.replace("\"", "\"\"")}\""
}
