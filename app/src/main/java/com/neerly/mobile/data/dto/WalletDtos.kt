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
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class WalletTransaction(
    val id: String,
    val type: String,               // TOPUP / ORDER / REFUND / CREDIT / BONUS
    val amount: BigDecimal,
    val runningBalance: BigDecimal,
    val description: String?,
    val occurredAt: String
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
    val osVersion: String? = null
)

// ---- S3 presign ----

@JsonClass(generateAdapter = true)
data class PresignRequest(
    val purpose: String,            // compliance / evidence / avatar / damage
    val contentType: String,
    val fileName: String
)

@JsonClass(generateAdapter = true)
data class PresignResponse(
    val uploadUrl: String,
    val s3Key: String,
    val expiresAt: String
)
