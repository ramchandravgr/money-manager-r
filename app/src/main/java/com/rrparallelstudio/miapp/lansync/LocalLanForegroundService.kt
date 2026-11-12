package com.rrparallelstudio.miapp.lansync

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.lansync.LocalLanServerMode
import com.rrparallelstudio.miapp.navigation.Screen
import com.rrparallelstudio.miapp.navigation.buildMainActivityNavIntent

class LocalLanForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val modeName = intent?.getStringExtra(EXTRA_MODE) ?: LocalLanServerMode.PEER_SYNC.name
        val mode = runCatching { LocalLanServerMode.valueOf(modeName) }.getOrDefault(LocalLanServerMode.PEER_SYNC)
        val notification = buildNotification(mode)
        if (!promoteToForeground(notification)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun promoteToForeground(notification: Notification): Boolean =
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                    )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    )
                else -> startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: ForegroundServiceStartNotAllowedException) {
            false
        } catch (e: RuntimeException) {
            if (e.isForegroundServiceQuotaBlocked()) {
                false
            } else {
                throw e
            }
        }

    private fun buildNotification(mode: LocalLanServerMode): Notification {
        ensureChannel()
        val (title, text) =
            when (mode) {
                LocalLanServerMode.WEB_MANAGE ->
                    "Web Manage active" to "Browser access on your Wi‑Fi network"
                LocalLanServerMode.PEER_SYNC ->
                    "Peer Device Sync active" to "Stays on so family phones can sync on this Wi‑Fi"
                LocalLanServerMode.NONE ->
                    "LAN service" to "Money Inbox"
            }
        val navRoute =
            when (mode) {
                LocalLanServerMode.WEB_MANAGE -> Screen.WebManage.route
                LocalLanServerMode.PEER_SYNC -> Screen.LanSync.route
                LocalLanServerMode.NONE -> Screen.Settings.route
            }
        val requestCode =
            when (mode) {
                LocalLanServerMode.WEB_MANAGE -> 1
                LocalLanServerMode.PEER_SYNC -> 2
                LocalLanServerMode.NONE -> 3
            }
        val openApp =
            PendingIntent.getActivity(
                this,
                requestCode,
                buildMainActivityNavIntent(this, navRoute),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "LAN access",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shown while Web Manage or Peer Device Sync is on"
                setShowBadge(false)
            }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_MODE = "mode"
        private const val CHANNEL_ID = "mmr_lan_server"
        private const val NOTIFICATION_ID = 4101
    }
}
