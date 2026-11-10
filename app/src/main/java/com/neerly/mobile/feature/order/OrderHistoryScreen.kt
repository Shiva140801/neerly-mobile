package com.neerly.mobile.feature.order

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
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

private val ENDED_UNDELIVERED = setOf("CANCELLED", "FAILED")
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
    // The tab keeps its view model across tab switches, and OnReturn skips the first
    // resume after the screen is rebuilt — so a cancel made on the tracking screen never
    // reached this list. Reload every time the screen appears, and keep re-checking while
    // an order is open: the shop and driver change its status from their phones.
    LaunchedEffect(Unit) { vm.refresh() }
    com.neerly.mobile.core.design.OnReturn(vm::refresh)
    val anyActive = state.orders.any { it.status in ACTIVE }
    LaunchedEffect(anyActive) {
        while (anyActive) { kotlinx.coroutines.delay(OrderHistoryViewModel.LIVE_REFRESH_MS); vm.refresh() }
    }
    BoondhPage(stringResource(R.string.cust_oh_title), bottomBar = { CustomerBottomBar(current = CustomerTab.Orders, onSelect = onSelectTab) }) {
        if (state.orders.isEmpty()) {
            item { LoadState(state.loading, state.error, vm::refresh, stringResource(R.string.cust_oh_empty), stringResource(R.string.cust_oh_empty_body)) }
            return@BoondhPage
        }
        val (active, earlier) = state.orders.partition { it.status in ACTIVE }
        if (active.isNotEmpty()) {
            // Board Orders: the label lives inside the navy card, not above it.
            items(active, key = { it.id }) { o -> ActiveCard(o, vm.codeFor(o.id)) { onOpen(o.id) } }
        }
        if (earlier.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.cust_oh_earlier)) }
            items(earlier, key = { it.id }) { o ->
                // Board Orders: hairline rows; filled ink "Reorder", outlined pills beside it.
                Column(Modifier.fillMaxWidth().clickable { onOpen(o.id) }.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(o.vendorName ?: stringResource(R.string.cust_oh_order_no, o.orderNumber), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                color = if (o.status in ENDED_UNDELIVERED) NeerlyColors.Ink600 else NeerlyColors.Ink900)
                            Text(listOf(shortDay(o.placedAt), itemsLine(o)).filter { it.isNotBlank() }.joinToString(" · "),
                                fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600)
                        }
                        // A cancelled or failed order wasn't paid for (or was refunded): strike the amount.
                        Text(o.totalAmount.rupees(), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            color = if (o.status in ENDED_UNDELIVERED) NeerlyColors.Ink500 else NeerlyColors.Ink900,
                            textDecoration = if (o.status in ENDED_UNDELIVERED) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                    }
                    // Not delivered: say so with a status pill and what happened to the money,
                    // instead of a word tacked on the end of the line.
                    if (o.status in ENDED_UNDELIVERED) Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.neerly.mobile.core.design.Pill(endLine(o),
                            if (o.status == "CANCELLED") com.neerly.mobile.core.design.Tone.Bad else com.neerly.mobile.core.design.Tone.Warn)
                        Text(if (o.paymentMethod == "COD") stringResource(R.string.cust_oh_nothing_charged) else stringResource(R.string.cust_oh_refund_line),
                            fontSize = 13.sp, color = NeerlyColors.Ink600)
                    }
                    if (o.status == "DELIVERED") Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pillbutton(stringResource(R.string.cust_oh_reorder), filled = true) { if (vm.reorder(o)) onCart() }
                        Pillbutton(stringResource(R.string.cust_oh_rate), filled = false) { onRate(o.id, o.vendorName ?: o.orderNumber) }
                        Pillbutton(stringResource(R.string.cust_oh_invoice), filled = false) { onInvoice(o.id) }
                    }
                }
                HorizontalDivider(color = NeerlyColors.Ink200)
            }
        }
    }
}

@Composable
private fun Pillbutton(text: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(NeerlyRadius.pill)
    if (filled) Button(onClick = onClick, shape = shape, modifier = Modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.Ink900, contentColor = NeerlyColors.Paper)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
    else OutlinedButton(onClick = onClick, shape = shape, modifier = Modifier.heightIn(min = 48.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ink300)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900, maxLines = 1) }
}

/** Board Orders "on the way" card: status + order no., serif line, items, then code and Track ›. */
@Composable
private fun ActiveCard(o: OrderResponse, code: String?, onTrack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(NeerlyRadius.lg)).clickable(onClick = onTrack)
        .padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The label follows the order: "On the way" only once a driver has it.
            Text((if (o.status in setOf("DISPATCHED", "ARRIVING")) stringResource(R.string.cust_oh_on_way)
                else if (o.status in setOf("VENDOR_ACCEPTED", "PREPARING")) stringResource(R.string.cust_home_being_packed)
                else stringResource(R.string.cust_home_finding_shop)).uppercase(), Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp, color = Boondh.Amber)
            Text(o.orderNumber, fontSize = 13.sp, color = Boondh.MutedOnNavy)
        }
        Text(when (o.status) {
            "DISPATCHED", "ARRIVING" -> o.driverName?.let { stringResource(R.string.cust_tr_driver_on_way, it.substringBefore(' ')) } ?: stringResource(R.string.cust_home_on_the_way)
            "VENDOR_ACCEPTED", "PREPARING" -> stringResource(R.string.cust_oh_packing)
            else -> stringResource(R.string.cust_home_finding_shop)
        }, fontFamily = FontFamily.Serif, fontSize = 24.sp, lineHeight = 28.sp, color = NeerlyColors.Paper)
        Text(listOf(itemsLine(o), o.totalAmount.rupees()).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 14.sp, color = Boondh.MutedOnNavy)
        HorizontalDivider(color = Boondh.MutedOnNavy.copy(alpha = 0.25f), modifier = Modifier.padding(vertical = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 32.dp)) {
            Box(Modifier.weight(1f)) {
                code?.let { Text(stringResource(R.string.cust_oh_code, it.toCharArray().joinToString(" ")), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Paper) }
            }
            Text(stringResource(R.string.cust_oh_track) + " ›", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Paper)
        }
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
