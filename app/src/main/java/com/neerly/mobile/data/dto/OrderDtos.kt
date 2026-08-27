package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Mirrors backend `PlaceOrderRequest`. `orderType` is required by the backend;
 * `vendorId`/`promoCode` are extra fields the backend ignores. Slots are ISO
 * instants and only sent for scheduled orders.
 */
@JsonClass(generateAdapter = true)
data class PlaceOrderRequest(
    val vendorId: String,
    val addressId: String,
    val items: List<OrderItemRequest>,
    val orderType: String = "ON_DEMAND",
    val paymentMethod: String = "UPI",          // UPI | CARD | WALLET | COD | UPI_AUTOPAY
    val promoCode: String? = null,
    val requestedSlotStart: String? = null,     // ISO instant, optional
    val requestedSlotEnd: String? = null        // ISO instant, optional
)

@JsonClass(generateAdapter = true)
data class OrderItemRequest(
    val productId: String,
    val quantity: Int,
    val containerMode: String = "KEEP"          // KEEP | TRANSFER_AND_RETURN
)

@JsonClass(generateAdapter = true)
data class OrderResponse(
    val id: String,
    val orderNumber: String,
    val customerId: String,
    val vendorId: String?,
    val driverId: String?,
    val status: String,
    val subtotal: BigDecimal,
    val deliveryFee: BigDecimal,
    val surgeAmount: BigDecimal?,
    val taxAmount: BigDecimal,
    val discount: BigDecimal?,
    val depositAmount: BigDecimal?,
    val totalAmount: BigDecimal,
    val placedAt: String,
    val items: List<OrderItemResponse> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OrderItemResponse(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val keepContainer: Boolean,
    val depositPerContainer: BigDecimal?
)

@JsonClass(generateAdapter = true)
data class CancelOrderRequest(val reason: String)
