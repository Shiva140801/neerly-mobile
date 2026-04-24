package com.neerly.mobile.data.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the bearer token + X-Request-Id on every outgoing request. Skips auth
 * endpoints (exchange / refresh) because they don't expect a bearer and would
 * loop on the 401 path.
 *
 * The Authenticator (separate class) handles token rotation on 401.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokens: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath

        val builder = req.newBuilder()
        if (!isAuthEndpoint(path)) {
            tokens.accessToken?.takeIf { it.isNotBlank() }?.let {
                builder.header("Authorization", "Bearer $it")
            }
        }
        if (req.method in WRITE_METHODS && req.header("X-Request-Id") == null) {
            builder.header("X-Request-Id", java.util.UUID.randomUUID().toString())
        }
        return chain.proceed(builder.build())
    }

    private fun isAuthEndpoint(path: String): Boolean =
        path.endsWith("/api/v1/auth/exchange") ||
        path.endsWith("/api/v1/auth/refresh")  ||
        path.endsWith("/api/v1/auth/admin/login") ||
        path.endsWith("/api/v1/auth/admin/totp-verify")

    private companion object {
        val WRITE_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
