package com.mmr.app

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration as AndroidConfiguration
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mmr.app.widget.MmrWidgetPreviewRegistrar
import com.mmr.app.widget.MmrWidgetRefresher
import com.mmr.core.parser.catalog.UpiDisplayNameTable
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MmrApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var widgetRefresher: MmrWidgetRefresher

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
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
