package com.neerly.mobile.feature.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlySpacing

@Composable
fun OrderPlacedScreen(
    orderId: String,
    orderNumber: String,
    onTrack: () -> Unit,
    onHome: () -> Unit
) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(NeerlySpacing.x8)) {
            Surface(color = NeerlyColors.Ok, shape = CircleShape, modifier = Modifier.size(84.dp)) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("✓", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(NeerlySpacing.x5))
            Text("Order placed",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Spacer(Modifier.height(6.dp))
            Text("Order #$orderNumber",
                fontSize = 14.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(6.dp))
            Text("We're finding the best vendor for you.",
                fontSize = 13.sp, color = NeerlyColors.Ink500)

            Spacer(Modifier.height(NeerlySpacing.x6))
            Button(
                onClick = onTrack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
            ) {
                Text("Track order", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(NeerlySpacing.x3))
            TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                Text("Back to home", color = NeerlyColors.Ink700)
            }
        }
    }
}
