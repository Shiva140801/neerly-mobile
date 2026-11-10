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
    val paymentMethod: String? = null,
    val items: List<OrderItemResponse> = emptyList(),
    val vendorName: String? = null,
    val driverName: String? = null,
    /**
     * The delivery code — present only on the place-order response. The server keeps a
     * hash, so this is the one chance to keep it (see OrderLocalStore).
     */
    val otp: String? = null,
    /** Where it was going — the re-delivery address picker marks it "Tried here". */
    val addressId: String? = null,
    /** Set when the driver couldn't hand it over (board DeliveryFailed). */
    val failedDelivery: FailedDeliveryView? = null,
    /** This order re-delivers that failed one. */
    val redeliveryOf: String? = null
)

/** Mirrors backend `FailedDeliveryView`: why a drop failed and what's still possible. */
@JsonClass(generateAdapter = true)
data class FailedDeliveryView(
    /** NOT_HOME | WRONG_ADDRESS | REFUSED | UNSAFE | OTHER */
    val reason: String? = null,
    val note: String? = null,
    val photoKey: String? = null,
    val failedAt: String? = null,
    /** AWAITING | DISPUTED | REDELIVERED | CLOSED */
    val state: String? = null,
    val decideBy: String? = null,
    val keptIfCancelled: BigDecimal = BigDecimal.ZERO,
    val refundIfCancelled: BigDecimal = BigDecimal.ZERO,
    val redeliveredAs: String? = null,
    /** What trying again or sending it elsewhere costs. */
    val newTripFee: BigDecimal? = null
) {
    /** The customer can still pick try-again, somewhere else or cancel. */
    val open: Boolean get() = state == "AWAITING" || state == "DISPUTED"
}

/** Mirrors backend `OrderQuoteResponse` — what placement will charge, before it does. */
@JsonClass(generateAdapter = true)
data class OrderQuoteResponse(
    val subtotal: BigDecimal,
    val deliveryFee: BigDecimal,
    val surgeSurcharge: BigDecimal,
    val taxes: BigDecimal,
    val discount: BigDecimal,
    val depositTotal: BigDecimal,
    val totalAmount: BigDecimal,
    val promoMessage: String? = null
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
    // Full numbers for the call buttons — null outside the window the backend allows.
    val driverPhone: String? = null,
    val shopPhone: String? = null,
    val vehicle: String? = null,
    val deliveredAt: String? = null,
    val driverLat: Double? = null,
    val driverLng: Double? = null,
    val driverHeadingDeg: Double? = null,
    val driverLocationAt: String? = null,
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val dropLat: Double? = null,
    val dropLng: Double? = null,
    /** Earlier orders this driver delivered to this customer. */
    val deliveriesWithCustomer: Int? = null
)
