package com.rrparallelstudio.miapp.core.asset

import com.rrparallelstudio.miapp.core.database.dao.NotificationRuleDao
import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.database.entity.NotificationRuleEntity
import com.rrparallelstudio.miapp.core.lansync.LanSyncTombstoneRecorder
import com.rrparallelstudio.miapp.core.lansync.SyncEntityType
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import com.rrparallelstudio.miapp.core.model.enums.NotificationRuleType
import com.rrparallelstudio.miapp.core.reminder.MmrReminderPresets

internal object AssetValuationReminderNotificationSync {
    suspend fun syncForAsset(
        asset: AssetEntity,
        notificationRuleDao: NotificationRuleDao,
        tombstoneRecorder: LanSyncTombstoneRecorder,
        now: Long = System.currentTimeMillis(),
    ) {
        tombstoneUnfiredForAsset(asset.id, notificationRuleDao, tombstoneRecorder)
        notificationRuleDao.deleteUnfiredForAsset(asset.id)

        if (!shouldSchedule(asset)) return

        val dueAt = asset.nextValuationDueMillis ?: return
        if (dueAt <= now) return

        val days = asset.valuationReminderDaysBefore
        if (!MmrReminderPresets.isValidLoanEmiReminderDays(days)) return

        val remindAt = dueAt - days * MmrReminderPresets.DAY_MILLIS
        val scheduleTime = if (remindAt <= now) now else remindAt

        notificationRuleDao.insert(
            NotificationRuleEntity(
                type = NotificationRuleType.ASSET_VALUATION_REMINDER,
                enabled = true,
                scheduleTime = scheduleTime,
                targetAssetId = asset.id,
                title = "Update asset value: ${asset.name}",
                message = "Tap to record a new valuation.",
            ),
        )
    }

    private fun shouldSchedule(asset: AssetEntity): Boolean {
        if (asset.status != AssetStatus.ACTIVE) return false
        if (asset.nextValuationDueMillis == null) return false
        if (asset.valuationReminderDaysBefore <= 0) return false
        return true
    }

    private suspend fun tombstoneUnfiredForAsset(
        assetId: Long,
        notificationRuleDao: NotificationRuleDao,
        tombstoneRecorder: LanSyncTombstoneRecorder,
    ) {
        notificationRuleDao.listForAsset(assetId).filter { !it.isFired }.forEach { notification ->
            tombstoneRecorder.recordDeletion(notification.syncId, SyncEntityType.NOTIFICATION_RULE)
        }
    }
}
