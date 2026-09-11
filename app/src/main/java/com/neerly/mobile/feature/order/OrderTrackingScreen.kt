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
import com.neerly.mobile.core.design.OfflineBanner
import com.neerly.mobile.core.design.OfflineCapabilityCard
import com.neerly.mobile.core.design.OrderTrackingSkeleton
import com.neerly.mobile.core.design.StaleDataStamp
import com.neerly.mobile.core.util.asRupees

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
    val online by vm.isOnline.collectAsState()

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
        Column(Modifier.fillMaxSize().padding(padding)) {
        // Non-blocking: whatever already loaded stays on screen and readable.
        if (!online) OfflineBanner(onRetry = vm::refresh)
        when {
            state.loading && state.order == null -> OrderTrackingSkeleton()
            state.order == null ->
                Column(
                    Modifier.fillMaxSize().padding(NeerlySpacing.x6),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "We can't show this order",
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900
                    )
                    Spacer(Modifier.height(NeerlySpacing.x2))
                    Text(
                        state.error ?: "This order isn't available right now.",
                        fontSize = 13.sp, color = NeerlyColors.Ink500
                    )
                    Spacer(Modifier.height(NeerlySpacing.x5))
                    Button(
                        onClick = vm::refresh,
                        colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
                    ) { Text("Try again") }
                }
            else -> {
                val order = state.order!!
                Column(
                    Modifier.fillMaxSize().padding(NeerlySpacing.x5),
                    verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x4)
                ) {
                    Text("Order #${order.orderNumber}",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text("Total ${order.totalAmount.asRupees()}",
                        fontSize = 14.sp, color = NeerlyColors.Ink500)
                    // Only stamped when we actually have a fetch time to quote.
                    if (!online) {
                        state.lastUpdatedLabel?.let { StaleDataStamp(it) }
                    }

                    // Live map while the driver is on the road. Gated on the order
                    // status so a finished/early order shows the plain timeline.
                    if (order.status in OrderTrackingViewModel.IN_FLIGHT) {
                        state.tracking?.let { t ->
                            OrderLiveMap(t)
                            if (t.driverName != null || t.vehicle != null) {
                                Text(
                                    listOfNotNull(t.driverName, t.vehicle, t.driverPhoneMask)
                                        .joinToString("  ·  "),
                                    fontSize = 13.sp, color = NeerlyColors.Ink700,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

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

                    if (!online) OfflineCapabilityCard(Modifier.fillMaxWidth())

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
}

@Composable
private fun StatusTimeline(current: String) {
    // Step keys are the backend's OrderStatus names — the old "OUT_FOR_DELIVERY"
    // label was never a real status, so DISPATCHED orders under-highlighted.
    val steps = listOf(
        "PLACED"            to "Order placed",
        "VENDOR_ACCEPTED"   to "Vendor confirmed",
        "PREPARING"         to "Preparing",
        "DISPATCHED"        to "Out for delivery",
        "ARRIVING"          to "Arriving",
        "DELIVERED"         to "Delivered"
    )
    // Waiting-on-a-vendor states sit between "placed" and "confirmed".
    val effective = when (current) {
        "VENDOR_ASSIGNED", "VENDOR_REJECTED" -> "PLACED"
        else -> current
    }
    val currentIdx = steps.indexOfFirst { it.first == effective }.coerceAtLeast(0)
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
