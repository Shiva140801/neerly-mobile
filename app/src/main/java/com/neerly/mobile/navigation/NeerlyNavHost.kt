package com.neerly.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.neerly.mobile.feature.auth.AddressScreen
import com.neerly.mobile.feature.auth.LanguageScreen
import com.neerly.mobile.feature.auth.NameScreen
import com.neerly.mobile.feature.auth.OtpScreen
import com.neerly.mobile.feature.auth.PhoneScreen
import com.neerly.mobile.feature.auth.SplashScreen
import com.neerly.mobile.feature.auth.WelcomeScreen
import com.neerly.mobile.feature.complaint.ComplaintFileScreen
import com.neerly.mobile.feature.customer.CustomerHomeScreen
import com.neerly.mobile.feature.customer.VendorDetailScreen
import com.neerly.mobile.feature.notification.NotificationFeedScreen
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

    fun otp(phone: String): String = "otp/$phone"
    fun vendorDetail(vendorId: String): String = "customer/vendor/$vendorId"
    fun reviewSubmit(orderId: String, vendorName: String): String =
        "customer/review/$orderId/${vendorName.replace("/", "-")}"
    fun complaintFile(orderId: String?): String =
        if (orderId.isNullOrBlank()) "customer/complaint?orderId=" else "customer/complaint?orderId=$orderId"
}

@Composable
fun NeerlyNavHost(nav: NavHostController) {
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
            CustomerHomeScreen(onVendorClick = { id -> nav.navigate(Routes.vendorDetail(id)) })
        }
        composable(Routes.VendorDetail) { entry ->
            val vendorId = entry.arguments?.getString("vendorId").orEmpty()
            VendorDetailScreen(
                vendorId = vendorId,
                onBack = { nav.popBackStack() },
                onAddToCart = { /* TODO cart in Session 3 */ }
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
    }
}
