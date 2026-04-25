package com.neerly.mobile.data.dto

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
    val vendorName: String?,
    val orderId: String?,
    val productId: String?,
    val productName: String?,
    val amount: BigDecimal,
    val status: String,                    // HELD / RETURNED / FORFEITED / WAIVED
    val heldAt: String,
    val returnDeadline: String?,
    val returnedAt: String?,
    val forfeitedAt: String?,
    val lateFeeAccrued: BigDecimal,
    val gracePeriodEndAt: String?
)

@JsonClass(generateAdapter = true)
data class ReturnRequest(
    val depositId: String,
    val mode: String,                      // SCHEDULED_PICKUP / DROPOFF / AUTO_NEXT_DELIVERY
    val scheduledSlot: String? = null,     // ISO instant for SCHEDULED_PICKUP
    val photoS3Keys: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ReturnResponse(
    val id: String,
    val depositId: String,
    val mode: String,
    val status: String,                    // SCHEDULED / IN_PROGRESS / COMPLETED / DISPUTED
    val scheduledSlot: String?,
    val completedAt: String?,
    val refundAmount: BigDecimal?,
    val photoS3Keys: List<String>
)
