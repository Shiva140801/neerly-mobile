package com.neerly.mobile.feature.event

import com.neerly.mobile.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.CancelEventRequest
import com.neerly.mobile.data.dto.EventBookingResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailUiState(
    val booking: EventBookingResponse? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val withdrawing: Boolean = false
)

/** Where an event request stands, for the eyebrow/title/actions of board EventRequestSent. */
enum class EventStage { WAITING, BOOKED, DONE, SAID_NO, WITHDRAWN, EXPIRED }

fun eventStage(status: String): EventStage = when (status) {
    "PENDING_CONFIRMATION" -> EventStage.WAITING
    "VENDOR_CONFIRMED", "ADVANCE_PAID", "IN_PROGRESS", "DELIVERED" -> EventStage.BOOKED
    "PICKED_UP", "COMPLETED" -> EventStage.DONE
    "VENDOR_REJECTED" -> EventStage.SAID_NO
    "EXPIRED" -> EventStage.EXPIRED
    else -> EventStage.WITHDRAWN   // CANCELLED, FAILED
}

@HiltViewModel
class EventBookingDetailViewModel @Inject constructor(
    private val api: NeerlyApi,
    saved: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val id: String = saved["id"] ?: error("id required")
    private val _state = MutableStateFlow(EventDetailUiState())
    val state: StateFlow<EventDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.eventBooking(id) }
                .onSuccess { _state.value = EventDetailUiState(booking = it, loading = false) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("event booking", R.string.acct_evt_load_failed, strings)) }
        }
    }

    /** Nothing has been paid while a request waits, so withdrawing costs nothing (board copy). */
    fun withdraw() {
        if (_state.value.withdrawing) return
        _state.value = _state.value.copy(withdrawing = true, error = null)
        viewModelScope.launch {
            runCatching { api.cancelEventBooking(id, CancelEventRequest("Withdrawn by the customer")) }
                .onSuccess { _state.value = EventDetailUiState(booking = it, loading = false) }
                .onFailure { _state.value = _state.value.copy(withdrawing = false,
                    error = it.userMessage("withdraw event", R.string.acct_evt_withdraw_failed, strings)) }
        }
    }
}

private fun eventWhen(iso: String): String {
    val t = istTime(iso) ?: return shortDay(iso)
    return shortDay(iso) + ", " + clockTime(iso)
}

/**
 * Board EventRequestSent, for the one shop the request went to: eyebrow + headline for
 * where it stands, the request in a sentence, the shop's price once it confirms (or a
 * dashed "still pricing it…"), and Withdraw while nothing has been paid.
 */
@Composable
fun EventBookingDetailScreen(onBack: () -> Unit, onAskAnother: () -> Unit, vm: EventBookingDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var confirmWithdraw by remember { mutableStateOf(false) }
    val b = s.booking
    Scaffold(containerColor = NeerlyColors.Canvas) { padding ->
        if (b == null) {
            Column(Modifier.padding(padding).padding(20.dp)) {
                BackChevron(onBack, stringResource(R.string.core_back))
                LoadState(s.loading, s.error, vm::load)
            }
            return@Scaffold
        }
        val stage = eventStage(b.status)
        val shop = b.vendorName ?: stringResource(R.string.acct_dep_the_shop)
        val jars = b.items.sumOf { it.quantity }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { BackChevron(onBack, stringResource(R.string.core_back)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(when (stage) {
                        EventStage.WAITING -> stringResource(R.string.acct_evtd_eyebrow_sent, shop)
                        EventStage.BOOKED, EventStage.DONE -> stringResource(R.string.acct_evtd_eyebrow_booked, shop)
                        EventStage.SAID_NO -> stringResource(R.string.acct_evtd_eyebrow_no, shop)
                        EventStage.WITHDRAWN -> stringResource(R.string.acct_evtd_eyebrow_withdrawn)
                        EventStage.EXPIRED -> stringResource(R.string.acct_evtd_eyebrow_expired)
                    }.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                        color = when (stage) { EventStage.SAID_NO, EventStage.EXPIRED -> NeerlyColors.Err; EventStage.WITHDRAWN -> NeerlyColors.Ink600; else -> NeerlyColors.Ok })
                    PageTitle(when (stage) {
                        EventStage.WAITING -> b.answerBy?.let { stringResource(R.string.acct_evtd_title_answer_by, shop, eventWhen(it)) }
                            ?: stringResource(R.string.acct_evtd_title_waiting, shop)
                        EventStage.BOOKED -> stringResource(R.string.acct_evtd_title_booked, eventWhen(b.eventStart))
                        EventStage.DONE -> stringResource(R.string.acct_evtd_title_done)
                        EventStage.SAID_NO -> stringResource(R.string.acct_evtd_title_no, shop)
                        EventStage.WITHDRAWN -> stringResource(R.string.acct_evtd_title_withdrawn)
                        EventStage.EXPIRED -> stringResource(R.string.acct_evtd_title_expired)
                    }, size = 32)
                    val what = listOfNotNull(b.eventName?.takeIf { it.isNotBlank() }, eventWhen(b.eventStart),
                        pluralStringResource(R.plurals.acct_evtd_jars, jars, jars)).joinToString(" · ")
                    Text(if (stage == EventStage.WAITING) stringResource(R.string.acct_evtd_summary_unpaid, what) else what,
                        fontSize = 16.sp, lineHeight = 23.sp, color = NeerlyColors.Ink600)
                }
            }
            when (stage) {
                EventStage.WAITING -> item {
                    SectionLabel(stringResource(R.string.acct_evtd_the_price))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().drawBehind {
                        drawRoundRect(NeerlyColors.Ink300, cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
                    }.padding(18.dp)) {
                        Text(stringResource(R.string.acct_evtd_still_pricing, shop), fontSize = 16.sp, color = NeerlyColors.Ink600)
                    }
                }
                EventStage.BOOKED, EventStage.DONE -> item { PriceCard(b, shop) }
                EventStage.SAID_NO -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        b.rejectionReason?.takeIf { it.isNotBlank() }?.let { Notice(stringResource(R.string.acct_evtd_reason, it), Tone.Bad) }
                        PrimaryButton(stringResource(R.string.acct_evtd_ask_another), onAskAnother)
                    }
                }
                EventStage.EXPIRED -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.acct_evtd_expired_body, shop), fontSize = 16.sp, lineHeight = 23.sp, color = NeerlyColors.Ink700)
                        PrimaryButton(stringResource(R.string.acct_evtd_ask_another), onAskAnother)
                    }
                }
                EventStage.WITHDRAWN -> item {
                    Text(stringResource(R.string.acct_evtd_withdrawn_body), fontSize = 16.sp, color = NeerlyColors.Ink700)
                }
            }
            b.pickup?.let { p -> item {
                Notice(if (p.completedAt != null) stringResource(R.string.acct_evtd_pickup_done, shortDay(p.completedAt))
                    else stringResource(R.string.acct_evtd_pickup_on, eventWhen(p.scheduledFor)), Tone.Info)
            } }
            s.error?.let { e -> item { Notice(e, Tone.Bad) } }
            if (stage == EventStage.WAITING) item {
                TextButton(onClick = { confirmWithdraw = true }, enabled = !s.withdrawing,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (s.withdrawing) stringResource(R.string.acct_evtd_withdrawing) else stringResource(R.string.acct_evtd_withdraw),
                        color = NeerlyColors.Err, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (confirmWithdraw) AlertDialog(
        onDismissRequest = { confirmWithdraw = false },
        title = { Text(stringResource(R.string.acct_evtd_withdraw_q)) },
        text = { Text(stringResource(R.string.acct_evtd_withdraw_body, s.booking?.vendorName ?: stringResource(R.string.acct_dep_the_shop))) },
        confirmButton = { TextButton(onClick = { confirmWithdraw = false; vm.withdraw() }) {
            Text(stringResource(R.string.acct_evtd_withdraw_yes), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = { confirmWithdraw = false }) { Text(stringResource(R.string.acct_evtd_keep_asking)) } }
    )
}

/** The shop's confirmed price, framed like the board's chosen quote. */
@Composable
private fun PriceCard(b: EventBookingResponse, shop: String) {
    Column(Modifier.fillMaxWidth().background(NeerlyColors.Paper, RoundedCornerShape(16.dp))
        .border(2.dp, NeerlyColors.CustomerPrimary, RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(shop, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text(b.totalAmount.rupees(), fontFamily = FontFamily.Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        }
        val lines = b.items.map { "${it.quantity} × ${it.productName} ${it.subtotal.rupees()}" } +
            listOfNotNull(
                if (b.deliveryFee.signum() > 0) stringResource(R.string.acct_evtd_delivery, b.deliveryFee.rupees()) else stringResource(R.string.acct_evtd_delivery_free),
                if (b.depositTotal.signum() > 0) stringResource(R.string.acct_evtd_deposit, b.depositTotal.rupees()) else null)
        Text(lines.joinToString(" · "), fontSize = 15.sp, lineHeight = 21.sp, color = NeerlyColors.Ink600)
        if (b.advancePaidAmount.signum() > 0) Text(stringResource(R.string.acct_evtd_paid, b.advancePaidAmount.rupees()),
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ok)
        b.vendorNotes?.takeIf { it.isNotBlank() }?.let { Text("“$it”", fontSize = 15.sp, color = NeerlyColors.Ink700) }
    }
}

/** All the customer's event requests (Profile → Book for an event; Orders → Event bookings). */
@HiltViewModel
class EventBookingsViewModel @Inject constructor(private val api: NeerlyApi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val _list = MutableStateFlow<List<EventBookingResponse>?>(null)
    val list: StateFlow<List<EventBookingResponse>?> = _list.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    init { load() }
    fun load() {
        _error.value = null
        viewModelScope.launch {
            runCatching { api.eventBookings() }.onSuccess { _list.value = it }
                .onFailure { _error.value = it.userMessage("event bookings", R.string.acct_evt_load_failed, strings) }
        }
    }
}

@Composable
fun EventBookingsScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onNew: () -> Unit, vm: EventBookingsViewModel = hiltViewModel()) {
    val list by vm.list.collectAsState()
    val error by vm.error.collectAsState()
    OnReturn(vm::load)
    BoondhPage(stringResource(R.string.acct_evtl_title), onBack = onBack,
        footer = { PrimaryButton(stringResource(R.string.acct_evtl_new), onNew) }) {
        val l = list
        if (l == null) { item { LoadState(error == null, error, vm::load) }; return@BoondhPage }
        if (l.isEmpty()) { item { Text(stringResource(R.string.acct_evtl_empty), fontSize = 16.sp, lineHeight = 23.sp, color = NeerlyColors.Ink600) }; return@BoondhPage }
        items(l, key = { it.id }) { b ->
            ListRow(onClick = { onOpen(b.id) }) {
                Column(Modifier.weight(1f)) {
                    Text(b.eventName?.takeIf { it.isNotBlank() } ?: (b.vendorName ?: stringResource(R.string.acct_dep_the_shop)),
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                    Text(listOfNotNull(b.vendorName, eventWhen(b.eventStart)).joinToString(" · "), fontSize = 14.sp, color = NeerlyColors.Ink600)
                }
                val stage = eventStage(b.status)
                Pill(stringResource(when (stage) {
                    EventStage.WAITING -> R.string.acct_evtl_waiting; EventStage.BOOKED -> R.string.acct_evtl_booked
                    EventStage.DONE -> R.string.acct_evtl_done; EventStage.SAID_NO -> R.string.acct_evtl_no
                    EventStage.WITHDRAWN -> R.string.acct_evtl_withdrawn; EventStage.EXPIRED -> R.string.acct_evtl_expired
                }), when (stage) { EventStage.WAITING -> Tone.Warn; EventStage.BOOKED, EventStage.DONE -> Tone.Ok; EventStage.WITHDRAWN -> Tone.Neutral; else -> Tone.Bad })
            }
        }
    }
}
