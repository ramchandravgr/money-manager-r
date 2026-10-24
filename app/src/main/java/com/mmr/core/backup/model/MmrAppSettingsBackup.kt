package com.mmr.core.backup.model

import com.mmr.core.parser.scanner.SmsScanPeriod

/**
 * User preferences stored outside Room (SharedPreferences, etc.).
 * Optional nested blocks stay null on older backup files; import applies only what is present.
 */
data class MmrAppSettingsBackup(
    val smsScanMonths: Int = SmsScanPeriod.DEFAULT_MONTHS,
    val display: MmrDisplaySettingsBackup? = null,
    val smsInbound: MmrSmsInboundSettingsBackup? = null,
    val lanSync: MmrLanSyncSettingsBackup? = null,
    val webManage: MmrWebManageSettingsBackup? = null,
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
)

data class MmrSmsInboundSettingsBackup(
    val notifyOnNewPending: Boolean,
    val readSmsInboundPromptShown: Boolean,
)

data class MmrLanSyncSettingsBackup(
    val lanSyncEnabled: Boolean,
    val deviceId: String,
    val inboundAuthToken: String,
    val displayName: String,
    val peerMode: String,
    val selectedPeerDeviceId: String?,
    val includeAttachments: Boolean,
    val trustedWifiSsids: List<String>,
    val duplicateEntityPolicies: Map<String, String>,
)

data class MmrWebManageSettingsBackup(
    val enabled: Boolean,
    val passwordSalt: String?,
    val passwordHash: String?,
)
