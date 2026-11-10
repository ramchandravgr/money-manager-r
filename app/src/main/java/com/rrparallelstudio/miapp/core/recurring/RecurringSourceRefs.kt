package com.rrparallelstudio.miapp.core.recurring

object RecurringSourceRefs {
    fun occurrence(
        ruleId: Long,
        runDateMillis: Long,
    ): String = "recurring:$ruleId:$runDateMillis"
}
