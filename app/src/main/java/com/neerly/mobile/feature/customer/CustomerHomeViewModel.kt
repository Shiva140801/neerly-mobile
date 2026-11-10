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
    private val strings: com.neerly.mobile.core.util.Strings,
    private val discovery: com.neerly.mobile.data.api.DiscoveryApi,
    private val api: com.neerly.mobile.data.api.NeerlyApi
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

    /** Tap a chip to filter; tap it again to clear. */
    fun toggleFilter(f: HomeFilter) {
        _state.value = _state.value.copy(filter = if (_state.value.filter == f) null else f)
    }

    /** Board Home: one-tap "Skip" on tomorrow's subscription strip. */
    fun skipNextDelivery() {
        val sub = _state.value.nextSubscription ?: return
        val date = sub.nextDeliveryDate ?: return
        if (_state.value.skipping) return
        _state.value = _state.value.copy(skipping = true, message = null)
        viewModelScope.launch {
            runCatching { repo.skipSubscription(sub.id, com.neerly.mobile.data.dto.SkipSubscriptionRequest(date.take(10))) }
                .onSuccess {
                    _state.value = _state.value.copy(skipping = false,
                        message = strings.get(R.string.cust_home_skip_done, com.neerly.mobile.core.design.shortDay(date)))
                    refresh()
                }
                .onFailure { _state.value = _state.value.copy(skipping = false, message = strings.get(R.string.cust_home_skip_failed)) }
        }
    }

    /** Board NoSuppliers: "Leave your number, we'll text you the day one goes live." */
    fun joinWaitlist() {
        val pincode = _state.value.primaryAddress?.pincode ?: return
        if (_state.value.onWaitlist || _state.value.joiningWaitlist) return
        _state.value = _state.value.copy(joiningWaitlist = true, message = null)
        viewModelScope.launch {
            runCatching { discovery.joinWaitlist(com.neerly.mobile.data.dto.JoinWaitlistRequest(pincode = pincode)) }
                .onSuccess { _state.value = _state.value.copy(joiningWaitlist = false, onWaitlist = true) }
                .onFailure { _state.value = _state.value.copy(joiningWaitlist = false, message = strings.get(R.string.cust_waitlist_failed)) }
        }
    }

    /** Re-read on every return to Home so the badge clears after the feed is opened. */
    fun refreshUnread() {
        viewModelScope.launch {
            runCatching { api.unreadCount("CUSTOMER")["unread"]?.toInt() ?: 0 }
                .onSuccess { n -> _state.value = _state.value.copy(unread = n) }
        }
    }

    fun refresh() {
        connectivity.refresh()
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val addresses = repo.addresses()
                val primary = addresses.firstOrNull { it.isPrimary } ?: addresses.firstOrNull()
                val pincode = primary?.pincode ?: "500032"
                val vendors = repo.vendors(pincode, primary?.latitude, primary?.longitude)
                val active = runCatching { repo.activeOrders() }.getOrDefault(emptyList())
                val wallet = runCatching { repo.wallet() }.getOrNull()
                // "Your usual": the last delivered order, one tap to repeat it.
                val usual = runCatching { repo.myOrders(0, 10) }.getOrDefault(emptyList()).firstOrNull { it.status == "DELIVERED" }
                // Tomorrow's subscription strip.
                val nextSub = runCatching { repo.subscriptions() }.getOrDefault(emptyList())
                    .filter { it.status == "ACTIVE" && it.nextDeliveryDate != null }.minByOrNull { it.nextDeliveryDate!! }
                val name = runCatching { repo.me().displayName }.getOrNull()
                // Nobody serves this pincode: is the customer already on its waitlist?
                val waiting = vendors.isEmpty() && runCatching { discovery.myWaitlist() }.getOrDefault(emptyList())
                    .any { it.vendorId == null && it.pincode == pincode }
                HomeUiState(
                    unread = _state.value.unread,
                    filter = _state.value.filter,
                    onWaitlist = waiting,
                    message = _state.value.message,
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

/** The Home filter chips (board Home). */
enum class HomeFilter { OPEN_NOW, TOP_RATED, CHILLED, TANKER }

object HomeFilters {
    /** Shops taking orders, filtered and ordered by the chip. */
    fun open(vendors: List<VendorCardResponse>, filter: HomeFilter?): List<VendorCardResponse> =
        vendors.filter { !it.closedForNow }.let { apply(it, filter) }

    /** Shops shut for now, listed under the open ones with when they're back — never under "Open now". */
    fun closed(vendors: List<VendorCardResponse>, filter: HomeFilter?): List<VendorCardResponse> =
        if (filter == HomeFilter.OPEN_NOW) emptyList() else vendors.filter { it.closedForNow }.let { apply(it, filter) }

    private fun apply(list: List<VendorCardResponse>, filter: HomeFilter?): List<VendorCardResponse> = when (filter) {
        null -> list
        HomeFilter.OPEN_NOW -> list.filter { it.takingOrders }
        HomeFilter.TOP_RATED -> list.filter { it.avgRating != null }.sortedByDescending { it.avgRating }
        HomeFilter.CHILLED -> list.filter { "CHILLED" in it.tags }
        HomeFilter.TANKER -> list.filter { "TANKER" in it.tags }
    }
}

data class HomeUiState(
    val loading: Boolean = true,
    /** Unread updates for the bell (board Home: "Notifications, 3 unread"). */
    val unread: Int = 0,
    val primaryAddress: AddressResponse? = null,
    val vendors: List<VendorCardResponse> = emptyList(),
    val activeOrders: List<OrderResponse> = emptyList(),
    val wallet: WalletResponse? = null,
    val usual: com.neerly.mobile.data.dto.OrderResponse? = null,
    val nextSubscription: com.neerly.mobile.data.dto.SubscriptionResponse? = null,
    val name: String? = null,
    val error: String? = null,
    val filter: HomeFilter? = null,
    /** Signed up for "text me when a shop delivers here". */
    val onWaitlist: Boolean = false,
    val joiningWaitlist: Boolean = false,
    val skipping: Boolean = false,
    /** One-off line after Skip / sign-up ("Skipped Sun 8 Nov."). */
    val message: String? = null
) {
    val openVendors: List<VendorCardResponse> get() = HomeFilters.open(vendors, filter)
    val closedVendors: List<VendorCardResponse> get() = HomeFilters.closed(vendors, filter)
}
