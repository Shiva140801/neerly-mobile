package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass

/**
 * Mirrors `com.neerly.address.api.dto.AddressDtos` — the backend contract is the
 * source of truth. Field names must match the backend JSON exactly.
 */
@JsonClass(generateAdapter = true)
data class AddressResponse(
    val id: String,
    val label: String,
    val flatNumber: String,
    val buildingName: String,
    val street: String,
    val landmark: String? = null,
    val city: String,
    val pincode: String,
    val latitude: Double,
    val longitude: Double,
    val deliveryInstructions: String? = null,
    val floorNumber: Int? = null,
    val hasLift: Boolean = false,
    val securityContactName: String? = null,
    val securityContactPhone: String? = null,
    val isPrimary: Boolean = false,
    val isServiceable: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class CreateAddressRequest(
    val label: String,
    val flatNumber: String,
    val buildingName: String,
    val street: String,
    val landmark: String? = null,
    val city: String = "Hyderabad",
    val pincode: String,
    val latitude: Double,
    val longitude: Double,
    val deliveryInstructions: String? = null,
    val floorNumber: Int? = null,
    val hasLift: Boolean = false,
    val securityContactName: String? = null,
    val securityContactPhone: String? = null,
    val setAsPrimary: Boolean = false
)

/** All-optional PATCH body for `PATCH /customer/addresses/{id}`. */
@JsonClass(generateAdapter = true)
data class UpdateAddressRequest(
    val label: String? = null,
    val flatNumber: String? = null,
    val buildingName: String? = null,
    val street: String? = null,
    val landmark: String? = null,
    val city: String? = null,
    val pincode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val deliveryInstructions: String? = null,
    val floorNumber: Int? = null,
    val hasLift: Boolean? = null,
    val securityContactName: String? = null,
    val securityContactPhone: String? = null
)
