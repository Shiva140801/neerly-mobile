package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass

/**
 * Auth exchange + refresh DTOs — mirror neerly-backend/.../user/api/dto/AuthDtos.kt.
 * Kept in a separate file from the Session-7 DTOs for readability.
 */

@JsonClass(generateAdapter = true)
data class ExchangeRequest(
    val firebaseIdToken: String,
    val role: String,              // CUSTOMER / VENDOR_OWNER / DRIVER / ADMIN_L1 etc.
    val deviceId: String,
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val appVersion: String? = null
)

@JsonClass(generateAdapter = true)
data class ExchangeResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: String,      // ISO-8601 instant
    val refreshExpiresAt: String,
    val isNewUser: Boolean,
    val activeRole: String,
    val grantedRoles: List<String>,
    val user: UserSummary
)

@JsonClass(generateAdapter = true)
data class UserSummary(
    val id: String,
    val displayName: String,
    val phoneMask: String,
    val email: String?,
    val preferredLanguage: String,
    val status: String,
    val referralCode: String? = null,
    val refundDestination: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(
    val refreshToken: String,
    val deviceId: String? = null
)

@JsonClass(generateAdapter = true)
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: String,
    val refreshExpiresAt: String,
    /** Role the refreshed token carries; when it differs from the screen's, the app switches back. */
    val activeRole: String? = null
)

@JsonClass(generateAdapter = true)
data class SwitchRoleRequest(
    val newRole: String,
    /** So the server remembers the role on this session and the next refresh keeps it. */
    val refreshToken: String? = null,
    val deviceId: String? = null
)

@JsonClass(generateAdapter = true)
data class SwitchRoleResponse(
    val accessToken: String,
    val accessExpiresAt: String,
    val activeRole: String,
    // The backend's switch response carries only the new access token and role; granted
    // roles don't change on a switch. Required here, it failed every switch with a 200.
    val grantedRoles: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class LogoutRequest(val refreshToken: String)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val displayName: String? = null,
    val preferredLanguage: String? = null,
    val email: String? = null
)
