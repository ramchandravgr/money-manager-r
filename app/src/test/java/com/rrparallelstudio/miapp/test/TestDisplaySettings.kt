package com.rrparallelstudio.miapp.test

import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import org.robolectric.RuntimeEnvironment

fun robolectricDisplaySettingsStore(): AppDisplaySettingsStore {
    val context = RuntimeEnvironment.getApplication()
    return AppDisplaySettingsStore(context)
}
