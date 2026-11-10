package com.rrparallelstudio.miapp.ui.components

import android.content.Context
import android.widget.TextView
import android.widget.Toast

/** Replaces the previous toast so rapid summary-icon taps do not queue (~2s). */
object MmrToast {
    private var active: Toast? = null

    fun show(
        context: Context,
        message: CharSequence,
    ) {
        synchronized(this) {
            active?.cancel()
            val toast =
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).also {
                    configureMultilineMessage(it)
                }
            active = toast
            toast.show()
        }
    }

    @Suppress("DEPRECATION")
    private fun configureMultilineMessage(toast: Toast) {
        toast.view?.findViewById<TextView>(android.R.id.message)?.apply {
            maxLines = 8
            isSingleLine = false
            ellipsize = null
        }
    }
}
