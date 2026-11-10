package com.rrparallelstudio.miapp.core.backup.model

import com.rrparallelstudio.miapp.core.parser.scanner.SmsScanPeriod

/** User preferences stored outside Room (SMS scan, display, SMS inbound toggles). */
data class MmrAppSettingsBackup(
    val smsScanMonths: Int = SmsScanPeriod.DEFAULT_MONTHS,
    val display: MmrDisplaySettingsBackup? = null,
    val smsInbound: MmrSmsInboundSettingsBackup? = null,
    val ownerProfile: MmrOwnerProfileBackup? = null,
    val knownProfiles: List<MmrKnownProfileBackup> = emptyList(),
)

data class MmrOwnerProfileBackup(
    val profileId: String,
    val displayName: String,
)

data class MmrKnownProfileBackup(
    val profileId: String,
    val displayName: String,
    val updatedAt: Long = 0L,
)

data class MmrDisplaySettingsBackup(
    val mainCurrencyCode: String,
    val subCurrencyCode: String?,
    val startScreenTab: String,
    val monthStartDay: Int,
    val weekStartDay: String,
    val budgetCarryOver: Boolean,
    val swipeBehavior: String,
    val amountColorScheme: String,
    val timeDisplay: String,
    val showDescription: Boolean,
    val autocompleteEnabled: Boolean,
    val inputOrder: String,
    val appColorTheme: String,
    val themeMode: String = "SYSTEM",
    val hiddenMainNavIds: List<String> = emptyList(),
)

data class MmrSmsInboundSettingsBackup(
    val notifyOnNewPending: Boolean,
    val readSmsInboundPromptShown: Boolean,
)
