package com.mmr.app

import android.app.Application
import android.content.ComponentCallbacks
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mmr.app.reminder.ScheduledReminderWorkScheduler
import com.mmr.app.widget.MmrWidgetPreviewRegistrar
import com.mmr.app.widget.MmrWidgetRefresher
import com.mmr.core.parser.catalog.UpiDisplayNameTable
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.res.Configuration as AndroidConfiguration

@HiltAndroidApp
class MmrApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var widgetRefresher: MmrWidgetRefresher

    @Inject lateinit var scheduledReminderWorkScheduler: ScheduledReminderWorkScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        scheduledReminderWorkScheduler.ensurePeriodicReminderChecks()
        UpiDisplayNameTable.load(this)
        applicationScope.launch {
            MmrWidgetPreviewRegistrar.registerAll(this@MmrApplication)
        }
        widgetRefresher.refreshAll()
        registerComponentCallbacks(
            object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: AndroidConfiguration) {
                    widgetRefresher.refreshAll()
                }

                override fun onLowMemory() = Unit
            },
        )
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
