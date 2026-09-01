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

/** The product-template block `ProductResponse` nests, per the backend JSON. */
@JsonClass(generateAdapter = true)
data class ProductTemplate(
    val id: String,
    val categoryCode: String,
    val displayName: String,
    val sizeLabel: String? = null,
    val containerType: String? = null,
    val waterType: String? = null,
    val allowsDeposit: Boolean = false,
    val sortOrder: Int = 0
)

/**
 * `GET /customer/vendors/{id}/products` nests the name and category under a
 * `template` object, and sends neither `lateFeePercentPerDay` nor
 * `gracePeriodHours`. Declaring those flat and required made Moshi throw
 * "Required value 'templateId' missing", which crashed the app the moment a
 * customer opened a vendor.
 *
 * `templateId` / `name` / `categoryCode` stay available as derived properties so
 * call sites read the same as before.
 */
@JsonClass(generateAdapter = true)
data class ProductResponse(
    val id: String,
    val vendorId: String,
    val template: ProductTemplate,
    val brand: String?,
    val price: BigDecimal,
    val photoUrl: String? = null,
    val description: String? = null,
    val dailyCapacity: Int? = null,
    val status: String,
    val allowKeepContainer: Boolean = false,
    val allowTransferAndReturn: Boolean = false,
    val depositAmount: BigDecimal? = null,
    val retentionHours: Int? = null,
    val lateFeePercentPerDay: BigDecimal = BigDecimal.ZERO,
    val gracePeriodHours: Int = 0,
    val returnModes: List<String> = emptyList()
) {
    val templateId: String get() = template.id
    val name: String get() = template.displayName
    val categoryCode: String get() = template.categoryCode
}
