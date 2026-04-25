package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class WalletResponse(
    val userId: String,
    val balance: BigDecimal,
    val heldAmount: BigDecimal,
    val availableAmount: BigDecimal
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
