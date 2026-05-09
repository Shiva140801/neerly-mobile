package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass

/**
 * Mirrors the backend's `VendorBusinessConfigController` shapes.
 *
 * dayOfWeek follows ISO-8601 (1 = Mon … 7 = Sun) so `java.time.DayOfWeek.value`
 * round-trips cleanly. openTime/closeTime are HH:mm:ss (LocalTime.toString()).
 */

@JsonClass(generateAdapter = true)
data class VendorHoursRow(
    val dayOfWeek: Int,
    val openTime: String,
    val closeTime: String
)

@JsonClass(generateAdapter = true)
data class ReplaceVendorHoursRequest(val rows: List<VendorHoursRow>)

@JsonClass(generateAdapter = true)
data class VendorHolidayRow(val date: String, val label: String?)

@JsonClass(generateAdapter = true)
data class AddVendorHolidayRequest(val date: String, val label: String? = null)

@JsonClass(generateAdapter = true)
data class VendorBusinessStatus(
    val isOpen: Boolean,
    val pausedUntil: String?,
    val pausedReason: String?
)

@JsonClass(generateAdapter = true)
data class VendorEmergencyCloseRequest(val hours: Int = 24, val reason: String? = null)

@JsonClass(generateAdapter = true)
data class VendorTeamMember(
    val membershipId: String,
    val driverId: String,
    val name: String?,
    val phoneMasked: String?,
    val invitedAt: String,
    val acceptedAt: String?,
    val notes: String?
)

@JsonClass(generateAdapter = true)
data class AddVendorDriverRequest(val phone: String, val notes: String? = null)
