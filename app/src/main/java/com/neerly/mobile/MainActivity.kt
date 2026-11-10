package com.neerly.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.neerly.mobile.core.design.NeerlyTheme
import com.neerly.mobile.core.design.Role
import com.neerly.mobile.data.auth.TokenStore
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.navigation.NeerlyNavHost
import com.neerly.mobile.feature.checkout.GatewayResult
import com.neerly.mobile.feature.checkout.GatewayResults
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    @Inject lateinit var cartStore: CartStore
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var gatewayResults: GatewayResults
    @Inject lateinit var appConfig: com.neerly.mobile.data.config.AppConfigRepository
    @Inject lateinit var languageSync: com.neerly.mobile.core.util.LanguageSync
    @Inject lateinit var deepLinks: com.neerly.mobile.push.DeepLinkBus
    @Inject lateinit var pushRegistration: com.neerly.mobile.push.PushRegistration
    @Inject lateinit var sessionExpiry: com.neerly.mobile.data.auth.SessionExpiry

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.neerly.mobile.core.util.AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Server notices in the language picked in the app (also covers a choice made on the
        // Welcome screen before signing in).
        languageSync.push()
        // The app draws edge-to-edge, so *something* has to inset the content or
        // it renders under the clock and the gesture bar. Doing it once here —
        // rather than per screen — fixes every destination at the root, and
        // Material3 `Scaffold`/`TopAppBar` below subtract what we consume here,
        // so nothing double-pads. IME insets are deliberately left unconsumed
        // for `Modifier.imePadding()` on the screens that take text input.
        enableEdgeToEdge()
        // Business values (fees, cut-offs, limits, support hours) come from the server; the
        // last copy is used until this lands, and stays if the phone is offline.
        lifecycleScope.launch { appConfig.refresh() }
        // Keeps the device row (token, role, app version) fresh for pushes on every launch.
        if (tokenStore.hasSession()) pushRegistration.register()
        takePushLink(intent)
        setContent {
            val config by appConfig.config.collectAsState()
            CompositionLocalProvider(com.neerly.mobile.data.config.LocalAppConfig provides config) {
                NeerlyTheme(role = Role.CUSTOMER) {
                    Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                        val navController = rememberNavController()
                        NeerlyNavHost(navController, cartStore, tokenStore, languageSync)
                        com.neerly.mobile.navigation.PushLinkHandler(navController, deepLinks, tokenStore)
                        com.neerly.mobile.navigation.SessionExpiryHandler(navController, sessionExpiry)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takePushLink(intent)
    }

    /** A notification tap (`boondh://…`) becomes a pending route the NavHost opens once past sign-in. */
    private fun takePushLink(intent: android.content.Intent?) {
        val link = intent?.dataString
        val route = com.neerly.mobile.push.DeepLinks.route(link) ?: return
        com.neerly.mobile.push.DeepLinks.declineOrderId(link)?.let(com.neerly.mobile.push.PendingDecline::add)
        intent?.data = null  // don't reopen it on rotation
        deepLinks.open(route)
    }

    /**
     * Razorpay reports here when its overlay closes. The "with data" listener is the one
     * that carries the signature the backend needs to verify a capture; the plain one
     * gave only a payment id, so nothing could ever be captured. Whoever opened the
     * payment (checkout, wallet) is waiting on [GatewayResults].
     */
    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        val id = razorpayPaymentId ?: data?.paymentId
        if (id == null) {
            gatewayResults.publish(GatewayResult.NotPaid(-1, null))
            return
        }
        gatewayResults.publish(GatewayResult.Paid(id, data?.orderId, data?.signature))
    }

    override fun onPaymentError(code: Int, description: String?, data: PaymentData?) {
        gatewayResults.publish(GatewayResult.NotPaid(code, description))
    }
}
