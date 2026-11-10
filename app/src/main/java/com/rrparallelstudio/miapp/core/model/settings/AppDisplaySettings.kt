package com.rrparallelstudio.miapp.core.model.settings

enum class MmrStartScreenTab {
    DAILY,
    CALENDAR,
    ;

    val label: String
        get() =
            when (this) {
                DAILY -> "Daily"
                CALENDAR -> "Monthly"
            }
}

enum class MmrWeekStartDay(
    val javaDayOfWeek: Int,
) {
    MONDAY(java.time.DayOfWeek.MONDAY.value),
    TUESDAY(java.time.DayOfWeek.TUESDAY.value),
    WEDNESDAY(java.time.DayOfWeek.WEDNESDAY.value),
    THURSDAY(java.time.DayOfWeek.THURSDAY.value),
    FRIDAY(java.time.DayOfWeek.FRIDAY.value),
    SATURDAY(java.time.DayOfWeek.SATURDAY.value),
    SUNDAY(java.time.DayOfWeek.SUNDAY.value),
    ;

    val label: String
        get() = name.lowercase().replaceFirstChar { it.titlecase() }
}

/** App chrome palette preset (light mode today; each preset has a paired dark palette for later). */
enum class MmrAppColorTheme {
    CLASSIC_CORAL,
    OCEAN,
    FOREST,
    SLATE,
    ROYAL,
    SUNSET,
    MIDNIGHT,
    ROSE_GOLD,
    MINT,
    CHARCOAL_GOLD,
    ;

    val label: String
        get() =
            when (this) {
                CLASSIC_CORAL -> "Classic Coral"
                OCEAN -> "Ocean"
                FOREST -> "Forest"
                SLATE -> "Slate"
                ROYAL -> "Royal"
                SUNSET -> "Sunset"
                MIDNIGHT -> "Midnight"
                ROSE_GOLD -> "Rose Gold"
                MINT -> "Mint"
                CHARCOAL_GOLD -> "Charcoal & Gold"
            }

    /** Classic Coral is kept for backup decode only — it is not offered in Settings. */
    fun forUi(): MmrAppColorTheme = if (this == CLASSIC_CORAL) OCEAN else this

    companion object {
        val pickerEntries: List<MmrAppColorTheme>
            get() = entries.filter { it != CLASSIC_CORAL }
    }
}

enum class MmrAmountColorScheme {
    SET_A,
    SET_B,
    SET_C,
    SET_D,
    SET_E,
    ;

    val label: String
        get() =
            when (this) {
                SET_A -> "Teal & brick red"
                SET_B -> "Emerald & bright red"
                SET_C -> "Sky & crimson"
                SET_D -> "Lime & amber"
                SET_E -> "Indigo & magenta"
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

/** Light/dark appearance for app chrome and widget amount colors. */
enum class MmrThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    OLED,
    ;

    val label: String
        get() =
            when (this) {
                SYSTEM -> "System default"
                LIGHT -> "Light"
                DARK -> "Dark"
                OLED -> "Pure black"
            }
}
