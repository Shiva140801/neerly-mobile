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
                val summary = async { repo.today() }
                val pending = async { runCatching { repo.pendingOrders() }.getOrDefault(emptyList()) }
                val active = async { runCatching { repo.activeOrders() }.getOrDefault(emptyList()) }
                val completed = async { runCatching { repo.completedToday() }.getOrDefault(emptyList()) }
                Quartet(summary.await(), pending.await(), active.await(), completed.await())
            }
                .onSuccess {
                    _state.value = VendorTodayUiState(
                        loading = false,
                        summary = it.a,
                        pending = it.b,
                        active = it.c,
                        completed = it.d
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }

    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            runCatching { repo.accept(orderId) }.onSuccess { refresh() }
        }
    }

    fun rejectOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            runCatching { repo.reject(orderId, reason) }.onSuccess { refresh() }
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
