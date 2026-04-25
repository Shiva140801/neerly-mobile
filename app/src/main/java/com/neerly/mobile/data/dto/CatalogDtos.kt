package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class VendorCardResponse(
    val id: String,
    val businessName: String,
    val tier: String,                 // TIER_1 / TIER_2 / TIER_3_REJECTED
    val status: String,               // ACTIVE / SUSPENDED / etc.
    val businessCity: String,
    val businessPincode: String,
    val avgRating: BigDecimal?,
    val totalOrders: Long,
    val fssaiNumber: String?
)

@JsonClass(generateAdapter = true)
data class ProductResponse(
    val id: String,
    val vendorId: String,
    val templateId: String,
    val name: String,                 // from template
    val categoryCode: String,         // from template
    val brand: String?,
    val price: BigDecimal,
    val photoUrl: String?,
    val description: String?,
    val status: String,
    val allowKeepContainer: Boolean,
    val allowTransferAndReturn: Boolean,
    val depositAmount: BigDecimal?,
    val retentionHours: Int?,
    val lateFeePercentPerDay: BigDecimal,
    val gracePeriodHours: Int,
    val returnModes: List<String>
)
