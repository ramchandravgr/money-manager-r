package com.rrparallelstudio.miapp.ads.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.rrparallelstudio.miapp.R

/** Fixed width × height banner (dp), e.g. Home Daily empty ledger centre slot 300×200. */
@Composable
fun MmrFixedSizeBannerAd(
    adUnitId: String,
    widthDp: Int,
    heightDp: Int,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    AndroidView(
        modifier =
            modifier
                .width(widthDp.dp)
                .height(heightDp.dp)
                .testTag(testTag),
        factory = {
            AdView(context).apply {
                visibility = View.GONE
                setAdSize(AdSize(widthDp, heightDp))
                this.adUnitId = adUnitId
                adListener =
                    object : AdListener() {
                        override fun onAdLoaded() {
                            visibility = View.VISIBLE
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            visibility = View.GONE
                        }
                    }
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}

@Composable
fun MmrAdaptiveBannerAd(
    adUnitId: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag(testTag),
        factory = {
            AdView(context).apply {
                visibility = View.GONE
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp),
                )
                this.adUnitId = adUnitId
                adListener =
                    object : AdListener() {
                        override fun onAdLoaded() {
                            visibility = View.VISIBLE
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            visibility = View.GONE
                        }
                    }
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}

@Composable
fun MmrMediumRectangleAd(
    adUnitId: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag(testTag),
        factory = {
            AdView(context).apply {
                visibility = View.GONE
                setAdSize(AdSize.MEDIUM_RECTANGLE)
                this.adUnitId = adUnitId
                adListener =
                    object : AdListener() {
                        override fun onAdLoaded() {
                            visibility = View.VISIBLE
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            visibility = View.GONE
                        }
                    }
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}

@Composable
fun MmrNativeAd(
    adUnitId: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var nativeAd by remember(adUnitId) { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adUnitId) {
        val adLoader =
            AdLoader
                .Builder(context, adUnitId)
                .forNativeAd { ad ->
                    nativeAd?.destroy()
                    nativeAd = ad
                }.withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            nativeAd = null
                        }
                    },
                ).build()
        adLoader.loadAd(AdRequest.Builder().build())
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return

    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag(testTag),
        factory = { ctx ->
            val adView =
                LayoutInflater
                    .from(ctx)
                    .inflate(R.layout.mmr_native_ad, FrameLayout(ctx), false) as NativeAdView
            adView.headlineView = adView.findViewById(R.id.mmr_native_ad_headline)
            adView.bodyView = adView.findViewById(R.id.mmr_native_ad_body)
            adView.callToActionView = adView.findViewById(R.id.mmr_native_ad_cta)
            (adView.headlineView as TextView).text = ad.headline
            ad.body?.let { (adView.bodyView as TextView).text = it }
            ad.callToAction?.let { (adView.callToActionView as Button).text = it }
                ?: run { adView.callToActionView?.visibility = View.GONE }
            adView.setNativeAd(ad)
            adView
        },
    )
}
