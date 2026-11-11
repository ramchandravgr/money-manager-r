package com.rrparallelstudio.miapp.core.moneymanager

/**
 * Collapses paired Money Manager transfer legs ([DO_TYPE] 2/3/4) into one import row each.
 */
internal object MoneyManagerMmbakTransferPlanner {
    data class PlannedTransfer(
        val dateTimeMillis: Long,
        val amount: Double,
        val fromAssetUid: String,
        val toAssetUid: String,
        val note: String?,
        val sourceReferenceId: String,
        val legRowKeys: List<String>,
    )

    data class Plan(
        val incomeExpense: List<MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow>,
        val transfers: List<PlannedTransfer>,
        val unsupportedRowKeys: List<String>,
    )

    fun plan(rows: List<MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow>): Plan {
        val incomeExpense = ArrayList<MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow>()
        val transferLegs = ArrayList<MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow>()
        val unsupported = ArrayList<String>()
        for (row in rows) {
            when (row.doType) {
                0, 1 -> incomeExpense.add(row)
                2, 3, 4 -> transferLegs.add(row)
                else -> unsupported.add(row.rowKey)
            }
        }
        val transfers = ArrayList<PlannedTransfer>()
        val grouped =
            transferLegs.groupBy { leg ->
                leg.transferGroupId?.takeIf { it.isNotBlank() } ?: fallbackGroupKey(leg)
            }
        for ((_, legs) in grouped) {
            val planned = planTransferGroup(legs)
            if (planned != null) {
                transfers.add(planned)
            } else {
                unsupported.addAll(legs.map { it.rowKey })
            }
        }
        return Plan(
            incomeExpense = incomeExpense,
            transfers = transfers,
            unsupportedRowKeys = unsupported,
        )
    }

    fun transferSourceReference(transferGroupId: String?): String? =
        transferGroupId?.takeIf { it.isNotBlank() }?.let { "mmbak:tx:$it" }

    private fun planTransferGroup(
        legs: List<MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow>,
    ): PlannedTransfer? {
        val canonical = selectCanonicalLeg(legs) ?: return null
        val from = canonical.assetUid?.takeIf { it.isNotBlank() } ?: return null
        val to = canonical.toAssetUid?.takeIf { it.isNotBlank() } ?: return null
        if (from == to) return null
        val groupId = legs.firstNotNullOfOrNull { it.transferGroupId?.takeIf { id -> id.isNotBlank() } }
        val sourceRef = transferSourceReference(groupId) ?: "mmbak:transfer:${canonical.rowKey}"
        return PlannedTransfer(
            dateTimeMillis = canonical.dateTimeMillis,
            amount = canonical.amount,
            fromAssetUid = from,
            toAssetUid = to,
            note = canonical.note,
            sourceReferenceId = sourceRef,
            legRowKeys = legs.map { it.rowKey },
        )
    }

    private fun selectCanonicalLeg(
        legs: List<MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow>,
    ): MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow? {
        val withEnds =
            legs.filter { row ->
                !row.assetUid.isNullOrBlank() && !row.toAssetUid.isNullOrBlank()
            }
        if (withEnds.isEmpty()) return null
        return withEnds.firstOrNull { it.doType == 3 }
            ?: withEnds.firstOrNull { it.doType == 2 }
            ?: withEnds.first()
    }

    private fun fallbackGroupKey(leg: MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow): String {
        val a = leg.assetUid.orEmpty()
        val b = leg.toAssetUid.orEmpty()
        val pair = if (a <= b) "$a|$b" else "$b|$a"
        val note = leg.note?.trim().orEmpty()
        return "${leg.dateTimeMillis}|${leg.amount}|$pair|$note"
    }
}
