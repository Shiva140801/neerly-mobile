package com.neerly.mobile.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the Home tab. Sequences three parallel-friendly calls on load:
 *   1. `GET /customer/addresses`   — primary address drives the pincode
 *   2. `GET /customer/vendors`     — nearby vendors for the active pincode
 *   3. `GET /customer/orders`      — active-orders strip (non-terminal states)
 *   4. `GET /customer/wallet`      — balance chip
 *
 * Errors surface through the inline `error` flag; the UI shows a retry card.
 */
@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val addresses = repo.addresses()
                val primary = addresses.firstOrNull { it.isPrimary } ?: addresses.firstOrNull()
                val pincode = primary?.pincode ?: "500032"
                val vendors = repo.vendors(pincode)
                val active = runCatching { repo.activeOrders() }.getOrDefault(emptyList())
                val wallet = runCatching { repo.wallet() }.getOrNull()
                HomeUiState(
                    loading = false,
                    primaryAddress = primary,
                    vendors = vendors,
                    activeOrders = active,
                    wallet = wallet
                )
            }.onSuccess { _state.value = it }
             .onFailure { _state.value = HomeUiState(loading = false, error = it.message ?: "Load failed") }
        }
    }
}

data class HomeUiState(
    val loading: Boolean = true,
    val primaryAddress: AddressResponse? = null,
    val vendors: List<VendorCardResponse> = emptyList(),
    val activeOrders: List<OrderResponse> = emptyList(),
    val wallet: WalletResponse? = null,
    val error: String? = null
)
