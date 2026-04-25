package com.neerly.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.InstrumentSerif
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/** S-CUST-REG-01 — Welcome. Matches design-reference/flows-customer.jsx::ScreenWelcome */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .padding(horizontal = NeerlySpacing.x6, vertical = NeerlySpacing.x6)
    ) {
        // Language chip
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(NeerlyRadius.pill),
                color = NeerlyColors.Ink100
            ) {
                Text("EN · తె", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
            }
        }

        Spacer(Modifier.height(NeerlySpacing.x5))

        // Logo mark
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NeerlyColors.CustomerPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("💧", fontSize = 22.sp)
        }

        Spacer(Modifier.height(NeerlySpacing.x6))

        // "Water, sorted." display
        val headline = buildAnnotatedString {
            append("Water, ")
            withStyle(SpanStyle(color = NeerlyColors.CustomerPrimary, fontStyle = FontStyle.Italic)) {
                append("sorted.")
            }
        }
        Text(
            headline,
            fontFamily = InstrumentSerif,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            color = NeerlyColors.Ink900
        )

        Spacer(Modifier.height(NeerlySpacing.x3))

        Text(
            "Order clean water from trusted suppliers near you.",
            fontSize = 15.sp,
            color = NeerlyColors.Ink600
        )

        Spacer(Modifier.height(NeerlySpacing.x6))

        // Hero placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(NeerlyRadius.lg))
                .border(1.dp, NeerlyColors.Water100, RoundedCornerShape(NeerlyRadius.lg))
                .background(
                    Brush.verticalGradient(
                        listOf(NeerlyColors.Water50, NeerlyColors.Paper)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("💧  20L jar + phone", color = NeerlyColors.CustomerDark.copy(alpha = 0.9f), fontSize = 13.sp)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) {
            Text("Get started", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(NeerlySpacing.x3))

        Text(
            buildAnnotatedString {
                append("I sell water → ")
                withStyle(SpanStyle(color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)) {
                    append("Register as vendor")
                }
            },
            fontSize = 13.sp,
            color = NeerlyColors.Ink600,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(NeerlySpacing.x4))

        Text(
            "By continuing you agree to our Terms\nand Privacy Policy.",
            fontSize = 11.sp,
            color = NeerlyColors.Ink400,
            lineHeight = 17.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(NeerlySpacing.x2))
    }
}
