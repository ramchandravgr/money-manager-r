package com.mmr.app.release

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
                        "Signed .mibxbac import with payload limits and sanitizer checks.",
                        "LAN sync pairing uses a host PIN and pending approval (legacy paste link removed).",
                        "Web Manage: shorter session lifetime, login rate limit, token tied to client IP.",
                        "Backup restore rotates LAN identity and clears trusted peers / Web Manage password.",
                        "Deep links from other apps strip sensitive query params unless delivery is trusted.",
                        "Notifications respect POST_NOTIFICATIONS on Android 13+.",
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
