package com.neerly.mobile.feature.vendor.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.neerly.mobile.data.dto.VendorOrderResponse

/**
 * Vendor "Today" tab — top bar shows summary KPIs, then 3 segments: Pending,
 * Active, Completed. Pending orders show a 3-min countdown card with Accept /
 * Reject buttons; per Boondi vendor flow §8 acceptance window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorTodayScreen(
    onOpen: (String) -> Unit,
    onCatalog: () -> Unit,
    onEarnings: () -> Unit,
    onSettings: () -> Unit,
    vm: VendorTodayViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var rejectTarget by remember { mutableStateOf<VendorOrderResponse?>(null) }
    var rejectReason by remember { mutableStateOf("Out of stock") }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Today", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = onCatalog) { Text("Catalog", color = NeerlyColors.VendorPrimary) }
                    TextButton(onClick = onEarnings) { Text("Earnings", color = NeerlyColors.VendorPrimary) }
                    TextButton(onClick = onSettings) { Text("More", color = NeerlyColors.Ink700) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Loading…", color = NeerlyColors.Ink500)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item { SummaryCard(state) }
                if (state.pending.isNotEmpty()) {
                    item { Section("New orders — accept or reject", state.pending.size) }
                    items(state.pending, key = { it.id }) { order ->
                        PendingOrderCard(
                            order = order,
                            onAccept = { vm.acceptOrder(order.id) },
                            onReject = { rejectTarget = order; rejectReason = "Out of stock" }
                        )
                    }
                }
                if (state.active.isNotEmpty()) {
                    item { Section("Active", state.active.size) }
                    items(state.active, key = { it.id }) { OrderRow(it, onOpen) }
                }
                if (state.completed.isNotEmpty()) {
                    item { Section("Completed today", state.completed.size) }
                    items(state.completed, key = { it.id }) { OrderRow(it, onOpen) }
                }
                if (state.pending.isEmpty() && state.active.isEmpty() && state.completed.isEmpty()) {
                    item {
                        Text(
                            "Quiet so far. Orders show up here as customers place them.",
                            fontSize = 13.sp, color = NeerlyColors.Ink500
                        )
                    }
                }
                state.error?.let {
                    item { Text(it, color = NeerlyColors.Err, fontSize = 13.sp) }
                }
            }
        }
    }

    rejectTarget?.let { order ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject order #${order.orderNumber}?") },
            text = {
                Column {
                    Text("Reason (shown to customer):", fontSize = 13.sp, color = NeerlyColors.Ink700)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.rejectOrder(order.id, rejectReason)
                    rejectTarget = null
                }) { Text("Reject", color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SummaryCard(state: VendorTodayUiState) {
    val s = state.summary ?: return
    Surface(
        color = NeerlyColors.VendorPrimary,
        shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x5)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Today", fontSize = 12.sp, color = NeerlyColors.Paper.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold)
                    Text("₹${s.earningToday}", fontSize = 28.sp,
                        fontWeight = FontWeight.Bold, color = NeerlyColors.Paper)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.deliveredToday} delivered",
                        fontSize = 12.sp, color = NeerlyColors.Paper)
                    Text("${s.activeOrders} active · ${s.pendingAccept} pending",
                        fontSize = 11.sp, color = NeerlyColors.Paper.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.height(NeerlySpacing.x2))
            Row {
                s.avgRating?.let {
                    Text("$it ★ avg",
                        fontSize = 12.sp, color = NeerlyColors.Paper.copy(alpha = 0.85f))
                }
                if (s.strikesLast30d > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("⚠ ${s.strikesLast30d} strikes (30d)",
                        fontSize = 12.sp, color = NeerlyColors.Paper)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = NeerlyColors.Ink500,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("$count", fontSize = 12.sp, color = NeerlyColors.Ink500,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PendingOrderCard(
    order: VendorOrderResponse,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("#${order.orderNumber}", fontSize = 12.sp, color = NeerlyColors.Ink500)
                    Text(order.customerFirstName,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                }
                Surface(color = NeerlyColors.WarnSoft, shape = RoundedCornerShape(NeerlyRadius.pill)) {
                    Text("3:00 to accept",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp, color = NeerlyColors.Warn,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(NeerlySpacing.x2))
            order.items.forEach {
                Text("${it.quantity} × ${it.productName} @ ₹${it.unitPrice}",
                    fontSize = 13.sp, color = NeerlyColors.Ink700)
            }
            Spacer(Modifier.height(NeerlySpacing.x2))
            Text("${order.deliveryAddress} · ${order.pincode}" +
                (order.distanceKm?.let { " · ${it} km" } ?: ""),
                fontSize = 12.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(NeerlySpacing.x2))
            Row {
                Text("₹${order.orderValue} order · ₹${order.yourEarning} earning",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = NeerlyColors.VendorDark, modifier = Modifier.weight(1f))
                Text(order.paymentStatus, fontSize = 11.sp, color = NeerlyColors.Ink500)
            }
            Spacer(Modifier.height(NeerlySpacing.x3))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeerlyColors.Err)
                ) { Text("Reject") }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.Ok)
                ) { Text("Accept", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun OrderRow(order: VendorOrderResponse, onOpen: (String) -> Unit) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable { onOpen(order.id) }
    ) {
        Row(Modifier.padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("#${order.orderNumber} · ${order.customerFirstName}",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text(order.status.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() },
                    fontSize = 11.sp, color = NeerlyColors.VendorDark)
                Text("${order.items.sumOf { it.quantity }} items · ${order.pincode}",
                    fontSize = 11.sp, color = NeerlyColors.Ink500)
            }
            Text("₹${order.yourEarning}",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
        }
    }
}
