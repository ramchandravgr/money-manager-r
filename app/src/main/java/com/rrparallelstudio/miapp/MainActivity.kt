package com.rrparallelstudio.miapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.rrparallelstudio.miapp.ads.LocalMmrAdCoordinator
import com.rrparallelstudio.miapp.ads.MmrAdCoordinator
import com.rrparallelstudio.miapp.ads.firebase.MmrAppVersionPolicy
import com.rrparallelstudio.miapp.ads.firebase.MmrFirebaseRemoteConfigSync
import com.rrparallelstudio.miapp.backup.LocalAutoBackupTrigger
import com.rrparallelstudio.miapp.core.approval.ApprovalSmsIntakeCoordinator
import com.rrparallelstudio.miapp.core.data.catalog.MmrCategorizationRuleSeeder
import com.rrparallelstudio.miapp.core.data.catalog.MmrDefaultCatalogSeeder
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.AppSecuritySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.FtueStore
import com.rrparallelstudio.miapp.core.data.preferences.LaunchPermissionStore
import com.rrparallelstudio.miapp.core.data.preferences.ReleaseNotesStore
import com.rrparallelstudio.miapp.core.database.MmrDatabase
import com.rrparallelstudio.miapp.core.lansync.LanSyncAutoSyncCoordinator
import com.rrparallelstudio.miapp.core.loan.EmiAutoPostCoordinator
import com.rrparallelstudio.miapp.core.recurring.RecurringAutoPostCoordinator
import com.rrparallelstudio.miapp.drive.GoogleDriveAutoBackupTrigger
import com.rrparallelstudio.miapp.navigation.HomeLedgerTabReselectNotifier
import com.rrparallelstudio.miapp.navigation.MmrExternalNavRouteAllowlist
import com.rrparallelstudio.miapp.navigation.MmrNavHost
import com.rrparallelstudio.miapp.navigation.MmrNavRouteIntentTrust
import com.rrparallelstudio.miapp.permission.MmrLaunchPermissionGate
import com.rrparallelstudio.miapp.reminder.PendingNotificationCoordinator
import com.rrparallelstudio.miapp.ui.adaptive.LocalMmrWindowSizeClass
import com.rrparallelstudio.miapp.ui.ftue.MmrFtueWelcomeFlow
import com.rrparallelstudio.miapp.ui.release.MmrReleaseNotesDialog
import com.rrparallelstudio.miapp.ui.security.MmrAppLockGate
import com.rrparallelstudio.miapp.ui.splash.MmrLaunchSplashScreen
import com.rrparallelstudio.miapp.ui.theme.MoneyInboxTheme
import com.rrparallelstudio.miapp.ui.update.MmrForceUpdateScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    companion object {
        const val EXTRA_NAV_ROUTE = "nav_route"
    }

    @Inject lateinit var emiAutoPostCoordinator: EmiAutoPostCoordinator

    @Inject lateinit var recurringAutoPostCoordinator: RecurringAutoPostCoordinator

    @Inject lateinit var database: MmrDatabase

    @Inject lateinit var displaySettingsStore: AppDisplaySettingsStore

    @Inject lateinit var googleDriveAutoBackupTrigger: GoogleDriveAutoBackupTrigger

    @Inject lateinit var localAutoBackupTrigger: LocalAutoBackupTrigger

    @Inject lateinit var pendingNotificationCoordinator: PendingNotificationCoordinator

    @Inject lateinit var launchPermissionStore: LaunchPermissionStore

    @Inject lateinit var releaseNotesStore: ReleaseNotesStore

    @Inject lateinit var ftueStore: FtueStore

    @Inject lateinit var adCoordinator: MmrAdCoordinator

    @Inject lateinit var approvalSmsIntakeCoordinator: ApprovalSmsIntakeCoordinator

    @Inject lateinit var appSecuritySettingsStore: AppSecuritySettingsStore

    @Inject lateinit var homeLedgerTabReselectNotifier: HomeLedgerTabReselectNotifier

    @Inject lateinit var appVersionPolicy: MmrAppVersionPolicy

    @Inject lateinit var firebaseRemoteConfigSync: MmrFirebaseRemoteConfigSync

    @Inject lateinit var lanSyncAutoSyncCoordinator: LanSyncAutoSyncCoordinator

    private var keepSystemSplash = true

    /** Bumped when a new [Intent] arrives so notification / widget routes apply while the app is open. */
    var externalNavIntentVersion by mutableIntStateOf(0)
        private set

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalNavIntentVersion++
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSystemSplash }
        super.onCreate(savedInstanceState)
        googleDriveAutoBackupTrigger.onAppForeground()
        localAutoBackupTrigger.onAppForeground()
        lifecycleScope.launch {
            MmrDefaultCatalogSeeder.seedIfMissing(database)
            MmrCategorizationRuleSeeder.seedIfMissing(this@MainActivity, database.categorizationRuleDao())
            emiAutoPostCoordinator.postDueEmiLedgerEntries()
            recurringAutoPostCoordinator.postDueRecurringTransactions()
            pendingNotificationCoordinator.reconcileAndDispatch()
        }
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val displaySettings by displaySettingsStore.settings.collectAsState()
            val appLockOnLaunch by appSecuritySettingsStore.appLockOnLaunch.collectAsState()
            var showLaunchAnimation by remember { mutableStateOf(true) }
            var showPermissionGate by remember {
                mutableStateOf(!launchPermissionStore.hasCompletedLaunchPermissionFlow())
            }
            var appUnlocked by remember { mutableStateOf(false) }
            val mustUpdate by appVersionPolicy.mustUpdate.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(appLockOnLaunch) {
                appUnlocked = !appLockOnLaunch
            }

            DisposableEffect(lifecycleOwner, appLockOnLaunch) {
                val observer =
                    LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> {
                                lifecycleScope.launch {
                                    firebaseRemoteConfigSync.refreshFromNetworkIfDueToday()
                                    lanSyncAutoSyncCoordinator.onAppForeground()
                                }
                            }
                            Lifecycle.Event.ON_STOP -> {
                                if (appLockOnLaunch) {
                                    appUnlocked = false
                                }
                            }
                            else -> Unit
                        }
                    }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            MoneyInboxTheme(
                appColorTheme = displaySettings.appColorTheme,
                themeMode = displaySettings.themeMode,
                amountColorScheme = displaySettings.amountColorScheme,
            ) {
                if (showLaunchAnimation) {
                    LaunchedEffect(Unit) {
                        keepSystemSplash = false
                    }
                    MmrLaunchSplashScreen(
                        onFinished = {
                            showLaunchAnimation = false
                        },
                    )
                } else if (mustUpdate) {
                    MmrForceUpdateScreen()
                } else if (showPermissionGate) {
                    MmrLaunchPermissionGate(
                        launchPermissionStore = launchPermissionStore,
                        onComplete = { showPermissionGate = false },
                    )
                } else if (appLockOnLaunch && !appUnlocked) {
                    MmrAppLockGate(
                        activity = this@MainActivity,
                        onUnlocked = { appUnlocked = true },
                    )
                } else {
                    CompositionLocalProvider(
                        LocalMmrWindowSizeClass provides windowSizeClass,
                        LocalMmrAdCoordinator provides adCoordinator,
                    ) {
                        val navController = rememberNavController()
                        val appVersionName = BuildConfig.VERSION_NAME
                        val ftueCompleteAtColdStart = remember { ftueStore.hasCompletedFtue() }
                        val showFtue by ftueStore.showFtue.collectAsState()
                        var showReleaseNotes by remember {
                            mutableStateOf(
                                releaseNotesStore.shouldShowFor(appVersionName) && ftueCompleteAtColdStart,
                            )
                        }
                        LaunchedEffect(showFtue) {
                            if (
                                !showFtue &&
                                !ftueCompleteAtColdStart &&
                                releaseNotesStore.shouldShowFor(appVersionName)
                            ) {
                                showReleaseNotes = true
                            }
                        }
                        LaunchedEffect(Unit) {
                            kotlin.runCatching {
                                approvalSmsIntakeCoordinator.importOnColdStartIfConfigured()
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            val navIntentVersion = externalNavIntentVersion
                            LaunchedEffect(navIntentVersion) {
                                val navIntent = this@MainActivity.intent
                                val trustedNav =
                                    MmrNavRouteIntentTrust.isTrustedDelivery(this@MainActivity, navIntent)
                                val deepLinkRoute =
                                    MmrExternalNavRouteAllowlist.sanitize(
                                        navIntent.getStringExtra(EXTRA_NAV_ROUTE),
                                        trustSensitiveQueryParams = trustedNav,
                                    )
                                deepLinkRoute?.let { route ->
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                    navIntent.removeExtra(EXTRA_NAV_ROUTE)
                                    navIntent.removeExtra(MmrNavRouteIntentTrust.EXTRA_INTERNAL_DELIVERY)
                                }
                            }
                            MmrNavHost(
                                navController = navController,
                                hiddenMainNavIds = displaySettings.hiddenMainNavIds,
                                homeLedgerTabReselectNotifier = homeLedgerTabReselectNotifier,
                            )
                            if (showReleaseNotes) {
                                MmrReleaseNotesDialog(
                                    appVersionName = appVersionName,
                                    onDismiss = {
                                        releaseNotesStore.markAcknowledged(appVersionName)
                                        showReleaseNotes = false
                                    },
                                )
                            }
                            if (showFtue) {
                                MmrFtueWelcomeFlow(
                                    onComplete = { ftueStore.markCompleted() },
                                    onSkip = { ftueStore.markCompleted() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
