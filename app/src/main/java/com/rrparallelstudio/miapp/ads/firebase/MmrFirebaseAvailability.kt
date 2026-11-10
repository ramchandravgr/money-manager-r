package com.rrparallelstudio.miapp.ads.firebase

import android.content.Context
import com.google.firebase.FirebaseApp

object MmrFirebaseAvailability {
    fun isConfigured(context: Context): Boolean =
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
}
