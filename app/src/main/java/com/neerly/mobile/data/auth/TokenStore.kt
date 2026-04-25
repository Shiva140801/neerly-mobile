package com.neerly.mobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted persistence for auth tokens. Uses AndroidX Security's EncryptedSharedPreferences
 * so the raw JWT/refresh never hit disk unencrypted.
 *
 * Access-token TTL is 15 min, refresh is 7-day sliding — we store the exact expiry
 * so the Authenticator can decide pre-emptively. On a cold start, the app reads
 * these and either jumps to /home (valid) or asks for re-login (none/both expired).
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences by lazy {
        val master = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        accessExpiresAt: String,
        refreshExpiresAt: String,
        activeRole: String
    ) {
        prefs.edit().apply {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
            putString(KEY_ACCESS_EXP, accessExpiresAt)
            putString(KEY_REFRESH_EXP, refreshExpiresAt)
            putString(KEY_ACTIVE_ROLE, activeRole)
            apply()
        }
    }

    fun updateAfterRefresh(accessToken: String, refreshToken: String,
                           accessExpiresAt: String, refreshExpiresAt: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
            putString(KEY_ACCESS_EXP, accessExpiresAt)
            putString(KEY_REFRESH_EXP, refreshExpiresAt)
            apply()
        }
    }

    val accessToken: String? get() = prefs.getString(KEY_ACCESS, null)
    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)
    val activeRole: String? get() = prefs.getString(KEY_ACTIVE_ROLE, null)

    fun accessExpiresAt(): Instant? = prefs.getString(KEY_ACCESS_EXP, null)
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }

    fun refreshExpiresAt(): Instant? = prefs.getString(KEY_REFRESH_EXP, null)
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }

    /** Bearer is valid if we have a token and its expiry hasn't passed (with 30s skew). */
    fun hasValidAccess(nowSupplier: () -> Instant = Instant::now): Boolean {
        val token = accessToken ?: return false
        if (token.isBlank()) return false
        val exp = accessExpiresAt() ?: return false
        return nowSupplier().isBefore(exp.minusSeconds(30))
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    /**
     * Cached deviceId — persisted across re-installs within the same
     * EncryptedSharedPreferences file because it's tied to the Android keystore.
     * We treat it as a pseudo-hardware id for refresh-token rotation.
     */
    fun deviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val id = "mobile-" + java.util.UUID.randomUUID().toString().take(12)
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    private companion object {
        const val PREFS_NAME = "neerly.auth.v1"
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_ACCESS_EXP = "accessExpiresAt"
        const val KEY_REFRESH_EXP = "refreshExpiresAt"
        const val KEY_ACTIVE_ROLE = "activeRole"
        const val KEY_DEVICE_ID = "deviceId"
    }
}
