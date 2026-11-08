package com.neerly.mobile.feature.vendor.orders

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

data class VendorOrdersUiState(
    val loading: Boolean = true,
    val orders: List<VendorOrderView> = emptyList(),
    val pendingEvents: Int = 0,
    val tab: OrderTab = OrderTab.New,
    val error: String? = null
) {
    fun inTab(t: OrderTab) = orders.filter { tabOf(it.status) == t }
}

@HiltViewModel
class VendorOrdersViewModel @Inject constructor(
    private val ops: VendorOpsApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(VendorOrdersUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.orders.isEmpty(), error = null)
        viewModelScope.launch {
            runCatching {
                supervisorScope {
                    val fresh = async { com.neerly.mobile.feature.vendor.dashboard.liveOffers(ops.orders("VENDOR_ASSIGNED"), java.time.Instant.now()) }
                    val active = async { ops.orders("ACTIVE") }
                    val delivered = async { ops.orders("COMPLETED_TODAY") }
                    val cancelled = async { runCatching { ops.orders("CANCELLED_TODAY") }.getOrDefault(emptyList()) }
                    val events = async { runCatching { ops.dashboard().pendingEvents }.getOrDefault(0) }
                    (fresh.await() + active.await() + delivered.await() + cancelled.await()).distinctBy { it.id } to events.await()
                }
            }.onSuccess { (list, ev) ->
                val first = _state.value.orders.isEmpty()
                _state.value = _state.value.copy(loading = false, orders = list, pendingEvents = ev,
                    // Land on the first tab with something in it the first time.
                    tab = if (first) OrderTab.entries.firstOrNull { t -> list.any { tabOf(it.status) == t } } ?: OrderTab.New else _state.value.tab)
            }.onFailure {
                _state.value = _state.value.copy(loading = false,
                    error = it.userMessage(context = "vendor orders", fallback = strings.get(R.string.vend_err_load_orders)))
            }
        }
    }

    fun select(t: OrderTab) { _state.value = _state.value.copy(tab = t) }
}

/** Board V09 — the order queue: New · Preparing · Out · Done. */
@Composable
fun VendorOrdersScreen(
    onOpen: (VendorOrderView) -> Unit,
    onEvents: () -> Unit = {},
    onSelectTab: (VendorTab) -> Unit = {},
    vm: VendorOrdersViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    OnReturn(vm::load)
    BoondhPage(
        title = stringResource(R.string.vend_tab_orders),
        subtitle = stringResource(R.string.vend_open_now, s.orders.count { it.status !in setOf("CANCELLED", "DELIVERED", "FAILED") }),
        bottomBar = { VendorTabBar(VendorTab.Orders, onSelectTab, ordersBadge = s.inTab(OrderTab.New).size) }
    ) {
        item {
            ScrollableTabRow(selectedTabIndex = s.tab.ordinal, edgePadding = 0.dp, containerColor = NeerlyColors.Canvas,
                contentColor = NeerlyColors.Ink900) {
                OrderTab.entries.forEach { t ->
                    val n = s.inTab(t).size + if (t == OrderTab.New) s.pendingEvents else 0
                    Tab(selected = s.tab == t, onClick = { vm.select(t) }, modifier = Modifier.heightIn(min = 48.dp),
                        text = { Text(if (n > 0) stringResource(R.string.vend_tab_count, stringResource(t.label), n) else stringResource(t.label), fontWeight = FontWeight.SemiBold) })
                }
            }
        }
        if (s.orders.isEmpty() && (s.loading || s.error != null)) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        s.error?.let { item { Notice(it, Tone.Bad) } }
        val list = s.inTab(s.tab)
        if (s.tab == OrderTab.New && s.pendingEvents > 0) item {
            BoondhCard(onClick = onEvents) {
                Pill(stringResource(R.string.vend_confirm_or_decline), Tone.Warn)
                InfoRow(pluralStringResource(R.plurals.vend_n_event_bookings, s.pendingEvents, s.pendingEvents), stringResource(R.string.vend_events_kinds))
            }
        }
        if (list.isEmpty() && !(s.tab == OrderTab.New && s.pendingEvents > 0)) item {
            Notice(when (s.tab) {
                OrderTab.New -> stringResource(R.string.vend_empty_new)
                OrderTab.Preparing -> stringResource(R.string.vend_empty_preparing)
                OrderTab.Out -> stringResource(R.string.vend_empty_out)
                OrderTab.Done -> stringResource(R.string.vend_empty_done)
            }, Tone.Neutral)
        }
        items(list, key = { it.id }) { o -> OrderRow(o) { onOpen(o) } }
    }
}

@Composable
private fun OrderRow(o: VendorOrderView, onClick: () -> Unit) {
    BoondhCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(o.customerFirstName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(o.orderNumber, fontSize = 12.sp, color = NeerlyColors.Ink600)
            }
            Text(o.orderValue.rupees(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        }
        Text(o.itemsLine.ifBlank { stringResource(R.string.vend_no_items) }, fontSize = 14.sp, color = NeerlyColors.Ink700)
        when {
            o.status == "VENDOR_ASSIGNED" -> AcceptClock(o.acceptDeadlineAt)
            o.status == "CANCELLED" -> Pill(if (o.tripPay.signum() > 0) stringResource(R.string.vend_cancelled_trip_paid, o.tripPay.rupees()) else stringResource(R.string.vend_cancelled), Tone.Neutral)
            o.status in setOf("VENDOR_ACCEPTED", "PREPARING") && o.driverId == null -> Pill(stringResource(R.string.vend_needs_driver), Tone.Bad)
            o.status == "DELIVERED" -> Pill(stringResource(R.string.vend_delivered), Tone.Ok)
            else -> Pill(com.neerly.mobile.feature.vendor.dashboard.statusWord(o.status), Tone.Info)
        }
    }
}
