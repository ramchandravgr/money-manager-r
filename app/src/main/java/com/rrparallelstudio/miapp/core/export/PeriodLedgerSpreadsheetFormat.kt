package com.rrparallelstudio.miapp.core.export

object PeriodLedgerSpreadsheetFormat {
    fun matchesDisplayName(displayName: String?): Boolean {
        val name = displayName?.lowercase()?.trim().orEmpty()
        return name.endsWith(".xlsx") || name.endsWith(".xml")
    }

    fun isProbablyLedgerSpreadsheet(
        data: ByteArray,
        displayName: String?,
    ): Boolean {
        if (data.isEmpty()) return false
        val nameMatch = matchesDisplayName(displayName)
        val zipMagic = data.size >= 2 && data[0] == 0x50.toByte() && data[1] == 0x4B.toByte()
        val headLen = minOf(data.size, 64)
        val xmlMagic =
            String(data, 0, headLen, Charsets.UTF_8)
                .trimStart()
                .startsWith("<")
        return nameMatch && (zipMagic || xmlMagic)
    }
}
