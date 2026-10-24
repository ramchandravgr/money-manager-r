package com.mmr.app.lansync

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
import com.mmr.app.MainActivity
import com.mmr.app.R
import com.mmr.core.lansync.LocalLanServerMode

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    private fun buildNotification(mode: LocalLanServerMode): Notification {
        ensureChannel()
        val (title, text) =
            when (mode) {
                LocalLanServerMode.WEB_MANAGE ->
                    "Web Manage active" to "Browser access on your Wi‑Fi network"
                LocalLanServerMode.PEER_SYNC ->
                    "Peer Device Sync active" to "Syncing with trusted devices on Wi‑Fi"
                LocalLanServerMode.NONE ->
                    "LAN service" to "Money Inbox"
            }
        val openApp =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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
