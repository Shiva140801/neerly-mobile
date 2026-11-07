package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

// ---------- Vendor incoming order (mirrors backend VendorOrderResponse) ----------

@JsonClass(generateAdapter = true)
data class VendorOrderResponse(
    val id: String,
    val orderNumber: String,
    val customerId: String,
    val customerFirstName: String,
    val customerPhoneMask: String,
    val deliveryAddress: String,
    val pincode: String,
    val distanceKm: BigDecimal?,
    val items: List<VendorOrderItem>,
    val orderValue: BigDecimal,
    val yourEarning: BigDecimal,
    val paymentStatus: String,            // UPI_PAID / COD
    val slotRequested: String,
    val notes: String?,
    val status: String,                   // PLACED / VENDOR_ASSIGNED / PREPARING / OUT_FOR_DELIVERY / ARRIVING / DELIVERED / CANCELLED / FAILED
    val placedAt: String,
    val acceptDeadlineAt: String?,        // 3-min countdown for new orders
    val jarsGoingOut: Int = 0,
    val emptiesExpected: Int? = null,
    val depositTotal: BigDecimal = BigDecimal.ZERO,
    val driverId: String? = null
)

@JsonClass(generateAdapter = true)
data class VendorOrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal
)

@JsonClass(generateAdapter = true)
data class DriverOfferResult(
    val offered: Boolean,
    val driverId: String? = null,
    val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AcceptOrderRequest(val driverId: String? = null)

@JsonClass(generateAdapter = true)
data class RejectOrderRequest(val reason: String)

@JsonClass(generateAdapter = true)
data class DispatchOrderRequest(val driverId: String)

@JsonClass(generateAdapter = true)
data class VendorTodaySummary(
    val ordersToday: Int,
    val deliveredToday: Int,
    val activeOrders: Int,
    val pendingAccept: Int,
    val grossToday: BigDecimal,
    val earningToday: BigDecimal,
    val avgRating: BigDecimal?,
    val strikesLast30d: Int
)

// ---------- Vendor catalog (light view) ----------

@JsonClass(generateAdapter = true)
data class VendorProductRow(
    val id: String,
    /** Backend nests the name in the template; a flat `name` never arrived and broke the catalog. */
    val template: VendorProductTemplate,
    val brand: String? = null,
    val price: BigDecimal,
    val photoUrl: String? = null,
    val status: String,                   // AVAILABLE / OUT_OF_STOCK / PAUSED
    val dailyCapacity: Int,
    val soldThisMonth: Long? = null,
    val containersInStock: Int? = null
) {
    /** "Plain 20L Jar" — brand then template, as orders snapshot it. */
    val name: String get() = listOfNotNull(brand, template.displayName).joinToString(" ")
}

@JsonClass(generateAdapter = true)
data class VendorProductTemplate(
    val id: String,
    val categoryCode: String,
    val displayName: String,
    val sizeLabel: String
)

@JsonClass(generateAdapter = true)
data class TogglePauseRequest(val paused: Boolean)

// ---------- Vendor earnings ----------

/** Mirror of backend `EarningsOverviewResponse` (GET /vendor/earnings/overview). */
@JsonClass(generateAdapter = true)
data class EarningsSummary(
    val today: EarningsBucket,
    val week: EarningsBucket,
    val month: EarningsBucket,
    val nextPayoutAt: String?,
    val nextPayoutAmount: BigDecimal?
)

/** Platform fee and the GST on it are the only deductions the backend makes. */
@JsonClass(generateAdapter = true)
data class EarningsBucket(
    val gross: BigDecimal,
    val commission: BigDecimal,
    val tax: BigDecimal,
    val net: BigDecimal,
    val orders: Int
)

// ---------- Compliance ----------

@JsonClass(generateAdapter = true)
data class ComplianceDocResponse(
    val id: String,
    val docType: String,
    val docNumber: String?,
    val s3Key: String,
    val status: String,                   // SUBMITTED / APPROVED / REJECTED / EXPIRED
    val expiresDate: String?,
    val rejectionReason: String?,
    val uploadedAt: String
)

// ---------- Today subscriptions ----------

@JsonClass(generateAdapter = true)
data class VendorSubscriptionTodayRow(
    val subscriptionId: String,
    val customerName: String,
    val customerPhoneMask: String,
    val productName: String,
    val quantity: Int,
    val deliveryAddress: String,
    val slot: String,                     // 7-9AM / 9-11AM / etc.
    val status: String                    // PENDING / DISPATCHED / DELIVERED / SKIPPED
)
