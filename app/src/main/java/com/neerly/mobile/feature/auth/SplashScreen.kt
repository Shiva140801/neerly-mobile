package com.neerly.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.InstrumentSerif
import com.neerly.mobile.core.design.NeerlyColors
import kotlinx.coroutines.delay

/** S-COMMON-SPLASH — neerly logo on blue gradient, matches design-reference/flows-customer.jsx */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { delay(1400); onDone() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to NeerlyColors.CustomerDark,
                    0.6f to NeerlyColors.CustomerPrimary,
                    1.0f to NeerlyColors.Water500
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                // Water drop glyph placeholder (replace with ImageVector in next pass)
                Text("💧", fontSize = 54.sp)
            }
            Text(
                "neerly",
                fontFamily = InstrumentSerif,
                fontSize = 48.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                "Water, sorted.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
