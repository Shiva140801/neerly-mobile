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
        val deposit = _state.value.deposits.firstOrNull { it.id == depositId } ?: return
        viewModelScope.launch {
            runCatching {
                // A pickup needs somewhere to pick up from: the customer's primary address.
                val addressId = if (mode == "SCHEDULED_PICKUP") {
                    repo.addresses().let { all -> all.firstOrNull { it.isPrimary } ?: all.firstOrNull() }?.id
                        ?: error("Add an address first so the driver knows where to collect the jar")
                } else null
                repo.scheduleReturn(ReturnRequest(
                    depositId = depositId, returnMode = mode,
                    containerCount = deposit.containerCount, addressId = addressId
                ))
            }
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
    // Backend statuses: HELD, LATE (still out, fee accruing), RETURNED, FORFEIT, WAIVED.
    // WAIVED rows are deposits voided on orders that never arrived — no jar, nothing to show.
    val held      get() = deposits.filter { it.status == "HELD" || it.status == "LATE" }
    val returned  get() = deposits.filter { it.status == "RETURNED" }
    val forfeited get() = deposits.filter { it.status == "FORFEIT" }
}
