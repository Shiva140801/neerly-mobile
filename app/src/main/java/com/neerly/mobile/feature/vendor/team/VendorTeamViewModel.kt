package com.neerly.mobile.feature.vendor.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.VendorTeamMember
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Vendor team management. Reads the active driver list and adds/removes by phone.
 *
 * Add validation:
 *   • Phone must be Indian E.164 (+91XXXXXXXXXX). Backend re-validates.
 *   • Driver must already be a Neerly user with the DRIVER role; the screen
 *     surfaces a dedicated error for the unregistered-driver 404.
 */
@HiltViewModel
class VendorTeamViewModel @Inject constructor(
    private val repo: VendorRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VendorTeamUiState())
    val state: StateFlow<VendorTeamUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.team() }
                .onSuccess { _state.value = VendorTeamUiState(loading = false, members = it) }
                .onFailure { _state.value = VendorTeamUiState(loading = false, error = it.message) }
        }
    }

    fun add(phone: String, notes: String?) {
        if (!phone.matches(Regex("^\\+91[6-9]\\d{9}$"))) {
            _state.value = _state.value.copy(error = "Phone must be +91 followed by 10 digits starting 6-9")
            return
        }
        _state.value = _state.value.copy(adding = true, error = null)
        viewModelScope.launch {
            runCatching { repo.addDriver(phone, notes) }
                .onSuccess {
                    _state.value = _state.value.copy(adding = false)
                    refresh()
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        adding = false,
                        error = it.message ?: "Could not add driver"
                    )
                }
        }
    }

    fun remove(driverId: String) {
        viewModelScope.launch {
            runCatching { repo.removeDriver(driverId) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}

data class VendorTeamUiState(
    val loading: Boolean = true,
    val adding: Boolean = false,
    val members: List<VendorTeamMember> = emptyList(),
    val error: String? = null
)
