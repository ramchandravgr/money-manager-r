package com.rrparallelstudio.miapp.core.moneymanager

import android.database.Cursor

internal object MoneyManagerMmbakStorageKeys {
    fun readStorageKey(
        cursor: Cursor,
        idx: Int,
    ): String =
        when (cursor.getType(idx)) {
            Cursor.FIELD_TYPE_NULL -> ""
            Cursor.FIELD_TYPE_STRING -> cursor.getString(idx).trim()
            Cursor.FIELD_TYPE_FLOAT, Cursor.FIELD_TYPE_INTEGER ->
                cursor.getLong(idx).toString()
            else -> cursor.getString(idx)?.trim().orEmpty()
        }

    fun readOptionalLongKey(
        cursor: Cursor,
        idx: Int,
    ): Long? =
        when (cursor.getType(idx)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_STRING -> cursor.getString(idx).trim().toLongOrNull()
            else -> cursor.getLong(idx)
        }
}
