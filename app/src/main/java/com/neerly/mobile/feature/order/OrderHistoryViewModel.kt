package com.neerly.mobile.feature.order

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val cart: com.neerly.mobile.data.cart.CartStore,
    private val codes: com.neerly.mobile.data.order.OrderLocalStore,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    fun codeFor(orderId: String): String? = codes.deliveryCode(orderId)

    /** Same items, same shop, into the cart — the customer reviews and pays as usual. */
    fun reorder(order: com.neerly.mobile.data.dto.OrderResponse): Boolean {
        val vendorId = order.vendorId ?: return false
        val lines = order.items
        if (lines.isEmpty()) return false
        lines.forEachIndexed { i, it ->
            val item = com.neerly.mobile.data.cart.CartItem(
                productId = it.productId, productName = it.productName, unitPrice = it.unitPrice,
                quantity = it.quantity, keepContainer = it.containerMode != "TRANSFER_AND_RETURN")
            if (i == 0) cart.replaceWithNewVendor(vendorId, order.vendorName ?: strings.get(R.string.cust_your_shop), item)
            else cart.addItem(vendorId, order.vendorName ?: strings.get(R.string.cust_your_shop), item)
        }
        return true
    }

    private val _state = MutableStateFlow(OrderHistoryUiState())
    val state: StateFlow<OrderHistoryUiState> = _state.asStateFlow()

    init { load() }

    fun refresh() = load()

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.myOrders(page = 0, size = 50) }
                .onSuccess { _state.value = OrderHistoryUiState(orders = it, loading = false) }
                .onFailure {
                    // `it.message` here was Retrofit/Moshi internals; never the customer's problem.
                    _state.value = OrderHistoryUiState(
                        loading = false,
                        error = it.userMessage(context = "order history", fallback = strings.get(R.string.cust_oh_load_failed))
                    )
                }
        }
    }
}

data class OrderHistoryUiState(
    val orders: List<OrderResponse> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)
