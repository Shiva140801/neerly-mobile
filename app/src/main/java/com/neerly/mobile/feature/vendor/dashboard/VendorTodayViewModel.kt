package com.neerly.mobile.feature.vendor.dashboard

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.VendorBusinessStatus
import com.neerly.mobile.data.dto.VendorProductRow
import com.neerly.mobile.data.dto.VendorSubscriptionTodayRow
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.vendor.orders.VendorAlertPrefsStore
import com.neerly.mobile.feature.vendor.orders.VendorDashboard
import com.neerly.mobile.feature.vendor.orders.VendorOpsApi
import com.neerly.mobile.feature.vendor.orders.VendorOrderView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

/**
 * Board V08 — what the shop needs to act on right now. It polls while the screen is in front
 * (there is no push yet), and the first time a new offer shows up it asks the screen to open
 * the ringing new-order alert (V10) — once per order per phone.
 */
/**
 * Offers the shop can still accept, soonest deadline first. The server can list an offer as
 * VENDOR_ASSIGNED for a moment after its clock ran out (re-routing runs on a schedule); those
 * must not ring the phone or sit in "Needs you now" as a negative countdown.
 */
internal fun liveOffers(offers: List<com.neerly.mobile.feature.vendor.orders.VendorOrderView>, now: java.time.Instant) =
    offers.filter { o -> o.acceptDeadlineAt?.let { runCatching { java.time.Instant.parse(it) }.getOrNull()?.isAfter(now.plusSeconds(5)) } ?: true }
        .sortedBy { it.acceptDeadlineAt ?: it.placedAt }

@HiltViewModel
class VendorTodayViewModel @Inject constructor(
    private val repo: VendorRepository,
    private val ops: VendorOpsApi,
    private val alerts: VendorAlertPrefsStore,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(VendorTodayUiState())
    val state: StateFlow<VendorTodayUiState> = _state.asStateFlow()
    private var poller: Job? = null

    init { refresh() }

    fun refresh(quiet: Boolean = false) {
        if (!quiet) _state.value = _state.value.copy(loading = _state.value.dashboard == null, error = null)
        viewModelScope.launch {
            runCatching {
                supervisorScope {
                    // Orders fail loudly: an empty list on a parse error would read "nothing waiting".
                    val dash = async { ops.dashboard() }
                    val pending = async { ops.orders("VENDOR_ASSIGNED") }
                    val active = async { ops.orders("ACTIVE") }
                    val status = async { runCatching { ops.businessStatus() }.getOrNull() }
                    val subs = async { runCatching { repo.subscriptionsToday() }.getOrDefault(emptyList()) }
                    val catalog = async { runCatching { repo.catalog() }.getOrDefault(emptyList()) }
                    VendorTodayUiState(
                        loading = false, dashboard = dash.await(),
                        pending = liveOffers(pending.await(), java.time.Instant.now()),
                        active = active.await().sortedByDescending { it.placedAt },
                        business = status.await(), subscriptions = subs.await(),
                        soldOut = catalog.await().filter { it.status != "AVAILABLE" }
                    )
                }
            }.onSuccess { s ->
                _state.value = s.copy(alertFor = _state.value.alertFor
                    ?: s.pending.firstOrNull { !alerts.alerted(it.id) }?.id)
            }.onFailure {
                _state.value = _state.value.copy(loading = false,
                    error = it.userMessage(context = "vendor today", fallback = strings.get(R.string.vend_err_load_today)))
            }
        }
    }

    /** The screen opened the alert for [id]; don't ring for it again. */
    fun alertShown(id: String) {
        alerts.markAlerted(id)
        _state.value = _state.value.copy(alertFor = null)
    }

    fun startPolling(everyMs: Long = 20_000) {
        if (poller?.isActive == true) return
        poller = viewModelScope.launch { while (isActive) { delay(everyMs); refresh(quiet = true) } }
    }
    fun stopPolling() { poller?.cancel(); poller = null }

    /** Stop new orders for the chosen time (board V08 pill → pause sheet). */
    fun pause(choice: PauseChoice, reason: String?) = business { repo.pause(choice, reason) }

    /** Take orders again now. */
    fun resume() = business { repo.reopen() }

    private fun business(call: suspend () -> com.neerly.mobile.data.dto.VendorBusinessStatus) {
        if (_state.value.working) return
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { _state.value = _state.value.copy(working = false, business = it); refresh(quiet = true) }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage(context = "pause orders", fallback = strings.get(R.string.vend_err_change))) }
        }
    }
}

data class VendorTodayUiState(
    val loading: Boolean = true,
    val dashboard: VendorDashboard? = null,
    val pending: List<VendorOrderView> = emptyList(),
    val active: List<VendorOrderView> = emptyList(),
    val business: VendorBusinessStatus? = null,
    val subscriptions: List<VendorSubscriptionTodayRow> = emptyList(),
    val soldOut: List<VendorProductRow> = emptyList(),
    val alertFor: String? = null,
    val working: Boolean = false,
    val error: String? = null
) {
    /** Packed (or accepted) orders nobody is taking yet — board "Anil P. needs a driver". */
    val needDriver get() = active.filter { it.driverId == null && it.status in setOf("VENDOR_ACCEPTED", "PREPARING") }
    val subsWithoutDriver get() = subscriptions.filter { it.status == "PENDING" }
    /** Late jars put a line in "Needs you now"; jars merely out are just a count. */
    val lateJars get() = dashboard?.jarsLate ?: 0
}
