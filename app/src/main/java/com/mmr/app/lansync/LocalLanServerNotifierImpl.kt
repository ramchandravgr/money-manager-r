package com.mmr.app.lansync

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mmr.core.lansync.LocalLanServerMode
import com.mmr.core.lansync.LocalLanServerNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLanServerNotifierImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LocalLanServerNotifier {
        override fun onLanServerStarted(mode: LocalLanServerMode) {
            if (mode == LocalLanServerMode.NONE) return
            val intent =
                Intent(context, LocalLanForegroundService::class.java).apply {
                    putExtra(LocalLanForegroundService.EXTRA_MODE, mode.name)
                }
            ContextCompat.startForegroundService(context, intent)
        }

        override fun onLanServerStopped() {
            context.stopService(Intent(context, LocalLanForegroundService::class.java))
        }
    }
