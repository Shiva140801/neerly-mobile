package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/** `GET /customer/vendors/{id}/slot-days` — the slot picker's days (board SlotPicker). */
@JsonClass(generateAdapter = true)
data class SlotCalendarResponse(
    val vendorId: String,
    val days: List<SlotDayDto>,
    val backOn: String? = null
)

@JsonClass(generateAdapter = true)
data class SlotDayDto(
    val date: String,                    // yyyy-MM-dd
    val open: Boolean,
    val closedReason: String? = null,    // HOLIDAY | CLOSED_DAY | PAUSED
    val label: String? = null,
    val opensAt: String? = null,         // HH:mm[:ss]
    val closesAt: String? = null,
    val slots: List<SlotOptionDto>
)

@JsonClass(generateAdapter = true)
data class SlotOptionDto(
    val start: String,
    val end: String,
    val state: String,                   // OPEN | FILLING | FULL | PASSED | CLOSED
    val remaining: Int? = null
) {
    val bookable: Boolean get() = state == "OPEN" || state == "FILLING"
}

/** `GET /customer/orders/{id}/alternatives` (board NoVendor). */
@JsonClass(generateAdapter = true)
data class OrderAlternativesResponse(
    val orderId: String,
    val orderNumber: String,
    val originalShopId: String? = null,
    val originalShopName: String? = null,
    val alternatives: List<AlternativeShopDto>
)

@JsonClass(generateAdapter = true)
data class AlternativeShopDto(
    val vendorId: String,
    val businessName: String,
    val avgRating: BigDecimal? = null,
    val etaMinutesMin: Int,
    val etaMinutesMax: Int,
    val lines: List<AlternativeLineDto>,
    val missing: List<String> = emptyList(),
    val subtotal: BigDecimal,
    val deliveryFee: BigDecimal,
    val depositTotal: BigDecimal,
    val totalAmount: BigDecimal,
    val promoDropped: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AlternativeLineDto(
    val productId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val containerMode: String
)

@JsonClass(generateAdapter = true)
data class RerouteRequest(val vendorId: String)

/** `POST /customer/waitlist` (boards NoSuppliers, VendorClosed). */
@JsonClass(generateAdapter = true)
data class JoinWaitlistRequest(
    val pincode: String? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val vendorId: String? = null
)

@JsonClass(generateAdapter = true)
data class WaitlistEntryDto(
    val id: String,
    val pincode: String? = null,
    val vendorId: String? = null,
    val createdAt: String? = null,
    val alreadyOnList: Boolean = false
)

/** `GET /customer/products/{id}/alternatives` (board OutOfStock). */
@JsonClass(generateAdapter = true)
data class ProductAlternativesResponse(
    val productId: String,
    val name: String,
    val vendorId: String,
    val status: String,
    val soldOutSince: String? = null,
    val backOn: String? = null,
    val sameShop: List<ProductOfferDto> = emptyList(),
    val otherShops: List<ProductOfferDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProductOfferDto(
    val vendorId: String,
    val vendorName: String? = null,
    val productId: String,
    val name: String,
    val price: BigDecimal
)
