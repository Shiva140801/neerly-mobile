package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AddressResponse(
    val id: String,
    val label: String,
    val flatNo: String,
    val buildingName: String? = null,
    val streetArea: String,
    val landmark: String? = null,
    val city: String,
    val pincode: String,
    val lat: Double,
    val lng: Double,
    val deliveryInstructions: String? = null,
    val liftAvailable: Boolean = true,
    val floorNumber: Int? = null,
    val securityContactName: String? = null,
    val securityContactPhone: String? = null,
    val isPrimary: Boolean = false,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CreateAddressRequest(
    val label: String,
    val flatNo: String,
    val buildingName: String? = null,
    val streetArea: String,
    val landmark: String? = null,
    val city: String = "Hyderabad",
    val pincode: String,
    val lat: Double,
    val lng: Double,
    val deliveryInstructions: String? = null,
    val liftAvailable: Boolean = true,
    val floorNumber: Int? = null,
    val securityContactName: String? = null,
    val securityContactPhone: String? = null,
    val setAsPrimary: Boolean = false
)
