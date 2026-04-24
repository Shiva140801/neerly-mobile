package com.neerly.mobile.data.auth

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.RefreshRequest
import com.squareup.moshi.Moshi
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator — rotates the refresh token when the server returns 401,
 * then retries the original request with the new access token.
 *
 * Guards:
 *   - Don't re-refresh if the *same* request has already been retried (response.priorResponse != null)
 *   - Refuse if refresh token is absent or refresh endpoint itself returned 401
 *     (caller should clear TokenStore + route to Welcome)
 *
 * Uses `Lazy<NeerlyApi>` because Retrofit needs the OkHttpClient, which needs
 * the Authenticator, which needs Retrofit — circular without laziness.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokens: TokenStore,
    private val apiLazy: Lazy<NeerlyApi>,
    private val moshi: Moshi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once — give up.
        if (response.priorResponse != null) return null

        // Refresh endpoint itself returning 401 means our refresh token is bad — bail.
        if (response.request.url.encodedPath.endsWith("/api/v1/auth/refresh")) return null

        val refresh = tokens.refreshToken ?: return null
        if (refresh.isBlank()) return null

        val deviceId = tokens.deviceId()
        val newAccess = runCatching {
            runBlocking {
                val pair = apiLazy.get().refresh(RefreshRequest(refreshToken = refresh, deviceId = deviceId))
                tokens.updateAfterRefresh(
                    accessToken = pair.accessToken,
                    refreshToken = pair.refreshToken,
                    accessExpiresAt = pair.accessExpiresAt,
                    refreshExpiresAt = pair.refreshExpiresAt
                )
                pair.accessToken
            }
        }.getOrNull() ?: run {
            // Refresh failed — clear tokens so the UI treats us as logged out.
            tokens.clear()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }
}
