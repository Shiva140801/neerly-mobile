package com.neerly.mobile.feature.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.CompleteDeliveryRequest
import com.neerly.mobile.data.dto.DriverAssignment
import com.neerly.mobile.data.dto.DriverShiftResponse
import com.neerly.mobile.data.repo.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.DriverOffer
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Driver home — three states:
 *   off-duty  → big "Start shift" button
 *   on-duty, idle → "Available" badge + empty assignment list
 *   on-duty, with assignments → list of jobs, top one is the active one
 *
 * Tapping an assignment moves it through DISPATCHED → EN_ROUTE → ARRIVED →
 * DELIVERED. The driver app deliberately keeps UI minimal — large taps,
 * Telugu-friendly labels, low literacy support.
 */
@HiltViewModel
class DriverHomeViewModel @Inject constructor(
    private val repo: DriverRepository,
    private val ticker: DriverLocationTicker
) : ViewModel() {

    private val _state = MutableStateFlow(DriverHomeUiState())
    val state: StateFlow<DriverHomeUiState> = _state.asStateFlow()

    private var sendJob: Job? = null
    private var offerJob: Job? = null
    private var visible = false

    /** Polling runs only while the screen is on screen — not for a backgrounded app. */
    fun setVisible(isVisible: Boolean) {
        visible = isVisible
        if (isVisible) refresh() else syncOfferLoop()
    }
    private var locationPermitted = false

    init { refresh() }

    /** The screen reports the runtime-permission result; nothing is sent without it. */
    fun onLocationPermission(granted: Boolean) {
        locationPermitted = granted
        syncLocationLoop()
    }

    /**
     * Broadcasts the driver's position while on shift: 10 s cadence on the move,
     * 30 s parked. The fix the OS handed us is only sent while it is younger than
     * [DriverLocationTicker.FIX_MAX_AGE_MS] — a stale fix reads as "no location"
     * on the customer's map rather than a marker pinned to an old road position.
     * Failures are dropped silently; the next tick is 10 s away and the customer
     * map treats a missing fix as "updating…", so there is nothing to surface.
     */
    private fun syncLocationLoop() {
        val shouldRun = locationPermitted && _state.value.isOnDuty
        if (!shouldRun) {
            sendJob?.cancel()
            sendJob = null
            ticker.stop()
            return
        }
        if (sendJob?.isActive == true) return
        ticker.start()
        sendJob = viewModelScope.launch {
            while (isActive) {
                ticker.freshFix()?.let { fix ->
                    runCatching {
                        repo.ping(
                            latitude = fix.latitude,
                            longitude = fix.longitude,
                            headingDeg = if (fix.hasBearing()) fix.bearing.toDouble() else null,
                            speedMps = if (fix.hasSpeed()) fix.speed.toDouble() else null,
                            accuracyM = if (fix.hasAccuracy()) fix.accuracy.toDouble() else null,
                            batteryPct = ticker.batteryPct(),
                            orderId = _state.value.activeAssignment?.orderId
                        )
                    }
                }
                delay(
                    if (ticker.isParked()) DriverLocationTicker.PARKED_SEND_MS
                    else DriverLocationTicker.ACTIVE_SEND_MS
                )
            }
        }
    }

    override fun onCleared() {
        ticker.stop()
        super.onCleared()
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val shift = repo.currentShift()
                val jobs = if (shift?.endedAt == null && shift != null) repo.assignments() else emptyList()
                shift to jobs
            }
                .onSuccess { (shift, jobs) ->
                    _state.value = _state.value.copy(
                        loading = false, shift = shift, assignments = jobs, error = null
                    )
                    syncLocationLoop()
                    syncOfferLoop()
                }
                .onFailure { _state.value = DriverHomeUiState(loading = false, error = it.say("driver home")) }
        }
    }

    fun startShift() {
        viewModelScope.launch {
            runCatching { repo.startShift() }
                .onSuccess { _state.value = _state.value.copy(shift = it); refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.say("start shift")) }
        }
    }

    fun endShift(codHandedOver: BigDecimal?) {
        val id = _state.value.shift?.id ?: return
        viewModelScope.launch {
            runCatching { repo.endShift(id, codHandedOver) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.say("end shift")) }
        }
    }

    fun markArrived(orderId: String) {
        viewModelScope.launch {
            runCatching { repo.markArrived(orderId) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.say("mark arrived")) }
        }
    }

    /** Returns through [onDone] so the dialog stays open on a wrong code. */
    fun completeDelivery(
        orderId: String, otp: String, photoS3Key: String, codCollected: BigDecimal?,
        emptiesCollected: Int?, codSettlement: String?, onDone: (ok: Boolean) -> Unit
    ) {
        _state.value = _state.value.copy(delivering = true, deliverError = null)
        viewModelScope.launch {
            runCatching { repo.complete(orderId, otp, photoS3Key, codCollected, emptiesCollected, codSettlement) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        delivering = false, lastDelivered = _state.value.assignments.firstOrNull { it.orderId == orderId }?.orderNumber
                    )
                    onDone(true); refresh()
                }
                .onFailure {
                    _state.value = _state.value.copy(delivering = false, deliverError = it.say("deliver"))
                    onDone(false)
                }
        }
    }

    // ---- Task offers (S-DRIV-ASSIGN) ----------------------------------------------------
    // A vendor offers a run; it reaches this driver with an accept window. The app had no
    // offer screen at all, so a driver could never take a run on their phone.

    private fun syncOfferLoop() {
        val shouldPoll = visible && _state.value.isOnDuty && _state.value.activeAssignment == null
        if (!shouldPoll) { offerJob?.cancel(); offerJob = null; _state.value = _state.value.copy(offer = null); return }
        if (offerJob?.isActive == true) return
        offerJob = viewModelScope.launch {
            while (true) {
                runCatching { repo.pendingOffer() }
                    .onSuccess { _state.value = _state.value.copy(offer = it) }
                delay(OFFER_POLL_MS)
            }
        }
    }

    fun acceptOffer() {
        val offer = _state.value.offer ?: return
        _state.value = _state.value.copy(offerBusy = true, error = null)
        viewModelScope.launch {
            runCatching { repo.acceptOffer(offer.orderId) }
                .onSuccess { _state.value = _state.value.copy(offer = null, offerBusy = false); refresh() }
                .onFailure {
                    _state.value = _state.value.copy(
                        offer = null, offerBusy = false,
                        error = it.say("accept run", "That run is no longer available.")
                    )
                }
        }
    }

    fun passOffer() {
        val offer = _state.value.offer ?: return
        _state.value = _state.value.copy(offer = null)
        viewModelScope.launch { runCatching { repo.passOffer(offer.orderId) } }
    }

    private fun Throwable.say(context: String, fallback: String = "That didn't go through. Try again.") =
        userMessage(context = context, fallback = fallback)

    private companion object {
        const val OFFER_POLL_MS = 5_000L
    }
}

data class DriverHomeUiState(
    val loading: Boolean = true,
    val shift: DriverShiftResponse? = null,
    val assignments: List<DriverAssignment> = emptyList(),
    val error: String? = null,
    val offer: DriverOffer? = null,
    val offerBusy: Boolean = false,
    val delivering: Boolean = false,
    val deliverError: String? = null,
    val lastDelivered: String? = null
) {
    val isOnDuty: Boolean get() = shift != null && shift.endedAt == null
    val activeAssignment: DriverAssignment? get() = assignments.firstOrNull {
        it.status in setOf("PREPARING", "DISPATCHED", "ARRIVING")
    }
}
