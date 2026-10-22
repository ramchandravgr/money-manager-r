package com.mmr.app.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.mmr.app.R

private val interFontName = GoogleFont("Inter")

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val MmrFontFamily: FontFamily = FontFamily(
    Font(interFontName, googleFontProvider, FontWeight.Normal),
    Font(interFontName, googleFontProvider, FontWeight.Medium),
    Font(interFontName, googleFontProvider, FontWeight.SemiBold),
    Font(interFontName, googleFontProvider, FontWeight.Bold)
)
