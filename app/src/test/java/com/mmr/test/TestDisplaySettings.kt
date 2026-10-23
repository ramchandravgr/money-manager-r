package com.mmr.test

import com.mmr.core.data.preferences.AppDisplaySettingsStore
import org.robolectric.RuntimeEnvironment

fun robolectricDisplaySettingsStore(): AppDisplaySettingsStore {
    val context = RuntimeEnvironment.getApplication()
    return AppDisplaySettingsStore(context)
}
