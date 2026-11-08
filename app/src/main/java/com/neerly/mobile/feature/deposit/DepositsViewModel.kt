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
import com.neerly.mobile.core.util.userMessage
import javax.inject.Inject

@HiltViewModel
class DepositsViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(DepositsUiState())
    val state: StateFlow<DepositsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val deposits = repo.deposits()
                // Booked returns come from the server, so a booking survives an app restart.
                val booked = runCatching { repo.openReturns() }.getOrDefault(emptyList())
                    .mapNotNull { r -> r.depositId?.let { it to r.returnMode } }.toMap()
                val refundTo = runCatching { repo.me().refundDestination }.getOrNull()
                Triple(deposits, booked, refundTo)
            }
                .onSuccess { (deposits, booked, refundTo) -> _state.value = _state.value.copy(
                    deposits = deposits, requested = booked, refundDestination = refundTo, loading = false, error = null) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("deposits", com.neerly.mobile.R.string.acct_dep_load_failed, strings)) }
        }
    }

    /**
     * Schedule a return — defaults to SCHEDULED_PICKUP. Drop-off mode is selected
     * by passing mode = "DROPOFF". For subscriptions the return is auto-attached
     * to the next delivery so it doesn't go through this entry point.
     */
    fun scheduleReturn(depositId: String, mode: String, preferredSlot: String? = null) {
        val deposit = _state.value.deposits.firstOrNull { it.id == depositId } ?: return
        _state.value = _state.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            runCatching {
                // A pickup needs somewhere to pick up from: the customer's primary address.
                val addressId = if (mode == "SCHEDULED_PICKUP") {
                    repo.addresses().let { all -> all.firstOrNull { it.isPrimary } ?: all.firstOrNull() }?.id
                        ?: error(strings.get(com.neerly.mobile.R.string.acct_dep_need_address))
                } else null
                repo.scheduleReturn(ReturnRequest(
                    depositId = depositId, returnMode = mode,
                    containerCount = deposit.containerCount, addressId = addressId,
                    preferredSlot = preferredSlot
                ))
            }
                .onSuccess {
                    // Show it at once; refresh() then reloads the booking from the server.
                    _state.value = _state.value.copy(submitting = false, requested = _state.value.requested + (depositId to mode))
                    refresh()
                }
                .onFailure { _state.value = _state.value.copy(submitting = false,
                    error = it.userMessage("schedule return", com.neerly.mobile.R.string.acct_dep_book_failed, strings)) }
        }
    }
}

data class DepositsUiState(
    val loading: Boolean = true,
    val deposits: List<DepositResponse> = emptyList(),
    val submitting: Boolean = false,
    /** depositId → return mode of an open booked return (from `GET /customer/returns`). */
    val requested: Map<String, String> = emptyMap(),
    /** The customer's refund setting (WALLET / ORIGINAL…) — where a returned deposit goes. */
    val refundDestination: String? = null,
    val error: String? = null
) {
    /** Where the deposit goes, in the customer's words — follows their refund setting. */
    @get:androidx.annotation.StringRes
    val refundWhere: Int get() = if (refundDestination == null || refundDestination == "WALLET") com.neerly.mobile.R.string.acct_dep_where_wallet
        else com.neerly.mobile.R.string.acct_dep_where_source
    val heldTotal: java.math.BigDecimal get() = held.fold(java.math.BigDecimal.ZERO) { a, d -> a + (d.refundableAmount ?: d.amount) }
    // Backend statuses: HELD, LATE (still out, fee accruing), RETURNED, FORFEIT, WAIVED.
    // WAIVED rows are deposits voided on orders that never arrived — no jar, nothing to show.
    val held      get() = deposits.filter { it.status == "HELD" || it.status == "LATE" }
    val returned  get() = deposits.filter { it.status == "RETURNED" }
    val forfeited get() = deposits.filter { it.status == "FORFEIT" }
}
