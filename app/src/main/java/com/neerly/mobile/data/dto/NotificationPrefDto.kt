package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass

/**
 * One row in the customer's notification preferences matrix.
 *
 *   channel ∈ {PUSH, SMS, EMAIL, IN_APP, WHATSAPP}
 *   category — free-form string; backend caps at 48 chars. We standardise
 *              on ORDER_UPDATES, DELIVERY_STATUS, PROMOTIONS, NEWS, BILLING,
 *              SUPPORT in the V1 mobile UI.
 *   enabled — true = receive on this channel for this category.
 *
 * Mirrors `com.neerly.user.rest.NotificationPrefResponse` /
 * `UpsertNotifPrefRequest` on the backend.
 */
@JsonClass(generateAdapter = true)
data class NotificationPrefDto(
    val channel: String,
    val category: String,
    val enabled: Boolean
)
