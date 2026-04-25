package com.neerly.mobile.feature.customer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.cart.AddOutcome
import com.neerly.mobile.data.cart.CartItem
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorDetailViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val cart: CartStore,
    savedState: SavedStateHandle
) : ViewModel() {

    private val vendorId: String = savedState["vendorId"]
        ?: error("VendorDetailViewModel requires vendorId")

    private val _state = MutableStateFlow(VendorDetailUiState())
    val state: StateFlow<VendorDetailUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val vendor = async { repo.vendor(vendorId) }
                val products = async { repo.vendorProducts(vendorId) }
                vendor.await() to products.await()
            }
                .onSuccess { (v, p) -> _state.value = VendorDetailUiState(vendor = v, products = p, loading = false) }
                .onFailure { _state.value = VendorDetailUiState(loading = false, error = it.message) }
        }
    }

    /**
     * Adds the product to cart. If the cart already has a different vendor,
     * returns AddOutcome.VendorMismatch so the UI can prompt "Switch vendor?".
     */
    fun addToCart(product: ProductResponse): AddOutcome {
        val v = _state.value.vendor ?: return AddOutcome.VendorMismatch("", "")
        val line = CartItem(
            productId = product.id,
            productName = product.name,
            unitPrice = product.price,
            quantity = 1,
            keepContainer = product.allowKeepContainer,
            depositPerContainer = product.depositAmount
        )
        return cart.addItem(v.id, v.businessName, line)
    }

    fun confirmReplaceWithNewVendor(product: ProductResponse) {
        val v = _state.value.vendor ?: return
        cart.replaceWithNewVendor(
            vendorId = v.id,
            vendorName = v.businessName,
            item = CartItem(
                productId = product.id,
                productName = product.name,
                unitPrice = product.price,
                quantity = 1,
                keepContainer = product.allowKeepContainer,
                depositPerContainer = product.depositAmount
            )
        )
    }
}

data class VendorDetailUiState(
    val vendor: VendorCardResponse? = null,
    val products: List<ProductResponse> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)
