package com.neerly.mobile.feature.customer

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.core.util.ConnectivityObserver
import com.neerly.mobile.core.util.userMessage
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
    private val repo: CustomerRepository,
    private val connectivity: ConnectivityObserver,
    private val cart: com.neerly.mobile.data.cart.CartStore,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Drives the offline banner and the cold-start offline screen. */
    val isOnline: StateFlow<Boolean> = connectivity.isOnline

    init {
        refresh()
    }

    /** Your usual → into the cart. */
    fun orderUsual(): Boolean {
        val o = _state.value.usual ?: return false
        val vendorId = o.vendorId ?: return false
        o.items.forEachIndexed { i, it ->
            val item = com.neerly.mobile.data.cart.CartItem(it.productId, it.productName, it.unitPrice, it.quantity,
                keepContainer = it.containerMode != "TRANSFER_AND_RETURN")
            if (i == 0) cart.replaceWithNewVendor(vendorId, o.vendorName ?: strings.get(R.string.cust_your_shop), item)
            else cart.addItem(vendorId, o.vendorName ?: strings.get(R.string.cust_your_shop), item)
        }
        return o.items.isNotEmpty()
    }

    fun refresh() {
        connectivity.refresh()
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val addresses = repo.addresses()
                val primary = addresses.firstOrNull { it.isPrimary } ?: addresses.firstOrNull()
                val pincode = primary?.pincode ?: "500032"
                val vendors = repo.vendors(pincode)
                val active = runCatching { repo.activeOrders() }.getOrDefault(emptyList())
                val wallet = runCatching { repo.wallet() }.getOrNull()
                // "Your usual": the last delivered order, one tap to repeat it.
                val usual = runCatching { repo.myOrders(0, 10) }.getOrDefault(emptyList()).firstOrNull { it.status == "DELIVERED" }
                // Tomorrow's subscription strip.
                val nextSub = runCatching { repo.subscriptions() }.getOrDefault(emptyList())
                    .filter { it.status == "ACTIVE" && it.nextDeliveryDate != null }.minByOrNull { it.nextDeliveryDate!! }
                val name = runCatching { repo.me().displayName }.getOrNull()
                HomeUiState(
                    loading = false,
                    primaryAddress = primary,
                    vendors = vendors,
                    activeOrders = active,
                    wallet = wallet,
                    usual = usual,
                    nextSubscription = nextSub,
                    name = name
                )
            }.onSuccess { _state.value = it }
             .onFailure {
                 _state.value = HomeUiState(
                     loading = false,
                     error = it.userMessage(
                         context = "customer home",
                         fallback = strings.get(R.string.cust_home_load_failed)
                     )
                 )
             }
        }
    }
}

data class HomeUiState(
    val loading: Boolean = true,
    val primaryAddress: AddressResponse? = null,
    val vendors: List<VendorCardResponse> = emptyList(),
    val activeOrders: List<OrderResponse> = emptyList(),
    val wallet: WalletResponse? = null,
    val usual: com.neerly.mobile.data.dto.OrderResponse? = null,
    val nextSubscription: com.neerly.mobile.data.dto.SubscriptionResponse? = null,
    val name: String? = null,
    val error: String? = null
)
