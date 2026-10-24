package com.mmr.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mmr.app.widget.MmrWidgetRefresher
import com.mmr.core.parser.catalog.UpiDisplayNameTable
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MmrApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var widgetRefresher: MmrWidgetRefresher

    override fun onCreate() {
        super.onCreate()
        UpiDisplayNameTable.load(this)
        widgetRefresher.refreshAll()
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
