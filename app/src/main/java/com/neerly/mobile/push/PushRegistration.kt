package com.neerly.mobile.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.neerly.mobile.BuildConfig
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.auth.TokenStore
import com.neerly.mobile.data.dto.RegisterDeviceRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Tells the server which phone (and FCM token) belongs to the signed-in user and which role
 * it is in, so pushes reach the right face (plan §f5). Sent after sign-in, on every role
 * switch, and when Firebase rotates the token. Works without Firebase configured: the token
 * is then null and the device row still records the role.
 */
@Singleton
class PushRegistration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: NeerlyApi,
    private val tokens: TokenStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fire-and-forget; a failure here must never block sign-in. */
    fun register(fcmToken: String? = null) {
        if (tokens.accessToken.isNullOrBlank()) return
        scope.launch {
            runCatching {
                api.registerDevice(RegisterDeviceRequest(
                    deviceId = tokens.deviceId(),
                    platform = "ANDROID",
                    fcmToken = fcmToken ?: currentFcmToken(),
                    appVersion = BuildConfig.VERSION_NAME,   // was the Android version (plan D27)
                    osVersion = android.os.Build.VERSION.RELEASE,
                    activeRole = tokens.activeRole
                ))
            }.onFailure { Timber.w(it, "device registration failed") }
        }
    }

    /** Before sign-out: stop pushes to this phone. */
    suspend fun unregister() {
        runCatching { api.revokeDevice(tokens.deviceId()) }
    }

    private suspend fun currentFcmToken(): String? {
        if (FirebaseApp.getApps(context).isEmpty()) return null  // google-services.json not added yet (D25)
        return runCatching {
            suspendCancellableCoroutine { cont ->
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }.getOrNull()
    }
}
