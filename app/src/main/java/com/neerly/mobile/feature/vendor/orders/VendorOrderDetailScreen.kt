package com.neerly.mobile.feature.vendor.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.core.util.asRupees

/**
 * Vendor order detail with state-machine actions:
 *   PREPARING        → "Mark ready"
 *   READY_FOR_DISPATCH → "Dispatch driver" (opens driver picker)
 *   OUT_FOR_DELIVERY → read-only, driver app handles handover
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorOrderDetailScreen(
    onBack: () -> Unit,
    vm: VendorOrderDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Order detail", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.VendorPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Loading…", color = NeerlyColors.Ink500)
            }
            state.order == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(state.error ?: "Order not found", color = NeerlyColors.Err)
            }
            else -> {
                val o = state.order!!
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(NeerlySpacing.x4),
                    verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
                ) {
                    Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
                        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(NeerlySpacing.x4)) {
                            Text("#${o.orderNumber}",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                            Text(o.status.replace("_", " "),
                                fontSize = 12.sp, color = NeerlyColors.VendorDark, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(NeerlySpacing.x3))
                            Text("Customer: ${o.customerFirstName} · ${o.customerPhoneMask}",
                                fontSize = 13.sp, color = NeerlyColors.Ink700)
                            Text(o.deliveryAddress, fontSize = 13.sp, color = NeerlyColors.Ink700)
                            if (o.jarsGoingOut > 0) {
                                Spacer(Modifier.height(NeerlySpacing.x2))
                                Text(
                                    buildString {
                                        append(if (o.jarsGoingOut == 1) "1 jar out" else "${o.jarsGoingOut} jars out")
                                        append(" · ")
                                        val e = o.emptiesExpected ?: 0
                                        append(if (e == 0) "no empties to collect" else if (e == 1) "collect 1 empty" else "collect $e empties")
                                        if (o.depositTotal.signum() > 0) append(" · ${o.depositTotal.asRupees()} deposit held")
                                    },
                                    fontSize = 12.sp, color = NeerlyColors.Ink500
                                )
                            }
                            o.notes?.let {
                                Spacer(Modifier.height(NeerlySpacing.x2))
                                Text("Note: $it", fontSize = 12.sp,
                                    color = NeerlyColors.Warn, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
                        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(NeerlySpacing.x4)) {
                            Text("Items", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink500)
                            Spacer(Modifier.height(8.dp))
                            o.items.forEach {
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("${it.quantity} × ${it.productName}",
                                        Modifier.weight(1f), fontSize = 14.sp, color = NeerlyColors.Ink800)
                                    Text(it.unitPrice.asRupees(),
                                        fontSize = 14.sp, color = NeerlyColors.Ink800)
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Text("Order value", Modifier.weight(1f), fontSize = 13.sp, color = NeerlyColors.Ink700)
                                Text(o.orderValue.asRupees(), fontSize = 13.sp, color = NeerlyColors.Ink900)
                            }
                            Row(Modifier.fillMaxWidth()) {
                                Text("Your earning",
                                    Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = NeerlyColors.VendorDark)
                                Text(o.yourEarning.asRupees(),
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.VendorDark)
                            }
                        }
                    }

                    // Actions follow the backend's real statuses. The old switch keyed on
                    // VENDOR_ASSIGNED and a READY_FOR_DISPATCH that does not exist, so an
                    // accepted order showed no next step at all.
                    val primary = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)
                    when (o.status) {
                        "VENDOR_ACCEPTED", "PREPARING" -> {
                            if (o.driverId == null) {
                                Button(
                                    onClick = vm::offerDriver,
                                    enabled = !state.offering,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = primary
                                ) { Text(if (state.offering) "Offering…" else "Offer to a driver", fontWeight = FontWeight.SemiBold) }
                            } else {
                                StatusNote("A driver has taken this run.")
                            }
                            if (o.status == "VENDOR_ACCEPTED") {
                                OutlinedButton(
                                    onClick = vm::markReady,
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text(
                                        if (o.driverId == null) "Mark load ready" else "Mark ready — send it out",
                                        color = NeerlyColors.VendorPrimary, fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                StatusNote(if (o.driverId == null) "Load is ready — waiting for a driver."
                                    else "Load is ready.")
                            }
                        }
                        "DISPATCHED" -> StatusNote("Out for delivery.")
                        "ARRIVING" -> StatusNote("Driver is at the customer's door.")
                        "DELIVERED" -> StatusNote("Delivered.")
                        "VENDOR_ASSIGNED" -> StatusNote("Accept this order from the Today screen.")
                        else -> StatusNote(o.status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
                    }
                    state.offerNote?.let { Text(it, color = NeerlyColors.Ink600, fontSize = 13.sp) }

                    state.error?.let { Text(it, color = NeerlyColors.Err, fontSize = 13.sp) }
                }
            }
        }
    }

}

@Composable
private fun StatusNote(text: String) {
    Text(text, fontSize = 14.sp, color = NeerlyColors.Ink700, fontWeight = FontWeight.SemiBold)
}
