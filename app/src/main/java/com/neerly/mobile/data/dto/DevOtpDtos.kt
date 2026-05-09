package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass

/**
 * Mirrors the backend `DevOtpAuthController` shapes.
 *
 * `phone` must be Indian E.164 (`+91` then 10 digits starting 6-9).
 * `role` is one of `CUSTOMER`, `VENDOR_OWNER`, `DRIVER`, `ADMIN_*`.
 */

@JsonClass(generateAdapter = true)
data class DevSendOtpRequest(
    val phone: String,
    val role: String
)

@JsonClass(generateAdapter = true)
data class DevSendOtpResponse(
    val phone: String,
    val expiresInSeconds: Int,
    val hint: String
)

@JsonClass(generateAdapter = true)
data class DevVerifyOtpRequest(
    val phone: String,
    val otp: String,
    val role: String,
    val deviceId: String,
    val name: String? = null
)
