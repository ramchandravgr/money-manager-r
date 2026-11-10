package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.database.entity.CategorizationRuleEntity

suspend fun LanSyncTombstoneRecorder.recordCategorizationRuleDeletion(rule: CategorizationRuleEntity) {
    recordDeletion(rule.syncId, SyncEntityType.CATEGORIZATION_RULE)
}

suspend fun LanSyncTombstoneRecorder.recordCategorizationRuleDeletion(syncId: String) {
    recordDeletion(syncId, SyncEntityType.CATEGORIZATION_RULE)
}
