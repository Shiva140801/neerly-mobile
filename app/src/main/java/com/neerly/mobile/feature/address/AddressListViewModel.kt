package com.neerly.mobile.feature.address

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.neerly.mobile.core.util.userMessage
import javax.inject.Inject

@HiltViewModel
class AddressListViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(AddressListUiState())
    val state: StateFlow<AddressListUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.addresses() }
                .onSuccess { list ->
                    _state.value = _state.value.copy(addresses = list, loading = false, error = null)
                    // Board AddressEdit: deleting an address a live plan delivers to pauses that plan — warn first.
                    val plans = runCatching { repo.subscriptions() }.getOrNull().orEmpty()
                        .filter { it.status in LIVE_PLAN && it.addressId != null }
                    _state.value = _state.value.copy(plansByAddress = plans.groupBy { it.addressId!! }
                        .mapValues { (_, p) -> p.map { it.vendorName ?: it.productName ?: "" }.filter(String::isNotBlank) })
                    // "2 suppliers deliver here" (board 34): one lookup per distinct pincode.
                    val counts = list.map { it.pincode }.distinct().associateWith { pin ->
                        runCatching { repo.vendors(pin).size }.getOrNull()
                    }.filterValues { it != null }.mapValues { it.value!! }
                    _state.value = _state.value.copy(suppliersByPincode = counts)
                }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage(context = "addresses", fallback = strings.get(R.string.cust_addr_list_failed))) }
        }
    }

    fun setPrimary(id: String) {
        viewModelScope.launch {
            runCatching { repo.setPrimary(id) }.onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage(context = "set default address",
                    fallback = strings.get(R.string.cust_addr_default_failed))) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { repo.deleteAddress(id) }.onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage(context = "delete address",
                    fallback = strings.get(R.string.cust_addr_remove_failed))) }
        }
    }
}

private val LIVE_PLAN = setOf("ACTIVE", "PAUSED", "PENDING_MANDATE")

data class AddressListUiState(
    val loading: Boolean = true,
    val addresses: List<AddressResponse> = emptyList(),
    val suppliersByPincode: Map<String, Int> = emptyMap(),
    /** addressId → names of active/paused plans delivering there (shop name, else product). */
    val plansByAddress: Map<String, List<String>> = emptyMap(),
    val error: String? = null
)
