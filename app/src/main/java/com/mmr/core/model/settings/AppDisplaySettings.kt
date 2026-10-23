package com.mmr.core.model.settings

enum class MmrStartScreenTab {
    DAILY,
    CALENDAR,
    ;

    val label: String
        get() =
            when (this) {
                DAILY -> "Daily"
                CALENDAR -> "Calendar"
            }
}

enum class MmrWeekStartDay(
    val javaDayOfWeek: Int,
) {
    MONDAY(java.time.DayOfWeek.MONDAY.value),
    SUNDAY(java.time.DayOfWeek.SUNDAY.value),
    SATURDAY(java.time.DayOfWeek.SATURDAY.value),
    ;

    val label: String
        get() = name.lowercase().replaceFirstChar { it.titlecase() }
}

enum class MmrSwipeBehavior {
    CHANGE_DATE,
    SCROLL_LIST,
    ;

    val label: String
        get() =
            when (this) {
                CHANGE_DATE -> "To Change Date"
                SCROLL_LIST -> "To Scroll List"
            }
}

enum class MmrAmountColorScheme {
    SET_A,
    SET_B,
    ;

    val label: String
        get() =
            when (this) {
                SET_A -> "Set. A"
                SET_B -> "Set. B"
            }
}

enum class MmrTimeDisplay {
    NONE_ASC,
    NONE_DESC,
    SHOW_ASC,
    SHOW_DESC,
    ;

    val label: String
        get() =
            when (this) {
                NONE_ASC -> "None, Asc."
                NONE_DESC -> "None, Desc."
                SHOW_ASC -> "Show, Asc."
                SHOW_DESC -> "Show, Desc."
            }

    val showTime: Boolean
        get() = this == SHOW_ASC || this == SHOW_DESC

    val ascending: Boolean
        get() = this == NONE_ASC || this == SHOW_ASC
}

enum class MmrInputOrder {
    FROM_AMOUNT,
    FROM_CATEGORY,
    ;

    val label: String
        get() =
            when (this) {
                FROM_AMOUNT -> "From Amount"
                FROM_CATEGORY -> "From Category"
            }
}
