package com.neerly.mobile.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.cart.Cart
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.QuotePromoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cart screen state. Mirrors CartStore for the item list and owns the UX-state
 * of the promo-code input (quoting / error / applied).
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    private val store: CartStore,
    private val api: NeerlyApi
) : ViewModel() {

    val cart: StateFlow<Cart> = store.state

    private val _promo = MutableStateFlow(PromoState())
    val promo: StateFlow<PromoState> = _promo.asStateFlow()

    fun updateQuantity(productId: String, newQty: Int) {
        store.updateQuantity(productId, newQty)
        // If the subtotal changed while a promo was quoted, re-quote so the
        // discount reflects the new subtotal.
        _promo.value.code?.let { if (_promo.value.applied) quotePromo(it, isFirstOrder = _promo.value.isFirstOrder) }
    }

    fun removeLine(productId: String) {
        store.removeLine(productId)
        _promo.value = PromoState() // clear promo when cart resets
    }

    fun clearCart() {
        store.clear()
        _promo.value = PromoState()
    }

    fun quotePromo(code: String, isFirstOrder: Boolean) {
        val subtotal = store.snapshot.subtotal
        if (subtotal.signum() <= 0) {
            _promo.value = PromoState(error = "Add items before applying a code")
            return
        }
        _promo.value = _promo.value.copy(quoting = true, error = null, code = code, isFirstOrder = isFirstOrder)
        viewModelScope.launch {
            runCatching { api.quotePromo(QuotePromoRequest(code, subtotal, isFirstOrder)) }
                .onSuccess { q ->
                    if (q.eligible) {
                        store.applyPromoQuote(q.code, q.discount)
                        _promo.value = PromoState(
                            code = q.code, applied = true,
                            discount = q.discount, isFirstOrder = isFirstOrder
                        )
                    } else {
                        store.clearPromo()
                        _promo.value = PromoState(
                            code = code, applied = false,
                            error = q.reason ?: "Not eligible", isFirstOrder = isFirstOrder
                        )
                    }
                }
                .onFailure { err ->
                    store.clearPromo()
                    _promo.value = PromoState(code = code, error = err.message ?: "Quote failed", isFirstOrder = isFirstOrder)
                }
        }
    }

    fun removePromo() {
        store.clearPromo()
        _promo.value = PromoState()
    }
}

data class PromoState(
    val code: String? = null,
    val quoting: Boolean = false,
    val applied: Boolean = false,
    val discount: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val isFirstOrder: Boolean = false,
    val error: String? = null
)
