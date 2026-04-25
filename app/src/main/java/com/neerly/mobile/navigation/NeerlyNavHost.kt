package com.neerly.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.feature.address.AddressFormScreen
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
import com.neerly.mobile.feature.complaint.ComplaintFileViewModel
import com.neerly.mobile.feature.complaint.ComplaintThreadScreen
import com.neerly.mobile.feature.customer.CustomerHomeScreen
import com.neerly.mobile.feature.customer.VendorDetailScreen
import com.neerly.mobile.feature.deposit.DepositsScreen
import com.neerly.mobile.feature.event.EventBookingScreen
import com.neerly.mobile.feature.notification.NotificationFeedScreen
import com.neerly.mobile.feature.notification.NotificationFeedViewModel
import com.neerly.mobile.feature.notification.NotificationPrefsScreen
import com.neerly.mobile.feature.order.OrderHistoryScreen
import com.neerly.mobile.feature.order.OrderPlacedScreen
import com.neerly.mobile.feature.order.OrderTrackingScreen
import com.neerly.mobile.feature.profile.ProfileScreen
import com.neerly.mobile.feature.review.ReviewSubmitScreen
import com.neerly.mobile.feature.review.ReviewSubmitViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.feature.subscription.SubscriptionCreateScreen
import com.neerly.mobile.feature.subscription.SubscriptionDetailScreen
import com.neerly.mobile.feature.subscription.SubscriptionListScreen
import com.neerly.mobile.feature.driver.DriverCodReconcileScreen
import com.neerly.mobile.feature.driver.DriverHomeScreen
import com.neerly.mobile.feature.vendor.VendorOnboardingWizard
import com.neerly.mobile.feature.vendor.catalog.VendorCatalogScreen
import com.neerly.mobile.feature.vendor.compliance.VendorComplianceScreen
import com.neerly.mobile.feature.vendor.dashboard.VendorTodayScreen
import com.neerly.mobile.feature.vendor.earnings.VendorEarningsScreen
import com.neerly.mobile.feature.vendor.orders.VendorOrderDetailScreen
import com.neerly.mobile.feature.vendor.bank.VendorBankScreen
import com.neerly.mobile.feature.vendor.settings.VendorBusinessConfigScreen
import com.neerly.mobile.feature.vendor.settings.VendorSettingsScreen
import com.neerly.mobile.feature.vendor.team.VendorTeamScreen
import com.neerly.mobile.feature.vendor.subscriptions.VendorSubscriptionsTodayScreen
import com.neerly.mobile.feature.wallet.WalletScreen

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

    // Session 7
    const val ReviewSubmit = "customer/review/{orderId}/{vendorName}"
    const val ComplaintFile = "customer/complaint?orderId={orderId}"
    const val NotificationFeed = "notifications"
    const val NotificationPrefs = "notifications/prefs"

    // Week 1
    const val AddressList = "customer/addresses"
    const val AddressNew = "customer/addresses/new"
    const val AddressEdit = "customer/addresses/edit/{addressId}"
    fun addressEdit(id: String) = "customer/addresses/edit/$id"

    // Week 2 — purchase flow
    const val Cart = "customer/cart"
    const val Checkout = "customer/checkout"
    const val OrderPlaced = "customer/order/{orderId}/placed/{orderNumber}"
    const val OrderTracking = "customer/order/{orderId}"
    const val OrderHistory = "customer/orders"

    // Week 3 — deep flows
    const val Wallet = "customer/wallet"
    const val Subscriptions = "customer/subscriptions"
    const val SubscriptionNew = "customer/subscription/new"
    const val EventBookingNew = "customer/event/new"
    const val SubscriptionDetail = "customer/subscription/{subscriptionId}"
    const val Deposits = "customer/deposits"
    const val ComplaintThread = "customer/complaint/{complaintId}"
    const val Profile = "customer/profile"

    // Week 4-5 — vendor app
    const val VendorToday = "vendor/today"
    const val VendorOrderDetail = "vendor/order/{orderId}"
    const val VendorCatalog = "vendor/catalog"
    const val VendorEarnings = "vendor/earnings"
    const val VendorCompliance = "vendor/compliance"
    const val VendorSubscriptionsToday = "vendor/subscriptions/today"
    const val VendorSettings = "vendor/settings"
    const val VendorBusinessConfig = "vendor/business-config"
    const val VendorTeam = "vendor/team"
    const val VendorBank = "vendor/bank"

    // Week 6 — driver app
    const val DriverHome = "driver/home"
    const val DriverCod = "driver/cod"

    fun vendorOrderDetail(id: String) = "vendor/order/$id"

    fun otp(phone: String): String = "otp/$phone"
    fun vendorDetail(vendorId: String): String = "customer/vendor/$vendorId"
    fun reviewSubmit(orderId: String, vendorName: String): String =
        "customer/review/$orderId/${vendorName.replace("/", "-")}"
    fun complaintFile(orderId: String?): String =
        if (orderId.isNullOrBlank()) "customer/complaint?orderId=" else "customer/complaint?orderId=$orderId"
    fun orderPlaced(orderId: String, orderNumber: String) = "customer/order/$orderId/placed/$orderNumber"
    fun orderTracking(orderId: String) = "customer/order/$orderId"
    fun subscriptionDetail(id: String) = "customer/subscription/$id"
    fun complaintThread(id: String) = "customer/complaint/$id"
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
        composable(Routes.Name) { NameScreen(onContinue = { nav.navigate(Routes.Language) }) }
        composable(Routes.Language) { LanguageScreen(onPicked = { nav.navigate(Routes.Address) }) }
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
            val vm: ReviewSubmitViewModel = hiltViewModel()
            ReviewSubmitScreen(
                orderId = orderId,
                vendorName = vendorName,
                onSubmit = { _, vendor, water, text ->
                    vm.submit(orderId, vendor, water, text) { nav.popBackStack() }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.ComplaintFile) { entry ->
            val orderId = entry.arguments?.getString("orderId").orEmpty().takeIf { it.isNotBlank() }
            val vm: ComplaintFileViewModel = hiltViewModel()
            ComplaintFileScreen(
                orderId = orderId,
                onSubmit = { category, subject, description ->
                    vm.submit(orderId, category, subject, description) { complaintId ->
                        // Drop the file screen, jump to the live thread.
                        nav.navigate(Routes.complaintThread(complaintId)) {
                            popUpTo(Routes.ComplaintFile) { inclusive = true }
                        }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.NotificationFeed) {
            val vm: NotificationFeedViewModel = hiltViewModel()
            val s = vm.state.collectAsState().value
            NotificationFeedScreen(
                items = s.items,
                loading = s.loading,
                onOpenItem = vm::markRead,
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.NotificationPrefs) {
            NotificationPrefsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.AddressList) {
            AddressListScreen(
                onBack = { nav.popBackStack() },
                onAddNew = { nav.navigate(Routes.AddressNew) },
                onEdit = { id -> nav.navigate(Routes.addressEdit(id)) }
            )
        }
        composable(Routes.AddressNew) {
            AddressFormScreen(
                onSaved = { nav.popBackStack() },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Routes.AddressEdit,
            arguments = listOf(androidx.navigation.navArgument("addressId") { })
        ) {
            AddressFormScreen(
                onSaved = { nav.popBackStack() },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.Cart) {
            CartScreen(onBack = { nav.popBackStack() }, onCheckout = { nav.navigate(Routes.Checkout) })
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
        // ---- Week 3 ----
        composable(Routes.Wallet) {
            WalletScreen(
                onBack = { nav.popBackStack() },
                onTopupReady = { _, _ -> }
            )
        }
        composable(Routes.Subscriptions) {
            SubscriptionListScreen(
                onBack = { nav.popBackStack() },
                onOpen = { id -> nav.navigate(Routes.subscriptionDetail(id)) },
                onNew = { nav.navigate(Routes.SubscriptionNew) }
            )
        }
        composable(Routes.SubscriptionNew) {
            SubscriptionCreateScreen(
                onCreated = { id ->
                    nav.navigate(Routes.subscriptionDetail(id)) {
                        popUpTo(Routes.Subscriptions)
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.EventBookingNew) {
            EventBookingScreen(
                onCreated = { _ ->
                    // TODO Sprint 2: navigate to a dedicated EventBookingDetailScreen
                    nav.popBackStack(Routes.CustomerHome, inclusive = false)
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.SubscriptionDetail) {
            SubscriptionDetailScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.Deposits) {
            DepositsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.ComplaintThread) {
            ComplaintThreadScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.Profile) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onAddresses = { nav.navigate(Routes.AddressList) },
                onWallet = { nav.navigate(Routes.Wallet) },
                onOrders = { nav.navigate(Routes.OrderHistory) },
                onSubscriptions = { nav.navigate(Routes.Subscriptions) },
                onDeposits = { nav.navigate(Routes.Deposits) },
                onNotifications = { nav.navigate(Routes.NotificationFeed) },
                onLogout = { nav.navigate(Routes.Welcome) { popUpTo(Routes.CustomerHome) { inclusive = true } } }
            )
        }
        // ---- Week 4-5 vendor app ----
        composable(Routes.VendorToday) {
            VendorTodayScreen(
                onOpen = { id -> nav.navigate(Routes.vendorOrderDetail(id)) },
                onCatalog = { nav.navigate(Routes.VendorCatalog) },
                onEarnings = { nav.navigate(Routes.VendorEarnings) },
                onSettings = { nav.navigate(Routes.VendorSettings) }
            )
        }
        composable(Routes.VendorOrderDetail) { VendorOrderDetailScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorCatalog) { VendorCatalogScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorEarnings) { VendorEarningsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorCompliance) { VendorComplianceScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorSubscriptionsToday) {
            VendorSubscriptionsTodayScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.VendorSettings) {
            VendorSettingsScreen(
                onBack = { nav.popBackStack() },
                onCompliance = { nav.navigate(Routes.VendorCompliance) },
                onSubscriptionsToday = { nav.navigate(Routes.VendorSubscriptionsToday) },
                onBusinessConfig = { nav.navigate(Routes.VendorBusinessConfig) },
                onTeam = { nav.navigate(Routes.VendorTeam) },
                onBank = { nav.navigate(Routes.VendorBank) },
                onLogout = { nav.navigate(Routes.Welcome) { popUpTo(Routes.VendorToday) { inclusive = true } } }
            )
        }
        composable(Routes.VendorBusinessConfig) {
            VendorBusinessConfigScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.VendorTeam) {
            VendorTeamScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.VendorBank) {
            VendorBankScreen(onBack = { nav.popBackStack() })
        }
        // ---- Week 6 driver app ----
        composable(Routes.DriverHome) {
            DriverHomeScreen(onCodReconcile = { nav.navigate(Routes.DriverCod) })
        }
        composable(Routes.DriverCod) {
            DriverCodReconcileScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
        }
    }
}
