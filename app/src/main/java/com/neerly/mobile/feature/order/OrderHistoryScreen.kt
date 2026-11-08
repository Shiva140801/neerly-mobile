package com.neerly.mobile.feature.order

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.dto.OrderResponse

private val ACTIVE = setOf("PLACED", "VENDOR_ASSIGNED", "VENDOR_REJECTED", "VENDOR_ACCEPTED", "PREPARING", "DISPATCHED", "ARRIVING")

/** Boondh board 24 — the order on its way first (with its code), then everything earlier. */
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onSelectTab: (CustomerTab) -> Unit = {},
    onRate: (String, String) -> Unit = { _, _ -> },
    onInvoice: (String) -> Unit = {},
    onCart: () -> Unit = {},
    vm: OrderHistoryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::refresh)
    BoondhPage(stringResource(R.string.cust_oh_title), bottomBar = { CustomerBottomBar(current = CustomerTab.Orders, onSelect = onSelectTab) }) {
        if (state.orders.isEmpty()) {
            item { LoadState(state.loading, state.error, vm::refresh, stringResource(R.string.cust_oh_empty), stringResource(R.string.cust_oh_empty_body)) }
            return@BoondhPage
        }
        val (active, earlier) = state.orders.partition { it.status in ACTIVE }
        if (active.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.cust_oh_on_way)) }
            items(active, key = { it.id }) { o -> ActiveCard(o, vm.codeFor(o.id)) { onOpen(o.id) } }
        }
        if (earlier.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.cust_oh_earlier)) }
            items(earlier, key = { it.id }) { o ->
                BoondhCard(onClick = { onOpen(o.id) }) {
                    InfoRow(o.vendorName ?: stringResource(R.string.cust_oh_order_no, o.orderNumber),
                        listOf(shortDay(o.placedAt), itemsLine(o), endLine(o)).filter { it.isNotBlank() }.joinToString(" · "),
                        o.totalAmount.rupees())
                    if (o.status == "DELIVERED") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { if (vm.reorder(o)) onCart() }, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.cust_oh_reorder), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = { onRate(o.id, o.vendorName ?: o.orderNumber) }, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.cust_oh_rate), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = { onInvoice(o.id) }, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.cust_oh_invoice), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveCard(o: OrderResponse, code: String?, onTrack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(o.orderNumber, fontSize = 13.sp, color = Boondh.MutedOnNavy, letterSpacing = 0.6.sp)
        Text(when (o.status) {
            "DISPATCHED", "ARRIVING" -> o.driverName?.let { stringResource(R.string.cust_tr_driver_on_way, it.substringBefore(' ')) } ?: stringResource(R.string.cust_home_on_the_way)
            "VENDOR_ACCEPTED", "PREPARING" -> stringResource(R.string.cust_oh_packing)
            else -> stringResource(R.string.cust_home_finding_shop)
        }, fontFamily = FontFamily.Serif, fontSize = 22.sp, color = NeerlyColors.Paper)
        Text(listOf(itemsLine(o), o.totalAmount.rupees()).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 14.sp, color = Boondh.MutedOnNavy)
        code?.let { Text(stringResource(R.string.cust_oh_code, it.toCharArray().joinToString(" ")), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Paper) }
        PrimaryButton(stringResource(R.string.cust_oh_track), onTrack, color = Boondh.Amber, contentColor = NeerlyColors.Ink900)
    }
}

@Composable
private fun itemsLine(o: OrderResponse) = o.items.map { stringResource(R.string.cust_qty_item, it.quantity, it.productName) }.joinToString(", ")
@Composable
private fun endLine(o: OrderResponse) = when (o.status) {
    "CANCELLED" -> stringResource(R.string.cust_oh_cancelled)
    "FAILED" -> stringResource(R.string.cust_oh_failed)
    else -> ""
}
