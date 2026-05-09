package com.neerly.mobile.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.CreateSubscriptionRequest
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Multi-step state machine that the [SubscriptionCreateScreen] drives.
 *
 * Steps (per Customer Flows PDF §SUB-NEW-01):
 *  1. Pick vendor (filtered by primary address pincode)
 *  2. Pick product (vendor's catalog, AVAILABLE only)
 *  3. Frequency + days-of-week + delivery slot + quantity
 *  4. Pick saved address
 *  5. Choose payment mode (UPI autopay vs Wallet) + confirm
 *
 * On confirm we POST CreateSubscriptionRequest. Status comes back as
 * PENDING_MANDATE for UPI autopay and ACTIVE for wallet.
 */
@HiltViewModel
class SubscriptionCreateViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionCreateUiState())
    val state: StateFlow<SubscriptionCreateUiState> = _state.asStateFlow()

    init { loadInitial() }

    private fun loadInitial() {
        viewModelScope.launch {
            runCatching {
                val addrs = repo.addresses()
                val primary = addrs.firstOrNull { it.isPrimary } ?: addrs.firstOrNull()
                val vendors = primary?.let { repo.vendors(it.pincode) } ?: emptyList()
                Pair(addrs, vendors)
            }
                .onSuccess { (addrs, vendors) ->
                    _state.value = _state.value.copy(
                        loading = false,
                        addresses = addrs,
                        selectedAddress = addrs.firstOrNull { it.isPrimary } ?: addrs.firstOrNull(),
                        vendors = vendors
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }

    fun pickVendor(v: VendorCardResponse) {
        _state.value = _state.value.copy(selectedVendor = v, products = emptyList(), loadingProducts = true)
        viewModelScope.launch {
            runCatching { repo.vendorProducts(v.id).filter { it.status == "AVAILABLE" } }
                .onSuccess { _state.value = _state.value.copy(products = it, loadingProducts = false, step = 1) }
                .onFailure { _state.value = _state.value.copy(loadingProducts = false, error = it.message) }
        }
    }

    fun pickProduct(p: ProductResponse) {
        _state.value = _state.value.copy(selectedProduct = p, step = 2)
    }

    fun setFrequency(f: String) {
        // CUSTOM keeps daysOfWeek; everything else clears it.
        val days = if (f == "CUSTOM") _state.value.daysOfWeek else emptyList()
        _state.value = _state.value.copy(frequency = f, daysOfWeek = days)
    }
    fun toggleDay(d: String) {
        val cur = _state.value.daysOfWeek.toMutableSet()
        if (!cur.add(d)) cur.remove(d)
        _state.value = _state.value.copy(daysOfWeek = cur.toList())
    }
    fun setSlot(s: String) { _state.value = _state.value.copy(slot = s) }
    fun setQuantity(q: Int) { _state.value = _state.value.copy(quantity = q.coerceIn(1, 20)) }

    fun confirmStep2() {
        val s = _state.value
        if (s.frequency.isBlank() || s.slot.isBlank()) return
        if (s.frequency == "CUSTOM" && s.daysOfWeek.isEmpty()) return
        _state.value = s.copy(step = 3)
    }

    fun pickAddress(a: AddressResponse) { _state.value = _state.value.copy(selectedAddress = a, step = 4) }

    fun setPaymentMode(mode: String) { _state.value = _state.value.copy(paymentMode = mode) }

    fun submit(onCreated: (String) -> Unit) {
        val s = _state.value
        val vendor = s.selectedVendor ?: return
        val product = s.selectedProduct ?: return
        val address = s.selectedAddress ?: return
        if (s.paymentMode.isBlank()) {
            _state.value = s.copy(error = "Pick a payment mode")
            return
        }
        _state.value = s.copy(submitting = true, error = null)
        val mandateMax: BigDecimal? = if (s.paymentMode == "UPI_AUTOPAY") {
            // Cap mandate at 30× monthly bill heuristic — covers daily + price hike headroom.
            (product.price * BigDecimal(s.quantity) * BigDecimal(45)).setScale(2, RoundingMode.HALF_UP)
        } else null
        val req = CreateSubscriptionRequest(
            vendorId = vendor.id,
            productId = product.id,
            frequency = s.frequency,
            daysOfWeek = s.daysOfWeek,
            quantity = s.quantity,
            deliverySlot = s.slot,
            addressId = address.id,
            paymentMethod = s.paymentMode,
            mandateMaxAmount = mandateMax
        )
        viewModelScope.launch {
            runCatching { repo.createSubscription(req) }
                .onSuccess { resp ->
                    _state.value = _state.value.copy(submitting = false)
                    onCreated(resp.id)
                }
                .onFailure {
                    _state.value = _state.value.copy(submitting = false, error = it.message ?: "Could not create")
                }
        }
    }

    fun back() {
        val s = _state.value
        if (s.step > 0) _state.value = s.copy(step = s.step - 1)
    }
}

data class SubscriptionCreateUiState(
    val loading: Boolean = true,
    val loadingProducts: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,

    /** 0 = vendor, 1 = product, 2 = frequency/slot, 3 = address, 4 = payment+confirm */
    val step: Int = 0,

    val vendors: List<VendorCardResponse> = emptyList(),
    val selectedVendor: VendorCardResponse? = null,
    val products: List<ProductResponse> = emptyList(),
    val selectedProduct: ProductResponse? = null,
    val frequency: String = "DAILY",
    val daysOfWeek: List<String> = emptyList(),
    val slot: String = "7-9AM",
    val quantity: Int = 1,
    val addresses: List<AddressResponse> = emptyList(),
    val selectedAddress: AddressResponse? = null,
    val paymentMode: String = "UPI_AUTOPAY"
) {
    val canSubmit: Boolean
        get() = selectedVendor != null && selectedProduct != null &&
            selectedAddress != null && frequency.isNotBlank() && slot.isNotBlank() &&
            paymentMode.isNotBlank()
}
