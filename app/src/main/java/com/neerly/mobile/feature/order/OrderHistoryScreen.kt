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
import androidx.compose.material.icons.outlined.KeyboardArrowRight
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
    onRefunds: () -> Unit = {},
    onTryAgain: (String) -> Unit = {},
    vm: OrderHistoryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    // Re-read on every return (OnReturn now survives tab switches), when a push says an
    // order moved, and on a timer while one is open — the shop and driver change its status
    // from their phones.
    com.neerly.mobile.core.design.OnReturn(vm::refresh)
    com.neerly.mobile.core.util.OnLiveChange(action = vm::refresh)
    com.neerly.mobile.core.util.LivePoll(state.orders.any { it.status in ACTIVE },
        com.neerly.mobile.data.config.LocalAppConfig.current.liveRefreshMs) { vm.refresh() }
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
        // Board OrdersStates: history grouped by day ("TODAY · TUE 10 NOV"); each row is the
        // shop, "items · #0050", a coloured state word + what happened to the money, the
        // amount (struck when nothing was delivered) and one text action on the right.
        // One list item per day: the page's 14 dp item gap then separates days only, and rows
        // inside a day sit as tight as the board.
        earlier.groupBy { dayKey(it.placedAt) }.forEach { (_, day) ->
            item(key = "day-" + day.first().id) {
                Column {
                    SectionLabel(dayHeader(day.first().placedAt), Modifier.padding(bottom = 2.dp))
                    day.forEach { o -> HistoryRow(o, onOpen, onRate, onInvoice, onRefunds, onTryAgain) { if (vm.reorder(o)) onCart() } }
                }
            }
        }
    }
}

@Composable
private fun Pillbutton(text: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(NeerlyRadius.pill)
    // Board Orders: compact pills — 48 dp tall for touch, but only ~16 dp of side padding.
    val pad = PaddingValues(horizontal = 18.dp)
    if (filled) Button(onClick = onClick, shape = shape, modifier = Modifier.heightIn(min = 48.dp), contentPadding = pad,
        colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.Ink900, contentColor = NeerlyColors.Paper)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
    else OutlinedButton(onClick = onClick, shape = shape, modifier = Modifier.heightIn(min = 48.dp), contentPadding = pad,
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
                code?.let { c ->
                    val label = stringResource(R.string.cust_oh_code, "\u0000")
                    Text(androidx.compose.ui.text.buildAnnotatedString {
                        append(label.substringBefore("\u0000"))
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)); append(c.toCharArray().joinToString(" ")); pop()
                        append(label.substringAfter("\u0000"))
                    }, fontSize = 16.sp, color = NeerlyColors.Paper)
                }
            }
            Text(stringResource(R.string.cust_oh_track), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Paper)
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.KeyboardArrowRight, contentDescription = null,
                tint = NeerlyColors.Paper, modifier = Modifier.size(22.dp))
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

@Composable
private fun HistoryRow(
    o: OrderResponse, onOpen: (String) -> Unit, onRate: (String, String) -> Unit, onInvoice: (String) -> Unit,
    onRefunds: () -> Unit, onTryAgain: (String) -> Unit, onReorder: () -> Unit
) {
    val undelivered = o.status in ENDED_UNDELIVERED
    val refund = o.moneyState in setOf("REFUND_PENDING", "REFUNDED", "REFUND_FAILED") ||
        (o.moneyState == null && undelivered && o.paymentMethod != "COD")
    Column(Modifier.fillMaxWidth().clickable { onOpen(o.id) }.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(o.vendorName ?: stringResource(R.string.cust_oh_order_no, o.orderNumber), Modifier.weight(1f),
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text(o.totalAmount.rupees(), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = if (undelivered) NeerlyColors.Ink500 else NeerlyColors.Ink900,
                textDecoration = if (undelivered) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
        }
        Text(listOf(itemsLine(o), "#" + o.orderNumber.substringAfterLast('-')).filter { it.isNotBlank() }.joinToString(" · "),
            fontSize = 14.sp, color = NeerlyColors.Ink600)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (word, wordColor) = when (o.status) {
                "CANCELLED" -> stringResource(R.string.cust_oh_cancelled) to NeerlyColors.Err
                "FAILED" -> stringResource(R.string.cust_oh_not_delivered) to NeerlyColors.Warn
                else -> stringResource(R.string.cust_oh_delivered) to NeerlyColors.Ok
            }
            Text(androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = wordColor, fontWeight = FontWeight.Bold)); append(word); pop()
                if (undelivered) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = if (refund) NeerlyColors.Ok else NeerlyColors.Ink600))
                    append(" · "); append(orderMoneyLine(o).replaceFirstChar { it.lowercase() }); pop()
                }
            }, Modifier.weight(1f), fontSize = 15.sp)
            val action: Pair<String, () -> Unit>? = when {
                !undelivered -> null
                refund -> stringResource(R.string.cust_oh_see_refund) to onRefunds
                o.status == "FAILED" -> stringResource(R.string.cust_oh_try_again) to { onTryAgain(o.id) }
                else -> stringResource(R.string.cust_oh_reorder) to onReorder
            }
            // Board OrdersStates: the action is plain text sitting on the state line, so the row
            // stays as tight as the board. Its tap area is the text plus 6 dp — below the 48 dp
            // guideline (logged as a design issue); the whole row still opens the order.
            action?.let { (label, go) ->
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary,
                    modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = go)
                        .padding(horizontal = 6.dp, vertical = 6.dp))
            }
        }
        if (o.status == "DELIVERED") Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
            Pillbutton(stringResource(R.string.cust_oh_reorder), filled = true, onClick = onReorder)
            Pillbutton(stringResource(R.string.cust_oh_rate), filled = false) { onRate(o.id, o.vendorName ?: o.orderNumber) }
            Pillbutton(stringResource(R.string.cust_oh_invoice), filled = false) { onInvoice(o.id) }
        }
    }
    HorizontalDivider(color = NeerlyColors.Ink200)
}

private fun dayKey(iso: String?): String = iso?.take(10).orEmpty()

/** "Today · Tue 10 Nov", "Yesterday · Mon 9 Nov", else "Sun 8 Nov" — uppercased by SectionLabel. */
@Composable
private fun dayHeader(iso: String?): String {
    val zone = java.time.ZoneId.of("Asia/Kolkata")
    val day = runCatching { java.time.OffsetDateTime.parse(iso).atZoneSameInstant(zone).toLocalDate() }
        .recoverCatching { java.time.Instant.parse(iso).atZone(zone).toLocalDate() }.getOrNull()
    val today = java.time.LocalDate.now(zone)
    return when (day) {
        today -> stringResource(R.string.cust_oh_today, shortDay(iso))
        today.minusDays(1) -> stringResource(R.string.cust_oh_yesterday, shortDay(iso))
        else -> shortDay(iso)
    }
}
