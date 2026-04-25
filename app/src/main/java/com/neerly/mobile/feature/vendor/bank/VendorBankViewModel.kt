package com.neerly.mobile.feature.vendor.bank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.AddVendorBankAccountRequest
import com.neerly.mobile.data.dto.VendorBankAccountResponse
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Vendor → Settings → Bank account screen. One active account per
 * vendor (DB partial-unique enforces it). Switching banks deactivates the
 * old row server-side; the user re-enters the new one.
 *
 * Penny-drop is a stub for V1 — `verify()` flips the row to verified
 * automatically. V1.1 wires Razorpay X /fund_account/validations.
 */
@HiltViewModel
class VendorBankViewModel @Inject constructor(
    private val repo: VendorRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VendorBankUiState())
    val state: StateFlow<VendorBankUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.bank() }
                .onSuccess { _state.value = VendorBankUiState(loading = false, current = it) }
                .onFailure { _state.value = VendorBankUiState(loading = false, error = it.message) }
        }
    }

    fun add(req: AddVendorBankAccountRequest) {
        if (!validate(req)) return
        _state.value = _state.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            runCatching { repo.addBank(req) }
                .onSuccess {
                    _state.value = VendorBankUiState(submitting = false, current = it)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        submitting = false,
                        error = it.message ?: "Couldn't save the account"
                    )
                }
        }
    }

    fun verify() {
        viewModelScope.launch {
            runCatching { repo.verifyBank() }
                .onSuccess { _state.value = _state.value.copy(current = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    private fun validate(req: AddVendorBankAccountRequest): Boolean {
        val ifscOk = req.ifsc.matches(Regex("^[A-Z]{4}0[A-Z0-9]{6}$"))
        val accOk = req.accountNumber.matches(Regex("^\\d{9,18}$"))
        val nameOk = req.accountHolderName.length >= 3
        if (!ifscOk) {
            _state.value = _state.value.copy(error = "IFSC must be 11 chars (e.g. HDFC0001234)")
            return false
        }
        if (!accOk) {
            _state.value = _state.value.copy(error = "Account number must be 9-18 digits")
            return false
        }
        if (!nameOk) {
            _state.value = _state.value.copy(error = "Account holder name is required")
            return false
        }
        return true
    }
}

data class VendorBankUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val current: VendorBankAccountResponse? = null,
    val error: String? = null
)
