package com.neerly.mobile.data.auth

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.ExchangeRequest
import com.neerly.mobile.data.dto.ExchangeResponse
import com.neerly.mobile.data.dto.LogoutRequest
import com.neerly.mobile.data.dto.RegisterDeviceRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles sign-in / sign-out / role-switch at the application level. Calls the
 * backend, updates TokenStore, and registers the FCM token as a post-login side
 * effect so notifications work from first use.
 *
 * The Firebase Phone Auth step (OTP) happens at the UI layer via the Firebase
 * Android SDK — this repository receives the resulting ID token and hands it
 * to /api/v1/auth/exchange.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: NeerlyApi,
    private val tokens: TokenStore
) {

    suspend fun exchange(
        firebaseIdToken: String,
        role: String,
        deviceName: String? = null,
        appVersion: String? = null
    ): ExchangeResponse {
        val resp = api.exchange(
            ExchangeRequest(
                firebaseIdToken = firebaseIdToken,
                role = role,
                deviceId = tokens.deviceId(),
                deviceName = deviceName,
                appVersion = appVersion
            )
        )
        tokens.saveTokens(
            accessToken = resp.accessToken,
            refreshToken = resp.refreshToken,
            accessExpiresAt = resp.accessExpiresAt,
            refreshExpiresAt = resp.refreshExpiresAt,
            activeRole = resp.activeRole
        )
        return resp
    }

    suspend fun logout() {
        val refresh = tokens.refreshToken
        tokens.clear()
        if (!refresh.isNullOrBlank()) {
            runCatching { api.logout(LogoutRequest(refresh)) }
        }
    }

    suspend fun registerDevice(fcmToken: String?, appVersion: String?, osVersion: String?) {
        runCatching {
            api.registerDevice(
                RegisterDeviceRequest(
                    deviceId = tokens.deviceId(),
                    platform = "ANDROID",
                    fcmToken = fcmToken,
                    appVersion = appVersion,
                    osVersion = osVersion
                )
            )
        }  // best-effort; a failed device register shouldn't block login
    }

    fun isLoggedIn(): Boolean = tokens.accessToken?.isNotBlank() == true
}
