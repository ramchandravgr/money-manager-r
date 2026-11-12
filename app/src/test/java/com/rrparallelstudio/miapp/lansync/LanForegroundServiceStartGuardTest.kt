package com.rrparallelstudio.miapp.lansync

import android.app.ForegroundServiceStartNotAllowedException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LanForegroundServiceStartGuardTest {
    @Test
    fun isForegroundServiceQuotaBlocked_detectsWrappedForegroundServiceStartNotAllowedException() {
        val wrapped =
            RuntimeException(
                "Unable to start service with null",
                ForegroundServiceStartNotAllowedException(
                    "Time limit already exhausted for foreground service type dataSync",
                ),
            )
        assertTrue(wrapped.isForegroundServiceQuotaBlocked())
    }

    @Test
    fun isForegroundServiceQuotaBlocked_returnsFalseForUnrelatedFailure() {
        assertFalse(IllegalStateException("other").isForegroundServiceQuotaBlocked())
    }
}
