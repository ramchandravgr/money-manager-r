package com.rrparallelstudio.miapp.release

/**
 * Curated release notes for the in-app "What's new" dialog.
 * Update [entries] with each store release; list [changesNewestFirst] with the latest change on top.
 */
object MmrReleaseNotesCatalog {
    data class VersionNotes(
        val version: String,
        val changesNewestFirst: List<String>,
    )

    private val entries: List<VersionNotes> =
        listOf(
            VersionNotes(
                version = "0.11.1",
                changesNewestFirst =
                    listOf(
                        "Trends Income/Expenses control is a sliding pill; period totals stay on the tabs.",
                    ),
            ),
            VersionNotes(
                version = "0.11.0",
                changesNewestFirst =
                    listOf(
                        "AdMob (banner, MREC, native) with placement rules; Firebase Remote Config ads_enabled (default off) and Crashlytics when google-services.json is present.",
                        "Max ad content rating T.",
                        "Main tab label: Trends (was Stats).",
                    ),
            ),
            VersionNotes(
                version = "0.10.13",
                changesNewestFirst =
                    listOf(
                        "Month/year picker uses shared MmrLedgerYearBounds (50 years back, 5 years ahead of current year).",
                    ),
            ),
            VersionNotes(
                version = "0.10.12",
                changesNewestFirst =
                    listOf(
                        "Main navigation tab renamed from Stats to Trends (route and icons unchanged).",
                    ),
            ),
            VersionNotes(
                version = "0.10.11",
                changesNewestFirst =
                    listOf(
                        "Ad placement scaffolding (Daily banner/MREC, Stats native): debug-only labeled placeholders; release shows no slot until AdMob loads.",
                    ),
            ),
            VersionNotes(
                version = "0.10.10",
                changesNewestFirst =
                    listOf(
                        "Period summary total receipt icon shows item lines again (cut-out detail, stays visible on dark UI).",
                    ),
            ),
            VersionNotes(
                version = "0.10.9",
                changesNewestFirst =
                    listOf(
                        "Period summary total icon is a filled receipt shape with stronger contrast (Introduction + Home strip).",
                    ),
            ),
            VersionNotes(
                version = "0.10.8",
                changesNewestFirst =
                    listOf(
                        "Introduction tour: solid readable panels, real summary/nav icons, more screens (tabs, settings, backup). Settings → Replay introduction.",
                    ),
            ),
            VersionNotes(
                version = "0.10.7",
                changesNewestFirst =
                    listOf(
                        "First launch shows a short Introduction (Home views, summary icons, approvals, backup). Settings → Replay introduction.",
                    ),
            ),
            VersionNotes(
                version = "0.10.6",
                changesNewestFirst =
                    listOf(
                        "Home period summary uses icon labels (↓ income, ↑ expense, calendar carry-over, Σ total); carry-over column and list row appear only when non-zero.",
                    ),
            ),
            VersionNotes(
                version = "0.10.5",
                changesNewestFirst =
                    listOf(
                        "Swipe on Home and Stats always changes month/period; the old swipe setting was removed.",
                    ),
            ),
            VersionNotes(
                version = "0.10.4",
                changesNewestFirst =
                    listOf(
                        "Balance carry-over is on by default; the month's opening day shows a read-only Carry-over ledger row.",
                    ),
            ),
            VersionNotes(
                version = "0.10.3",
                changesNewestFirst =
                    listOf(
                        "Home ledger view modes use a centered dropdown (Daily, Weekly, Monthly, Summary) instead of four tabs.",
                    ),
            ),
            VersionNotes(
                version = "0.10.2",
                changesNewestFirst =
                    listOf(
                        "Rose Gold is the default app color theme; its expressive background shows soft banknote stacks, coins, and guilloché-style curves.",
                    ),
            ),
            VersionNotes(
                version = "0.10.1",
                changesNewestFirst =
                    listOf(
                        "Play application ID is now com.rrparallelstudio.miapp (Kotlin packages under com.rrparallelstudio.miapp.*).",
                        "Backups and signed archives from older app IDs will not import (no backward compatibility for this change).",
                        "Re-add your Google Sign-In OAuth client for the new package name if you use Drive backup.",
                    ),
            ),
            VersionNotes(
                version = "0.10.0",
                changesNewestFirst =
                    listOf(
                        "Inbox scan and live SMS capture are off for Play (no READ_SMS / RECEIVE_SMS).",
                        "Approval Center: paste a bank SMS; approve, reject, merge, and edit still work.",
                        "Pending approvals home-screen widget is disabled (code kept for later).",
                    ),
            ),
            VersionNotes(
                version = "0.9.1",
                changesNewestFirst =
                    listOf(
                        "App lock on launch (Settings → Transaction Appearance → Security).",
                        "SMS approval notifications: Open and Approve actions.",
                    ),
            ),
            VersionNotes(
                version = "0.9.0",
                changesNewestFirst =
                    listOf(
                        "Backup import accepts signed .mibxbac files only, with size limits.",
                        "Peer Device Sync pairing uses a PIN and an on-phone approval step.",
                        "Web Manage sign-in sessions expire after two hours and stay bound to the browser " +
                            "that logged in.",
                        "Restoring a backup resets device sync identity and turns Web Manage off.",
                        "Links from other apps no longer open sensitive approval actions automatically.",
                        "Notification permission is checked on Android 13 and later.",
                    ),
            ),
        )

    fun notesForVersion(versionName: String): VersionNotes? = entries.firstOrNull { it.version == versionName.trim() }

    /**
     * Older major/minor milestones (newest first), excluding the current [appVersionName].
     */
    fun priorMajorMinorHistory(appVersionName: String): List<VersionNotes> =
        entries
            .filter { it.version.trim() != appVersionName.trim() }
            .sortedWith(
                compareByDescending<VersionNotes> { parseSemver(it.version)?.major ?: 0 }
                    .thenByDescending { parseSemver(it.version)?.minor ?: 0 }
                    .thenByDescending { parseSemver(it.version)?.patch ?: 0 },
            )

    private data class SemverTriple(
        val major: Int,
        val minor: Int,
        val patch: Int,
    )

    private fun parseSemver(version: String): SemverTriple? {
        val parts = version.trim().split(".")
        if (parts.size !in 2..3) return null
        val major = parts[0].toIntOrNull() ?: return null
        val minor = parts[1].toIntOrNull() ?: return null
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return SemverTriple(major, minor, patch)
    }
}
