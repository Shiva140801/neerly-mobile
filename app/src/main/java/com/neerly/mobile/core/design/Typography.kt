package com.neerly.mobile.core.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography. Real TTFs live at res/font — drop Plus Jakarta Sans,
 * Instrument Serif, and JetBrains Mono in there and this file can be upgraded
 * to Font(R.font.plus_jakarta_sans_regular, ...) per family. Until then we
 * fall back to the platform families so the app compiles on a blank checkout.
 *
 * Keep every weight/size parity with the design tokens — swapping the family
 * back to explicit TTFs should be a one-line change.
 */
val PlusJakartaSans: FontFamily = FontFamily.SansSerif
val InstrumentSerif: FontFamily = FontFamily.Serif
val JetBrainsMono:   FontFamily = FontFamily.Monospace

val NeerlyType = Typography(
    displayLarge = TextStyle(fontFamily = InstrumentSerif,  fontWeight = FontWeight.Normal,   fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.4).sp),
    displayMedium= TextStyle(fontFamily = InstrumentSerif,  fontWeight = FontWeight.Normal,   fontSize = 32.sp, lineHeight = 36.sp),
    headlineLarge= TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 32.sp),
    headlineMedium=TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.Bold,     fontSize = 22.sp),
    headlineSmall =TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.Bold,     fontSize = 18.sp),
    titleLarge   = TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium  = TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    titleSmall   = TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    bodyLarge    = TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium   = TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall    = TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge   = TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium  = TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall   = TextStyle(fontFamily = PlusJakartaSans,  fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.9.sp)
)
