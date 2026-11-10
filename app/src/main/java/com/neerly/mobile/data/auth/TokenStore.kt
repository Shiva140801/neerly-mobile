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
        activeRole: String,
        grantedRoles: List<String> = emptyList()
    ) {
        prefs.edit().apply {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
            putString(KEY_ACCESS_EXP, accessExpiresAt)
            putString(KEY_REFRESH_EXP, refreshExpiresAt)
            putString(KEY_ACTIVE_ROLE, activeRole)
            putStringSet(KEY_GRANTED_ROLES, grantedRoles.toSet())
            apply()
        }
    }

    fun updateAfterRefresh(accessToken: String, refreshToken: String,
                           accessExpiresAt: String, refreshExpiresAt: String, activeRole: String? = null) {
        prefs.edit().apply {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
            putString(KEY_ACCESS_EXP, accessExpiresAt)
            putString(KEY_REFRESH_EXP, refreshExpiresAt)
            // Keep the stored role true to the token, so the role gate knows when to switch.
            activeRole?.let { putString(KEY_ACTIVE_ROLE, it) }
            apply()
        }
    }

    fun updateAfterSwitch(
        accessToken: String,
        accessExpiresAt: String,
        activeRole: String,
        grantedRoles: List<String>
    ) {
        prefs.edit().apply {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_ACCESS_EXP, accessExpiresAt)
            putString(KEY_ACTIVE_ROLE, activeRole)
            putStringSet(KEY_GRANTED_ROLES, grantedRoles.toSet())
            apply()
        }
    }

    val accessToken: String? get() = prefs.getString(KEY_ACCESS, null)
    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)
    val activeRole: String? get() = prefs.getString(KEY_ACTIVE_ROLE, null)
    val grantedRoles: List<String> get() = prefs.getStringSet(KEY_GRANTED_ROLES, null)?.toList() ?: emptyList()

    /** Set when the server ended the session; Welcome shows "Please sign in again" once, even after a restart. */
    var sessionEndedNotice: Boolean
        get() = prefs.getBoolean("session_ended_notice", false)
        set(value) = prefs.edit().putBoolean("session_ended_notice", value).apply()

    var lastRolePreference: String?
        get() = prefs.getString(KEY_ROLE_PREF, null)
        set(value) = prefs.edit().putString(KEY_ROLE_PREF, value).apply()

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

    /**
     * True when a refresh token is stored — enough to attempt a silent session
     * restore on cold start (the Authenticator rotates the pair on first 401).
     */
    fun hasSession(): Boolean = !refreshToken.isNullOrBlank()

    fun clear() {
        prefs.edit().apply {
            remove(KEY_ACCESS)
            remove(KEY_REFRESH)
            remove(KEY_ACCESS_EXP)
            remove(KEY_REFRESH_EXP)
            remove(KEY_ACTIVE_ROLE)
            remove(KEY_GRANTED_ROLES)
            // Note: we keep KEY_ROLE_PREF and KEY_DEVICE_ID
            apply()
        }
    }

    /**
     * Cached deviceId, used for refresh-token rotation and by the backend's same-phone
     * rule (a customer's order can't be taken or delivered from the customer's phone).
     *
     * New installs derive it from ANDROID_ID — stable for this app on this phone across
     * clearing data and reinstalling — hashed so the raw id never leaves the phone. An
     * install that already has an id keeps it, so signed-in sessions stay valid.
     */
    fun deviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val id = stableDeviceId() ?: ("mobile-" + java.util.UUID.randomUUID().toString().take(12))
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    private fun stableDeviceId(): String? = runCatching {
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
    }.getOrNull()?.takeIf { it.isNotBlank() }?.let { raw ->
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(("neerly:" + raw).toByteArray())
        "android-" + digest.take(10).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PREFS_NAME = "neerly.auth.v1"
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_ACCESS_EXP = "accessExpiresAt"
        const val KEY_REFRESH_EXP = "refreshExpiresAt"
        const val KEY_ACTIVE_ROLE = "activeRole"
        const val KEY_GRANTED_ROLES = "grantedRoles"
        const val KEY_ROLE_PREF = "rolePreference"
        const val KEY_DEVICE_ID = "deviceId"
    }
}
