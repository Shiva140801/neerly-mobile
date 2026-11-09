package com.neerly.mobile.feature.vendor.orders

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OfferStage { Offer, Declining, Accepted, Declined, TimedOut, Withdrawn }

/** Board V19 reasons — what the shop picks is what the server gets as the reject reason. */
enum class DeclineReason(@androidx.annotation.StringRes val label: Int, /** What the server records — kept in English. */ val serverText: String) {
    Busy(R.string.vend_decline_busy, "All my drivers are out"), Stock(R.string.vend_decline_stock, "Out of one of the items"),
    Far(R.string.vend_decline_far, "Too far for us today"), Closing(R.string.vend_decline_closing, "Closing soon"),
    Other(R.string.vend_decline_other, "Something else")
}

data class NewOrderUiState(
    val order: VendorOrderView? = null,
    val stage: OfferStage = OfferStage.Offer,
    val reason: DeclineReason? = null,
    val otherText: String = "",
    val pausedUntil: String? = null,
    val working: Boolean = false,
    val error: String? = null,
    /** Offers this shop let run out in the last 7 days (board V20), once known. */
    val missedThisWeek: Long? = null
) {
    val canDecline get() = reason != null && (reason != DeclineReason.Other || otherText.isNotBlank()) && !working
}

@HiltViewModel
class VendorNewOrderViewModel @Inject constructor(
    private val ops: VendorOpsApi,
    private val repo: VendorRepository,
    val alertPrefs: VendorAlertPrefsStore,
    saved: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val orderId: String = saved["orderId"] ?: error("orderId required")
    private val _state = MutableStateFlow(NewOrderUiState())
    val state = _state.asStateFlow()

    init {
        // "Can't take" on the ring screen or the notification opens the reason sheet directly.
        if (com.neerly.mobile.push.PendingDecline.take(orderId)) _state.value = _state.value.copy(stage = OfferStage.Declining)
        load()
        viewModelScope.launch {
            runCatching { ops.dashboard().missedOffersLast7d }.onSuccess { _state.value = _state.value.copy(missedThisWeek = it) }
        }
    }

    /** While the alert rings, re-check every few seconds so a cancelled order stops it at once. */
    suspend fun watchWhileRinging() {
        while (_state.value.stage == OfferStage.Offer || _state.value.stage == OfferStage.Declining) {
            kotlinx.coroutines.delay(RECHECK_MS)
            load().join()
        }
    }

    fun load() = viewModelScope.launch {
        runCatching { ops.order(orderId) }
            .onSuccess { o ->
                val st = _state.value.stage
                val open = st == OfferStage.Offer || st == OfferStage.Declining || st == OfferStage.TimedOut
                _state.value = _state.value.copy(order = o,
                    stage = when {
                        // The customer cancelled while it was ringing — not a miss.
                        o.status == "CANCELLED" && open -> OfferStage.Withdrawn
                        o.status != "VENDOR_ASSIGNED" && st == OfferStage.Offer -> OfferStage.TimedOut
                        else -> st
                    })
            }
            // A 404 here means the offer moved on to another shop.
            .onFailure { _state.value = _state.value.copy(stage = OfferStage.TimedOut) }
    }

    /** Clock hit zero: check with the server before saying it's gone. */
    fun expired() {
        if (_state.value.stage == OfferStage.Offer || _state.value.stage == OfferStage.Declining) {
            _state.value = _state.value.copy(stage = OfferStage.TimedOut)
            load()
        }
    }

    fun accept() {
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { repo.accept(orderId) }
                .onSuccess { _state.value = _state.value.copy(working = false, stage = OfferStage.Accepted) }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage(context = "accept order", fallback = strings.get(R.string.vend_err_accept))); load() }
        }
    }

    fun startDecline() { _state.value = _state.value.copy(stage = OfferStage.Declining) }
    fun keep() { _state.value = _state.value.copy(stage = OfferStage.Offer) }
    fun pick(r: DeclineReason) { _state.value = _state.value.copy(reason = r) }
    fun other(t: String) { _state.value = _state.value.copy(otherText = t.take(200)) }

    fun decline() {
        val s = _state.value
        val r = s.reason ?: return
        val text = if (r == DeclineReason.Other) s.otherText.trim() else r.serverText
        _state.value = s.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { repo.reject(orderId, text) }
                .onSuccess { _state.value = _state.value.copy(working = false, stage = OfferStage.Declined) }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage(context = "decline order", fallback = strings.get(R.string.vend_err_decline))) }
        }
    }

    /** "Pause 1 hour" on V19 — stops new offers; this one can still be taken. */
    fun pauseHour() {
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { repo.emergencyClose(1, "Busy — paused from a new order") }
                .onSuccess { _state.value = _state.value.copy(working = false, pausedUntil = it.pausedUntil ?: "") }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage(context = "pause orders", fallback = strings.get(R.string.vend_err_pause))) }
        }
    }

    companion object {
        const val RECHECK_MS = 5_000L
    }
}

/** Boards V10 (new order, rings for 3 minutes), V19 (decline) and V20 (time ran out). */
@Composable
fun VendorNewOrderScreen(
    onDone: () -> Unit,
    onAccepted: (String) -> Unit,
    onAlertSettings: () -> Unit = {},
    onCatalog: () -> Unit = {},
    onCloseForHours: () -> Unit = {},
    vm: VendorNewOrderViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val player = remember { AlertPlayer(context.applicationContext) }
    val ringing = s.stage == OfferStage.Offer && s.order?.status == "VENDOR_ASSIGNED"
    DisposableEffect(ringing) {
        if (ringing) player.start(vm.alertPrefs.load())
        onDispose { player.stop() }
    }
    LaunchedEffect(s.stage) { if (s.stage == OfferStage.Accepted) s.order?.let { onAccepted(it.id) } }
    val left by rememberSecondsLeft(s.order?.acceptDeadlineAt)
    LaunchedEffect(left) { if (left == 0L) vm.expired() }
    LaunchedEffect(Unit) { vm.watchWhileRinging() }

    val o = s.order
    when (s.stage) {
        OfferStage.TimedOut -> TimedOut(o, s.missedThisWeek, onAlertSettings, onDone)
        OfferStage.Withdrawn -> BoondhPage(stringResource(R.string.vend_withdrawn_title),
            subtitle = o?.let { stringResource(R.string.vend_withdrawn_sub, it.customerFirstName, it.orderNumber) },
            footer = { PrimaryButton(stringResource(R.string.vend_back_to_today), onDone) }) {}
        OfferStage.Declined -> BoondhPage(stringResource(R.string.vend_declined_title), subtitle = stringResource(R.string.vend_declined_sub),
            footer = { PrimaryButton(stringResource(R.string.vend_back_to_today), onDone) }) {}
        OfferStage.Declining -> Decline(s, vm, onCatalog, onCloseForHours)
        else -> BoondhPage(
            title = left?.let { if (it > 0) mmss(it) else "0:00" } ?: stringResource(R.string.vend_new_order),
            subtitle = o?.let { stringResource(R.string.vend_offer_sub, it.orderNumber) },
            footer = {
                s.error?.let { Notice(it, Tone.Bad) }
                PrimaryButton(if (s.working) stringResource(R.string.vend_accepting) else stringResource(R.string.vend_accept_earn, o?.yourEarning?.rupees() ?: ""), vm::accept,
                    enabled = o != null && !s.working && (left ?: 1) > 0, color = NeerlyColors.Ok)
                SecondaryButton(stringResource(R.string.vend_cant_take), vm::startDecline, enabled = !s.working, color = NeerlyColors.Err)
            }
        ) {
            if (o == null) { item { LoadState(true, null, {}) }; return@BoondhPage }
            item {
                Text(stringResource(R.string.vend_new_order_caps), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                    color = NeerlyColors.Warn, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            }
            item {
                BoondhCard {
                    Text(o.customerFirstName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    val km = o.distanceKm?.let { stringResource(R.string.vend_km, it.stripTrailingZeros().toPlainString()) }
                    Text(listOfNotNull(o.deliveryAddress.ifBlank { null }, km)
                        .joinToString(" · "), fontSize = 14.sp, color = NeerlyColors.Ink600)
                }
            }
            item { BillCard(o) }
            o.notes?.let { n -> item { Notice(stringResource(R.string.vend_customer_note, n), Tone.Info) } }
        }
    }
}

/** The shop's sale (deposit excluded), the delivery part of it, and Boondh's fee + GST. */
internal fun vendorBill(o: VendorOrderView): Triple<java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal> {
    val items = o.items.fold(java.math.BigDecimal.ZERO) { acc, it -> acc + it.unitPrice * it.quantity.toBigDecimal() }
    val sale = o.orderValue - o.depositTotal
    return Triple(sale, sale - items, sale - o.yourEarning)
}

@Composable
internal fun BillCard(o: VendorOrderView, earning: VendorOrderEarning? = null) {
    BoondhCard {
        o.items.forEach { InfoRow(stringResource(R.string.vend_bill_line, it.quantity, it.productName), null, (it.unitPrice * it.quantity.toBigDecimal()).rupees()) }
        // The jar deposit is in the customer's total but is Boondh's to hold, not the shop's
        // sale — leaving it in made the fee line read "−₹623" on a ₹195 order.
        val (sale, delivery, boondhCut) = vendorBill(o)
        if (delivery.signum() > 0) InfoRow(stringResource(R.string.vend_bill_delivery), trailing = delivery.rupees())
        HorizontalDivider(color = NeerlyColors.Ink200)
        InfoRow(stringResource(R.string.vend_bill_sale), trailing = sale.rupees())
        if (earning != null) {
            InfoRow(stringResource(R.string.vend_bill_fee_rate, earning.commissionRate.stripTrailingZeros().toPlainString()), trailing = stringResource(R.string.vend_minus, earning.commissionAmount.rupees()))
            val gst = earning.grossAmount - earning.commissionAmount - earning.netPayable
            if (gst.signum() > 0) InfoRow(stringResource(R.string.vend_bill_gst), trailing = stringResource(R.string.vend_minus, gst.rupees()))
        } else {
            if (boondhCut.signum() > 0 && o.status != "CANCELLED") InfoRow(stringResource(R.string.vend_bill_fee_gst), trailing = stringResource(R.string.vend_minus, boondhCut.rupees()))
        }
        InfoRow(stringResource(if (o.status == "CANCELLED") R.string.vend_bill_paid else R.string.vend_bill_earn), trailing = o.yourEarning.rupees(), trailingColor = NeerlyColors.Ok)
        val jars = listOfNotNull(
            o.emptiesExpected?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.vend_collect_empties, it, it) },
            stringResource(if (o.paidOnline) R.string.vend_paid_online else R.string.vend_cash_door),
            o.depositTotal.takeIf { it.signum() > 0 }?.let { stringResource(R.string.vend_deposit_held, it.rupees()) })
        Text(jars.joinToString(" · "), fontSize = 13.sp, color = NeerlyColors.Ink600)
    }
}

@Composable
private fun TimedOut(o: VendorOrderView?, missedThisWeek: Long?, onAlertSettings: () -> Unit, onDone: () -> Unit) {
    BoondhPage(
        title = stringResource(R.string.vend_timeout_title),
        subtitle = o?.let { stringResource(R.string.vend_timeout_sub_order, it.customerFirstName, it.orderNumber, it.orderValue.rupees()) }
            ?: stringResource(R.string.vend_timeout_sub),
        footer = { PrimaryButton(stringResource(R.string.vend_back_to_today), onDone) }
    ) {
        missedThisWeek?.takeIf { it > 0 }?.let { n -> item {
            InfoRow(stringResource(R.string.vend_missed_week), trailing = pluralStringResource(R.plurals.vend_n_orders, n.toInt(), n.toInt()))
        } }
        item { Notice(stringResource(R.string.vend_timeout_tip), Tone.Warn) }
        item { SecondaryButton(stringResource(R.string.vend_alert_settings), onAlertSettings) }
    }
}

@Composable
private fun Decline(s: NewOrderUiState, vm: VendorNewOrderViewModel, onCatalog: () -> Unit, onCloseForHours: () -> Unit) {
    val o = s.order
    BoondhPage(
        title = stringResource(R.string.vend_decline_title),
        subtitle = stringResource(R.string.vend_decline_sub, o?.customerFirstName ?: stringResource(R.string.vend_the_customer)),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            PrimaryButton(stringResource(if (s.working) R.string.vend_declining else if (s.reason == null) R.string.vend_pick_reason else R.string.vend_decline_order), vm::decline,
                enabled = s.canDecline, color = NeerlyColors.Err)
            SecondaryButton(stringResource(R.string.vend_keep_order), vm::keep, enabled = !s.working)
        }
    ) {
        o?.let { item {
            val parts = listOfNotNull(it.orderNumber,
                it.jarsGoingOut.takeIf { j -> j > 0 }?.let { j -> pluralStringResource(R.plurals.vend_n_jars, j, j) },
                it.distanceKm?.let { d -> stringResource(R.string.vend_km_away, d.stripTrailingZeros().toPlainString()) })
            Text(stringResource(R.string.vend_decline_head, parts.joinToString(" · ")),
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink600) } }
        DeclineReason.entries.forEach { r -> item(key = r.name) {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(selected = s.reason == r, role = Role.RadioButton, onClick = { vm.pick(r) }),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = s.reason == r, onClick = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(r.label), fontSize = 16.sp, fontWeight = if (s.reason == r) FontWeight.Bold else FontWeight.Medium, color = NeerlyColors.Ink900)
            }
        } }
        when (s.reason) {
            DeclineReason.Busy -> item {
                BoondhCard {
                    val until = s.pausedUntil?.takeIf { it.isNotBlank() }?.let { runCatching { istClock(it) }.getOrDefault(it) }
                    Text(when {
                        s.pausedUntil == null -> stringResource(R.string.vend_breather)
                        until != null -> stringResource(R.string.vend_paused_until, until)
                        else -> stringResource(R.string.vend_paused)
                    }, fontSize = 14.sp, color = NeerlyColors.Ink700)
                    if (s.pausedUntil == null) SecondaryButton(stringResource(R.string.vend_pause_hour), vm::pauseHour, enabled = !s.working)
                }
            }
            DeclineReason.Stock -> item { BoondhCard(onClick = onCatalog) { InfoRow(stringResource(R.string.vend_mark_sold_out), stringResource(R.string.vend_mark_sold_out_sub)) } }
            DeclineReason.Closing -> item { BoondhCard(onClick = onCloseForHours) { InfoRow(stringResource(R.string.vend_closing_early), stringResource(R.string.vend_closing_early_sub)) } }
            DeclineReason.Other -> item {
                OutlinedTextField(s.otherText, vm::other, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.vend_tell_us)) }, minLines = 2)
            }
            else -> Unit
        }
    }
}
