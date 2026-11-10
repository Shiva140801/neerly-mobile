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
/**
 * Mirror of backend `EndShiftRequest`. The old `reason = "EOD"` is not a ShiftEndReason,
 * so every end-shift was refused, and `codHandedOver` was never read.
 */
data class EndShiftRequest(
    val endedReason: String = "DRIVER_ENDED",   // DRIVER_ENDED / AUTO_INACTIVE / VENDOR_ENDED
    val codCollected: BigDecimal? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class GpsPoint(val lat: Double, val lng: Double, val accuracy: Double? = null)

/**
 * Mirrors backend `GpsPingRequest` exactly — the backend requires `latitude`/`longitude`
 * (it 400s on the old `lat`/`lng` names) and stamps `recordedAt` server-side.
 */
@JsonClass(generateAdapter = true)
data class GpsPingRequest(
    val latitude: Double,
    val longitude: Double,
    val headingDeg: Double? = null,
    val speedMps: Double? = null,
    val accuracyM: Double? = null,
    val batteryPct: Int? = null,
    val orderId: String? = null
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
    // Backend order statuses: PREPARING (taken, load not ready) / DISPATCHED / ARRIVING.
    // The app used to look for EN_ROUTE / ARRIVED, which never arrive.
    val status: String,
    val deliveryOtp: String?,             // always null — the customer holds the code
    val notes: String?,
    val emptiesExpected: Int = 0,
    /** Full jars this drop takes from the shop (DRoute "jars in the auto"). */
    val jarsGoingOut: Int = 0,
    val depositHeld: BigDecimal = BigDecimal.ZERO,
    // Phase 5: the shop this drop is picked up from, so "back to the shop" works after a restart.
    val vendorId: String? = null,
    val shopName: String? = null,
    val pickupAddress: String? = null,
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    // Full numbers for the call buttons — the backend fills them only while the drop is live.
    val customerPhone: String? = null,
    val shopPhone: String? = null
)

/** Mirrors backend `DriverOfferResponse` — a run offered to this driver with a countdown. */
@JsonClass(generateAdapter = true)
data class DriverOffer(
    val orderId: String,
    val driverFee: BigDecimal,
    val offeredAt: String,
    val expiresAt: String,
    val secondsRemaining: Long,
    val task: DriverTaskCard? = null
)

@JsonClass(generateAdapter = true)
data class DriverTaskCard(
    val orderId: String,
    val orderNumber: String,
    val vendorName: String,
    val pickupAddress: String,
    val dropAddress: String,
    val dropDistanceKm: Double? = null,
    val jars: Int,
    val units: Int,
    val loadKg: BigDecimal,
    val paymentMethod: String,
    val codAmount: BigDecimal? = null,
    val emptiesExpected: Int,
    val customerFirstName: String,
    // Present on the live response (verified on :8081) — used by pickup/navigate (D04/D05/D33).
    val vendorId: String? = null,
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val dropLat: Double? = null,
    val dropLng: Double? = null,
    val customerPhoneMask: String? = null
)

@JsonClass(generateAdapter = true)
data class CompleteDeliveryRequest(
    val otp: String,
    val photoS3Key: String,
    val codCollected: BigDecimal? = null,
    /** Empties actually handed back — releases earlier deposits at the door. */
    val emptiesCollected: Int? = null,
    /** CASH / UPI_AT_DOOR / NOT_COLLECTED */
    val codSettlement: String? = null
)

@JsonClass(generateAdapter = true)
data class CodReconcileRequest(
    val collected: BigDecimal,
    val handedOver: BigDecimal,
    val notes: String? = null
)
