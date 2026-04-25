package com.neerly.mobile.feature.deposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.DepositResponse
import com.neerly.mobile.data.dto.ReturnRequest
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DepositsViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DepositsUiState())
    val state: StateFlow<DepositsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.deposits() }
                .onSuccess { _state.value = DepositsUiState(deposits = it, loading = false) }
                .onFailure { _state.value = DepositsUiState(loading = false, error = it.message) }
        }
    }

    /**
     * Schedule a return — defaults to SCHEDULED_PICKUP. Drop-off mode is selected
     * by passing mode = "DROPOFF". For subscriptions the return is auto-attached
     * to the next delivery so it doesn't go through this entry point.
     */
    fun scheduleReturn(depositId: String, mode: String) {
        viewModelScope.launch {
            runCatching { repo.scheduleReturn(ReturnRequest(depositId, mode)) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}

data class DepositsUiState(
    val loading: Boolean = true,
    val deposits: List<DepositResponse> = emptyList(),
    val error: String? = null
) {
    val held      get() = deposits.filter { it.status == "HELD" }
    val returned  get() = deposits.filter { it.status == "RETURNED" }
    val forfeited get() = deposits.filter { it.status == "FORFEITED" }
}
