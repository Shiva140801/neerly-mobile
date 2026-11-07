package com.neerly.mobile.data.order

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-order facts only this device knows.
 *
 * The delivery code is the one the customer reads out at the door. The server stores
 * just its hash, so the plaintext exists in exactly one place — the place-order (or
 * regenerate) response — and must be kept here or it is gone. Encrypted, like tokens:
 * it is what releases a paid order to whoever holds the phone.
 */
@Singleton
class OrderLocalStore @Inject constructor(
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

    fun deliveryCode(orderId: String): String? = prefs.getString(codeKey(orderId), null)

    fun saveDeliveryCode(orderId: String, code: String) {
        prefs.edit().putString(codeKey(orderId), code).apply()
    }

    /** Once delivered the code is spent; keeping it only widens what a lost phone leaks. */
    fun forgetDeliveryCode(orderId: String) {
        prefs.edit().remove(codeKey(orderId)).apply()
    }

    fun isReviewed(orderId: String): Boolean = prefs.getBoolean(reviewedKey(orderId), false)

    fun markReviewed(orderId: String) {
        prefs.edit().putBoolean(reviewedKey(orderId), true).apply()
    }

    private fun codeKey(orderId: String) = "code:$orderId"
    private fun reviewedKey(orderId: String) = "reviewed:$orderId"

    private companion object {
        const val PREFS_NAME = "neerly_orders"
    }
}
