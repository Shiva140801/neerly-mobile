package com.neerly.mobile.feature.vendor.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.VendorProductRow
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorCatalogViewModel @Inject constructor(
    private val repo: VendorRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VendorCatalogUiState())
    val state: StateFlow<VendorCatalogUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.catalog() }
                .onSuccess { _state.value = VendorCatalogUiState(products = it, loading = false) }
                .onFailure { _state.value = VendorCatalogUiState(loading = false, error = it.message) }
        }
    }

    fun togglePause(product: VendorProductRow) {
        val nowPaused = product.status != "PAUSED"
        viewModelScope.launch {
            runCatching { repo.pauseProduct(product.id, nowPaused) }.onSuccess { load() }
        }
    }
}

data class VendorCatalogUiState(
    val loading: Boolean = true,
    val products: List<VendorProductRow> = emptyList(),
    val error: String? = null
)
