package com.mmr.app

import android.app.Application
import com.mmr.core.parser.catalog.UpiDisplayNameTable
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MmrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UpiDisplayNameTable.load(this)
    }
}
