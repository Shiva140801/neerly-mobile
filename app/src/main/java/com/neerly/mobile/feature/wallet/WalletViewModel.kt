package com.neerly.mobile.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
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
    private val repo: CustomerRepository,
    private val gateway: com.neerly.mobile.feature.checkout.GatewayPayments
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
                    _state.value = WalletUiState(
                        loading = false,
                        error = it.userMessage(
                            context = "wallet balance",
                            fallback = "Couldn't load your balance. Please try again."
                        )
                    )
                }
        }
    }

    /**
     * Add money: the backend opens a top-up payment, Razorpay collects it, the backend
     * captures it and credits the wallet. The callback used to be a no-op, so no top-up
     * ever completed.
     */
    fun topup(activity: android.app.Activity, amount: BigDecimal) {
        if (amount.signum() <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid amount")
            return
        }
        _state.value = _state.value.copy(toppingUp = true, error = null, notice = null)
        viewModelScope.launch {
            val outcome = runCatching {
                val init = repo.walletTopup(WalletTopupRequest(amount, method = "UPI"))
                gateway.pay(activity, init, customerName = "", customerContact = null,
                    description = "Add ₹$amount to wallet")
            }.getOrElse {
                com.neerly.mobile.feature.checkout.PaymentOutcome.Declined(it.userMessage(
                    context = "wallet top-up", fallback = "Couldn't start the top-up. Nothing was taken."))
            }
            when (outcome) {
                com.neerly.mobile.feature.checkout.PaymentOutcome.Captured -> {
                    _state.value = _state.value.copy(toppingUp = false, notice = "₹$amount added to your wallet")
                    refresh()
                }
                is com.neerly.mobile.feature.checkout.PaymentOutcome.Unconfirmed ->
                    _state.value = _state.value.copy(toppingUp = false, notice = outcome.message)
                is com.neerly.mobile.feature.checkout.PaymentOutcome.Declined ->
                    _state.value = _state.value.copy(toppingUp = false, error = outcome.message)
            }
        }
    }
}

data class WalletUiState(
    val loading: Boolean = true,
    /** Good news to show once — "₹500 added". */
    val notice: String? = null,
    val toppingUp: Boolean = false,
    val balance: WalletResponse? = null,
    val transactions: List<WalletTransaction> = emptyList(),
    val error: String? = null
)
