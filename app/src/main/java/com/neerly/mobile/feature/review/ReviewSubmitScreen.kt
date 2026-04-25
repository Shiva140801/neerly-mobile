package com.neerly.mobile.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * Customer review submission. PRD §13: 2-axis rating — vendor experience +
 * water quality. Both 1-5 stars; optional written review applies to both.
 *
 * The "overall" rating sent to the backend's `rating` field is the average
 * of the two; vendor + waterQuality fields are also sent for backwards
 * compat with single-axis aggregation.
 *
 * Stays presentational — the caller (NavHost) wires the API call.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSubmitScreen(
    orderId: String,
    vendorName: String,
    onSubmit: (rating: Int, vendorRating: Int, waterQualityRating: Int, text: String?) -> Unit,
    onBack: () -> Unit
) {
    var vendorStars by remember { mutableIntStateOf(0) }
    var waterStars by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    val canSubmit = vendorStars in 1..5 && waterStars in 1..5 && !submitting

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Rate your order", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(NeerlySpacing.x5)
        ) {
            Text(vendorName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text("Order #${orderId.take(8)}", fontSize = 13.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(NeerlySpacing.x6))

            StarSection(
                label = "Vendor experience",
                hint = "Driver, packaging, on-time delivery",
                rating = vendorStars,
                onRate = { vendorStars = it }
            )
            Spacer(Modifier.height(NeerlySpacing.x5))
            StarSection(
                label = "Water quality",
                hint = "Taste, cleanliness, container condition",
                rating = waterStars,
                onRate = { waterStars = it }
            )

            Spacer(Modifier.height(NeerlySpacing.x6))
            Text("Tell us more (optional)",
                fontSize = 14.sp, color = NeerlyColors.Ink700, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(NeerlySpacing.x3))
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 2000) text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text("What went well? What could be better?", color = NeerlyColors.Ink400) },
                supportingText = { Text("${text.length} / 2000", color = NeerlyColors.Ink500, fontSize = 12.sp) }
            )

            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    submitting = true
                    val overall = (vendorStars + waterStars + 1) / 2  // round half-up
                    onSubmit(overall, vendorStars, waterStars, text.takeIf { it.isNotBlank() })
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
            ) {
                Text(if (submitting) "Submitting…" else "Submit review",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(NeerlySpacing.x3))
        }
    }
}

@Composable
private fun StarSection(label: String, hint: String, rating: Int, onRate: (Int) -> Unit) {
    Text(label, fontSize = 14.sp, color = NeerlyColors.Ink700, fontWeight = FontWeight.SemiBold)
    Text(hint, fontSize = 12.sp, color = NeerlyColors.Ink500)
    Spacer(Modifier.height(NeerlySpacing.x3))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..5) {
            val isOn = i <= rating
            val bg = if (isOn) NeerlyColors.CustomerPrimary else NeerlyColors.Ink100
            val fg = if (isOn) NeerlyColors.Paper else NeerlyColors.Ink500
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(NeerlyRadius.md))
                    .background(bg)
                    .clickable { onRate(i) },
                contentAlignment = Alignment.Center
            ) {
                Text("★", fontSize = 22.sp, color = fg)
            }
        }
    }
}
