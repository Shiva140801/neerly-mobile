package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class DriverShiftResponse(
    val id: String,
    val driverId: String,
    val vendorId: String,
    val startedAt: String,
    val endedAt: String?,
    val deliveriesCount: Int,
    val codCollected: BigDecimal
)

@JsonClass(generateAdapter = true)
data class StartShiftRequest(val initialLocation: GpsPoint? = null)

@JsonClass(generateAdapter = true)
data class EndShiftRequest(val reason: String = "EOD", val codHandedOver: BigDecimal? = null)

@JsonClass(generateAdapter = true)
data class GpsPoint(val lat: Double, val lng: Double, val accuracy: Double? = null)

@JsonClass(generateAdapter = true)
data class GpsPingRequest(
    val lat: Double, val lng: Double, val accuracy: Double? = null,
    val orderId: String? = null, val recordedAt: String
)

@JsonClass(generateAdapter = true)
data class DriverAssignment(
    val orderId: String,
    val orderNumber: String,
    val customerFirstName: String,
    val customerPhoneMask: String,
    val deliveryAddress: String,
    val deliveryLat: Double,
    val deliveryLng: Double,
    val product: String,                  // "20L Cool × 2"
    val paymentMethod: String,            // UPI / COD
    val codAmount: BigDecimal?,
    val status: String,                   // DISPATCHED / EN_ROUTE / ARRIVED / DELIVERED
    val deliveryOtp: String?,             // populated when ARRIVED
    val notes: String?
)

@JsonClass(generateAdapter = true)
data class CompleteDeliveryRequest(
    val otp: String,
    val photoS3Key: String,
    val codCollected: BigDecimal? = null
)

@JsonClass(generateAdapter = true)
data class CodReconcileRequest(
    val collected: BigDecimal,
    val handedOver: BigDecimal,
    val notes: String? = null
)
