package com.neerly.mobile.feature.address

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.CreateAddressRequest
import com.neerly.mobile.data.dto.UpdateAddressRequest
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
 * GPS picker is a stub for V1: we default to a Hyderabad lat/lng that the
 * backend will accept, and surface a "Use my location" hook the screen layer
 * can replace with the real Google Maps Compose pin once the API key is wired.
 */
@HiltViewModel
class AddressFormViewModel @Inject constructor(
    private val repo: CustomerRepository,
    saved: SavedStateHandle
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
                        _state.value = _state.value.copy(error = "Address not found", loading = false)
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
                        latitude = row.latitude, longitude = row.longitude,
                        deliveryInstructions = row.deliveryInstructions.orEmpty(),
                        hasLift = row.hasLift,
                        floorNumber = row.floorNumber?.toString().orEmpty(),
                        securityContactName = row.securityContactName.orEmpty(),
                        securityContactPhone = row.securityContactPhone.orEmpty(),
                        setAsPrimary = row.isPrimary
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }

    fun update(transform: AddressFormUiState.() -> AddressFormUiState) {
        _state.value = _state.value.transform()
    }

    /**
     * Stub: in V1 we accept the user-typed address and stamp it with a Hyderabad
     * centroid. Replace with Maps SDK reverse-geocode in the next sprint.
     */
    fun useCurrentLocation() {
        _state.value = _state.value.copy(
            latitude = HYDERABAD_LAT,
            longitude = HYDERABAD_LNG,
            locationCaptured = true
        )
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.isValid()) {
            _state.value = s.copy(error = "Fill label, flat, building, street, pincode")
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
                    _state.value = _state.value.copy(saving = false, error = it.message ?: "Save failed")
                }
        }
    }

}

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
    val setAsPrimary: Boolean = false
) {
    fun isValid(): Boolean =
        label.isNotBlank() &&
            flatNumber.isNotBlank() &&
            buildingName.isNotBlank() &&
            street.isNotBlank() &&
            pincode.matches(Regex("^[1-9]\\d{5}$"))
}
