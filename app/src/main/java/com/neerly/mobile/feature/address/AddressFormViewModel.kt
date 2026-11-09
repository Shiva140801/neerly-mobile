package com.neerly.mobile.feature.address

import com.neerly.mobile.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.CreateAddressRequest
import com.neerly.mobile.data.dto.UpdateAddressRequest
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.neerly.mobile.core.util.userMessage
import javax.inject.Inject

/**
 * Drives both create and edit modes for [AddressFormScreen].
 *
 * Edit mode:
 *   * `addressId` arg is non-null → we fetch the row, hydrate the fields, and
 *     PATCH on save.
 * Create mode:
 *   * No `addressId` → blank form, POST on save.
 *
 * The pin comes from a GPS fix, a search pick, or dragging the map under the pin
 * ([pinMoved]); the street/pincode/city follow the pin unless the customer typed them.
 */
@HiltViewModel
class AddressFormViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val locator: AddressLocator,
    private val places: PlaceSearch,
    private val mapApi: AddressMapApi,
    saved: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val editingId: String? = saved["addressId"]

    private val _state = MutableStateFlow(AddressFormUiState(editing = editingId != null))
    val state: StateFlow<AddressFormUiState> = _state.asStateFlow()

    init {
        if (editingId != null) hydrate(editingId)
    }

    private fun hydrate(id: String) {
        viewModelScope.launch {
            runCatching { repo.addresses().firstOrNull { it.id == id } }
                .onSuccess { row ->
                    if (row == null) {
                        _state.value = _state.value.copy(error = strings.get(R.string.cust_addr_not_found), loading = false)
                        return@onSuccess
                    }
                    _state.value = _state.value.copy(
                        loading = false,
                        label = row.label,
                        flatNumber = row.flatNumber,
                        buildingName = row.buildingName,
                        street = row.street,
                        landmark = row.landmark.orEmpty(),
                        city = row.city,
                        pincode = row.pincode,
                        latitude = row.latitude, longitude = row.longitude, locationCaptured = true,
                        originalLat = row.latitude, originalLng = row.longitude,
                        deliveryInstructions = row.deliveryInstructions.orEmpty(),
                        hasLift = row.hasLift,
                        floorNumber = row.floorNumber?.toString().orEmpty(),
                        securityContactName = row.securityContactName.orEmpty(),
                        securityContactPhone = row.securityContactPhone.orEmpty(),
                        setAsPrimary = row.isPrimary
                    )
                    checkService()
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }

    fun update(transform: AddressFormUiState.() -> AddressFormUiState) {
        _state.value = _state.value.transform()
    }

    /** Real GPS fix, then fill whatever the customer hasn't typed from the reverse geocode. */
    fun useCurrentLocation() {
        _state.value = _state.value.copy(locating = true, locationError = null)
        viewModelScope.launch {
            val fix = locator.currentFix()
            if (fix == null) {
                _state.value = _state.value.copy(locating = false,
                    locationError = strings.get(R.string.cust_addr_no_location))
                return@launch
            }
            if (!inIndia(fix.first, fix.second)) {
                _state.value = _state.value.copy(locating = false, locationCaptured = false,
                    locationError = strings.get(R.string.cust_addr_outside_india))
                return@launch
            }
            applyPlace(fix.first, fix.second, locator.reverse(fix.first, fix.second))
            _state.value = _state.value.copy(locating = false)
            checkService()
        }
    }

    private var pinJob: Job? = null

    /**
     * The map settled under the pin after a drag (boards Address, AddressPin, AddressPinEdit).
     * The pin counts at once; the street/pincode follow after a short pause so a few quick
     * drags cost one reverse geocode, not one each.
     */
    fun pinMoved(lat: Double, lng: Double) {
        if (!inIndia(lat, lng)) {
            _state.value = _state.value.copy(locationError = strings.get(R.string.cust_addr_outside_india))
            return
        }
        _state.value = _state.value.copy(latitude = lat, longitude = lng, locationCaptured = true,
            locationError = null, confirmMove = false)
        pinJob?.cancel()
        pinJob = viewModelScope.launch {
            delay(PIN_SETTLE_MS)
            applyPlace(lat, lng, locator.reverse(lat, lng))
            checkService()
        }
    }

    /** A field the customer typed stays; one the last pin filled follows the new pin. */
    private fun applyPlace(lat: Double, lng: Double, place: FoundPlace?) {
        val cur = _state.value
        fun pick(typed: String, lastAuto: String?, found: String?) =
            if (typed.isBlank() || typed == lastAuto) found ?: typed else typed
        _state.value = cur.copy(
            locationCaptured = true, latitude = lat, longitude = lng,
            street = pick(cur.street, cur.autoStreet, place?.street),
            pincode = pick(cur.pincode, cur.autoPincode, place?.pincode),
            city = pick(cur.city, cur.autoCity ?: "Hyderabad", place?.city),
            autoStreet = place?.street, autoPincode = place?.pincode, autoCity = place?.city,
            foundAt = place?.line ?: cur.foundAt
        )
    }

    private val serviceCache = mutableMapOf<String, Serviceability>()

    private suspend fun serviceFor(pincode: String): Serviceability? =
        serviceCache[pincode] ?: runCatching { mapApi.serviceability(pincode) }.getOrNull()?.also { serviceCache[pincode] = it }

    /** "N suppliers deliver here" for the pin's pincode, and in edit mode which subscribed shops it loses. */
    private suspend fun checkService() {
        val pin = _state.value.pincode.trim()
        if (!PINCODE.matches(pin)) { _state.value = _state.value.copy(serviceCount = null, lostShops = emptyList()); return }
        _state.value = _state.value.copy(serviceChecking = true)
        val svc = serviceFor(pin)
        val lost = if (editingId != null && _state.value.pinChanged && svc != null) {
            val reach = svc.shops.map { it.id }.toSet()
            // Only judge by names when the list is complete (≤10 shops came back).
            if (svc.shopCount > svc.shops.size) emptyList()
            else runCatching { repo.subscriptions() }.getOrDefault(emptyList())
                .filter { it.addressId == editingId && it.status != "CANCELLED" && it.vendorId !in reach }
                .mapNotNull { it.vendorName }.distinct()
        } else emptyList()
        _state.value = _state.value.copy(serviceChecking = false, serviceCount = svc?.shopCount, lostShops = lost)
    }

    private var searchJob: Job? = null
    private var saved: List<com.neerly.mobile.data.dto.AddressResponse>? = null

    /** Board AddressAdd / AddressType — search a building, road or area; no live location needed. */
    fun onQuery(q: String) {
        _state.value = _state.value.copy(query = q.take(120))
        searchJob?.cancel()
        if (q.trim().length < MIN_QUERY) { _state.value = _state.value.copy(matches = emptyList(), searching = false); return }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _state.value = _state.value.copy(searching = true)
            val found = places.search(q.trim())
            val mine = saved ?: runCatching { repo.addresses() }.getOrDefault(emptyList()).also { saved = it }
            val rows = found.map { m ->
                val twin = mine.firstOrNull { it.id != editingId && metres(it.latitude, it.longitude, m.lat, m.lng) < SAME_PLACE_M }
                val svc = if (twin == null) m.pincode?.takeIf { PINCODE.matches(it) }?.let { serviceFor(it) } else null
                MatchRow(m, savedAs = twin?.label, serviceCount = svc?.shopCount)
            }
            _state.value = _state.value.copy(searching = false, matches = rows, searchedOnce = true)
        }
    }

    /** Picking a match moves the pin there; the customer then drags it onto the gate. */
    fun pickMatch(row: MatchRow) {
        val m = row.match
        searchJob?.cancel()
        _state.value = _state.value.copy(query = "", matches = emptyList(), searchedOnce = false, locationError = null, confirmMove = false)
        applyPlace(m.lat, m.lng, FoundPlace(m.street, m.pincode, m.city, listOf(m.title, m.subtitle).filter { it.isNotBlank() }.joinToString(", ")))
        viewModelScope.launch { checkService() }
    }

    /** Board AddressPinEdit — "Keep the old pin". */
    fun keepOldPin() {
        val s = _state.value
        val lat = s.originalLat ?: return; val lng = s.originalLng ?: return
        _state.value = s.copy(latitude = lat, longitude = lng, lostShops = emptyList(), confirmMove = false)
    }

    /** Board AddressPinEdit — "Use this spot" even though a subscribed shop doesn't reach it. */
    fun confirmMove(onDone: () -> Unit) {
        _state.value = _state.value.copy(moveConfirmed = true, confirmMove = false)
        save(onDone)
    }

    /** Board LocationDenied → "Type my address instead": the search box is the typed path (AddressType). */
    fun typeInstead() {
        _state.value = _state.value.copy(typing = true, locationError = null)
    }

    fun locationDenied() {
        _state.value = _state.value.copy(locationError = strings.get(R.string.cust_addr_location_denied))
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.isValid()) {
            _state.value = s.copy(error = if (!s.locationCaptured) strings.get(R.string.cust_addr_need_pin)
                else strings.get(R.string.cust_addr_fill_fields))
            return
        }
        // Moving a saved pin away from a subscribed shop pauses that subscription — ask first.
        if (s.editing && s.pinChanged && s.lostShops.isNotEmpty() && !s.moveConfirmed) {
            _state.value = s.copy(confirmMove = true)
            return
        }
        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                if (editingId != null) {
                    repo.updateAddress(
                        editingId,
                        UpdateAddressRequest(
                            label = s.label.trim(),
                            flatNumber = s.flatNumber.trim(),
                            buildingName = s.buildingName.trim(),
                            street = s.street.trim(),
                            landmark = s.landmark.trim().ifBlank { null },
                            city = s.city.trim().ifBlank { "Hyderabad" },
                            pincode = s.pincode.trim(),
                            latitude = s.latitude,
                            longitude = s.longitude,
                            deliveryInstructions = s.deliveryInstructions.trim().ifBlank { null },
                            floorNumber = s.floorNumber.trim().toIntOrNull(),
                            hasLift = s.hasLift,
                            securityContactName = s.securityContactName.trim().ifBlank { null },
                            securityContactPhone = s.securityContactPhone.trim().ifBlank { null }
                        )
                    )
                    // PATCH has no setAsPrimary field — primary flag flips via its own endpoint.
                    if (s.setAsPrimary) repo.setPrimary(editingId)
                } else {
                    repo.createAddress(
                        CreateAddressRequest(
                            label = s.label.trim(),
                            flatNumber = s.flatNumber.trim(),
                            buildingName = s.buildingName.trim(),
                            street = s.street.trim(),
                            landmark = s.landmark.trim().ifBlank { null },
                            city = s.city.trim().ifBlank { "Hyderabad" },
                            pincode = s.pincode.trim(),
                            latitude = s.latitude,
                            longitude = s.longitude,
                            deliveryInstructions = s.deliveryInstructions.trim().ifBlank { null },
                            floorNumber = s.floorNumber.trim().toIntOrNull(),
                            hasLift = s.hasLift,
                            securityContactName = s.securityContactName.trim().ifBlank { null },
                            securityContactPhone = s.securityContactPhone.trim().ifBlank { null },
                            setAsPrimary = s.setAsPrimary
                        )
                    )
                }
            }
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    onDone()
                }
                .onFailure {
                    _state.value = _state.value.copy(saving = false,
                        error = it.userMessage(context = "save address", fallback = strings.get(R.string.cust_addr_save_failed)))
                }
        }
    }

}

private const val PIN_SETTLE_MS = 400L
private const val SEARCH_DEBOUNCE_MS = 350L
private const val MIN_QUERY = 3
/** Within this distance a search match "is" a saved address. */
private const val SAME_PLACE_M = 150.0
private val PINCODE = Regex("^[1-9]\\d{5}$")

/** Same bounds the server enforces — say it here, not as a failed save. */
private fun inIndia(lat: Double, lng: Double) = lat in 6.0..37.5 && lng in 68.0..97.5

private fun metres(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
    val dLat = Math.toRadians(bLat - aLat); val dLng = Math.toRadians(bLng - aLng)
    val h = Math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(aLat)) * Math.cos(Math.toRadians(bLat)) * Math.sin(dLng / 2).let { it * it }
    return 2 * 6_371_000.0 * Math.asin(Math.sqrt(h))
}

/** A search result plus what we know about it for this customer. */
data class MatchRow(val match: PlaceMatch, val savedAs: String? = null, val serviceCount: Long? = null)

// Hussain Sagar / Hyderabad city centre — sane fallback pin that passes the
// backend's India-bounds lat/lng validation until the Maps picker is wired.
private const val HYDERABAD_LAT = 17.4239
private const val HYDERABAD_LNG = 78.4738

data class AddressFormUiState(
    val editing: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val locationCaptured: Boolean = false,
    val locating: Boolean = false,
    val locationError: String? = null,
    val foundAt: String? = null,
    val autoStreet: String? = null,
    val autoPincode: String? = null,
    val autoCity: String? = null,

    val label: String = "Home",
    val flatNumber: String = "",
    val buildingName: String = "",
    val street: String = "",
    val landmark: String = "",
    val city: String = "Hyderabad",
    val pincode: String = "",
    val latitude: Double = HYDERABAD_LAT,
    val longitude: Double = HYDERABAD_LNG,
    val deliveryInstructions: String = "",
    val hasLift: Boolean = false,
    val floorNumber: String = "",
    val securityContactName: String = "",
    val securityContactPhone: String = "",
    val setAsPrimary: Boolean = false,

    // Map pin picker, search and serviceability (WP-2).
    val query: String = "",
    val searching: Boolean = false,
    val searchedOnce: Boolean = false,
    val matches: List<MatchRow> = emptyList(),
    val serviceChecking: Boolean = false,
    val serviceCount: Long? = null,
    val originalLat: Double? = null,
    val originalLng: Double? = null,
    /** Subscribed shops (by name) that don't reach the moved pin. */
    val lostShops: List<String> = emptyList(),
    val confirmMove: Boolean = false,
    val moveConfirmed: Boolean = false,
    /** The customer chose to type the address (location off). */
    val typing: Boolean = false
) {
    /** Edit mode, and the pin is no longer where it was saved (more than a few metres). */
    val pinChanged: Boolean get() = originalLat != null && originalLng != null &&
        metres(originalLat, originalLng, latitude, longitude) > 5.0

    fun isValid(): Boolean =
        locationCaptured &&
            label.isNotBlank() &&
            flatNumber.isNotBlank() &&
            buildingName.isNotBlank() &&
            street.isNotBlank() &&
            pincode.matches(Regex("^[1-9]\\d{5}$"))
}
