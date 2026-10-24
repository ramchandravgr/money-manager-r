package com.mmr.core.backup

import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.backup.model.MmrDisplaySettingsBackup
import com.mmr.core.backup.model.MmrLanSyncSettingsBackup
import com.mmr.core.backup.model.MmrSmsInboundSettingsBackup
import com.mmr.core.backup.model.MmrWebManageSettingsBackup
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.preferences.LanSyncDuplicateEntityPolicy
import com.mmr.core.data.preferences.LanSyncIdentityStore
import com.mmr.core.data.preferences.LanSyncPeerMode
import com.mmr.core.data.preferences.LanSyncPreferencesStore
import com.mmr.core.data.preferences.LanSyncWifiTrustStore
import com.mmr.core.data.preferences.SmsInboundSettingsStore
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.data.preferences.WebManagePreferencesStore
import com.mmr.core.model.settings.MmrAmountColorScheme
import com.mmr.core.model.settings.MmrAppColorTheme
import com.mmr.core.model.settings.MmrInputOrder
import com.mmr.core.model.settings.MmrStartScreenTab
import com.mmr.core.model.settings.MmrSwipeBehavior
import com.mmr.core.model.settings.MmrTimeDisplay
import com.mmr.core.model.settings.MmrWeekStartDay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsBackupCoordinator
    @Inject
    constructor(
        private val smsScanSettingsStore: SmsScanSettingsStore,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val smsInboundSettingsStore: SmsInboundSettingsStore,
        private val lanSyncIdentityStore: LanSyncIdentityStore,
        private val lanSyncPreferencesStore: LanSyncPreferencesStore,
        private val lanSyncWifiTrustStore: LanSyncWifiTrustStore,
        private val webManagePreferencesStore: WebManagePreferencesStore,
    ) {
        fun capture(): MmrAppSettingsBackup =
            MmrAppSettingsBackup(
                smsScanMonths = smsScanSettingsStore.getScanMonths(),
                display = displaySettingsStore.snapshot().toBackup(),
                smsInbound =
                    MmrSmsInboundSettingsBackup(
                        notifyOnNewPending = smsInboundSettingsStore.isNotifyOnNewPendingEnabled(),
                        readSmsInboundPromptShown = smsInboundSettingsStore.hasReadSmsInboundPromptBeenShown(),
                    ),
                lanSync =
                    MmrLanSyncSettingsBackup(
                        lanSyncEnabled = lanSyncIdentityStore.isLanSyncEnabled(),
                        deviceId = lanSyncIdentityStore.deviceId,
                        inboundAuthToken = lanSyncIdentityStore.inboundAuthToken,
                        displayName = lanSyncIdentityStore.displayName,
                        peerMode = lanSyncPreferencesStore.peerMode.name,
                        selectedPeerDeviceId = lanSyncPreferencesStore.selectedPeerDeviceId,
                        includeAttachments = lanSyncPreferencesStore.includeAttachments,
                        trustedWifiSsids = lanSyncWifiTrustStore.trustedSsids().sorted(),
                        duplicateEntityPolicies =
                            lanSyncPreferencesStore.duplicatePolicyEntries().mapValues { it.value.name },
                    ),
                webManage =
                    MmrWebManageSettingsBackup(
                        enabled = webManagePreferencesStore.isWebManageEnabled(),
                        passwordSalt = webManagePreferencesStore.getPasswordSalt(),
                        passwordHash = webManagePreferencesStore.getPasswordHash(),
                    ),
            )

        fun apply(backup: MmrAppSettingsBackup?): Boolean {
            if (backup == null) return false
            smsScanSettingsStore.setScanMonths(backup.smsScanMonths)
            backup.display?.let { displaySettingsStore.applySnapshot(it.toSnapshot()) }
            backup.smsInbound?.let {
                smsInboundSettingsStore.setNotifyOnNewPending(it.notifyOnNewPending)
                smsInboundSettingsStore.setReadSmsInboundPromptShown(it.readSmsInboundPromptShown)
            }
            backup.lanSync?.let {
                lanSyncIdentityStore.applyRestoredIdentity(
                    lanSyncEnabled = it.lanSyncEnabled,
                    deviceId = it.deviceId,
                    inboundAuthToken = it.inboundAuthToken,
                    displayName = it.displayName,
                )
                lanSyncPreferencesStore.peerMode =
                    runCatching { LanSyncPeerMode.valueOf(it.peerMode) }
                        .getOrDefault(LanSyncPeerMode.ALL_TRUSTED_ON_LAN)
                lanSyncPreferencesStore.selectedPeerDeviceId = it.selectedPeerDeviceId
                lanSyncPreferencesStore.includeAttachments = it.includeAttachments
                lanSyncWifiTrustStore.replaceTrustedSsids(it.trustedWifiSsids.toSet())
                lanSyncPreferencesStore.replaceDuplicatePolicies(
                    it.duplicateEntityPolicies.mapValues { (_, raw) ->
                        runCatching { LanSyncDuplicateEntityPolicy.valueOf(raw) }
                            .getOrDefault(LanSyncDuplicateEntityPolicy.ASK)
                    },
                )
            }
            backup.webManage?.let {
                webManagePreferencesStore.setWebManageEnabled(it.enabled)
                val salt = it.passwordSalt
                val hash = it.passwordHash
                if (!salt.isNullOrBlank() && !hash.isNullOrBlank()) {
                    webManagePreferencesStore.setAccessPassword(salt, hash)
                } else {
                    webManagePreferencesStore.clearAccessPassword()
                }
            }
            return true
        }

        private fun AppDisplaySettingsSnapshot.toBackup() =
            MmrDisplaySettingsBackup(
                mainCurrencyCode = mainCurrencyCode,
                subCurrencyCode = subCurrencyCode,
                startScreenTab = startScreenTab.name,
                monthStartDay = monthStartDay,
                weekStartDay = weekStartDay.name,
                budgetCarryOver = budgetCarryOver,
                swipeBehavior = swipeBehavior.name,
                amountColorScheme = amountColorScheme.name,
                timeDisplay = timeDisplay.name,
                showDescription = showDescription,
                autocompleteEnabled = autocompleteEnabled,
                inputOrder = inputOrder.name,
                appColorTheme = appColorTheme.name,
            )

        private fun MmrDisplaySettingsBackup.toSnapshot() =
            AppDisplaySettingsSnapshot(
                mainCurrencyCode = mainCurrencyCode,
                mainCurrencySymbol = currencySymbolFor(mainCurrencyCode),
                subCurrencyCode = subCurrencyCode?.takeIf { it.isNotBlank() },
                startScreenTab = enumOrDefault(startScreenTab, MmrStartScreenTab.DAILY),
                monthStartDay = monthStartDay.coerceIn(1, 28),
                weekStartDay = enumOrDefault(weekStartDay, MmrWeekStartDay.MONDAY),
                budgetCarryOver = budgetCarryOver,
                swipeBehavior = enumOrDefault(swipeBehavior, MmrSwipeBehavior.CHANGE_DATE),
                amountColorScheme = enumOrDefault(amountColorScheme, MmrAmountColorScheme.SET_B),
                timeDisplay = enumOrDefault(timeDisplay, MmrTimeDisplay.NONE_ASC),
                showDescription = showDescription,
                autocompleteEnabled = autocompleteEnabled,
                inputOrder = enumOrDefault(inputOrder, MmrInputOrder.FROM_AMOUNT),
                appColorTheme = enumOrDefault(appColorTheme, MmrAppColorTheme.CLASSIC_CORAL),
            )

        private inline fun <reified T : Enum<T>> enumOrDefault(
            raw: String,
            default: T,
        ): T = runCatching { enumValueOf<T>(raw) }.getOrDefault(default)

        private fun currencySymbolFor(code: String): String =
            when (code.uppercase()) {
                "INR" -> "₹"
                "USD" -> "$"
                "EUR" -> "€"
                "GBP" -> "£"
                else -> code
            }
    }
