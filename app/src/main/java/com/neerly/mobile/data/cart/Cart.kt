package com.neerly.mobile.data.cart

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Immutable cart snapshot. One vendor per cart — switching vendor clears.
 *
 * Money math follows the backend rules: scale 2, HALF_UP. The breakdown
 * mirrors the PRD pricing structure so the checkout screen can explain
 * every line item without re-computing on the server round-trip.
 *
 * `discount` is held here speculatively after a promo *quote* — the server
 * only commits it at `apply` time during place-order, so we re-fetch the
 * final total from the order response.
 */
data class Cart(
    val vendorId: String? = null,
    val vendorName: String? = null,
    val items: List<CartItem> = emptyList(),
    val deliveryFee: BigDecimal = BigDecimal.ZERO,
    val surge: BigDecimal = BigDecimal.ZERO,
    val deposit: BigDecimal = BigDecimal.ZERO,
    val promoCode: String? = null,
    val discount: BigDecimal = BigDecimal.ZERO
) {
    val isEmpty: Boolean get() = items.isEmpty()

    val totalQuantity: Int get() = items.sumOf { it.quantity }

    /** Sum of (unitPrice × qty) across all lines. */
    val subtotal: BigDecimal get() = items
        .map { it.unitPrice.multiply(BigDecimal(it.quantity)) }
        .fold(BigDecimal.ZERO) { a, b -> a + b }
        .setScale(2, RoundingMode.HALF_UP)

    /** What the customer owes at checkout. */
    val total: BigDecimal get() = (subtotal + deliveryFee + surge + deposit - discount)
        .max(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}

data class CartItem(
    val productId: String,
    val productName: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val keepContainer: Boolean = true,
    val depositPerContainer: BigDecimal? = null
) {
    init {
        require(quantity in 1..10) { "Quantity must be 1-10 (events flow for >10)" }
    }

    val lineTotal: BigDecimal get() = unitPrice.multiply(BigDecimal(quantity))
        .setScale(2, RoundingMode.HALF_UP)

    /** Deposit applies only when the customer keeps the container. */
    val lineDeposit: BigDecimal get() =
        if (keepContainer && depositPerContainer != null)
            depositPerContainer.multiply(BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP)
        else BigDecimal.ZERO
}
