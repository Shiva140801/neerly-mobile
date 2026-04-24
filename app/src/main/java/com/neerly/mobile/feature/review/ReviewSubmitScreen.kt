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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * Customer review submission screen — one review per (customer, order).
 * Backend: POST /api/v1/customer/reviews  (see CustomerReviewController).
 *
 * Wired to view-model-less for now; caller owns the actual API call so the
 * screen stays presentational. PRD §9: 1-5 stars, text optional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSubmitScreen(
    orderId: String,
    vendorName: String,
    onSubmit: (rating: Int, text: String?) -> Unit,
    onBack: () -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

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

            Text("How was it?", fontSize = 14.sp, color = NeerlyColors.Ink700, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(NeerlySpacing.x3))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..5) {
                    val isOn = i <= rating
                    val bg = if (isOn) NeerlyColors.CustomerPrimary else NeerlyColors.Ink100
                    val fg = if (isOn) NeerlyColors.Paper else NeerlyColors.Ink500
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(NeerlyRadius.md))
                            .background(bg)
                            .clickable { rating = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("★", fontSize = 24.sp, color = fg)
                    }
                }
            }

            Spacer(Modifier.height(NeerlySpacing.x6))
            Text("Tell us more (optional)", fontSize = 14.sp, color = NeerlyColors.Ink700, fontWeight = FontWeight.SemiBold)
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
                    onSubmit(rating, text.takeIf { it.isNotBlank() })
                },
                enabled = rating in 1..5 && !submitting,
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
