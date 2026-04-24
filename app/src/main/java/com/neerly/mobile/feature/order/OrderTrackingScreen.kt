package com.neerly.mobile.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * Live tracking — vertical timeline of status events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    onBack: () -> Unit,
    onRateOrder: (String) -> Unit,
    onFileComplaint: (String) -> Unit,
    vm: OrderTrackingViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Tracking", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                actions = {
                    TextButton(onClick = vm::refresh) { Text("Refresh", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        when {
            state.loading && state.order == null ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text("Loading…", color = NeerlyColors.Ink500)
                }
            state.order == null ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text(state.error ?: "Order not found", color = NeerlyColors.Err)
                }
            else -> {
                val order = state.order!!
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(NeerlySpacing.x5),
                    verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x4)
                ) {
                    Text("Order #${order.orderNumber}",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text("Total ₹${order.totalAmount}",
                        fontSize = 14.sp, color = NeerlyColors.Ink500)

                    Surface(
                        color = NeerlyColors.Paper,
                        shape = RoundedCornerShape(NeerlyRadius.md),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(NeerlySpacing.x5)) {
                            StatusTimeline(order.status)
                        }
                    }

                    when (order.status) {
                        "DELIVERED" -> {
                            Button(
                                onClick = { onRateOrder(order.id) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
                            ) { Text("Rate your order", fontWeight = FontWeight.SemiBold) }
                        }
                        "FAILED", "CANCELLED" -> {
                            Text("Status: ${order.status}",
                                fontSize = 13.sp, color = NeerlyColors.Err)
                        }
                    }

                    TextButton(
                        onClick = { onFileComplaint(order.id) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Report an issue", color = NeerlyColors.Ink500)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusTimeline(current: String) {
    val steps = listOf(
        "PLACED"            to "Order placed",
        "VENDOR_ASSIGNED"   to "Vendor confirmed",
        "PREPARING"         to "Preparing",
        "OUT_FOR_DELIVERY"  to "Out for delivery",
        "ARRIVING"          to "Arriving",
        "DELIVERED"         to "Delivered"
    )
    val currentIdx = steps.indexOfFirst { it.first == current }.coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        steps.forEachIndexed { idx, (_, label) ->
            val done = idx <= currentIdx
            val active = idx == currentIdx && current !in setOf("DELIVERED", "CANCELLED", "FAILED")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape)
                        .background(
                            when {
                                current == "FAILED" || current == "CANCELLED" -> NeerlyColors.Err
                                done -> NeerlyColors.Ok
                                else -> NeerlyColors.Ink200
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) Text("✓", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (done) NeerlyColors.Ink900 else NeerlyColors.Ink500
                    )
                    if (active) {
                        Text("In progress", fontSize = 11.sp, color = NeerlyColors.CustomerDark)
                    }
                }
            }
        }
    }
}
