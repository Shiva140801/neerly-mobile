package com.neerly.mobile.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.InitiatePaymentResult
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.dto.WalletTopupRequest
import com.neerly.mobile.data.dto.WalletTransaction
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Wallet screen state. One screen with three sections:
 *   - Balance card (available + held)
 *   - Top-up chips (₹200 / ₹500 / ₹1000 / custom) → Razorpay
 *   - Transactions list
 *
 * Top-up returns the same `InitiatePaymentResult` shape as a regular order
 * payment, so the host activity reuses `PaymentLauncher` to open Razorpay.
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WalletUiState())
    val state: StateFlow<WalletUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val balance = repo.wallet()
                val txns = runCatching { repo.walletTransactions() }.getOrDefault(emptyList())
                balance to txns
            }
                .onSuccess { (b, t) ->
                    _state.value = WalletUiState(loading = false, balance = b, transactions = t)
                }
                .onFailure {
                    _state.value = WalletUiState(loading = false, error = it.message)
                }
        }
    }

    fun topup(amount: BigDecimal, onReady: (InitiatePaymentResult) -> Unit) {
        if (amount.signum() <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid amount")
            return
        }
        _state.value = _state.value.copy(toppingUp = true, error = null)
        viewModelScope.launch {
            runCatching { repo.walletTopup(WalletTopupRequest(amount, method = "UPI")) }
                .onSuccess {
                    _state.value = _state.value.copy(toppingUp = false)
                    onReady(it)
                }
                .onFailure {
                    _state.value = _state.value.copy(toppingUp = false, error = it.message ?: "Top-up failed")
                }
        }
    }
}

data class WalletUiState(
    val loading: Boolean = true,
    val toppingUp: Boolean = false,
    val balance: WalletResponse? = null,
    val transactions: List<WalletTransaction> = emptyList(),
    val error: String? = null
)
