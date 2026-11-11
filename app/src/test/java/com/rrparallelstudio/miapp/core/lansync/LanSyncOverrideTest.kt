package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import com.rrparallelstudio.miapp.core.lansync.model.MmrLanSyncPayload
import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanSyncOverrideTest {
    @Test
    fun overrideJson_roundTripsReplacedRows() {
        val lines =
            listOf(
                LanSyncOverrideLine(
                    kind = "Loan",
                    title = "Home EMI",
                    yours = "Outstanding ₹80,000.00 · 8.5% · ACTIVE",
                    theirs = "Outstanding ₹76,000.00 · 8.5% · ACTIVE",
                ),
            )
        val decoded = LanSyncOverrideJson.decode(LanSyncOverrideJson.encode(lines))
        assertEquals(lines, decoded)
    }

    @Test
    fun withoutDeviceAppearance_dropsThemeSnapshot() {
        val payload =
            MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = 1L,
                sourceDeviceId = "a",
                sourceDeviceName = "Phone",
                categories = emptyList(),
                subcategories = emptyList(),
                accounts = emptyList(),
                loans = emptyList(),
                transactions = emptyList(),
                displaySettings =
                    AppDisplaySettingsSnapshot(
                        mainCurrencyCode = "INR",
                        mainCurrencySymbol = "₹",
                        appColorTheme = MmrAppColorTheme.ROSE_GOLD,
                    ),
            )
        assertNull(payload.withoutDeviceAppearance().displaySettings)
    }
}
