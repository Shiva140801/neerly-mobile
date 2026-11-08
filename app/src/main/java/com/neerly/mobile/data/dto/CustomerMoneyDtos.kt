package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/** GET /customer/orders/{id}/cancel-quote */
@JsonClass(generateAdapter = true)
data class CancelQuote(
    val allowed: Boolean,
    val late: Boolean,
    val fee: BigDecimal,
    val refund: BigDecimal,
    val paymentMethod: String,
    val reason: String? = null
)

/** Backend RefundResponse. */
@JsonClass(generateAdapter = true)
data class RefundResponse(
    val id: String,
    val paymentId: String,
    val orderId: String? = null,
    val amount: BigDecimal,
    val reason: String,
    val refundMode: String,        // ORIGINAL / WALLET / CASH
    val status: String,            // REQUESTED / APPROVED / PROCESSING / COMPLETED / REJECTED / FAILED
    val requestedAt: String,
    val approvedAt: String? = null,
    val completedAt: String? = null,
    val rejectionReason: String? = null,
    val failureReason: String? = null
)

@JsonClass(generateAdapter = true)
data class RefundDestinationRequest(val destination: String)

@JsonClass(generateAdapter = true)
data class DevChangePhoneRequest(val phone: String, val otp: String)

@JsonClass(generateAdapter = true)
data class ChangePhoneResponse(val phoneNumber: String)

/** One person who joined with my code (GET /customer/promos/referrals). */
@JsonClass(generateAdapter = true)
data class ReferralRow(
    val id: String,
    val referredId: String,
    val status: String,            // PENDING / QUALIFIED / REWARDED / EXPIRED
    val referrerReward: BigDecimal? = null,
    val createdAt: String
)

/** GET /customer/vendors/{id}/slots */
@JsonClass(generateAdapter = true)
data class SlotRow(
    val slotStart: String,
    val slotEnd: String,
    val maxOrders: Int,
    val remaining: Int,
    val level: String               // HIGH / MEDIUM / LOW / FULL
)

/** GET /customer/profile/stats */
@JsonClass(generateAdapter = true)
data class ProfileStats(
    val lifetimeOrders: Long = 0,
    val deliveredOrders: Long = 0,
    val memberSince: String? = null,
    val jarsOut: Int = 0,
    val referralCode: String? = null,
    val referralsJoined: Long = 0,
    val referralEarnings: BigDecimal = BigDecimal.ZERO
)
