package com.mmr.core.database.model

data class UpcomingReminder(
    val notificationId: Long,
    val ruleId: Long?,
    val title: String,
    val message: String,
    val scheduleTime: Long,
    val formattedScheduleDate: String,
    val amountFormatted: String?,
)
