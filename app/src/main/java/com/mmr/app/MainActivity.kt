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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mmr.app.drive.GoogleDriveAutoBackupTrigger
import com.mmr.app.navigation.MmrNavHost
import com.mmr.app.ui.adaptive.LocalMmrWindowSizeClass
import com.mmr.app.ui.theme.MoneyInboxTheme
import com.mmr.core.data.catalog.MmrCategorizationRuleSeeder
import com.mmr.core.data.catalog.MmrDefaultCatalogSeeder
import com.mmr.core.data.preferences.AppDisplaySettingsStore
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

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleDriveAutoBackupTrigger.onAppForeground()
        lifecycleScope.launch {
            MmrDefaultCatalogSeeder.seedIfMissing(database)
            MmrCategorizationRuleSeeder.seedIfMissing(this@MainActivity, database.categorizationRuleDao())
            emiAutoPostCoordinator.postDueEmiLedgerEntries()
            recurringAutoPostCoordinator.postDueRecurringTransactions()
        }
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val displaySettings by displaySettingsStore.settings.collectAsState()
            MoneyInboxTheme(
                appColorTheme = displaySettings.appColorTheme,
                amountColorScheme = displaySettings.amountColorScheme,
            ) {
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
                    MmrNavHost(navController = navController)
                }
            }
        }
    }
}
