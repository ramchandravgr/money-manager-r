package com.rrparallelstudio.miapp.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object MmrPlayStoreIntents {
    fun openAppListing(
        context: Context,
        packageName: String,
    ) {
        val market =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            val web =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(web)
        }
    }
}
