package com.neerly.mobile.feature.vendor.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.VendorOrderResponse
import com.neerly.mobile.data.dto.VendorTodaySummary
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import com.neerly.mobile.core.util.userMessage
import javax.inject.Inject

@HiltViewModel
class VendorTodayViewModel @Inject constructor(
    private val repo: VendorRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VendorTodayUiState())
    val state: StateFlow<VendorTodayUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                // supervisorScope: a failed async child (e.g. summary) must surface
                // via await() into runCatching, not cancel the whole scope.
                supervisorScope {
                    // The order lists fail loudly. Swallowed into empty lists, a parse
                    // failure read as "Quiet so far" over orders waiting to be accepted.
                    val summary = async { repo.today() }
                    val pending = async { repo.pendingOrders() }
                    val active = async { repo.activeOrders() }
                    val completed = async { repo.completedToday() }
                    Quartet(summary.await(), pending.await(), active.await(), completed.await())
                }
            }
                .onSuccess {
                    _state.value = VendorTodayUiState(
                        loading = false,
                        summary = it.a,
                        // Newest first: a just-accepted order belongs at the top, not below 18 old ones.
                        pending = it.b.sortedBy { o -> o.acceptDeadlineAt ?: o.placedAt },
                        active = it.c.sortedByDescending { o -> o.placedAt },
                        completed = it.d.sortedByDescending { o -> o.placedAt }
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.userMessage(context = "vendor today", fallback = "Couldn't load today's orders.")
                    )
                }
        }
    }

    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            runCatching { repo.accept(orderId) }
                .onSuccess { refresh() }
                .onFailure {
                    // Usually the 3-minute window closed — say so, and drop the stale card.
                    _state.value = _state.value.copy(
                        error = it.userMessage(context = "accept order", fallback = "Couldn't accept that order.")
                    )
                    refresh()
                }
        }
    }

    fun rejectOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            runCatching { repo.reject(orderId, reason) }
                .onSuccess { refresh() }
                .onFailure {
                    _state.value = _state.value.copy(
                        error = it.userMessage(context = "reject order", fallback = "Couldn't reject that order.")
                    )
                }
        }
    }

    private data class Quartet<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

data class VendorTodayUiState(
    val loading: Boolean = true,
    val summary: VendorTodaySummary? = null,
    val pending: List<VendorOrderResponse> = emptyList(),
    val active: List<VendorOrderResponse> = emptyList(),
    val completed: List<VendorOrderResponse> = emptyList(),
    val error: String? = null
)
