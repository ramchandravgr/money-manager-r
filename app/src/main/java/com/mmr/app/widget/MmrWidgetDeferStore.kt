package com.mmr.app.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Widget-only queue: "Later" defers an approval until cleared or the queue rotates. */
@Singleton
class MmrWidgetDeferStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun defer(approvalId: Long) {
            prefs.edit().putLong(deferKey(approvalId), System.currentTimeMillis()).apply()
        }

        fun clearDefer(approvalId: Long) {
            prefs.edit().remove(deferKey(approvalId)).apply()
        }

        fun isDeferred(approvalId: Long): Boolean = prefs.contains(deferKey(approvalId))

        private fun deferKey(approvalId: Long): String = "defer_$approvalId"
    }

private const val PREFS_NAME = "mmr_widget_approval_defer"
