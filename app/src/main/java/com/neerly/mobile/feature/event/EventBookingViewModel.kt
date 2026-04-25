package com.neerly.mobile.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.CreateEventBookingRequest
import com.neerly.mobile.data.dto.EventItemInput
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Lean V1 event-booking wizard. We don't yet do multi-vendor cross-quote;
 * customer picks one vendor, picks products + quantities, names the event,
 * and the backend produces the quote. Vendor confirms in the vendor app —
 * status returns as PENDING_VENDOR_CONFIRM.
 *
 * Steps:
 *   0 — vendor (filtered by primary pincode, same logic as subscription wiz)
 *   1 — items (multi-select from catalog with per-product qty)
 *   2 — date+time + guest count + chilling lead hours + notes
 *   3 — address
 *   4 — review + submit
 */
@HiltViewModel
class EventBookingViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventBookingUiState())
    val state: StateFlow<EventBookingUiState> = _state.asStateFlow()

    init {
        // 24h ahead default keeps the @Future validator happy.
        val start = Instant.now().plus(1, ChronoUnit.DAYS)
        _state.value = _state.value.copy(
            eventStart = start,
            eventEnd = start.plus(3, ChronoUnit.HOURS)
        )
        viewModelScope.launch {
            runCatching {
                val addrs = repo.addresses()
                val primary = addrs.firstOrNull { it.isPrimary } ?: addrs.firstOrNull()
                val vendors = primary?.let { repo.vendors(it.pincode) } ?: emptyList()
                Triple(addrs, primary, vendors)
            }
                .onSuccess { (addrs, primary, vendors) ->
                    _state.value = _state.value.copy(
                        loading = false,
                        addresses = addrs,
                        selectedAddress = primary,
                        vendors = vendors
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }

    fun pickVendor(v: VendorCardResponse) {
        _state.value = _state.value.copy(
            selectedVendor = v, products = emptyList(),
            loadingProducts = true, itemQty = emptyMap(), step = 1
        )
        viewModelScope.launch {
            runCatching { repo.vendorProducts(v.id).filter { it.status == "AVAILABLE" } }
                .onSuccess { _state.value = _state.value.copy(products = it, loadingProducts = false) }
                .onFailure { _state.value = _state.value.copy(loadingProducts = false, error = it.message) }
        }
    }

    fun setQty(productId: String, qty: Int) {
        val sanitized = qty.coerceIn(0, 500)
        val map = _state.value.itemQty.toMutableMap()
        if (sanitized == 0) map.remove(productId) else map[productId] = sanitized
        _state.value = _state.value.copy(itemQty = map)
    }

    fun goToDetails() {
        if (_state.value.itemQty.isEmpty()) return
        _state.value = _state.value.copy(step = 2)
    }

    fun setEventName(v: String) { _state.value = _state.value.copy(eventName = v.take(200)) }
    fun setExpectedGuests(v: String) {
        val parsed = v.toIntOrNull()
        _state.value = _state.value.copy(expectedGuests = parsed?.takeIf { it >= 1 })
    }
    fun setChillingHours(v: Int) { _state.value = _state.value.copy(chillingLeadHours = v.coerceIn(0, 48)) }
    fun setStart(t: Instant) {
        // Keep duration if end > start, otherwise set end = start + 3h
        val cur = _state.value
        val newEnd = if (cur.eventEnd.isAfter(t)) cur.eventEnd else t.plus(3, ChronoUnit.HOURS)
        _state.value = cur.copy(eventStart = t, eventEnd = newEnd)
    }
    fun setEnd(t: Instant) {
        val cur = _state.value
        if (t.isAfter(cur.eventStart)) _state.value = cur.copy(eventEnd = t)
    }
    fun setNotes(v: String) { _state.value = _state.value.copy(customerNotes = v.take(1000)) }

    fun goToAddress() {
        if (_state.value.eventEnd.isBefore(_state.value.eventStart)) return
        _state.value = _state.value.copy(step = 3)
    }

    fun pickAddress(a: AddressResponse) {
        _state.value = _state.value.copy(selectedAddress = a, step = 4)
    }

    fun submit(onCreated: (String) -> Unit) {
        val s = _state.value
        val vendor = s.selectedVendor ?: return
        val addr = s.selectedAddress ?: return
        if (s.itemQty.isEmpty()) return
        _state.value = s.copy(submitting = true, error = null)
        val items = s.itemQty.map { (productId, qty) -> EventItemInput(productId, qty) }
        val req = CreateEventBookingRequest(
            vendorId = vendor.id,
            addressId = addr.id,
            eventName = s.eventName.ifBlank { null },
            expectedGuests = s.expectedGuests,
            eventStart = s.eventStart.toString(),
            eventEnd = s.eventEnd.toString(),
            chillingLeadHours = s.chillingLeadHours,
            items = items,
            customerNotes = s.customerNotes.ifBlank { null }
        )
        viewModelScope.launch {
            runCatching { repo.createEventBooking(req) }
                .onSuccess {
                    _state.value = _state.value.copy(submitting = false)
                    onCreated(it.id)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        submitting = false,
                        error = it.message ?: "Could not create booking"
                    )
                }
        }
    }

    fun back() {
        val s = _state.value
        if (s.step > 0) _state.value = s.copy(step = s.step - 1)
    }
}

data class EventBookingUiState(
    val loading: Boolean = true,
    val loadingProducts: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,

    /** 0 vendor, 1 items, 2 details, 3 address, 4 review */
    val step: Int = 0,

    val vendors: List<VendorCardResponse> = emptyList(),
    val selectedVendor: VendorCardResponse? = null,
    val products: List<ProductResponse> = emptyList(),
    val itemQty: Map<String, Int> = emptyMap(),

    val eventName: String = "",
    val expectedGuests: Int? = null,
    val chillingLeadHours: Int = 6,
    val eventStart: Instant = Instant.now(),
    val eventEnd: Instant = Instant.now(),
    val customerNotes: String = "",

    val addresses: List<AddressResponse> = emptyList(),
    val selectedAddress: AddressResponse? = null
)
