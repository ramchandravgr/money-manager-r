package com.mmr.app.navigation

/**
 * Routes that external apps may pass via [com.mmr.app.MainActivity.EXTRA_NAV_ROUTE]
 * (widgets, notifications). Rejects arbitrary deep links from other packages.
 */
object MmrExternalNavRouteAllowlist {
    private val allowedPatterns =
        listOf(
            Regex("^home$"),
            Regex("^budgets$"),
            Regex("^google_drive_backup$"),
            Regex("^approval_center(\\?.*)?$"),
            Regex("^add_transaction(\\?.*)?$"),
            Regex("^loan_detail/\\d+$"),
            Regex("^recurring_detail/\\d+$"),
            Regex("^asset_detail/\\d+$"),
        )

    fun sanitize(
        route: String?,
        trustSensitiveQueryParams: Boolean = true,
    ): String? {
        if (route.isNullOrBlank()) return null
        val trimmed = route.trim()
        if (trimmed.contains("..") || trimmed.contains("//")) return null
        val path = trimmed.substringBefore('?')
        if (path.isBlank()) return null
        val allowed =
            trimmed.takeIf { candidate ->
                allowedPatterns.any { pattern -> pattern.matches(candidate) }
            } ?: return null
        if (trustSensitiveQueryParams || !trimmed.contains('?')) {
            return allowed
        }
        val pathOnly = trimmed.substringBefore('?')
        return pathOnly.takeIf { candidate ->
            allowedPatterns.any { pattern -> pattern.matches(candidate) }
        }
    }
}
