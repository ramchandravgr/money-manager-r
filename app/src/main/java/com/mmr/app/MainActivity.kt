package com.mmr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mmr.app.drive.GoogleDriveAutoBackupTrigger
import com.mmr.app.navigation.MmrNavHost
import com.mmr.app.permission.MmrLaunchPermissionGate
import com.mmr.app.reminder.PendingNotificationCoordinator
import com.mmr.app.ui.adaptive.LocalMmrWindowSizeClass
import com.mmr.app.ui.splash.MmrLaunchSplashScreen
import com.mmr.app.ui.theme.MoneyInboxTheme
import com.mmr.core.data.catalog.MmrCategorizationRuleSeeder
import com.mmr.core.data.catalog.MmrDefaultCatalogSeeder
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.preferences.LaunchPermissionStore
import com.mmr.core.database.MmrDatabase
import com.mmr.core.loan.EmiAutoPostCoordinator
import com.mmr.core.recurring.RecurringAutoPostCoordinator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_NAV_ROUTE = "nav_route"
    }

    @Inject lateinit var emiAutoPostCoordinator: EmiAutoPostCoordinator

    @Inject lateinit var recurringAutoPostCoordinator: RecurringAutoPostCoordinator

    @Inject lateinit var database: MmrDatabase

    @Inject lateinit var displaySettingsStore: AppDisplaySettingsStore

    @Inject lateinit var googleDriveAutoBackupTrigger: GoogleDriveAutoBackupTrigger

    @Inject lateinit var pendingNotificationCoordinator: PendingNotificationCoordinator

    @Inject lateinit var launchPermissionStore: LaunchPermissionStore

    private var keepSystemSplash = true

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSystemSplash }
        super.onCreate(savedInstanceState)
        googleDriveAutoBackupTrigger.onAppForeground()
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
            var showLaunchAnimation by remember { mutableStateOf(true) }
            var showPermissionGate by remember {
                mutableStateOf(!launchPermissionStore.hasCompletedLaunchPermissionFlow())
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
                } else if (showPermissionGate) {
                    MmrLaunchPermissionGate(
                        launchPermissionStore = launchPermissionStore,
                        onComplete = { showPermissionGate = false },
                    )
                } else {
                    CompositionLocalProvider(LocalMmrWindowSizeClass provides windowSizeClass) {
                        val navController = rememberNavController()
                        val deepLinkRoute = intent?.getStringExtra(EXTRA_NAV_ROUTE)
                        LaunchedEffect(deepLinkRoute) {
                            deepLinkRoute?.let { route ->
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                                intent?.removeExtra(EXTRA_NAV_ROUTE)
                            }
                        }
                        MmrNavHost(
                            navController = navController,
                            hiddenMainNavIds = displaySettings.hiddenMainNavIds,
                        )
                    }
                }
            }
        }
    }
}
