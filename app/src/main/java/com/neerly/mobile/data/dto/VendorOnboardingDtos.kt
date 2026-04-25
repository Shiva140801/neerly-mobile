package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Mirrors `com.neerly.vendor.api.dto.SubmitVendorOnboardingRequest`.
 * BusinessType / ServiceAreaModel are sent as strings; backend deserialises to enums.
 *
 * Defaults match what the wizard collects today (Hyderabad, PINCODE service-area model);
 * everything else is optional V1.
 */
@JsonClass(generateAdapter = true)
data class SubmitVendorOnboardingRequest(
    val businessName: String,
    val proprietorName: String,
    val businessType: String = "PROPRIETOR",         // PROPRIETOR / PARTNERSHIP / PRIVATE_LIMITED / LLP
    val categories: Set<String> = setOf("WATER_SUPPLIER"),

    val businessFlatNumber: String? = null,
    val businessBuilding: String? = null,
    val businessStreet: String? = null,
    val businessLandmark: String? = null,
    val businessCity: String = "Hyderabad",
    val businessPincode: String,
    val businessLatitude: BigDecimal? = null,
    val businessLongitude: BigDecimal? = null,

    val gstin: String? = null,
    val pan: String? = null,
    val fssaiNumber: String? = null,
    val fssaiExpiresAt: String? = null,              // YYYY-MM-DD

    val serviceAreaModel: String = "PINCODE",
    val serviceablePincodes: Set<String> = emptySet(),
    val serviceAreaRadiusM: Int? = null,

    val isTier2: Boolean = false
)

@JsonClass(generateAdapter = true)
data class VendorResponse(
    val id: String,
    val ownerId: String,
    val businessName: String,
    val proprietorName: String,
    val businessType: String,
    val categories: List<String>,
    val tier: String,
    val status: String,
    val businessCity: String,
    val businessPincode: String,
    val fssaiNumber: String?,
    val fssaiExpiresAt: String?,
    val avgRating: BigDecimal?,
    val totalOrders: Int,
    val joinedAt: String,
    val approvedAt: String?,
    val servicedPincodes: List<String>
)
