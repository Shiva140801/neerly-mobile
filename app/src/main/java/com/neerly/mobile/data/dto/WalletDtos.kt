package com.neerly.mobile.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * `GET /customer/wallet` actually returns
 * `{"balance":0,"holdAmount":0,"available":0,"currency":"INR","status":"ACTIVE"}`
 * — no `userId`, and two of the amounts are named differently. The Kotlin
 * property names stay as the UI already reads them; `@Json` carries the wire
 * names so nothing else has to change.
 */
@JsonClass(generateAdapter = true)
data class WalletResponse(
    val userId: String? = null,
    val balance: BigDecimal,
    @Json(name = "holdAmount") val heldAmount: BigDecimal,
    @Json(name = "available") val availableAmount: BigDecimal,
    val currency: String = "INR",
    val status: String? = null,
    /** Referral / promo credit inside the balance: spendable, not withdrawable. */
    val promoBalance: BigDecimal = BigDecimal.ZERO,
    /** What can go back to the customer's bank. */
    val withdrawable: BigDecimal = BigDecimal.ZERO
)

/** POST /customer/wallet/withdraw */
@JsonClass(generateAdapter = true)
data class WithdrawRequest(val amount: BigDecimal, val upiId: String)

/** Backend `WithdrawalView`. */
@JsonClass(generateAdapter = true)
data class WithdrawalResponse(
    val id: String,
    val amount: BigDecimal,
    val upiId: String,
    val status: String,          // REQUESTED / PAID / FAILED
    val requestedAt: String,
    val processedAt: String? = null,
    val utr: String? = null,
    val failureReason: String? = null
)

@JsonClass(generateAdapter = true)
data class WalletTransaction(
    val id: String,
    val type: String,               // TOPUP / ORDER_DEBIT / ORDER_REFUND / CASHBACK / REFERRAL …
    // Backend `WalletTransactionResponse` names; the old ones never matched, so the
    // list stayed "No transactions yet" under a ₹300 balance.
    @Json(name = "amountSigned") val amount: BigDecimal,
    @Json(name = "balanceAfter") val runningBalance: BigDecimal,
    val description: String?,
    @Json(name = "createdAt") val occurredAt: String
)

@JsonClass(generateAdapter = true)
data class WalletTopupRequest(
    val amount: BigDecimal,
    val method: String = "UPI"       // UPI / CARD
)

// ---- Devices ----

@JsonClass(generateAdapter = true)
data class RegisterDeviceRequest(
    val deviceId: String,
    val platform: String = "ANDROID",
    val fcmToken: String? = null,
    val apnsToken: String? = null,
    val appVersion: String? = null,
    val osVersion: String? = null,
    /** CUSTOMER / VENDOR_OWNER / DRIVER — pushes go to devices in the matching role (plan §f5). */
    val activeRole: String? = null
)

// ---- S3 presign ----

@JsonClass(generateAdapter = true)
data class PresignRequest(
    // Mirrors backend `UploadController.PresignRequest`. The old purpose/fileName shape was
    // never accepted (400 UPLOAD_SCOPE_INVALID).
    val scope: String,              // VENDOR_DOCS, VENDOR_PRODUCTS, DRIVER_DOCS, ORDER_PROOF, CLAIM_EVIDENCE, COMPLAINT_EVIDENCE, CUSTOMER_AVATAR
    val kind: String,               // free-form sub-folder, e.g. "fssai", "pickup-photo"
    val contentType: String
)

@JsonClass(generateAdapter = true)
data class PresignResponse(
    val uploadUrl: String,
    val s3Key: String,
    val headers: Map<String, String> = emptyMap(),
    val expiresInSeconds: Long = 0
)
