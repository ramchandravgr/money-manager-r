package com.mmr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import com.mmr.app.navigation.MmrNavHost
import com.mmr.app.ui.adaptive.LocalMmrWindowSizeClass
import com.mmr.app.ui.theme.MoneyManagerRTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            MoneyManagerRTheme {
                CompositionLocalProvider(LocalMmrWindowSizeClass provides windowSizeClass) {
                    val navController = rememberNavController()
                    MmrNavHost(navController = navController)
                }
            }
        }
    }
}
