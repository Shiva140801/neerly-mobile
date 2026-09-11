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
    // Field names mirror the backend exactly: it serializes `surgeSurcharge`,
    // `taxes` and `depositTotal`. The old aliases (`surgeAmount`/`taxAmount`/
    // `depositAmount`) never matched — taxAmount being non-null made Moshi
    // reject every order payload and blank the whole Orders screen.
    val surgeSurcharge: BigDecimal?,
    val taxes: BigDecimal?,
    val discount: BigDecimal?,
    val depositTotal: BigDecimal?,
    val totalAmount: BigDecimal,
    val placedAt: String,
    val items: List<OrderItemResponse> = emptyList()
)

/** Field names mirror the backend `OrderItemResponse` exactly — see note on [OrderResponse]. */
@JsonClass(generateAdapter = true)
data class OrderItemResponse(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val subtotal: BigDecimal,
    val containerMode: String? = null,
    val depositAmount: BigDecimal? = null
)

@JsonClass(generateAdapter = true)
data class CancelOrderRequest(val reason: String)

/**
 * Mirrors backend `OrderTrackingResponse`. The driver fix and map pins are only
 * present while the order is DISPATCHED/ARRIVING and the backend judged the fix
 * fresh — a null driverLat means "no live location", not "at 0,0".
 */
@JsonClass(generateAdapter = true)
data class OrderTrackingResponse(
    val orderId: String,
    val orderNumber: String,
    val status: String,
    val deliverBy: String,
    val driverName: String? = null,
    val driverPhoneMask: String? = null,
    val vehicle: String? = null,
    val deliveredAt: String? = null,
    val driverLat: Double? = null,
    val driverLng: Double? = null,
    val driverHeadingDeg: Double? = null,
    val driverLocationAt: String? = null,
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val dropLat: Double? = null,
    val dropLng: Double? = null
)
