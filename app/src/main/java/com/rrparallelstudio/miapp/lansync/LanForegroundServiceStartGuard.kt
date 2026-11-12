package com.rrparallelstudio.miapp.lansync

import android.app.ForegroundServiceStartNotAllowedException

internal fun Throwable.isForegroundServiceQuotaBlocked(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is ForegroundServiceStartNotAllowedException) return true
        current = current.cause
    }
    return false
}
