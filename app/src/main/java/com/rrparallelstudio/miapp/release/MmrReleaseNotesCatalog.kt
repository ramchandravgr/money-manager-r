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
                version = "0.11.17",
                changesNewestFirst =
                    listOf(
                        "Import from Money Manager (.mmbak): income, expense, and transfers merge in; " +
                            "paired transfer legs dedupe to one row; ISO dates map to your ledger dates.",
                        "Pending SMS: if you edit and pick an account, the approved transaction uses " +
                            "that account (not SMS card digits); quick approve unchanged.",
                    ),
            ),
            VersionNotes(
                version = "0.11.16",
                changesNewestFirst =
                    listOf(
                        "Web Manage: account pickers show Auto Created labels; transactions show Created by after peer sync; view refunds and apply card refunds on expenses.",
                        "Peer Device Sync: Auto sync on app open (optional) merges when trusted peers are on Wi‑Fi; off by default; new Wi‑Fi needs Sync now once; turning sync off clears auto sync.",
                        "Peer Device Sync: Created by note under your name; On this network section; trust uses picked phone and pairing code only.",
                        "Backup (format v5): SMS account corrections are included in export and restore; v4 backup files still import (without override rows).",
                        "Peer Device Sync (LAN format v7): SMS account corrections (template or instrument + last 4 → account) sync between trusted phones; update both devices before pairing.",
                    ),
            ),
            VersionNotes(
                version = "0.11.15",
                changesNewestFirst =
                    listOf(
                        "After you save a new manual transaction, the home ledger scrolls to the top.",
                        "Tapping the Peer Device Sync or Web Manage notification opens that settings screen directly.",
                        "Accounts created from SMS quick approve show Auto Created in Settings → Accounts, account edit, and account pickers; after you edit them, Auto Created - Modified.",
                    ),
            ),
            VersionNotes(
                version = "0.11.14",
                changesNewestFirst =
                    listOf(
                        "If you correct the account on an SMS approval or later edit, the next similar SMS uses that account instead of creating the same credit card again.",
                    ),
            ),
            VersionNotes(
                version = "0.11.13",
                changesNewestFirst =
                    listOf(
                        "Transaction note autocomplete shows matching prior notes in a dropdown under the note field.",
                    ),
            ),
            VersionNotes(
                version = "0.11.12",
                changesNewestFirst =
                    listOf(
                        "Peer Device Sync: after merge, account balances are recalculated from opening plus transactions (not by adding both phones’ stored balances).",
                    ),
            ),
            VersionNotes(
                version = "0.11.11",
                changesNewestFirst =
                    listOf(
                        "Peer Device Sync: simpler post-sync messages (short lines and bullet notes instead of one long warning sentence).",
                    ),
            ),
            VersionNotes(
                version = "0.11.10",
                changesNewestFirst =
                    listOf(
                        "Peer Device Sync: tap a phone on the network to use its address for pairing.",
                        "Shorter 7-character pairing codes.",
                    ),
            ),
            VersionNotes(
                version = "0.11.9",
                changesNewestFirst =
                    listOf(
                        "Peer Device Sync merges categories that share a name and type, so Sync now no longer stops with HTTP 400.",
                    ),
            ),
            VersionNotes(
                version = "0.11.8",
                changesNewestFirst =
                    listOf(
                        "Peer Device Sync shows why the other phone rejected a sync instead of only an HTTP code.",
                    ),
            ),
            VersionNotes(
                version = "0.11.7",
                changesNewestFirst =
                    listOf(
                        "Peer Device Sync sends this phone's trust token, so Sync now is accepted after the other phone approves pairing.",
                    ),
            ),
            VersionNotes(
                version = "0.11.6",
                changesNewestFirst =
                    listOf(
                        "Peer Device Sync keeps the connection open long enough to read the other phone's pairing reply.",
                    ),
            ),
            VersionNotes(
                version = "0.11.5",
                changesNewestFirst =
                    listOf(
                        "Peer Device Sync calls the other phone over the Wi‑Fi network, so pairing works when Web Manage already can.",
                    ),
            ),
            VersionNotes(
                version = "0.11.4",
                changesNewestFirst =
                    listOf(
                        "Peer Device Sync: restart LAN server when it stopped; optional other-phone address for pairing.",
                        "Clearer pairing errors when the code is from the wrong phone or the peer is unreachable.",
                    ),
            ),
            VersionNotes(
                version = "0.11.3",
                changesNewestFirst =
                    listOf(
                        "Targets Android API 36 for Google Play requirements (security and performance).",
                    ),
            ),
            VersionNotes(
                version = "0.11.2",
                changesNewestFirst =
                    listOf(
                        "Settings sheets and SMS backup setup guide: readable text on light and expressive themes.",
                        "Settings rows wrap long labels and values so backup rows stay readable.",
                        "Tapping Ledger again no longer jumps to Settings.",
                    ),
            ),
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
