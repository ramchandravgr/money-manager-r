package com.mmr.core.recurring

object RecurringSourceRefs {
    fun occurrence(
        ruleId: Long,
        runDateMillis: Long,
    ): String = "recurring:$ruleId:$runDateMillis"
}
