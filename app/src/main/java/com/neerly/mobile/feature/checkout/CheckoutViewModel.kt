package com.neerly.mobile.feature.checkout

import com.neerly.mobile.R
import android.app.Activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.InitiatePaymentRequest
import com.neerly.mobile.data.dto.OrderItemRequest
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.PlaceOrderRequest
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.OrderQuoteResponse
import com.neerly.mobile.data.order.OrderLocalStore
import kotlinx.coroutines.Job
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the checkout flow:
 *   1. Load addresses (defaults to primary)
 *   2. User picks payment method (the delivery slot comes from the cart's "When?")
 *   3. placeOrder → server returns an orderId (status PLACED)
 *   4. If payment method != COD: initiate payment → Razorpay launcher → capture
 *   5. On success, caller navigates to OrderPlacedScreen
 *
 * State machine mirrors what backend sees: CartReview → PlacingOrder →
 * AwaitingPayment → Confirmed | Failed.
 */
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val api: NeerlyApi,
    private val cart: CartStore,
    private val orderStore: OrderLocalStore,
    private val gateway: GatewayPayments,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    /**
     * One id per checkout attempt, reused on every retry of it. Minted fresh by the
     * interceptor on each call, a double tap became two orders.
     */
    private var requestId: String = UUID.randomUUID().toString()
    private var quoteJob: Job? = null

    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    init {
        loadAddresses()
        // The wallet option shows its balance (or how much it is short) before choosing it.
        viewModelScope.launch {
            runCatching { repo.wallet() }.onSuccess { _state.value = _state.value.copy(walletBalance = it.availableAmount) }
        }
    }

    fun loadAddresses() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.addresses() }
                .onSuccess { addrs ->
                    val primary = addrs.firstOrNull { it.isPrimary } ?: addrs.firstOrNull()
                    _state.value = _state.value.copy(
                        loading = false,
                        addresses = addrs,
                        selectedAddress = primary
                    )
                    refreshQuote()
                }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun selectAddress(id: String) {
        val a = _state.value.addresses.firstOrNull { it.id == id }
        _state.value = _state.value.copy(selectedAddress = a)
        refreshQuote()
    }

    /**
     * Ask the server what this cart will cost at this address — delivery fee, surge and
     * deposit included. Checkout used to show the bare subtotal (₹85) and then charge
     * ₹445; the number on the button must be the number on the order.
     */
    fun refreshQuote() {
        val req = buildRequest() ?: return
        quoteJob?.cancel()
        _state.value = _state.value.copy(quoting = true)
        quoteJob = viewModelScope.launch {
            runCatching { api.quoteOrder(req) }
                .onSuccess { _state.value = _state.value.copy(quoting = false, quote = it) }
                .onFailure { _state.value = _state.value.copy(quoting = false, quote = null) }
        }
    }

    private fun buildRequest(): PlaceOrderRequest? {
        val s = _state.value
        val snap = cart.snapshot
        val address = s.selectedAddress ?: return null
        if (snap.isEmpty) return null
        val vendorId = snap.vendorId ?: return null
        return PlaceOrderRequest(
            vendorId = vendorId,
            addressId = address.id,
            items = snap.items.map {
                OrderItemRequest(
                    productId = it.productId,
                    quantity = it.quantity,
                    containerMode = if (it.keepContainer) "KEEP" else "TRANSFER_AND_RETURN"
                )
            },
            orderType = "ON_DEMAND",
            paymentMethod = s.paymentMethod,
            promoCode = snap.promoCode,
            // Null slot is "Right now" (on-demand); a picked window goes to the shop's run-sheet.
            requestedSlotStart = snap.slot?.startIso,
            requestedSlotEnd = snap.slot?.endIso
        )
    }

    fun selectPaymentMethod(method: String) {
        _state.value = _state.value.copy(paymentMethod = method)
    }

    /**
     * Server round-trip 1 — creates the order in PLACED state. Returns the
     * [OrderResponse] to the UI so it can launch the payment step.
     */
    fun placeOrder(onSuccess: (OrderResponse) -> Unit) {
        val s = _state.value
        if (s.placing) return
        if (s.selectedAddress == null) {
            _state.value = s.copy(error = strings.get(R.string.cust_co_pick_address))
            return
        }
        if (cart.snapshot.isEmpty) {
            _state.value = s.copy(error = strings.get(R.string.cust_co_cart_empty))
            return
        }
        val req = buildRequest() ?: run {
            _state.value = s.copy(error = strings.get(R.string.cust_co_cart_no_shop))
            return
        }
        _state.value = s.copy(placing = true, error = null)
        viewModelScope.launch {
            runCatching { api.placeOrder(req, requestId) }
                .onSuccess {
                    it.otp?.let { code -> orderStore.saveDeliveryCode(it.id, code) }
                    requestId = UUID.randomUUID().toString()   // this attempt is done
                    _state.value = _state.value.copy(placing = false, placedOrder = it)
                    onSuccess(it)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        placing = false,
                        error = it.userMessage(context = "place order", fallback = strings.get(R.string.cust_co_place_failed))
                    )
                }
        }
    }

    /**
     * The checkout button. Cash and wallet orders are settled when they are placed (the
     * backend debits the wallet inside placement). UPI and card orders then go through
     * Razorpay and are only "placed" for the customer once the backend has captured the
     * money; a declined payment keeps the cart and the placed order, and the button
     * retries the payment instead of placing a second order.
     */
    fun placeAndPay(activity: Activity, onDone: (OrderResponse) -> Unit) {
        val unpaid = _state.value.awaitingPaymentFor
        if (unpaid != null) {
            payOnline(activity, unpaid, onDone)
            return
        }
        placeOrder { order ->
            when (_state.value.paymentMethod) {
                "COD", "WALLET" -> { cart.clear(); onDone(order) }
                else -> payOnline(activity, order, onDone)
            }
        }
    }

    private fun payOnline(activity: Activity, order: OrderResponse, onDone: (OrderResponse) -> Unit) {
        _state.value = _state.value.copy(paying = true, error = null, paymentProblem = null)
        viewModelScope.launch {
            val outcome = runCatching {
                // The amount is only echoed back: the backend charges the order's own total
                // and refuses anything else.
                val init = api.initiatePayment(
                    InitiatePaymentRequest(orderId = order.id, amount = order.totalAmount,
                        method = _state.value.paymentMethod)
                )
                gateway.pay(activity, init, customerName = "", customerContact = null,
                    description = strings.get(R.string.cust_co_payment_desc, order.orderNumber))
            }.getOrElse {
                PaymentOutcome.Declined(it.userMessage(context = "start the payment",
                    fallback = strings.get(R.string.cust_co_payment_start_failed)))
            }
            when (outcome) {
                PaymentOutcome.Captured, is PaymentOutcome.Unconfirmed -> {
                    // Unconfirmed means Razorpay took the money and our server will hear it
                    // from the webhook; offering "pay again" here would charge twice.
                    cart.clear()
                    _state.value = _state.value.copy(paying = false, paymentCaptured = true,
                        awaitingPaymentFor = null,
                        paymentProblem = (outcome as? PaymentOutcome.Unconfirmed)?.message)
                    onDone(order)
                }
                is PaymentOutcome.Declined -> _state.value = _state.value.copy(
                    paying = false, awaitingPaymentFor = order, paymentProblem = outcome.message)
            }
        }
    }
}

data class CheckoutUiState(
    val loading: Boolean = false,
    val placing: Boolean = false,
    val addresses: List<AddressResponse> = emptyList(),
    val selectedAddress: AddressResponse? = null,
    val paymentMethod: String = "UPI",
    val placedOrder: OrderResponse? = null,
    val quote: OrderQuoteResponse? = null,
    val quoting: Boolean = false,
    val paymentCaptured: Boolean = false,
    val walletBalance: java.math.BigDecimal? = null,
    val paying: Boolean = false,
    /** Placed but not paid: the button retries the payment, it doesn't place again. */
    val awaitingPaymentFor: OrderResponse? = null,
    /** Why the last online payment didn't complete, in words for the customer. */
    val paymentProblem: String? = null,
    val error: String? = null
)
