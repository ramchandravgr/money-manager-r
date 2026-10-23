package com.mmr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mmr.app.navigation.MmrNavHost
import com.mmr.app.ui.adaptive.LocalMmrWindowSizeClass
import com.mmr.app.ui.theme.MoneyManagerRTheme
import com.mmr.core.data.catalog.MmrCategorizationRuleSeeder
import com.mmr.core.data.catalog.MmrDefaultCatalogSeeder
import com.mmr.core.database.MmrDatabase
import com.mmr.core.loan.EmiAutoPostCoordinator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var emiAutoPostCoordinator: EmiAutoPostCoordinator

    @Inject lateinit var database: MmrDatabase

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            MmrDefaultCatalogSeeder.seedIfMissing(database)
            MmrCategorizationRuleSeeder.seedIfMissing(this@MainActivity, database.categorizationRuleDao())
            emiAutoPostCoordinator.postDueEmiLedgerEntries()
        }
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            MoneyManagerRTheme(darkTheme = false) {
                CompositionLocalProvider(LocalMmrWindowSizeClass provides windowSizeClass) {
                    val navController = rememberNavController()
                    MmrNavHost(navController = navController)
                }
            }
        }
    }
}
