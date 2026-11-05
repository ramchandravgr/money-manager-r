package com.mmr.core.backup

import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.backup.model.MmrDisplaySettingsBackup
import com.mmr.core.backup.model.MmrSmsInboundSettingsBackup
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.preferences.SmsInboundSettingsStore
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.data.preferences.WebManagePreferencesStore
import com.mmr.core.model.settings.MmrAmountColorScheme
import com.mmr.core.model.settings.MmrAppColorTheme
import com.mmr.core.model.settings.MmrInputOrder
import com.mmr.core.model.settings.MmrStartScreenTab
import com.mmr.core.model.settings.MmrSwipeBehavior
import com.mmr.core.model.settings.MmrThemeMode
import com.mmr.core.model.settings.MmrTimeDisplay
import com.mmr.core.model.settings.MmrWeekStartDay
import com.mmr.core.owner.MmrOwnerProfileCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsBackupCoordinator
    @Inject
    constructor(
        private val smsScanSettingsStore: SmsScanSettingsStore,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val smsInboundSettingsStore: SmsInboundSettingsStore,
        private val webManagePreferencesStore: WebManagePreferencesStore,
        private val ownerProfileCoordinator: MmrOwnerProfileCoordinator,
    ) {
        suspend fun capture(): MmrAppSettingsBackup {
            val (ownerProfile, knownProfiles) = ownerProfileCoordinator.captureForBackup()
            return MmrAppSettingsBackup(
                smsScanMonths = smsScanSettingsStore.getScanMonths(),
                display = displaySettingsStore.snapshot().toBackup(),
                smsInbound =
                    MmrSmsInboundSettingsBackup(
                        notifyOnNewPending = smsInboundSettingsStore.isNotifyOnNewPendingEnabled(),
                        readSmsInboundPromptShown = smsInboundSettingsStore.hasReadSmsInboundPromptBeenShown(),
                    ),
                ownerProfile = ownerProfile,
                knownProfiles = knownProfiles,
            )
        }

        suspend fun apply(backup: MmrAppSettingsBackup?): Boolean {
            if (backup == null) return false
            smsScanSettingsStore.setScanMonths(backup.smsScanMonths)
            backup.display?.let { displaySettingsStore.applySnapshot(it.toSnapshot()) }
            backup.smsInbound?.let {
                smsInboundSettingsStore.setNotifyOnNewPending(it.notifyOnNewPending)
                smsInboundSettingsStore.setReadSmsInboundPromptShown(it.readSmsInboundPromptShown)
            }
            ownerProfileCoordinator.restoreFromBackup(backup.ownerProfile, backup.knownProfiles)
            resetNetworkFeaturesAfterRestore()
            return true
        }

        private fun resetNetworkFeaturesAfterRestore() {
            webManagePreferencesStore.setWebManageEnabled(false)
            webManagePreferencesStore.clearAccessPassword()
            ownerProfileCoordinator.rotateLanIdentityAfterBackupRestore()
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
                themeMode = themeMode.name,
                hiddenMainNavIds = hiddenMainNavIds.sorted(),
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
                themeMode = enumOrDefault(themeMode, MmrThemeMode.SYSTEM),
                hiddenMainNavIds = hiddenMainNavIds.toSet(),
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
