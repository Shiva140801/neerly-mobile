package com.neerly.mobile.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.neerly.mobile.data.auth.AuthRepository
import com.neerly.mobile.data.auth.TokenStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging receiver + token rotation handler. When Firebase
 * issues a new token we call POST /api/v1/devices to keep the server-side
 * record fresh. onMessageReceived surfaces into the in-app notification feed
 * plus a system tray notification (system tray wiring TBD in a follow-up).
 *
 * Registered in AndroidManifest.xml under <service android:name=…/>.
 */
@AndroidEntryPoint
class NeerlyMessagingService : FirebaseMessagingService() {

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var tokens: TokenStore

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        // Only try to send if we're signed in — otherwise the Authenticator would
        // spam refresh attempts. Login flow re-sends after sign-in.
        if (tokens.accessToken.isNullOrBlank()) return
        scope.launch {
            runCatching {
                auth.registerDevice(
                    fcmToken = newToken,
                    appVersion = android.os.Build.VERSION.RELEASE,
                    osVersion = android.os.Build.VERSION.INCREMENTAL
                )
            }
        }
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        // TODO: show a system tray notification for data-only pushes.
        // For Session-7+ the app already shows in-app feed rows because the
        // backend wrote the NOTIFICATION row before pinging FCM.
    }
}
