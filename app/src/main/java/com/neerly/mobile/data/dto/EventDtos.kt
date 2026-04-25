package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Mirrors `com.neerly.event.api.dto.EventDtos` on the backend.
 * Used by the customer event-booking wizard and the vendor-side confirm/reject screens.
 */

@JsonClass(generateAdapter = true)
data class CreateEventBookingRequest(
    val vendorId: String,
    val addressId: String,
    val eventName: String? = null,
    val expectedGuests: Int? = null,
    val eventStart: String,                 // ISO instant
    val eventEnd: String,
    val chillingLeadHours: Int = 6,
    val items: List<EventItemInput>,
    val customerNotes: String? = null
)

@JsonClass(generateAdapter = true)
data class EventItemInput(
    val productId: String,
    val quantity: Int,
    val containerMode: String = "KEEP"
)

@JsonClass(generateAdapter = true)
data class EventItemResponse(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val subtotal: BigDecimal,
    val containerMode: String,
    val depositAmount: BigDecimal?
)

@JsonClass(generateAdapter = true)
data class EventBookingResponse(
    val id: String,
    val customerId: String,
    val vendorId: String,
    val addressId: String,
    val eventName: String?,
    val expectedGuests: Int?,
    val eventStart: String,
    val eventEnd: String,
    val chillingLeadHours: Int,
    val subtotal: BigDecimal,
    val deliveryFee: BigDecimal,
    val depositTotal: BigDecimal,
    val taxes: BigDecimal,
    val totalAmount: BigDecimal,
    val advancePaidAmount: BigDecimal,
    val status: String,                     // DRAFT/PENDING_VENDOR_CONFIRM/CONFIRMED/...
    val items: List<EventItemResponse>,
    val customerNotes: String?,
    val vendorNotes: String?,
    val rejectionReason: String?,
    val pickup: PickupScheduleResponse?
)

@JsonClass(generateAdapter = true)
data class PickupScheduleResponse(
    val id: String,
    val scheduledFor: String,
    val status: String,
    val containersRecovered: Int?,
    val containersMissing: Int?,
    val completedAt: String?
)

@JsonClass(generateAdapter = true)
data class CancelEventRequest(val reason: String? = null)
