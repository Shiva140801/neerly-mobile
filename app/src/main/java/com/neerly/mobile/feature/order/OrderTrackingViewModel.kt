package com.neerly.mobile.feature.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    savedState: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedState["orderId"]
        ?: error("OrderTrackingViewModel requires an orderId route arg")

    private val _state = MutableStateFlow(OrderTrackingUiState())
    val state: StateFlow<OrderTrackingUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init { startPolling() }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                runCatching { repo.order(orderId) }
                    .onSuccess { updateFrom(it) }
                    .onFailure { _state.value = _state.value.copy(error = it.message, loading = false) }
                if (_state.value.order?.status in TERMINAL) break
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.order(orderId) }.onSuccess { updateFrom(it) }
        }
    }

    private fun updateFrom(o: OrderResponse) {
        _state.value = OrderTrackingUiState(order = o, loading = false, error = null)
    }

    companion object {
        val TERMINAL = setOf("DELIVERED", "CANCELLED", "FAILED")
        const val POLL_INTERVAL_MS = 15_000L
    }
}

data class OrderTrackingUiState(
    val order: OrderResponse? = null,
    val loading: Boolean = true,
    val error: String? = null
)
