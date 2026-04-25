package com.neerly.mobile.feature.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.OrderResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    vm: OrderHistoryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Your orders", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Loading…", color = NeerlyColors.Ink500)
            }
            state.orders.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No orders yet",
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                    Spacer(Modifier.height(6.dp))
                    Text("Place your first order from a vendor on Home.",
                        fontSize = 13.sp, color = NeerlyColors.Ink500)
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(state.orders, key = { it.id }) { OrderRow(it, onOpen) }
            }
        }
    }
}

@Composable
private fun OrderRow(order: OrderResponse, onOpen: (String) -> Unit) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable { onOpen(order.id) }
    ) {
        Row(
            Modifier.padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Order #${order.orderNumber}",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text(order.status.replace("_", " "),
                    fontSize = 11.sp, color = statusColor(order.status),
                    fontWeight = FontWeight.SemiBold)
                Text(order.placedAt.take(10), fontSize = 11.sp, color = NeerlyColors.Ink500)
            }
            Text("₹${order.totalAmount}",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
        }
    }
}

@Composable
private fun statusColor(status: String) = when (status) {
    "DELIVERED" -> NeerlyColors.Ok
    "CANCELLED", "FAILED" -> NeerlyColors.Err
    else -> NeerlyColors.CustomerDark
}
