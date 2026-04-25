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
    var dispatchOpen by remember { mutableStateOf(false) }
    var driverIdDraft by remember { mutableStateOf("") }

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
                                    Text("₹${it.unitPrice}",
                                        fontSize = 14.sp, color = NeerlyColors.Ink800)
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Text("Order value", Modifier.weight(1f), fontSize = 13.sp, color = NeerlyColors.Ink700)
                                Text("₹${o.orderValue}", fontSize = 13.sp, color = NeerlyColors.Ink900)
                            }
                            Row(Modifier.fillMaxWidth()) {
                                Text("Your earning",
                                    Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = NeerlyColors.VendorDark)
                                Text("₹${o.yourEarning}",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.VendorDark)
                            }
                        }
                    }

                    when (o.status) {
                        "PREPARING", "VENDOR_ASSIGNED" -> Button(
                            onClick = vm::markReady,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)
                        ) { Text("Mark ready for dispatch", fontWeight = FontWeight.SemiBold) }

                        "READY_FOR_DISPATCH" -> Button(
                            onClick = { dispatchOpen = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)
                        ) { Text("Dispatch driver", fontWeight = FontWeight.SemiBold) }
                    }

                    state.error?.let { Text(it, color = NeerlyColors.Err, fontSize = 13.sp) }
                }
            }
        }
    }

    if (dispatchOpen) {
        AlertDialog(
            onDismissRequest = { dispatchOpen = false },
            title = { Text("Dispatch to driver") },
            text = {
                Column {
                    Text("Driver id (V1.1 will replace with a picker):",
                        fontSize = 12.sp, color = NeerlyColors.Ink500)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = driverIdDraft, onValueChange = { driverIdDraft = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (driverIdDraft.isNotBlank()) {
                        vm.dispatch(driverIdDraft.trim())
                        dispatchOpen = false
                    }
                }) { Text("Dispatch", color = NeerlyColors.VendorPrimary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { dispatchOpen = false }) { Text("Cancel") } }
        )
    }
}
