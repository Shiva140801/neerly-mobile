package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Mirror of `com.neerly.subscription.api.dto.SubscriptionDtos` — kept lean for V1.0.
 * Supplementary fields (vendor name, product name) are denormalised by the backend
 * so the customer screen doesn't have to chain calls.
 */
@JsonClass(generateAdapter = true)
data class SubscriptionResponse(
    val id: String,
    val customerId: String,
    val vendorId: String,
    val vendorName: String,
    val productId: String,
    val productName: String,
    val frequency: String,                 // DAILY / ALT_DAY / TWICE_WEEK / WEEKLY / CUSTOM
    val daysOfWeek: List<String> = emptyList(),
    val quantity: Int,
    val deliverySlot: String,              // e.g. "7-9AM"
    val addressId: String,
    val unitPrice: BigDecimal,
    val status: String,                    // PENDING_MANDATE / ACTIVE / PAUSED / CANCELLED
    val pausedFrom: String?,
    val pausedUntil: String?,
    val nextDeliveryDate: String?,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CreateSubscriptionRequest(
    val vendorId: String,
    val productId: String,
    val frequency: String,
    val daysOfWeek: List<String> = emptyList(),
    val quantity: Int,
    val deliverySlot: String,
    val addressId: String,
    val paymentMethod: String,             // UPI_AUTOPAY / WALLET
    val mandateMaxAmount: BigDecimal? = null
)

@JsonClass(generateAdapter = true)
data class PauseSubscriptionRequest(
    val pausedFrom: String,                // ISO instant
    val pausedUntil: String,
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class SkipSubscriptionRequest(
    val deliveryDate: String               // YYYY-MM-DD
)

@JsonClass(generateAdapter = true)
data class CancelSubscriptionRequest(
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class ModifySubscriptionRequest(
    val quantity: Int? = null,
    val frequency: String? = null,
    val deliverySlot: String? = null,
    val addressId: String? = null
)
