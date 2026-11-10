package com.neerly.mobile.feature.order.redeliver

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.R
import com.neerly.mobile.core.design.BoondhCard
import com.neerly.mobile.core.design.BoondhPage
import com.neerly.mobile.core.design.InfoRow
import com.neerly.mobile.core.design.LoadState
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.Notice
import com.neerly.mobile.core.design.Pill
import com.neerly.mobile.core.design.PrimaryButton
import com.neerly.mobile.core.design.SecondaryButton
import com.neerly.mobile.core.design.Tone
import com.neerly.mobile.core.design.rupees
import com.neerly.mobile.core.design.timeWindow
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.WalletTopupRequest
import com.neerly.mobile.feature.address.AddressMapApi
import com.neerly.mobile.feature.checkout.GatewayPayments
import com.neerly.mobile.feature.checkout.PaymentOutcome
import com.neerly.mobile.feature.checkout.findActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Which board: SlotPickerRedeliver ("time") or AddressPickerRedeliver ("place"). */
enum class RedeliverMode { Time, Place }

data class AddressChoice(val address: AddressResponse, val served: Boolean)

data class RedeliverUi(
    val loading: Boolean = true,
    val order: OrderResponse? = null,
    val days: List<SlotDayRow> = emptyList(),
    val dayIndex: Int = 0,
    val slot: SlotOptionRow? = null,
    val addresses: List<AddressChoice> = emptyList(),
    val addressId: String? = null,
    val booking: Boolean = false,
    /** The new order, once booked. */
    val booked: OrderResponse? = null,
    /** REDELIVERY_TOP_UP_NEEDED: add this much to the wallet first. */
    val topUp: BigDecimal? = null,
    val error: String? = null
) {
    val fee: BigDecimal? get() = order?.failedDelivery?.newTripFee
    val canBook: Boolean get() = !booking && order != null && (slot != null || addressId != null)
}

@HiltViewModel
class RedeliverViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val api: NeerlyApi,
    private val redelivery: RedeliveryApi,
    private val maps: AddressMapApi,
    private val gateway: GatewayPayments,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val orderId: String = saved["orderId"] ?: error("orderId required")
    val mode: RedeliverMode = if (saved.get<String>("mode") == "place") RedeliverMode.Place else RedeliverMode.Time
    private val _state = MutableStateFlow(RedeliverUi())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val order = api.order(orderId)
                if (mode == RedeliverMode.Time) {
                    val days = order.vendorId?.let { redelivery.slotDays(it, DAYS).days }.orEmpty()
                    _state.value = _state.value.copy(order = order, days = days,
                        dayIndex = days.indexOfFirst { d -> d.open && d.slots.any { it.bookable } }.coerceAtLeast(0))
                } else {
                    val all = api.addresses()
                    val served = all.map { it.pincode }.distinct().associateWith { pin ->
                        runCatching { maps.serviceability(pin).shops.any { it.id == order.vendorId } }.getOrDefault(false)
                    }
                    _state.value = _state.value.copy(order = order, addresses = all.map {
                        AddressChoice(it, served[it.pincode] == true)
                    })
                }
            }.onSuccess { _state.value = _state.value.copy(loading = false) }
             .onFailure { _state.value = _state.value.copy(loading = false,
                 error = it.userMessage(context = "re-delivery", fallback = strings.get(R.string.cust_rd_load_failed))) }
        }
    }

    fun pickDay(i: Int) { _state.value = _state.value.copy(dayIndex = i, slot = null) }
    fun pickSlot(s: SlotOptionRow) { if (s.bookable) _state.value = _state.value.copy(slot = s) }
    fun pickAddress(a: AddressChoice) { if (a.served) _state.value = _state.value.copy(addressId = a.address.id) }

    fun book() {
        val s = _state.value
        if (!s.canBook) return
        val day = s.days.getOrNull(s.dayIndex)
        val req = RedeliverRequest(
            addressId = s.addressId,
            requestedSlotStart = s.slot?.let { day?.let { d -> iso(d.date, it.start) } },
            requestedSlotEnd = s.slot?.let { day?.let { d -> iso(d.date, it.end) } }
        )
        _state.value = s.copy(booking = true, error = null, topUp = null)
        viewModelScope.launch {
            runCatching { redelivery.redeliver(orderId, req) }
                .onSuccess { _state.value = _state.value.copy(booking = false, booked = it) }
                .onFailure { e ->
                    val err = (e as? HttpException)?.let(::envelope)
                    _state.value = _state.value.copy(booking = false,
                        topUp = if (err?.code == "REDELIVERY_TOP_UP_NEEDED") err.shortfall else null,
                        error = err?.message ?: e.userMessage(context = "re-delivery", fallback = strings.get(R.string.cust_rd_book_failed)))
                }
        }
    }

    /** "Add ₹25 and book": top the wallet up through Razorpay, then book again. */
    fun topUpAndBook(activity: android.app.Activity) {
        val amount = _state.value.topUp ?: return
        _state.value = _state.value.copy(booking = true, error = null)
        viewModelScope.launch {
            val outcome = runCatching {
                gateway.pay(activity, api.walletTopup(WalletTopupRequest(amount)), customerName = "", customerContact = null,
                    description = strings.get(R.string.acct_wallet_topup_desc, amount.toPlainString()))
            }.getOrElse { PaymentOutcome.Declined(it.userMessage(context = "wallet top-up", fallback = strings.get(R.string.acct_wallet_topup_failed))) }
            _state.value = _state.value.copy(booking = false)
            when (outcome) {
                PaymentOutcome.Captured -> { _state.value = _state.value.copy(topUp = null); book() }
                is PaymentOutcome.Unconfirmed -> _state.value = _state.value.copy(error = outcome.message)
                is PaymentOutcome.Declined -> _state.value = _state.value.copy(error = outcome.message)
            }
        }
    }

    private data class Envelope(val code: String?, val message: String?, val shortfall: BigDecimal?)

    /** One read of the error body: code, the server's words and the shortfall detail. */
    private fun envelope(e: HttpException): Envelope? = runCatching {
        val body = e.response()?.errorBody()?.string() ?: return null
        fun str(k: String) = Regex(""""$k"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(body)?.groupValues?.get(1)
        Envelope(str("code"), str("message"),
            Regex(""""shortfall"\s*:\s*([0-9.]+)""").find(body)?.groupValues?.get(1)?.toBigDecimalOrNull())
    }.getOrNull()

    companion object {
        const val DAYS = 3
        private val IST: ZoneId = ZoneId.of("Asia/Kolkata")

        /** A slot on a day in Hyderabad, as the instant the server stores. */
        fun iso(date: String, time: String): String =
            LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(IST).toInstant().toString()
    }
}

/** Boards SlotPickerRedeliver and AddressPickerRedeliver. */
@Composable
fun RedeliverScreen(
    onBack: () -> Unit,
    onSeeOrder: (orderId: String, orderNumber: String) -> Unit,
    onAddAddress: () -> Unit = {},
    vm: RedeliverViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    // Back from adding an address: list it.
    if (vm.mode == RedeliverMode.Place) com.neerly.mobile.core.design.OnReturn(vm::load)
    val context = LocalContext.current
    val order = s.order
    val time = vm.mode == RedeliverMode.Time
    val fee = s.fee
    val prepaid = order?.paymentMethod != "COD"

    s.booked?.let { b ->
        val day = s.days.getOrNull(s.dayIndex)
        val whenText = s.slot?.let { sl -> listOfNotNull(day?.let { dayLabel(it.date) }, timeWindow(sl.start, sl.end)).joinToString(", ") }
        BoondhPage(
            title = if (whenText != null) stringResource(R.string.cust_rd_booked_for, whenText) else stringResource(R.string.cust_rd_booked),
            footer = { PrimaryButton(stringResource(R.string.cust_rd_see_in_orders), { onSeeOrder(b.id, b.orderNumber) }) }
        ) {
            item {
                Notice(
                    if (b.paymentMethod == "COD") stringResource(R.string.cust_rd_booked_cash, b.totalAmount.rupees(), order?.vendorName ?: stringResource(R.string.cust_the_shop))
                    else stringResource(R.string.cust_rd_booked_paid, (fee ?: b.deliveryFee).rupees(), order?.vendorName ?: stringResource(R.string.cust_the_shop)),
                    Tone.Ok)
            }
        }
        return
    }

    BoondhPage(
        title = stringResource(if (time) R.string.cust_rd_when_title else R.string.cust_rd_where_title),
        subtitle = order?.let { stringResource(R.string.cust_rd_subtitle, it.orderNumber) },
        onBack = onBack, backLabel = stringResource(R.string.cust_tr_back_orders),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            val topUp = s.topUp
            if (topUp != null) PrimaryButton(stringResource(R.string.cust_rd_add_and_book, topUp.rupees()),
                { context.findActivity()?.let(vm::topUpAndBook) }, enabled = !s.booking)
            else PrimaryButton(
                if (s.booking) stringResource(R.string.cust_rd_booking)
                else fee?.let { stringResource(R.string.cust_rd_book_fee, it.rupees()) } ?: stringResource(R.string.cust_rd_book),
                vm::book, enabled = s.canBook)
        }
    ) {
        if (s.loading || order == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        if (!time) item { Text(stringResource(R.string.cust_rd_where_body, order.vendorName ?: stringResource(R.string.cust_the_shop)),
            fontSize = 14.sp, color = NeerlyColors.Ink700) }
        if (time) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = androidx.compose.ui.Modifier.horizontalScroll(rememberScrollState())) {
                    s.days.forEachIndexed { i, d ->
                        FilterChip(selected = i == s.dayIndex, onClick = { vm.pickDay(i) }, enabled = d.open,
                            label = { Text(dayLabel(d.date) + if (!d.open) " · " + stringResource(R.string.cust_rd_closed) else "") })
                    }
                }
            }
            val day = s.days.getOrNull(s.dayIndex)
            val slots = day?.slots.orEmpty().filter { it.state != "CLOSED" }
            if (day == null || slots.none { it.bookable }) item { Notice(stringResource(R.string.cust_rd_no_slots), Tone.Warn) }
            slots.forEach { sl ->
                item {
                    BoondhCard(onClick = if (sl.bookable) ({ vm.pickSlot(sl) }) else null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(timeWindow(sl.start, sl.end), fontSize = 16.sp,
                                fontWeight = if (s.slot == sl) FontWeight.Bold else FontWeight.Medium,
                                color = if (sl.bookable) NeerlyColors.Ink900 else NeerlyColors.Ink400)
                            when {
                                s.slot == sl -> Pill(stringResource(R.string.cust_rd_picked), Tone.Info)
                                sl.state == "FULL" -> Pill(stringResource(R.string.cust_rd_full), Tone.Neutral)
                                sl.state == "PASSED" -> Pill(stringResource(R.string.cust_rd_passed), Tone.Neutral)
                                sl.state == "FILLING" -> Pill(stringResource(R.string.cust_rd_filling), Tone.Warn)
                            }
                        }
                    }
                }
            }
        } else {
            s.addresses.forEach { a ->
                item {
                    BoondhCard(onClick = if (a.served) ({ vm.pickAddress(a) }) else null) {
                        Text(a.address.label.ifBlank { a.address.buildingName }, fontSize = 16.sp,
                            fontWeight = if (s.addressId == a.address.id) FontWeight.Bold else FontWeight.Medium,
                            color = if (a.served) NeerlyColors.Ink900 else NeerlyColors.Ink400)
                        Text(listOf(a.address.flatNumber, a.address.buildingName, a.address.street).filter { it.isNotBlank() }.joinToString(", ") + " · " + a.address.pincode,
                            fontSize = 13.sp, color = NeerlyColors.Ink600)
                        when {
                            s.addressId == a.address.id -> Pill(stringResource(R.string.cust_rd_picked), Tone.Info)
                            a.address.id == order.addressId -> Pill(stringResource(R.string.cust_rd_failed_here), Tone.Neutral)
                            !a.served -> Text(stringResource(R.string.cust_rd_not_served), fontSize = 12.sp, color = NeerlyColors.Ink600)
                        }
                    }
                }
            }
            item { SecondaryButton(stringResource(R.string.cust_rd_add_address), onAddAddress) }
        }
        item {
            BoondhCard {
                fee?.let { InfoRow(stringResource(R.string.cust_rd_new_trip_fee), trailing = it.rupees()) }
                if (prepaid) InfoRow(stringResource(R.string.cust_rd_water_paid), trailing = BigDecimal.ZERO.rupees())
                else InfoRow(stringResource(R.string.cust_rd_water_cash), trailing = order.subtotal.rupees())
            }
        }
        fee?.let { item { Text(stringResource(R.string.cust_rd_every_trip, it.rupees()), fontSize = 13.sp, color = NeerlyColors.Ink600) } }
    }
}

/** "Mon 9" */
@Composable
private fun dayLabel(date: String): String = runCatching {
    DateTimeFormatter.ofPattern("EEE d", com.neerly.mobile.core.util.AppLocale.locale).format(LocalDate.parse(date))
}.getOrDefault(date)
