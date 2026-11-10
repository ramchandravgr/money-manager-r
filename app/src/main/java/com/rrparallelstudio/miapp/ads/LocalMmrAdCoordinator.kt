package com.rrparallelstudio.miapp.ads

import androidx.compose.runtime.staticCompositionLocalOf

val LocalMmrAdCoordinator =
    staticCompositionLocalOf<MmrAdCoordinator> {
        error("MmrAdCoordinator not provided")
    }
