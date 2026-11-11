package com.neerly.mobile.feature.order

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.order.OrderLocalStore
import com.neerly.mobile.core.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderPlacedViewModel @Inject constructor(
    private val api: NeerlyApi,
    private val codes: OrderLocalStore,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    val order = MutableStateFlow<OrderResponse?>(null)
    /** Board OrderPlacedCash: cancelling, done, or why it couldn't. */
    val cancelling = MutableStateFlow(false)
    val cancelled = MutableStateFlow(false)
    val cancelError = MutableStateFlow<String?>(null)
    /** The shop's arrival estimate for this order's address ("20–30 min"); null until known. */
    val eta = MutableStateFlow<Pair<Int, Int>?>(null)
    fun load(id: String) {
        if (order.value != null) return
        viewModelScope.launch {
            val o = runCatching { api.order(id) }.getOrNull() ?: return@launch
            order.value = o
            val vendorId = o.vendorId ?: return@launch
            if (o.requestedSlotStart != null) return@launch   // a booked slot shows its window instead
            val a = runCatching { api.addresses() }.getOrNull()?.firstOrNull { it.id == o.addressId }
            runCatching { api.vendor(vendorId, a?.latitude, a?.longitude) }.getOrNull()?.let { v ->
                v.etaMinutesMin?.let { lo -> eta.value = lo to (v.etaMinutesMax ?: lo) }
            }
        }
    }

    /**
     * Routing can fail moments after the order is placed (no shop serves it, or each one
     * is excluded). Re-read for a short while so the page never says "on its way" for an
     * order that already FAILED.
     */
    fun watchBriefly(id: String) = viewModelScope.launch {
        repeat(6) {
            kotlinx.coroutines.delay(3_000)
            val o = runCatching { api.order(id) }.getOrNull() ?: return@repeat
            order.value = o
            if (o.status != "PLACED" && o.status != "VENDOR_ASSIGNED") return@launch
        }
    }
    fun code(id: String) = codes.deliveryCode(id)

    /** Free for a cash order until the driver leaves the shop — nothing was paid, nothing to refund. */
    fun cancelCash(id: String) {
        if (cancelling.value) return
        cancelling.value = true; cancelError.value = null
        viewModelScope.launch {
            runCatching { api.cancelOrder(id, com.neerly.mobile.data.dto.CancelOrderRequest("CHANGED_MIND")) }
                .onSuccess { order.value = it; cancelled.value = true; codes.forgetDeliveryCode(id) }
                .onFailure { cancelError.value = it.userMessage(context = "cancel order",
                    fallback = strings.get(com.neerly.mobile.R.string.cust_placed_cancel_failed)) }
            cancelling.value = false
        }
    }

    companion object {
        /** Before the driver leaves the shop. */
        val CASH_CANCEL_FREE = setOf("PLACED", "VENDOR_ASSIGNED", "VENDOR_ACCEPTED", "PREPARING")
    }
}

/** Boards 14 (paid) and 69 (cash) — done, what happens next, what to keep ready. */
@Composable
fun OrderPlacedScreen(orderId: String, orderNumber: String, onTrack: () -> Unit, onHome: () -> Unit,
                      onOtherShops: () -> Unit = onTrack,
                      vm: OrderPlacedViewModel = hiltViewModel()) {
    LaunchedEffect(orderId) { vm.load(orderId); vm.watchBriefly(orderId) }
    // The moment the customer wants updates: right after placing an order (plan §e), not at install.
    com.neerly.mobile.push.AskNotificationsOnce("customer", true)
    val o by vm.order.collectAsState()
    val eta by vm.eta.collectAsState()
    val cancelling by vm.cancelling.collectAsState()
    val cancelled by vm.cancelled.collectAsState()
    val cancelError by vm.cancelError.collectAsState()
    var confirmCancel by remember { mutableStateOf(false) }
    val cash = o?.paymentMethod == "COD"
    val canCancelFree = cash && o?.status in OrderPlacedViewModel.CASH_CANCEL_FREE
    if (cancelled) {
        BoondhPage(title = stringResource(R.string.cust_placed_cancelled_title, orderNumber),
            footer = { PrimaryButton(stringResource(R.string.cust_placed_home), onHome) }) {
            item { Notice(stringResource(R.string.cust_placed_cancelled_cash), Tone.Ok) }
        }
        return
    }
    if (confirmCancel) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.cust_placed_cancel_q, orderNumber)) },
            text = { Text(stringResource(R.string.cust_placed_cancel_free_body)) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmCancel = false; vm.cancelCash(orderId) }) {
                Text(stringResource(R.string.cust_placed_cancel_yes), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmCancel = false }) {
                Text(stringResource(R.string.cust_placed_cancel_keep)) } }
        )
    }
    if (o?.status == "FAILED") {
        // Board NoVendor's entry: nobody could take it — say so and offer the other shops.
        BoondhPage(title = stringResource(R.string.cust_placed_failed_title),
            subtitle = stringResource(R.string.cust_placed_failed_sub, orderNumber),
            footer = {
                PrimaryButton(stringResource(R.string.cust_placed_failed_other), onOtherShops)
                SecondaryButton(stringResource(R.string.cust_placed_home), onHome)
            }) {
            if (o?.paymentMethod != "COD") item { Notice(stringResource(R.string.cust_placed_failed_refund), Tone.Info) }
        }
        return
    }
    val swaps = o?.items?.filter { it.containerMode == "TRANSFER_AND_RETURN" }?.sumOf { it.quantity } ?: 0
    // Board OrderPlaced: a quiet confirmation — drop with a tick, the three facts that
    // matter (when, which order, what was paid), the jar swap, then Track / Back to home.
    // The delivery code lives on the tracking screen.
    Scaffold(containerColor = NeerlyColors.Canvas, bottomBar = {
        Column(Modifier.fillMaxWidth().background(NeerlyColors.Canvas).navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            cancelError?.let { Notice(it, Tone.Bad) }
            PrimaryButton(stringResource(R.string.cust_placed_track), onTrack)
            TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cust_placed_home), color = NeerlyColors.CustomerPrimary,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            if (canCancelFree) TextButton(onClick = { confirmCancel = true }, enabled = !cancelling,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (cancelling) stringResource(R.string.cust_placed_cancelling) else stringResource(R.string.cust_placed_cancel),
                    color = NeerlyColors.Err, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(24.dp))
            DropCheck()
            PageTitle(stringResource(R.string.cust_placed_title), size = 34)
            Text(stringResource(R.string.cust_placed_subtitle, o?.vendorName ?: stringResource(R.string.cust_the_shop)),
                fontSize = 16.sp, lineHeight = 23.sp, color = NeerlyColors.Ink600)
            Column(Modifier.fillMaxWidth().background(NeerlyColors.Paper, RoundedCornerShape(14.dp))
                .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 4.dp)) {
                FactRow(stringResource(R.string.cust_placed_arriving), arrivingText(o, eta))
                DashedRule()
                FactRow(stringResource(R.string.cust_order), orderNumber)
                o?.let {
                    DashedRule()
                    FactRow(if (cash) stringResource(R.string.cust_pay_at_door) else stringResource(R.string.cust_paid),
                        if (cash) stringResource(R.string.cust_placed_amount_cash, it.totalAmount.rupees())
                        else stringResource(R.string.cust_placed_amount_online, it.totalAmount.rupees()))
                }
            }
            if (swaps > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🫙", fontSize = 22.sp, modifier = Modifier.clearAndSetSemantics { })
                Text(androidx.compose.ui.res.pluralStringResource(R.plurals.cust_placed_keep_empties, swaps, swaps),
                    fontSize = 15.sp, lineHeight = 21.sp, color = NeerlyColors.Ink900)
            }
            if (cash) Text(stringResource(R.string.cust_placed_keep_cash, o!!.totalAmount.rupees()),
                fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600)
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink600)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
    }
}

@Composable
private fun DashedRule() {
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(NeerlyColors.Ink200, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
    }
}

/** The Boondh drop in customer indigo with a white tick — the "done" mark on OrderPlaced. */
@Composable
private fun DropCheck() {
    androidx.compose.foundation.Canvas(Modifier.size(width = 64.dp, height = 84.dp).clearAndSetSemantics { }) {
        val w = size.width; val h = size.height
        val drop = Path().apply {
            moveTo(w / 2f, 0f)
            cubicTo(w * 0.78f, h * 0.30f, w, h * 0.50f, w, h * 0.66f)
            cubicTo(w, h * 0.86f, w * 0.78f, h, w / 2f, h)
            cubicTo(w * 0.22f, h, 0f, h * 0.86f, 0f, h * 0.66f)
            cubicTo(0f, h * 0.50f, w * 0.22f, h * 0.30f, w / 2f, 0f)
            close()
        }
        drawPath(drop, NeerlyColors.CustomerPrimary)
        val tick = Path().apply { moveTo(w * 0.30f, h * 0.66f); lineTo(w * 0.45f, h * 0.79f); lineTo(w * 0.72f, h * 0.52f) }
        drawPath(tick, Color.White, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** "Tomorrow, 6–9 AM" for a booked slot; otherwise the shop's estimate, or the admin-default one. */
@Composable
private fun arrivingText(o: OrderResponse?, eta: Pair<Int, Int>?): String {
    val start = istTime(o?.requestedSlotStart); val end = istTime(o?.requestedSlotEnd)
    if (start != null && end != null) {
        val window = timeWindow(start.toLocalTime().toString(), end.toLocalTime().toString())
        val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))
        return when (start.toLocalDate()) {
            today -> stringResource(R.string.cust_placed_slot_today, window)
            today.plusDays(1) -> stringResource(R.string.cust_placed_slot_tomorrow, window)
            else -> stringResource(R.string.cust_placed_slot_day, shortDay(o?.requestedSlotStart), window)
        }
    }
    val (lo, hi) = eta ?: com.neerly.mobile.data.config.LocalAppConfig.current.fallbackEta
    return stringResource(R.string.cust_cart_now_eta, lo, hi)
}
