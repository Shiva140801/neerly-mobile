package com.neerly.mobile.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.VendorCardResponse

/**
 * Customer home — live data from `GET /customer/vendors?pincode=…`.
 *
 * Layout (top → bottom):
 *   header  : greeting + primary address pill + wallet chip + notification bell slot
 *   errors  : error banner with Retry if the initial load failed
 *   active  : horizontal strip of in-flight orders (empty section suppressed)
 *   vendors : vertical list of vendor cards
 */
@Composable
fun CustomerHomeScreen(
    onVendorClick: (String) -> Unit,
    onOrderClick: (String) -> Unit = {},
    vm: CustomerHomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = { HomeHeader(state) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x4)
        ) {
            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Loading…", color = NeerlyColors.Ink500)
                    }
                }
                state.error != null -> {
                    Surface(
                        color = NeerlyColors.ErrSoft,
                        shape = RoundedCornerShape(NeerlyRadius.md),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(NeerlySpacing.x4)) {
                            Text(
                                "Couldn't load home",
                                fontWeight = FontWeight.SemiBold,
                                color = NeerlyColors.Err
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                state.error ?: "",
                                color = NeerlyColors.Ink700,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(NeerlySpacing.x2))
                            Button(
                                onClick = { vm.refresh() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
                            ) { Text("Retry") }
                        }
                    }
                }
                else -> {
                    if (state.activeOrders.isNotEmpty()) {
                        Text(
                            "Active orders",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeerlyColors.Ink700
                        )
                        Spacer(Modifier.height(NeerlySpacing.x2))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.activeOrders, key = { it.id }) { o ->
                                ActiveOrderCard(o, onClick = { onOrderClick(o.id) })
                            }
                        }
                        Spacer(Modifier.height(NeerlySpacing.x5))
                    }
                    Text(
                        "Vendors near you",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeerlyColors.Ink900
                    )
                    Spacer(Modifier.height(NeerlySpacing.x3))
                    if (state.vendors.isEmpty()) {
                        Text(
                            "No vendors live in your pincode yet. Join the waitlist from Profile.",
                            color = NeerlyColors.Ink500,
                            fontSize = 13.sp
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.vendors, key = { it.id }) { v ->
                                VendorRow(v, onClick = { onVendorClick(v.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(state: HomeUiState) {
    Surface(color = NeerlyColors.Paper, shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                state.primaryAddress?.label?.let { "Deliver to · $it" } ?: "Welcome",
                fontSize = 12.sp,
                color = NeerlyColors.Ink500,
                fontWeight = FontWeight.SemiBold
            )
            val subtitle = state.primaryAddress?.let { "${it.streetArea} · ${it.pincode}" } ?: "Set your address"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subtitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeerlyColors.Ink900,
                    modifier = Modifier.weight(1f)
                )
                state.wallet?.let {
                    Surface(
                        color = NeerlyColors.CustomerSofter,
                        shape = RoundedCornerShape(NeerlyRadius.pill)
                    ) {
                        Text(
                            "₹${it.availableAmount}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = NeerlyColors.CustomerDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveOrderCard(order: OrderResponse, onClick: () -> Unit) {
    Surface(
        color = NeerlyColors.CustomerSofter,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier
            .widthIn(min = 220.dp)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Text(order.status, fontSize = 11.sp, color = NeerlyColors.CustomerDark, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Order #${order.orderNumber}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeerlyColors.Ink900
            )
            Spacer(Modifier.height(2.dp))
            Text("₹${order.totalAmount}", fontSize = 12.sp, color = NeerlyColors.Ink700)
        }
    }
}

@Composable
private fun VendorRow(vendor: VendorCardResponse, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NeerlyRadius.md),
        color = NeerlyColors.Paper,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeerlyColors.CustomerSofter),
                contentAlignment = Alignment.Center
            ) { Text("💧", fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vendor.businessName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeerlyColors.Ink900
                )
                Spacer(Modifier.height(2.dp))
                val rating = vendor.avgRating?.toPlainString() ?: "—"
                val tierLabel = vendor.tier.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                Text(
                    "$rating ★  ·  $tierLabel  ·  ${vendor.businessPincode}",
                    fontSize = 12.sp,
                    color = NeerlyColors.Ink500
                )
            }
            Text(">", fontSize = 20.sp, color = NeerlyColors.Ink400)
        }
    }
}
