package com.mmr.core.lansync.model

/** Profile directory entry shipped in LAN sync / restored from backup. */
data class LanOwnerProfileRecord(
    val profileId: String,
    val displayName: String,
    val updatedAt: Long = 0L,
)

data class LanCreatedByFields(
    val profileId: String?,
    val displayName: String?,
)

fun lanCreatedByFromRecord(
    recordId: String?,
    recordName: String?,
    payload: MmrLanSyncPayload,
): LanCreatedByFields {
    val profileId =
        recordId?.takeIf { it.isNotBlank() }
            ?: payload.sourceProfileId?.takeIf { it.isNotBlank() }
    val displayName =
        recordName?.takeIf { it.isNotBlank() }
            ?: payload.sourceProfileName?.takeIf { it.isNotBlank() }
    return LanCreatedByFields(profileId = profileId, displayName = displayName)
}
