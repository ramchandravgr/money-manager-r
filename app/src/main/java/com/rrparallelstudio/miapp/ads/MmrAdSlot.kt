package com.rrparallelstudio.miapp.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrparallelstudio.miapp.BuildConfig
import com.rrparallelstudio.miapp.ads.ui.MmrAdaptiveBannerAd
import com.rrparallelstudio.miapp.ads.ui.MmrFixedSizeBannerAd
import com.rrparallelstudio.miapp.ads.ui.MmrNativeAd
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@Composable
fun MmrAdSlot(
    placement: MmrAdPlacement?,
    modifier: Modifier = Modifier,
) {
    if (placement == null) return
    val coordinator = LocalMmrAdCoordinator.current
    val adsEnabled by coordinator.adsEnabled.collectAsState()
    if (!adsEnabled) {
        if (BuildConfig.ADS_DEBUG_PLACEHOLDERS) {
            MmrAdDebugPlaceholder(placement = placement, modifier = modifier)
        }
        return
    }
    val unitId = MmrAdMobUnits.unitId(placement)
    when (placement) {
        MmrAdPlacement.HOME_DAILY_BANNER ->
            MmrAdaptiveBannerAd(
                adUnitId = unitId,
                testTag = MmrTestTags.adHomeDailyBanner,
                modifier = modifier,
            )
        MmrAdPlacement.HOME_DAILY_MREC ->
            MmrFixedSizeBannerAd(
                adUnitId = unitId,
                widthDp = MmrDimens.homeDailyCentreBannerWidth.value.toInt(),
                heightDp = MmrDimens.homeDailyCentreBannerHeight.value.toInt(),
                testTag = MmrTestTags.adHomeDailyMrec,
                modifier = modifier,
            )
        MmrAdPlacement.STATS_NATIVE ->
            MmrNativeAd(
                adUnitId = unitId,
                testTag = MmrTestTags.adStatsNative,
                modifier =
                    modifier.padding(
                        horizontal =
                            com.rrparallelstudio.miapp.ui.adaptive
                                .mmrScreenHorizontalPadding(),
                        vertical = MmrDimens.spaceSm,
                    ),
            )
    }
}

@Composable
private fun MmrAdDebugPlaceholder(
    placement: MmrAdPlacement,
    modifier: Modifier = Modifier,
) {
    val (height, label, subtitle) =
        when (placement) {
            MmrAdPlacement.HOME_DAILY_BANNER ->
                Triple(50.dp, "Banner (debug)", "Home · Daily · non-empty · adaptive strip")
            MmrAdPlacement.HOME_DAILY_MREC ->
                Triple(
                    MmrDimens.homeDailyCentreBannerHeight,
                    "Centre banner (debug)",
                    "Home · Daily · empty · 300×200 above FAB",
                )
            MmrAdPlacement.STATS_NATIVE ->
                Triple(120.dp, "Native ad (debug)", "Trends · below cylinder")
        }
    val testTag =
        when (placement) {
            MmrAdPlacement.HOME_DAILY_BANNER -> MmrTestTags.adHomeDailyBanner
            MmrAdPlacement.HOME_DAILY_MREC -> MmrTestTags.adHomeDailyMrec
            MmrAdPlacement.STATS_NATIVE -> MmrTestTags.adStatsNative
        }
    val widthModifier =
        if (placement == MmrAdPlacement.HOME_DAILY_MREC) {
            Modifier.width(MmrDimens.homeDailyCentreBannerWidth)
        } else {
            Modifier.fillMaxWidth()
        }
    Box(
        modifier =
            modifier
                .then(widthModifier)
                .padding(horizontal = MmrDimens.listItemPaddingHorizontal)
                .padding(vertical = MmrDimens.spaceSm)
                .heightIn(min = height)
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = MmrBrandColors.BorderMain,
                    shape = RoundedCornerShape(8.dp),
                ).background(MmrBrandColors.settingsSheetGroupedListFill())
                .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MmrBrandColors.dialogTitleText(),
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MmrBrandColors.dialogMessageText(),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
