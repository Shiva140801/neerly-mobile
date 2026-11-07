package com.neerly.mobile.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
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
    onRateOrder: (orderId: String, orderNumber: String) -> Unit,
    onFileComplaint: (String) -> Unit,
    vm: OrderTrackingViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val online by vm.isOnline.collectAsState()

    // Coming back from the review screen: re-read so "Rate your order" does not linger.
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

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
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(NeerlySpacing.x5),
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

                    if (order.status !in OrderTrackingViewModel.TERMINAL) {
                        DeliveryCodeCard(
                            code = state.deliveryCode,
                            requesting = state.codeRequesting,
                            error = state.codeError,
                            onRequest = vm::requestNewDeliveryCode
                        )
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
                        "DELIVERED" -> if (state.reviewed) {
                            Text(
                                "Thanks — you've rated this order.",
                                fontSize = 13.sp, color = NeerlyColors.Ink500,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Button(
                                onClick = { onRateOrder(order.id, order.orderNumber) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
                            ) { Text("Rate your order", fontWeight = FontWeight.SemiBold) }
                        }
                        "FAILED", "CANCELLED" -> EndedCard(order.status, order.paymentMethod)
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
private fun DeliveryCodeCard(code: String?, requesting: Boolean, error: String?, onRequest: () -> Unit) {
    Surface(
        color = NeerlyColors.CustomerSofter,
        shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Text("DELIVERY CODE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = NeerlyColors.CustomerDark, letterSpacing = 1.sp)
            if (code != null) {
                Text(
                    code.chunked(3).joinToString(" "),
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                    letterSpacing = 2.sp
                )
                Text("Read this to your driver at the door — they can't complete the drop without it.",
                    fontSize = 12.sp, color = NeerlyColors.Ink600)
            } else {
                Text("This phone doesn't have the code for this order.",
                    fontSize = 13.sp, color = NeerlyColors.Ink700)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onRequest, enabled = !requesting) {
                    Text(if (requesting) "Getting a code…" else "Get a delivery code",
                        color = NeerlyColors.CustomerPrimary)
                }
            }
            error?.let { Text(it, fontSize = 12.sp, color = NeerlyColors.Err) }
        }
    }
}

@Composable
private fun EndedCard(status: String, paymentMethod: String?) {
    val (title, body) = if (status == "FAILED") {
        "No vendor could take this order" to
            if (paymentMethod == "COD") "Nothing was charged. Try again in a little while, or pick another vendor."
            else "Any amount you paid is being refunded to your original payment method."
    } else {
        "This order was cancelled" to
            if (paymentMethod == "COD") "Nothing was charged." else "Any amount you paid is being refunded."
    }
    Surface(
        color = NeerlyColors.ErrSoft,
        shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Err)
            Spacer(Modifier.height(4.dp))
            Text(body, fontSize = 13.sp, color = NeerlyColors.Ink700)
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
                        // Reached steps stay green on an ended order; painting every step red
                        // read as "all of this failed". The ended card says what happened.
                        .background(if (done) NeerlyColors.Ok else NeerlyColors.Ink200),
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
