package com.neerly.mobile.feature.vendor.compliance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.ComplianceDocResponse
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorComplianceViewModel @Inject constructor(
    private val repo: VendorRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VendorComplianceUiState())
    val state: StateFlow<VendorComplianceUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.complianceDocs() }
                .onSuccess { _state.value = VendorComplianceUiState(loading = false, docs = it) }
                .onFailure { _state.value = VendorComplianceUiState(loading = false, error = it.message) }
        }
    }
}

data class VendorComplianceUiState(
    val loading: Boolean = true,
    val docs: List<ComplianceDocResponse> = emptyList(),
    val error: String? = null
)
