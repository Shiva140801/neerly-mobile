package com.neerly.mobile.feature.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.ConnectivityObserver
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.OrderTrackingResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Polls the order every 15 s while it's not yet terminal. Reads the orderId
 * from the nav savedStateHandle — the screen route declares `{orderId}`.
 *
 * For V1.1 we'll replace the polling loop with a Firestore live-mirror
 * subscription so we don't hit REST 4× per minute per active tracker.
 */
@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val connectivity: ConnectivityObserver,
    savedState: SavedStateHandle
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivity.isOnline

    private val orderId: String = savedState["orderId"]
        ?: error("OrderTrackingViewModel requires an orderId route arg")

    private val _state = MutableStateFlow(OrderTrackingUiState())
    val state: StateFlow<OrderTrackingUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init { startPolling() }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var consecutiveFailures = 0
            while (true) {
                runCatching { fetchOnce() }
                    .onSuccess { consecutiveFailures = 0 }
                    .onFailure {
                        consecutiveFailures++
                        // Never `it.message`: Retrofit/Moshi failures read like
                        // "Unable to create converter for …", which means
                        // nothing to a customer and leaks internal type names.
                        _state.value = _state.value.copy(
                            error = it.userMessage(
                                context = "order tracking",
                                fallback = "Couldn't refresh this order. Please try again."
                            ),
                            loading = false
                        )
                    }
                // Stop polling once the order is terminal OR we've had repeated
                // failures — the latter prevents a runaway tight loop in tests
                // (and on devices with no network) where every fetch throws.
                if (_state.value.order?.status in TERMINAL) break
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) break
                // A driver on the road moves a block in 15 s — poll faster only
                // for the stretch where a map marker is actually moving.
                delay(if (_state.value.order?.status in IN_FLIGHT) LIVE_POLL_INTERVAL_MS else POLL_INTERVAL_MS)
            }
        }
    }

    fun refresh() {
        connectivity.refresh()
        viewModelScope.launch { runCatching { fetchOnce() } }
    }

    private suspend fun fetchOnce() {
        val order = repo.order(orderId)
        // The tracking payload only carries a fix while in flight; a failure here
        // must not take down the whole screen, so it degrades to timeline-only.
        val tracking = if (order.status in IN_FLIGHT) {
            runCatching { repo.orderTracking(orderId) }.getOrNull()
        } else null
        updateFrom(order, tracking)
    }

    private fun updateFrom(o: OrderResponse, t: OrderTrackingResponse?) {
        // Poll responses can land out of order; never rewind the marker.
        val prev = _state.value.tracking
        val newAt = t?.let(::fixInstant)
        val prevAt = prev?.let(::fixInstant)
        val next = if (t != null && prev != null && newAt != null && prevAt != null && newAt.isBefore(prevAt)) {
            t.copy(
                driverLat = prev.driverLat, driverLng = prev.driverLng,
                driverHeadingDeg = prev.driverHeadingDeg, driverLocationAt = prev.driverLocationAt
            )
        } else t
        _state.value = OrderTrackingUiState(
            order = o,
            tracking = next,
            loading = false,
            error = null,
            // Stamped from the clock at the moment the fetch succeeded, so the
            // offline "last updated" line quotes a time that really happened.
            lastUpdatedLabel = runCatching { TIME_FORMAT.format(Instant.now()) }.getOrNull()
        )
    }

    private fun fixInstant(t: OrderTrackingResponse): Instant? =
        t.driverLocationAt?.let { runCatching { Instant.parse(it) }.getOrNull() }

    companion object {
        val TERMINAL = setOf("DELIVERED", "CANCELLED", "FAILED")

        /** Statuses during which the driver is on the road and the map is live. */
        val IN_FLIGHT = setOf("DISPATCHED", "ARRIVING")

        const val POLL_INTERVAL_MS = 15_000L
        const val LIVE_POLL_INTERVAL_MS = 5_000L

        /** After this many back-to-back failures we give up polling (test-safe). */
        const val MAX_CONSECUTIVE_FAILURES = 3

        private val TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
    }
}

data class OrderTrackingUiState(
    val order: OrderResponse? = null,
    /** Live-map payload — non-null only while the backend says the order is in flight. */
    val tracking: OrderTrackingResponse? = null,
    val loading: Boolean = true,
    val error: String? = null,
    /** Wall-clock time of the last successful fetch, e.g. "9:38 AM". */
    val lastUpdatedLabel: String? = null
)
