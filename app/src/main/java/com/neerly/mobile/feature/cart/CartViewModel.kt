package com.neerly.mobile.feature.cart

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.cart.Cart
import com.neerly.mobile.data.cart.CartItem
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.QuotePromoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val api: NeerlyApi,
    private val strings: com.neerly.mobile.core.util.Strings
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
            _promo.value = PromoState(error = strings.get(R.string.cust_cart_add_items_first))
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
                            error = q.reason ?: strings.get(R.string.cust_promo_not_eligible), isFirstOrder = isFirstOrder
                        )
                    }
                }
                .onFailure { err ->
                    store.clearPromo()
                    _promo.value = PromoState(code = code, error = strings.get(R.string.cust_promo_quote_failed), isFirstOrder = isFirstOrder)
                }
        }
    }

    fun removePromo() {
        store.clearPromo()
        _promo.value = PromoState()
    }

    // --- "When?" (board Cart) -------------------------------------------------

    /** Back to "Right now" — the picked window is dropped from the cart. */
    fun deliverNow() = store.setSlot(null)

    // --- Quick adds (board CartMinimum) ----------------------------------------

    private val _quickAdds = MutableStateFlow<List<QuickAdd>>(emptyList())
    val quickAdds: StateFlow<List<QuickAdd>> = _quickAdds.asStateFlow()
    private var shelf: List<com.neerly.mobile.data.dto.ProductResponse> = emptyList()
    private var shelfOf: String? = null

    /** Below the shop's minimum: one more of what's in the cart, and the cheapest thing that fits. */
    fun loadQuickAdds() {
        val vendorId = store.snapshot.vendorId ?: return
        if (shelfOf == vendorId) { _quickAdds.value = QuickAdds.pick(store.snapshot, shelf); return }
        viewModelScope.launch {
            runCatching { api.vendorProducts(vendorId) }.onSuccess {
                shelf = it; shelfOf = vendorId
                _quickAdds.value = QuickAdds.pick(store.snapshot, it)
            }
        }
    }

    fun quickAdd(q: QuickAdd) {
        val c = store.snapshot
        val existing = c.items.firstOrNull { it.productId == q.productId }
        if (existing != null) updateQuantity(q.productId, existing.quantity + 1)
        else store.addItem(c.vendorId ?: return, c.vendorName ?: "", CartItem(
            productId = q.productId, productName = q.name, unitPrice = q.price, quantity = 1,
            keepContainer = q.keepContainer, depositPerContainer = q.deposit))
        _quickAdds.value = QuickAdds.pick(store.snapshot, shelf)
    }

    // ---- Board Cart: where it goes, and the bill for that address --------------------
    private val _addresses = MutableStateFlow<List<com.neerly.mobile.data.dto.AddressResponse>>(emptyList())
    val addresses: StateFlow<List<com.neerly.mobile.data.dto.AddressResponse>> = _addresses.asStateFlow()
    private val _quote = MutableStateFlow<com.neerly.mobile.data.dto.OrderQuoteResponse?>(null)
    /** Null until the server has priced this basket for the chosen address. */
    val quote: StateFlow<com.neerly.mobile.data.dto.OrderQuoteResponse?> = _quote.asStateFlow()
    private var quoteJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            runCatching { api.addresses() }.onSuccess { list ->
                _addresses.value = list
                if (store.snapshot.addressId == null || list.none { it.id == store.snapshot.addressId }) {
                    store.setAddress((list.firstOrNull { it.isPrimary } ?: list.firstOrNull())?.id)
                }
                // Coordinates are known now — estimate for the real address, not the default distance.
                refreshEta(store.snapshot.vendorId, store.snapshot.addressId)
            }
        }
        // The shop's real arrival estimate for the chosen address (no more literal "20–30").
        store.state.map { it.vendorId to it.addressId }.distinctUntilChanged()
            .onEach { (vendorId, addressId) -> refreshEta(vendorId, addressId) }.launchIn(viewModelScope)
        // Re-price whenever the basket, the slot or the address changes.
        store.state.map { Triple(it.items, it.addressId, it.promoCode to it.slot) }
            .onEach { refreshQuote() }.launchIn(viewModelScope)
    }

    fun selectAddress(id: String) = store.setAddress(id)

    private fun refreshEta(vendorId: String?, addressId: String?) {
        vendorId ?: return
        viewModelScope.launch {
            val a = _addresses.value.firstOrNull { it.id == addressId }
            runCatching { api.vendor(vendorId, a?.latitude, a?.longitude) }
                .onSuccess { v -> store.setEta(vendorId, v.etaMinutesMin, v.etaMinutesMax) }
        }
    }

    private fun refreshQuote() {
        val c = store.snapshot
        val addressId = c.addressId
        val vendorId = c.vendorId
        if (c.isEmpty || addressId == null || vendorId == null) { _quote.value = null; return }
        quoteJob?.cancel()
        quoteJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250) // a stepper burst sends one quote
            _quote.value = runCatching {
                api.quoteOrder(com.neerly.mobile.data.dto.PlaceOrderRequest(
                    vendorId = vendorId, addressId = addressId,
                    items = c.items.map { com.neerly.mobile.data.dto.OrderItemRequest(it.productId, it.quantity,
                        if (it.keepContainer) "KEEP" else "TRANSFER_AND_RETURN") },
                    orderType = "ON_DEMAND", paymentMethod = "COD", promoCode = c.promoCode,
                    requestedSlotStart = c.slot?.startIso, requestedSlotEnd = c.slot?.endIso))
            }.getOrNull()
        }
    }

}

/** One tap on the below-minimum cart (board CartMinimum: "One more 20L jar", "5L can"). */
data class QuickAdd(
    val productId: String,
    val name: String,
    val price: java.math.BigDecimal,
    /** True for "One more …" of a line already in the cart. */
    val oneMore: Boolean,
    val keepContainer: Boolean = true,
    val deposit: java.math.BigDecimal? = null
)

object QuickAdds {
    /**
     * At most two: one more of the first line (same container choice as that line), and
     * the cheapest available product not in the cart. A new product is only offered when
     * adding it can't attach a deposit the customer didn't choose — those go through the
     * product sheet, where the keep/swap choice is explicit.
     */
    fun pick(cart: com.neerly.mobile.data.cart.Cart, shelf: List<com.neerly.mobile.data.dto.ProductResponse>): List<QuickAdd> {
        val out = mutableListOf<QuickAdd>()
        cart.items.firstOrNull { it.quantity < 10 }?.let {
            out += QuickAdd(it.productId, it.productName, it.unitPrice, oneMore = true, it.keepContainer, it.depositPerContainer)
        }
        val inCart = cart.items.map { it.productId }.toSet()
        shelf.asSequence()
            .filter { it.status == "AVAILABLE" && it.id !in inCart }
            .filter { !it.allowKeepContainer || it.depositAmount == null || it.depositAmount.signum() == 0 }
            .minByOrNull { it.price }
            ?.let { p ->
                // Swap-only containers come back at the door; everything else is kept with no deposit.
                val keep = !(p.allowTransferAndReturn && !p.allowKeepContainer)
                out += QuickAdd(p.id, p.name, p.price, oneMore = false, keepContainer = keep, deposit = null)
            }
        return out
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
