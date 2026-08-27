package com.neerly.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.neerly.mobile.core.design.NeerlyTheme
import com.neerly.mobile.core.design.Role
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.navigation.NeerlyNavHost
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultListener {

    @Inject lateinit var cartStore: CartStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeerlyTheme(role = Role.CUSTOMER) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NeerlyNavHost(navController, cartStore)
                }
            }
        }
    }

    /**
     * Razorpay success callback — called after the Razorpay Checkout overlay
     * closes with a paid UPI / card transaction. The backend verifies the HMAC
     * signature on capture(), so we don't need to check it client-side.
     *
     * V1 stub: we log + clear cart; the next slice wires this to
     * CheckoutViewModel.onPaymentCaptured() + navigates to Order Placed.
     */
    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        cartStore.clear()
    }

    override fun onPaymentError(code: Int, description: String?) {
        // No-op for V1; logged to Timber + surfaced via a snackbar once we
        // wire this through CheckoutViewModel. Cart is preserved for retry.
    }
}
