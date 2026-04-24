package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class InitiatePaymentRequest(
    val orderId: String?,
    val amount: BigDecimal,
    val method: String           // UPI / CARD / WALLET / COD / UPI_AUTOPAY
)

@JsonClass(generateAdapter = true)
data class InitiatePaymentResult(
    val paymentId: String,
    val razorpayOrderId: String?,   // null for COD
    val amountPaise: Long,
    val keyId: String?,             // Razorpay publishable key for Android SDK
    val currency: String = "INR"
)

@JsonClass(generateAdapter = true)
data class CapturePaymentRequest(
    val razorpayPaymentId: String,
    val razorpaySignature: String
)

@JsonClass(generateAdapter = true)
data class PaymentSnapshot(
    val id: String,
    val orderId: String?,
    val customerId: String,
    val amount: BigDecimal,
    val method: String,
    val status: String,
    val razorpayPaymentId: String?
)
