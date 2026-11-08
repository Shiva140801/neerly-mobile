package com.neerly.mobile.feature.vendor.bank

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.AddVendorBankAccountRequest
import com.neerly.mobile.data.dto.VendorBankAccountResponse
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.vendor.money.VendorMoneyApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Boards V04 / V33 — where payouts go. One active account per vendor; saving a new one
 * replaces the old server-side. The backend doesn't run a penny-drop yet (it marks a saved
 * account "MANUAL" straight away), so the screen calls it "saved", never "verified".
 */
@HiltViewModel
class VendorBankViewModel @Inject constructor(
    private val repo: VendorRepository,
    private val money: VendorMoneyApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(VendorBankUiState())
    val state: StateFlow<VendorBankUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.bank() }
                .onSuccess { cur ->
                    val history = runCatching { money.bankHistory() }.getOrDefault(emptyList()).filter { it.id != cur?.id }
                    _state.value = _state.value.copy(loading = false, current = cur, history = history, editing = cur == null)
                }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("vendor bank", strings.get(R.string.vend_err_load_bank))) }
        }
    }

    fun edit(on: Boolean) { _state.value = _state.value.copy(editing = on, error = null, saved = false) }

    fun add(req: AddVendorBankAccountRequest, confirmAccount: String) {
        val problem = validate(req, confirmAccount)
        if (problem != null) { _state.value = _state.value.copy(error = strings.get(problem)); return }
        _state.value = _state.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            runCatching { repo.addBank(req) }
                .onSuccess {
                    _state.value = _state.value.copy(submitting = false, editing = false, saved = true)
                    refresh()
                }
                .onFailure { _state.value = _state.value.copy(submitting = false,
                    error = it.userMessage("save vendor bank", strings.get(R.string.vend_err_save_bank))) }
        }
    }

    companion object {
        /** The first problem with the form, as a string id; null when it can be sent. */
        @androidx.annotation.StringRes
        fun validate(req: AddVendorBankAccountRequest, confirmAccount: String): Int? = when {
            req.accountHolderName.trim().length < 3 -> R.string.vend_bv_name
            !req.accountNumber.matches(Regex("^\\d{9,18}$")) -> R.string.vend_bv_number
            req.accountNumber != confirmAccount -> R.string.vend_bv_match
            !req.ifsc.matches(Regex("^[A-Z]{4}0[A-Z0-9]{6}$")) -> R.string.vend_bv_ifsc
            else -> null
        }

        /** What we can honestly say about an account: only a real test deposit counts as checked. */
        fun checked(a: VendorBankAccountResponse): Boolean =
            a.verifiedAt != null && a.verificationMethod != null && a.verificationMethod != "MANUAL"
    }
}

data class VendorBankUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val current: VendorBankAccountResponse? = null,
    val history: List<VendorBankAccountResponse> = emptyList(),
    val editing: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)
