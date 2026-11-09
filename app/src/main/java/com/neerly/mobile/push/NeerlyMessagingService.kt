package com.neerly.mobile.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * FCM entry point. The server sends data-only messages (plan §c), so every push — app open
 * or closed — arrives here and goes through [PushRouter], the same path Push lab uses.
 *
 * Registered in AndroidManifest.xml.
 */
@AndroidEntryPoint
class NeerlyMessagingService : FirebaseMessagingService() {

    @Inject lateinit var router: PushRouter
    @Inject lateinit var registration: PushRegistration

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        // Ignored while signed out; sign-in registers again with the current token.
        registration.register(newToken)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        val data = msg.data.takeIf { it.isNotEmpty() }
            // A notification-only message (old server) still shows, as an unknown type.
            ?: msg.notification?.let { n -> mapOf("v" to "1", "title" to n.title.orEmpty(), "body" to n.body.orEmpty()) }
            ?: return
        router.handle(data)
    }
}
