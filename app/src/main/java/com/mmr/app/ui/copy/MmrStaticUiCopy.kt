package com.mmr.app.ui.copy

/**
 * Title-cases static UI copy: capitalizes the first letter of each word.
 * Words are bounded by whitespace or `/`, `·`, or `-` (including `‑`).
 *
 * Use only for developer-authored labels, not user-entered names or dynamic values.
 */
fun String.mmrStaticLabelCase(): String {
    if (isEmpty()) return this
    return buildString(length) {
        var capitalizeNext = true
        for (ch in this@mmrStaticLabelCase) {
            when {
                ch.isWhitespace() || ch == '/' || ch == '·' || ch == '-' || ch == '‑' -> {
                    append(ch)
                    capitalizeNext = true
                }
                capitalizeNext && ch.isLowerCase() -> {
                    append(ch.titlecaseChar())
                    capitalizeNext = false
                }
                else -> {
                    append(ch)
                    capitalizeNext = false
                }
            }
        }
    }
}
