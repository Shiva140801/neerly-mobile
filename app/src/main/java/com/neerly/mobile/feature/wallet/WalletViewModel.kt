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
    private val gateway: com.neerly.mobile.feature.checkout.GatewayPayments,
    private val strings: com.neerly.mobile.core.util.Strings
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
                // The jar card: deposits the customer has parked against jars at home.
                val held = runCatching { repo.deposits() }.getOrDefault(emptyList())
                    .filter { it.status == "HELD" || it.status == "LATE" }
                Triple(balance, txns, held)
            }
                .onSuccess { (b, t, held) ->
                    _state.value = WalletUiState(loading = false, balance = b, transactions = t,
                        jarsOut = held.sumOf { it.containerCount },
                        depositHeld = held.fold(BigDecimal.ZERO) { acc, d -> acc + d.amount })
                }
                .onFailure {
                    _state.value = WalletUiState(
                        loading = false,
                        error = it.userMessage("wallet balance", com.neerly.mobile.R.string.acct_wallet_load_failed, strings)
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
            _state.value = _state.value.copy(error = strings.get(com.neerly.mobile.R.string.acct_wallet_invalid_amount))
            return
        }
        _state.value = _state.value.copy(toppingUp = true, error = null, notice = null)
        viewModelScope.launch {
            val outcome = runCatching {
                val init = repo.walletTopup(WalletTopupRequest(amount, method = "UPI"))
                gateway.pay(activity, init, customerName = "", customerContact = null,
                    description = strings.get(com.neerly.mobile.R.string.acct_wallet_topup_desc, amount.toPlainString()))
            }.getOrElse {
                com.neerly.mobile.feature.checkout.PaymentOutcome.Declined(it.userMessage("wallet top-up", com.neerly.mobile.R.string.acct_wallet_topup_failed, strings))
            }
            when (outcome) {
                com.neerly.mobile.feature.checkout.PaymentOutcome.Captured -> {
                    _state.value = _state.value.copy(toppingUp = false, notice = strings.get(com.neerly.mobile.R.string.acct_wallet_topup_done, amount.toPlainString()))
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
    val jarsOut: Int = 0,
    val depositHeld: BigDecimal = BigDecimal.ZERO,
    val transactions: List<WalletTransaction> = emptyList(),
    val error: String? = null
)
