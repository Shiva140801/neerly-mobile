package com.neerly.mobile.feature.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrderHistoryUiState())
    val state: StateFlow<OrderHistoryUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.myOrders(page = 0, size = 50) }
                .onSuccess { _state.value = OrderHistoryUiState(orders = it, loading = false) }
                .onFailure { _state.value = OrderHistoryUiState(loading = false, error = it.message) }
        }
    }
}

data class OrderHistoryUiState(
    val orders: List<OrderResponse> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)
