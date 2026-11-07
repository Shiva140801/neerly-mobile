package com.neerly.mobile.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Backend `com.neerly.deposit.api.dto.DepositResponse` — one row per container
 * the customer is currently holding (or has held). Drives the Deposits / Returns
 * dashboard.
 */
@JsonClass(generateAdapter = true)
data class DepositResponse(
    val id: String,
    val customerId: String,
    val vendorId: String,
    val orderId: String?,
    val containerCount: Int = 1,
    // Wire names are the backend's DepositSnapshot. The old shape (amount / heldAt /
    // lateFeeAccrued, all non-null) matched none of them, so Moshi rejected the page
    // and the screen said "No deposits held" over a ₹300 HELD deposit.
    @Json(name = "totalDeposit") val amount: BigDecimal,
    @Json(name = "accruedLateFee") val lateFeeAccrued: BigDecimal = BigDecimal.ZERO,
    val refundableAmount: BigDecimal? = null,
    val status: String,                    // HELD / LATE / RETURNED / FORFEIT / WAIVED
    val heldAt: String? = null,
    val returnDeadline: String? = null,
    @Json(name = "graceExpiresAt") val gracePeriodEndAt: String? = null,
    val returnedAt: String? = null,
    // Not served yet; kept nullable so the screen's fallbacks apply.
    val vendorName: String? = null,
    val productName: String? = null
)

/**
 * Mirror of backend `RequestReturnRequest`. The old shape (`mode`, `scheduledSlot`, no
 * container count) was refused with a 400 on every return.
 */
@JsonClass(generateAdapter = true)
data class ReturnRequest(
    val depositId: String,
    val returnMode: String,                // SCHEDULED_PICKUP / DROPOFF / DRIVER_COLLECTED
    val containerCount: Int,
    val addressId: String? = null,         // required for SCHEDULED_PICKUP
    val preferredSlot: String? = null,     // ISO instant
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class ReturnResponse(
    val id: String,
    val depositId: String? = null,
    val orderId: String? = null,
    val returnMode: String,
    val containerCount: Int,
    val status: String,                    // REQUESTED / SCHEDULED / EN_ROUTE / COMPLETED / FAILED / CANCELLED
    val scheduledFor: String? = null,
    val completedAt: String? = null,
    val pickupPhotoUrl: String? = null
)
