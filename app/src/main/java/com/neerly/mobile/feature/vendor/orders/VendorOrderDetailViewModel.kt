package com.neerly.mobile.feature.vendor.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.VendorOrderResponse
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorOrderDetailViewModel @Inject constructor(
    private val repo: VendorRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedState["orderId"] ?: error("orderId required")

    private val _state = MutableStateFlow(VendorOrderDetailUiState())
    val state: StateFlow<VendorOrderDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.order(orderId) }
                .onSuccess { _state.value = _state.value.copy(loading = false, order = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun markReady() = transition { repo.ready(orderId) }
    fun dispatch(driverId: String) = transition { repo.dispatch(orderId, driverId) }

    private fun transition(call: suspend () -> VendorOrderResponse) {
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { _state.value = _state.value.copy(order = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}

data class VendorOrderDetailUiState(
    val loading: Boolean = true,
    val order: VendorOrderResponse? = null,
    val error: String? = null
)
