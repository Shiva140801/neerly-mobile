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
    private val strings: com.neerly.mobile.core.util.Strings,
    /** Boards PaymentPending / PaymentFailed; null in tests that don't exercise them. */
    private val payments: PaymentStatusApi? = null
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
                    // The cart's pick wins; otherwise the primary address.
                    val picked = cart.snapshot.addressId?.let { id -> addrs.firstOrNull { it.id == id } }
                    val primary = picked ?: addrs.firstOrNull { it.isPrimary } ?: addrs.firstOrNull()
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
        cart.setAddress(id)
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
        _state.value = _state.value.copy(paying = true, error = null, paymentProblem = null, paymentPage = null)
        pageDone = onDone
        viewModelScope.launch {
            var paymentId: String? = null
            val outcome = runCatching {
                // The amount is only echoed back: the backend charges the order's own total
                // and refuses anything else.
                val init = api.initiatePayment(
                    InitiatePaymentRequest(orderId = order.id, amount = order.totalAmount,
                        method = _state.value.paymentMethod)
                )
                paymentId = init.paymentId
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
                is PaymentOutcome.Declined -> {
                    _state.value = _state.value.copy(
                        paying = false, awaitingPaymentFor = order, paymentProblem = outcome.message)
                    paymentId?.let { showPaymentPage(order, it, outcome) }
                }
            }
        }
    }

    private var pageDone: ((OrderResponse) -> Unit)? = null
    private var pollJob: Job? = null

    /**
     * Razorpay closed unpaid. A UPI request the customer closed may still be approved in
     * their UPI app (board PaymentPending); a bank refusal gets its own page with the
     * bank's reason and other ways to pay (board PaymentFailed). Anything else — no
     * internet, or no word from Razorpay — stays the inline note under the button.
     */
    private suspend fun showPaymentPage(order: OrderResponse, paymentId: String, outcome: PaymentOutcome.Declined) {
        val api = payments ?: return
        val code = outcome.code ?: return
        if (code == NETWORK_ERROR) return
        val bank = bankReason(outcome.description)
        val method = _state.value.paymentMethod
        if (code == PAYMENT_CANCELED && method == "UPI") {
            // Not a failure yet: the collect request may still be approved, so nothing is
            // reported — the webhook decides.
            val view = runCatching { api.status(paymentId) }.getOrNull()
            _state.value = _state.value.copy(paymentPage = PaymentPage.Pending(order, paymentId, view?.holdUntil))
            startPolling(order, paymentId)
            return
        }
        // The customer closed a card/wallet sheet themselves: nothing was declined, so no
        // "your bank declined" page — they stay on checkout with "Payment cancelled".
        if (code == PAYMENT_CANCELED) return
        val view = runCatching { api.clientError(paymentId, ClientPaymentError(code, bank)) }.getOrNull()
        _state.value = _state.value.copy(paymentPage =
            PaymentPage.Failed(order, paymentId, method, meaningful(view?.failureReason) ?: bank, view?.holdUntil))
    }

    /** Pending: the server hears the UPI app's answer through the webhook — check every few seconds. */
    private fun startPolling(order: OrderResponse, paymentId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (_state.value.paymentPage is PaymentPage.Pending) {
                checkPayment(order, paymentId)
                kotlinx.coroutines.delay(POLL_MS)
            }
        }
    }

    /** "I've approved it" — look now instead of waiting for the next poll. */
    fun checkPaymentNow() {
        val page = _state.value.paymentPage as? PaymentPage.Pending ?: return
        viewModelScope.launch { checkPayment(page.order, page.paymentId) }
    }

    private suspend fun checkPayment(order: OrderResponse, paymentId: String) {
        val view = runCatching { payments?.status(paymentId) }.getOrNull() ?: return
        when (view.state) {
            "PAID" -> {
                cart.clear()
                _state.value = _state.value.copy(paymentPage = null, awaitingPaymentFor = null,
                    paymentProblem = null, paymentCaptured = true)
                pageDone?.invoke(order)
            }
            "FAILED" -> if (_state.value.paymentPage is PaymentPage.Pending) {
                _state.value = _state.value.copy(paymentPage =
                    PaymentPage.Failed(order, paymentId, view.method, meaningful(view.failureReason), view.holdUntil))
            }
        }
    }

    /** PaymentFailed / PaymentPending "Pay another way": back to the methods, same order. */
    fun closePaymentPage() {
        pollJob?.cancel()
        _state.value = _state.value.copy(paymentPage = null)
    }

    /** PaymentFailed: try again with [method] — the same order, a new payment. */
    fun retryWith(activity: Activity, method: String) {
        val order = _state.value.awaitingPaymentFor ?: return
        pollJob?.cancel()
        _state.value = _state.value.copy(paymentMethod = method, paymentPage = null)
        payOnline(activity, order, pageDone ?: {})
    }

    /** PaymentFailed "Cash on delivery": the order becomes a cash order and goes ahead. */
    fun payCashInstead() {
        val order = _state.value.awaitingPaymentFor ?: return
        val api = payments ?: return
        _state.value = _state.value.copy(paying = true, paymentProblem = null)
        viewModelScope.launch {
            runCatching { api.payCash(order.id).also { if (!it.isSuccessful) throw retrofit2.HttpException(it) } }
                .onSuccess {
                    pollJob?.cancel()
                    cart.clear()
                    _state.value = _state.value.copy(paying = false, paymentPage = null, awaitingPaymentFor = null,
                        paymentMethod = "COD")
                    pageDone?.invoke(order.copy(paymentMethod = "COD"))
                }
                .onFailure {
                    _state.value = _state.value.copy(paying = false,
                        paymentProblem = it.userMessage(context = "pay cash instead", fallback = strings.get(R.string.cust_payst_cash_failed)))
                }
        }
    }

    /** PaymentPending "Cancel": drop the unpaid order; the cart stays for another go. */
    fun cancelUnpaid(onCancelled: () -> Unit) {
        val order = _state.value.awaitingPaymentFor ?: return
        pollJob?.cancel()
        viewModelScope.launch {
            runCatching { api.cancelOrder(order.id, com.neerly.mobile.data.dto.CancelOrderRequest("Payment not completed")) }
            _state.value = _state.value.copy(paymentPage = null, awaitingPaymentFor = null, paymentProblem = null)
            requestId = UUID.randomUUID().toString()
            onCancelled()
        }
    }

    companion object {
        /** Razorpay Checkout result codes. */
        const val PAYMENT_CANCELED = 0
        const val NETWORK_ERROR = 2
        const val POLL_MS = 5_000L

        /** Razorpay's error is a JSON string; the bank's words are error.description. */
        fun bankReason(raw: String?): String? {
            val text = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
            if (!text.startsWith("{")) return meaningful(text)
            return runCatching {
                val err = org.json.JSONObject(text).optJSONObject("error")
                meaningful(err?.optString("description"))
            }.getOrNull()
        }

        /** Razorpay's web checkout sometimes sends the JS words "undefined"/"null" as the reason. */
        fun meaningful(text: String?): String? =
            text?.trim()?.takeIf { it.isNotBlank() && it.lowercase() !in setOf("undefined", "null", "nan") }
    }
}

/** Boards PaymentPending and PaymentFailed, shown over checkout. */
sealed interface PaymentPage {
    val order: OrderResponse
    val paymentId: String
    /** Waiting for the customer to approve the UPI request in their UPI app. */
    data class Pending(override val order: OrderResponse, override val paymentId: String, val holdUntil: String?) : PaymentPage
    data class Failed(override val order: OrderResponse, override val paymentId: String, val method: String,
                      val bankSaid: String?, val holdUntil: String?) : PaymentPage
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
    /** A full page over checkout: PaymentPending or PaymentFailed. */
    val paymentPage: PaymentPage? = null,
    val error: String? = null
)
