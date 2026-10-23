package com.mmr.app.ui.state

object MmrUiMessages {
    const val GENERIC_LOAD = "We couldn't load this right now. Please try again."
    const val GENERIC_SAVE = "We couldn't save your changes. Please try again."
    const val GENERIC_ACTION = "Something went wrong. Please try again."

    fun friendly(
        technical: String?,
        fallback: String = GENERIC_ACTION,
    ): String {
        val trimmed = technical?.trim()
        return if (trimmed.isNullOrEmpty()) fallback else trimmed
    }
}
