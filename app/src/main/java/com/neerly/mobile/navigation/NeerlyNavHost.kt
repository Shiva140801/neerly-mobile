package com.neerly.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.feature.address.AddressListScreen
import com.neerly.mobile.feature.auth.AddressScreen
import com.neerly.mobile.feature.auth.LanguageScreen
import com.neerly.mobile.feature.auth.NameScreen
import com.neerly.mobile.feature.auth.OtpScreen
import com.neerly.mobile.feature.auth.PhoneScreen
import com.neerly.mobile.feature.auth.SplashScreen
import com.neerly.mobile.feature.auth.WelcomeScreen
import com.neerly.mobile.feature.cart.CartScreen
import com.neerly.mobile.feature.checkout.CheckoutScreen
import com.neerly.mobile.feature.complaint.ComplaintFileScreen
import com.neerly.mobile.feature.customer.CustomerHomeScreen
import com.neerly.mobile.feature.customer.VendorDetailScreen
import com.neerly.mobile.feature.notification.NotificationFeedScreen
import com.neerly.mobile.feature.order.OrderHistoryScreen
import com.neerly.mobile.feature.order.OrderPlacedScreen
import com.neerly.mobile.feature.order.OrderTrackingScreen
import com.neerly.mobile.feature.review.ReviewSubmitScreen
import com.neerly.mobile.feature.vendor.VendorOnboardingWizard

object Routes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val Phone = "phone"
    const val Otp = "otp/{phone}"
    const val Name = "name"
    const val Language = "language"
    const val Address = "address"
    const val CustomerHome = "customer/home"
    const val VendorDetail = "customer/vendor/{vendorId}"
    const val VendorOnboarding = "vendor/onboarding"

    // Session 7 – Trust & Ops
    const val ReviewSubmit = "customer/review/{orderId}/{vendorName}"
    const val ComplaintFile = "customer/complaint?orderId={orderId}"
    const val NotificationFeed = "notifications"

    // Week-1 customer shell
    const val AddressList = "customer/addresses"

    // Week-2 cart → checkout → order
    const val Cart = "customer/cart"
    const val Checkout = "customer/checkout"
    const val OrderPlaced = "customer/order/{orderId}/placed/{orderNumber}"
    const val OrderTracking = "customer/order/{orderId}"
    const val OrderHistory = "customer/orders"

    fun otp(phone: String): String = "otp/$phone"
    fun vendorDetail(vendorId: String): String = "customer/vendor/$vendorId"
    fun reviewSubmit(orderId: String, vendorName: String): String =
        "customer/review/$orderId/${vendorName.replace("/", "-")}"
    fun complaintFile(orderId: String?): String =
        if (orderId.isNullOrBlank()) "customer/complaint?orderId=" else "customer/complaint?orderId=$orderId"
    fun orderPlaced(orderId: String, orderNumber: String) = "customer/order/$orderId/placed/$orderNumber"
    fun orderTracking(orderId: String) = "customer/order/$orderId"
}

@Composable
fun NeerlyNavHost(nav: NavHostController, cartStore: CartStore) {
    NavHost(navController = nav, startDestination = Routes.Splash) {
        composable(Routes.Splash) {
            SplashScreen(onDone = { nav.navigate(Routes.Welcome) { popUpTo(Routes.Splash) { inclusive = true } } })
        }
        composable(Routes.Welcome) {
            WelcomeScreen(onGetStarted = { nav.navigate(Routes.Phone) })
        }
        composable(Routes.Phone) {
            PhoneScreen(onOtpSent = { phone -> nav.navigate(Routes.otp(phone)) })
        }
        composable(Routes.Otp) { entry ->
            val phone = entry.arguments?.getString("phone").orEmpty()
            OtpScreen(phone = phone, onVerified = { nav.navigate(Routes.Name) })
        }
        composable(Routes.Name) {
            NameScreen(onContinue = { nav.navigate(Routes.Language) })
        }
        composable(Routes.Language) {
            LanguageScreen(onPicked = { nav.navigate(Routes.Address) })
        }
        composable(Routes.Address) {
            AddressScreen(onSaved = {
                nav.navigate(Routes.CustomerHome) { popUpTo(Routes.Welcome) { inclusive = true } }
            })
        }
        composable(Routes.CustomerHome) {
            CustomerHomeScreen(
                onVendorClick = { id -> nav.navigate(Routes.vendorDetail(id)) },
                onOrderClick = { id -> nav.navigate(Routes.orderTracking(id)) }
            )
        }
        composable(Routes.VendorDetail) { entry ->
            val vendorId = entry.arguments?.getString("vendorId").orEmpty()
            VendorDetailScreen(
                vendorId = vendorId,
                onBack = { nav.popBackStack() },
                onAddToCart = { nav.navigate(Routes.Cart) }
            )
        }
        composable(Routes.VendorOnboarding) {
            VendorOnboardingWizard(onSubmitted = { nav.popBackStack() })
        }
        composable(Routes.ReviewSubmit) { entry ->
            val orderId = entry.arguments?.getString("orderId").orEmpty()
            val vendorName = entry.arguments?.getString("vendorName").orEmpty()
            ReviewSubmitScreen(
                orderId = orderId,
                vendorName = vendorName,
                onSubmit = { _, _ -> nav.popBackStack() },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.ComplaintFile) { entry ->
            val orderId = entry.arguments?.getString("orderId").orEmpty().takeIf { it.isNotBlank() }
            ComplaintFileScreen(
                orderId = orderId,
                onSubmit = { _, _, _ -> nav.popBackStack() },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.NotificationFeed) {
            NotificationFeedScreen(
                items = emptyList(),
                loading = false,
                onOpenItem = { /* TODO open deep link from payload */ },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.AddressList) {
            AddressListScreen(
                onBack = { nav.popBackStack() },
                onAddNew = { /* TODO wire AddressScreen in create mode */ },
                onEdit = { /* TODO wire AddressScreen in edit mode with id */ }
            )
        }
        composable(Routes.Cart) {
            CartScreen(
                onBack = { nav.popBackStack() },
                onCheckout = { nav.navigate(Routes.Checkout) }
            )
        }
        composable(Routes.Checkout) {
            CheckoutScreen(
                cart = cartStore,
                onBack = { nav.popBackStack() },
                onOrderPlaced = { order ->
                    nav.navigate(Routes.orderPlaced(order.id, order.orderNumber)) {
                        popUpTo(Routes.CustomerHome)
                    }
                },
                onLaunchPayment = { order, _ ->
                    // Razorpay integration launches from the host Activity; for V1 navigate
                    // to the placed screen after a successful launch. Payment capture
                    // is handled in the activity's onPaymentSuccess callback.
                    nav.navigate(Routes.orderPlaced(order.id, order.orderNumber)) {
                        popUpTo(Routes.CustomerHome)
                    }
                }
            )
        }
        composable(Routes.OrderPlaced) { entry ->
            val orderId = entry.arguments?.getString("orderId").orEmpty()
            val orderNumber = entry.arguments?.getString("orderNumber").orEmpty()
            OrderPlacedScreen(
                orderId = orderId,
                orderNumber = orderNumber,
                onTrack = { nav.navigate(Routes.orderTracking(orderId)) { popUpTo(Routes.CustomerHome) } },
                onHome = { nav.popBackStack(Routes.CustomerHome, inclusive = false) }
            )
        }
        composable(Routes.OrderTracking) {
            OrderTrackingScreen(
                onBack = { nav.popBackStack() },
                onRateOrder = { id -> nav.navigate(Routes.reviewSubmit(id, "this order")) },
                onFileComplaint = { id -> nav.navigate(Routes.complaintFile(id)) }
            )
        }
        composable(Routes.OrderHistory) {
            OrderHistoryScreen(
                onBack = { nav.popBackStack() },
                onOpen = { id -> nav.navigate(Routes.orderTracking(id)) }
            )
        }
    }
}
