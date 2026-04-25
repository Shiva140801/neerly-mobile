package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass

/**
 * Mirrors backend `VendorBankAccountResponse` / `AddVendorBankAccountRequest`.
 * Account number is sent in plain (HTTPS only) and stored encrypted server-side.
 * Reads always come back masked.
 */

@JsonClass(generateAdapter = true)
data class VendorBankAccountResponse(
    val id: String,
    val accountHolderName: String,
    val accountNumberMasked: String,
    val ifsc: String,
    val bankName: String?,
    val branchName: String?,
    val accountType: String,
    val isActive: Boolean,
    val verifiedAt: String?,
    val verificationMethod: String?,
    val addedAt: String,
    val deactivatedAt: String?
)

@JsonClass(generateAdapter = true)
data class AddVendorBankAccountRequest(
    val accountHolderName: String,
    val accountNumber: String,
    val ifsc: String,
    val bankName: String? = null,
    val branchName: String? = null,
    val accountType: String = "SAVINGS",
    val chequeProofS3: String? = null
)
