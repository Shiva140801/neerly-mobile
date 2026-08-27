package com.neerly.mobile.feature.checkout

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
import javax.inject.Inject

/**
 * Drives the checkout flow:
 *   1. Load addresses (defaults to primary)
 *   2. User picks payment method + slot (default NOW)
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
    private val cart: CartStore
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    init {
        loadAddresses()
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
                }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun selectAddress(id: String) {
        val a = _state.value.addresses.firstOrNull { it.id == id }
        _state.value = _state.value.copy(selectedAddress = a)
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
        val snap = cart.snapshot
        val address = s.selectedAddress
        if (address == null) {
            _state.value = s.copy(error = "Please pick a delivery address")
            return
        }
        if (snap.isEmpty) {
            _state.value = s.copy(error = "Cart is empty")
            return
        }
        val vendorId = snap.vendorId ?: run {
            _state.value = s.copy(error = "Cart vendor missing")
            return
        }
        _state.value = s.copy(placing = true, error = null)
        viewModelScope.launch {
            runCatching {
                api.placeOrder(
                    PlaceOrderRequest(
                        vendorId = vendorId,
                        addressId = address.id,
                        items = snap.items.map {
                            OrderItemRequest(
                                productId = it.productId,
                                quantity = it.quantity,
                                keepContainer = it.keepContainer
                            )
                        },
                        promoCode = snap.promoCode,
                        slotRequested = s.slot
                    )
                )
            }
                .onSuccess {
                    _state.value = _state.value.copy(placing = false, placedOrder = it)
                    onSuccess(it)
                }
                .onFailure {
                    _state.value = _state.value.copy(placing = false, error = it.message ?: "Place order failed")
                }
        }
    }

    /**
     * Server round-trip 2 — initiates the payment. UI then launches Razorpay
     * (for UPI/CARD) or jumps straight to confirmation (for COD).
     */
    fun initiatePayment(order: OrderResponse, onReady: (InitiatePaymentRequest, String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                api.initiatePayment(
                    InitiatePaymentRequest(
                        orderId = order.id,
                        amount = order.totalAmount,
                        method = _state.value.paymentMethod
                    )
                )
            }
                .onSuccess { init ->
                    onReady(
                        InitiatePaymentRequest(order.id, order.totalAmount, _state.value.paymentMethod),
                        init.razorpayOrderId
                    )
                    if (_state.value.paymentMethod == "COD") {
                        cart.clear()
                    }
                }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message ?: "Payment init failed")
                }
        }
    }

    /** Call after Razorpay success callback. */
    fun onPaymentCaptured() {
        cart.clear()
        _state.value = _state.value.copy(paymentCaptured = true)
    }
}

data class CheckoutUiState(
    val loading: Boolean = false,
    val placing: Boolean = false,
    val addresses: List<AddressResponse> = emptyList(),
    val selectedAddress: AddressResponse? = null,
    val paymentMethod: String = "UPI",
    val slot: String = "NOW",
    val placedOrder: OrderResponse? = null,
    val paymentCaptured: Boolean = false,
    val error: String? = null
)
