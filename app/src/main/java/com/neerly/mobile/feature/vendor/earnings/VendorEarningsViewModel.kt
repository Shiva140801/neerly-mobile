package com.neerly.mobile.feature.vendor.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.EarningsSummary
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorEarningsViewModel @Inject constructor(
    private val repo: VendorRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VendorEarningsUiState())
    val state: StateFlow<VendorEarningsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.earnings() }
                .onSuccess { _state.value = VendorEarningsUiState(loading = false, summary = it) }
                .onFailure { _state.value = VendorEarningsUiState(loading = false, error = it.message) }
        }
    }
}

data class VendorEarningsUiState(
    val loading: Boolean = true,
    val summary: EarningsSummary? = null,
    val error: String? = null
)
