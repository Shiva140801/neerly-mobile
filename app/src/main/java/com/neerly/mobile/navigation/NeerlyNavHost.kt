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
import com.neerly.mobile.feature.customer.CustomerHomeScreen
import com.neerly.mobile.feature.customer.VendorDetailScreen
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

    fun otp(phone: String): String = "otp/$phone"
    fun vendorDetail(vendorId: String): String = "customer/vendor/$vendorId"
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
    }
}
