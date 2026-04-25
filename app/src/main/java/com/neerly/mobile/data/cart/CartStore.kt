package com.neerly.mobile.data.cart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cart. One cart active at a time — adding a product from a
 * different vendor clears the existing cart (with vendor-switch UX handled
 * at the ViewModel layer).
 *
 * For V1.0 the cart is ephemeral — a process kill loses it. V1.1 will persist
 * to DataStore so "resume where you left off" works after a backgrounded
 * swipe-away.
 */
@Singleton
class CartStore @Inject constructor() {

    private val _state = MutableStateFlow(Cart())
    val state: StateFlow<Cart> = _state.asStateFlow()

    val snapshot: Cart get() = _state.value

    /**
     * Adds (or increments) a line. If vendorId differs from the current cart
     * vendor, returns [AddOutcome.VendorMismatch] without mutating; the UI
     * should prompt "Switch vendor? Clears existing cart".
     */
    fun addItem(
        vendorId: String,
        vendorName: String,
        item: CartItem
    ): AddOutcome {
        val current = _state.value
        if (current.vendorId != null && current.vendorId != vendorId) {
            return AddOutcome.VendorMismatch(
                existingVendor = current.vendorName ?: current.vendorId,
                newVendor = vendorName
            )
        }
        val merged = mergeLine(current.items, item)
        _state.value = current.copy(
            vendorId = vendorId,
            vendorName = vendorName,
            items = merged
        )
        return AddOutcome.Added
    }

    /** Force a vendor switch — clears and adds fresh. */
    fun replaceWithNewVendor(
        vendorId: String,
        vendorName: String,
        item: CartItem
    ) {
        _state.value = Cart(
            vendorId = vendorId,
            vendorName = vendorName,
            items = listOf(item)
        )
    }

    fun updateQuantity(productId: String, newQty: Int) {
        val current = _state.value
        val updated = if (newQty <= 0) {
            current.items.filterNot { it.productId == productId }
        } else {
            current.items.map { if (it.productId == productId) it.copy(quantity = newQty.coerceIn(1, 10)) else it }
        }
        _state.value = if (updated.isEmpty()) Cart() else current.copy(items = updated)
    }

    fun removeLine(productId: String) = updateQuantity(productId, 0)

    fun clear() { _state.value = Cart() }

    /**
     * Quote result from the promo endpoint is stored on the cart — this
     * doesn't itself call the backend (ViewModel does).
     */
    fun applyPromoQuote(code: String, discount: BigDecimal) {
        val c = _state.value
        _state.value = c.copy(
            promoCode = code,
            discount = discount.setScale(2, RoundingMode.HALF_UP)
        )
    }

    fun clearPromo() {
        val c = _state.value
        _state.value = c.copy(promoCode = null, discount = BigDecimal.ZERO)
    }

    /** Hook for pricing-engine updates — delivery/surge are server-driven. */
    fun setPricing(deliveryFee: BigDecimal, surge: BigDecimal, deposit: BigDecimal) {
        val c = _state.value
        _state.value = c.copy(deliveryFee = deliveryFee, surge = surge, deposit = deposit)
    }

    private fun mergeLine(items: List<CartItem>, incoming: CartItem): List<CartItem> {
        val existing = items.firstOrNull { it.productId == incoming.productId }
        return if (existing == null) {
            items + incoming
        } else {
            items.map {
                if (it.productId == incoming.productId) {
                    it.copy(quantity = (it.quantity + incoming.quantity).coerceAtMost(10))
                } else it
            }
        }
    }
}

sealed interface AddOutcome {
    data object Added : AddOutcome
    data class VendorMismatch(val existingVendor: String, val newVendor: String) : AddOutcome
}
