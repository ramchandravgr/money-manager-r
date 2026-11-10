package com.rrparallelstudio.miapp.ads

import com.rrparallelstudio.miapp.BuildConfig

object MmrAdMobUnits {
    fun unitId(placement: MmrAdPlacement): String =
        when (placement) {
            MmrAdPlacement.HOME_DAILY_BANNER -> BuildConfig.ADMOB_UNIT_HOME_BANNER
            MmrAdPlacement.HOME_DAILY_MREC -> BuildConfig.ADMOB_UNIT_HOME_MREC
            MmrAdPlacement.STATS_NATIVE -> BuildConfig.ADMOB_UNIT_STATS_NATIVE
        }
}
