package com.neerly.mobile.core.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.neerly.mobile.R

// Drop Plus Jakarta Sans + Instrument Serif + JetBrains Mono into res/font/ using filenames below.
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium,  FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold,    FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular,  FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_italic,   FontWeight.Normal, FontStyle.Italic)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium,  FontWeight.Medium)
)

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
