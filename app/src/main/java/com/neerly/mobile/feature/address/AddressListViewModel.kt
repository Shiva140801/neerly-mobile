package com.neerly.mobile.feature.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddressListViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddressListUiState())
    val state: StateFlow<AddressListUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.addresses() }
                .onSuccess { _state.value = AddressListUiState(addresses = it, loading = false) }
                .onFailure { _state.value = AddressListUiState(loading = false, error = it.message) }
        }
    }

    fun setPrimary(id: String) {
        viewModelScope.launch {
            runCatching { repo.setPrimary(id) }.onSuccess { refresh() }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { repo.deleteAddress(id) }.onSuccess { refresh() }
        }
    }
}

data class AddressListUiState(
    val loading: Boolean = true,
    val addresses: List<AddressResponse> = emptyList(),
    val error: String? = null
)
