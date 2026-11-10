package com.rrparallelstudio.miapp.core.owner

import com.rrparallelstudio.miapp.core.backup.model.MmrKnownProfileBackup
import com.rrparallelstudio.miapp.core.backup.model.MmrOwnerProfileBackup
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncIdentityStore
import com.rrparallelstudio.miapp.core.database.dao.OwnerProfileDao
import com.rrparallelstudio.miapp.core.database.entity.OwnerProfileEntity
import com.rrparallelstudio.miapp.core.lansync.model.LanOwnerProfileRecord
import com.rrparallelstudio.miapp.core.lansync.model.MmrLanSyncPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrOwnerProfileCoordinator
    @Inject
    constructor(
        private val identityStore: LanSyncIdentityStore,
        private val ownerProfileDao: OwnerProfileDao,
    ) {
        val localProfileId: String
            get() = identityStore.ownerProfileId

        suspend fun localStamp(): MmrCreatedByStamp {
            registerLocalProfile()
            return MmrCreatedByStamp(
                profileId = identityStore.ownerProfileId,
                displayName = identityStore.displayName,
            )
        }

        suspend fun registerLocalProfile() {
            val now = System.currentTimeMillis()
            ownerProfileDao.upsert(
                OwnerProfileEntity(
                    profileId = identityStore.ownerProfileId,
                    displayName = identityStore.displayName,
                    updatedAt = now,
                ),
            )
        }

        suspend fun onLocalDisplayNameChanged(name: String) {
            identityStore.displayName = name
            registerLocalProfile()
        }

        suspend fun restoreFromBackup(
            owner: MmrOwnerProfileBackup?,
            known: List<MmrKnownProfileBackup>,
        ) {
            owner?.let {
                identityStore.restoreOwnerProfile(it.profileId, it.displayName)
                registerLocalProfile()
            }
            if (known.isNotEmpty()) {
                ownerProfileDao.upsertAll(
                    known.map {
                        OwnerProfileEntity(
                            profileId = it.profileId,
                            displayName = it.displayName,
                            updatedAt = it.updatedAt,
                        )
                    },
                )
            }
        }

        suspend fun captureForBackup(): Pair<MmrOwnerProfileBackup, List<MmrKnownProfileBackup>> {
            registerLocalProfile()
            val owner =
                MmrOwnerProfileBackup(
                    profileId = identityStore.ownerProfileId,
                    displayName = identityStore.displayName,
                )
            val known =
                ownerProfileDao.getAllOnce().map {
                    MmrKnownProfileBackup(
                        profileId = it.profileId,
                        displayName = it.displayName,
                        updatedAt = it.updatedAt,
                    )
                }
            return owner to known
        }

        suspend fun ingestSyncPayload(payload: MmrLanSyncPayload) {
            val now = System.currentTimeMillis()
            val toUpsert = mutableListOf<OwnerProfileEntity>()
            payload.sourceProfileId?.takeIf { it.isNotBlank() }?.let { id ->
                toUpsert +=
                    OwnerProfileEntity(
                        profileId = id,
                        displayName = payload.sourceProfileName?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown",
                        updatedAt = now,
                    )
            }
            payload.knownProfiles.forEach { record ->
                toUpsert +=
                    OwnerProfileEntity(
                        profileId = record.profileId,
                        displayName = record.displayName,
                        updatedAt = record.updatedAt,
                    )
            }
            if (toUpsert.isNotEmpty()) {
                ownerProfileDao.upsertAll(toUpsert)
            }
        }

        suspend fun knownProfilesForExport(): List<LanOwnerProfileRecord> =
            ownerProfileDao.getAllOnce().map {
                LanOwnerProfileRecord(
                    profileId = it.profileId,
                    displayName = it.displayName,
                    updatedAt = it.updatedAt,
                )
            }

        suspend fun displayNameFor(profileId: String?): String? {
            if (profileId.isNullOrBlank()) return null
            if (profileId == identityStore.ownerProfileId) {
                return identityStore.displayName
            }
            return ownerProfileDao.getById(profileId)?.displayName
        }

        suspend fun shouldShowCreatedByLabel(profileId: String?): Boolean {
            if (!identityStore.isLanSyncEnabled()) return false
            if (profileId.isNullOrBlank()) return false
            if (profileId != identityStore.ownerProfileId) return true
            return ownerProfileDao.countOnce() > 1
        }

        suspend fun resolveCreatedByLabel(
            profileId: String?,
            snapshotName: String?,
        ): String? {
            if (!shouldShowCreatedByLabel(profileId)) return null
            return displayNameFor(profileId)
                ?: snapshotName?.trim()?.takeIf { it.isNotEmpty() }
                ?: profileId?.take(8)?.let { "Profile $it…" }
        }

        fun rotateLanIdentityAfterBackupRestore() {
            identityStore.rotateIdentityAfterBackupRestore()
        }
    }
