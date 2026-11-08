package com.neerly.mobile.feature.customer

import com.neerly.mobile.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.ConnectivityObserver
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.cart.AddOutcome
import com.neerly.mobile.data.cart.CartItem
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorDetailViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val cart: CartStore,
    private val api: com.neerly.mobile.data.api.NeerlyApi,
    connectivity: ConnectivityObserver,
    savedState: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val vendorId: String = savedState["vendorId"]
        ?: error("VendorDetailViewModel requires vendorId")

    private val _state = MutableStateFlow(VendorDetailUiState())
    val state: StateFlow<VendorDetailUiState> = _state.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivity.isOnline

    init { load() }

    fun refresh() = load()

    private fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                // `async` children must live inside a `coroutineScope`, otherwise a
                // failure propagates past `runCatching` to viewModelScope's parent
                // job and kills the app instead of landing in the error state.
                coroutineScope {
                    val vendor = async { repo.vendor(vendorId) }
                    val products = async { repo.vendorProducts(vendorId) }
                    vendor.await() to products.await()
                }
            }
                .onSuccess { (v, p) -> _state.value = VendorDetailUiState(vendor = v, products = p, loading = false) }
                .onFailure {
                    _state.value = VendorDetailUiState(
                        loading = false,
                        error = it.userMessage(
                            context = "vendor detail",
                            fallback = strings.get(R.string.cust_vendor_load_failed)
                        )
                    )
                }
        }
    }

    /** Quantity of [productId] already in the cart, so the sheet opens pre-filled. */
    /** The shop's reviews, for the Reviews tab (loaded on first open). */
    val reviews = kotlinx.coroutines.flow.MutableStateFlow<List<com.neerly.mobile.data.dto.ReviewResponse>?>(null)
    fun loadReviews() { if (reviews.value == null) viewModelScope.launch {
        reviews.value = runCatching { api.vendorReviews(vendorId) }.getOrDefault(emptyList())
    } }
    val cartState = cart.state

    fun quantityInCart(productId: String): Int =
        cart.snapshot.items.firstOrNull { it.productId == productId }?.quantity ?: 0

    /**
     * Commits the customer's sheet choice — quantity *and* container mode.
     * Returns [AddOutcome.VendorMismatch] when the cart belongs to another
     * vendor so the screen can prompt before clearing it.
     */
    fun setCartLine(selection: ProductSheetSelection): AddOutcome {
        val v = _state.value.vendor ?: return AddOutcome.VendorMismatch("", "")
        return cart.setLine(v.id, v.businessName, selection.toCartItem())
    }

    fun confirmReplaceWithNewVendor(selection: ProductSheetSelection) {
        val v = _state.value.vendor ?: return
        cart.replaceWithNewVendor(
            vendorId = v.id,
            vendorName = v.businessName,
            item = selection.toCartItem()
        )
    }

    /**
     * The container mode is now the customer's explicit choice from the sheet,
     * not `allowKeepContainer` applied on their behalf — which is what silently
     * attached a deposit to orders nobody agreed to.
     */
    private fun ProductSheetSelection.toCartItem() = CartItem(
        productId = product.id,
        productName = product.name,
        unitPrice = product.price,
        quantity = quantity,
        keepContainer = keepContainer,
        depositPerContainer = product.depositAmount
    )
}

data class VendorDetailUiState(
    val vendor: VendorCardResponse? = null,
    val products: List<ProductResponse> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)
