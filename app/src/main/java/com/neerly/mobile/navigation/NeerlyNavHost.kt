package com.neerly.mobile.navigation

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.neerly.mobile.data.auth.TokenStore
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.feature.address.AddressFormScreen
import com.neerly.mobile.feature.address.AddressListScreen
import com.neerly.mobile.feature.auth.LanguageScreen
import com.neerly.mobile.feature.auth.NameScreen
import com.neerly.mobile.feature.auth.OtpScreen
import com.neerly.mobile.feature.auth.PhoneScreen
import com.neerly.mobile.feature.auth.PortalRole
import com.neerly.mobile.feature.auth.RoleGateScreen
import com.neerly.mobile.feature.auth.SessionViewModel
import com.neerly.mobile.feature.auth.SplashScreen
import com.neerly.mobile.feature.auth.WelcomeScreen
import com.neerly.mobile.feature.cart.CartScreen
import com.neerly.mobile.feature.checkout.CheckoutScreen
import com.neerly.mobile.feature.complaint.ComplaintFileScreen
import com.neerly.mobile.feature.complaint.ComplaintFileViewModel
import com.neerly.mobile.feature.complaint.ComplaintThreadScreen
import com.neerly.mobile.core.design.CustomerTab
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
import com.neerly.mobile.feature.search.CustomerSearchScreen
import com.neerly.mobile.feature.wallet.WalletScreen

object Routes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val Phone = "phone"
    const val Otp = "otp/{phone}/{role}"
    const val Name = "name"
    const val Language = "language"
    const val Address = "address"
    const val ReferralCode = "referral-code"
    const val RolePicker = "auth/role-picker"
    const val AdminTotp = "auth/admin-totp"
    const val RoleGate = "auth/enter/{role}?remember={remember}"
    /** Terms / Privacy, plain words (doc = TERMS | PRIVACY). */
    const val Legal = "legal/{doc}"
    /** Location is off for Boondh — steps, Open settings, or type the address. */
    const val LocationDenied = "location-denied"
    fun legal(doc: com.neerly.mobile.feature.legal.LegalDoc): String = "legal/${doc.name}"
    fun roleGate(role: String, remember: Boolean = false) = "auth/enter/$role?remember=$remember"
    const val CustomerHome = "customer/home"
    const val CustomerSearch = "customer/search"
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
    const val WalletWithdraw = "customer/wallet/withdraw"
    const val Subscriptions = "customer/subscriptions"
    const val SubscriptionNew = "customer/subscription/new"
    const val EventBookingNew = "customer/event/new"
    const val SubscriptionDetail = "customer/subscription/{subscriptionId}"
    const val Deposits = "customer/deposits"
    const val ComplaintThread = "customer/complaint/{complaintId}"
    const val Profile = "customer/profile"
    const val CancelOrder = "customer/order/{orderId}/cancel"
    fun cancelOrder(orderId: String) = "customer/order/$orderId/cancel"
    const val Refunds = "customer/refunds"
    const val RefundDetail = "customer/refunds/{refundId}"
    fun refundDetail(id: String) = "customer/refunds/$id"
    const val RefundSetting = "customer/profile/refund-destination"
    const val EditProfile = "customer/profile/edit"
    const val ChangePhone = "customer/profile/change-phone"
    const val DeleteAccount = "customer/profile/delete"
    const val ReferEarn = "customer/refer"
    const val Invoice = "customer/invoice/{orderId}"
    fun invoice(orderId: String) = "customer/invoice/$orderId"
    const val Complaints = "customer/complaints"
    const val Help = "customer/help?orderId={orderId}"

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
    const val VendorShop = "vendor/shop"
    const val VendorArea = "vendor/area"
    const val VendorReviews = "vendor/reviews"
    const val VendorComplaints = "vendor/complaints"
    const val VendorEvents = "vendor/events"
    const val VendorBank = "vendor/bank"
    const val VendorProductEdit = "vendor/catalog/edit/{productId}"
    const val VendorProductNew = "vendor/catalog/new"
    const val VendorJars = "vendor/jars"
    const val VendorJarCount = "vendor/jars/count"
    const val VendorJarPickups = "vendor/jars/pickups"
    const val VendorClaims = "vendor/jars/claims"
    const val VendorClaimNew = "vendor/jars/claims/new/{returnId}"
    const val VendorClaimDetail = "vendor/jars/claims/detail/{claimId}"
    fun vendorProductEdit(id: String) = "vendor/catalog/edit/$id"
    fun vendorClaimNew(returnId: String) = "vendor/jars/claims/new/$returnId"
    fun vendorClaimDetail(id: String) = "vendor/jars/claims/detail/$id"
    const val VendorOrders = "vendor/orders"
    const val VendorNewOrder = "vendor/new-order/{orderId}"
    const val VendorAlertSettings = "vendor/alert-settings"

    // Week 6 — driver app
    const val DriverHome = "driver/home"
    const val DriverCod = "driver/cod"
    const val DriverSos = "driver/sos?send={send}"
    const val DriverVehicle = "driver/vehicle"
    const val DriverDocuments = "driver/documents"
    const val DriverPickups = "driver/pickups"
    fun driverSos(send: Boolean) = "driver/sos?send=$send"

    fun vendorOrderDetail(id: String) = "vendor/order/$id"
    fun vendorNewOrder(id: String) = "vendor/new-order/$id"

    fun otp(phone: String, role: String): String = "otp/$phone/$role"
    fun vendorDetail(vendorId: String): String = "customer/vendor/$vendorId"
    fun reviewSubmit(orderId: String, orderNumber: String): String =
        "customer/review/$orderId/${android.net.Uri.encode(orderNumber)}"
    fun complaintFile(orderId: String?): String =
        if (orderId.isNullOrBlank()) "customer/complaint?orderId=" else "customer/complaint?orderId=$orderId"
    fun orderPlaced(orderId: String, orderNumber: String) = "customer/order/$orderId/placed/$orderNumber"
    fun orderTracking(orderId: String) = "customer/order/$orderId"
    fun subscriptionDetail(id: String) = "customer/subscription/$id"
    fun complaintThread(id: String) = "customer/complaint/$id"
    fun help(orderId: String?) = "customer/help?orderId=${orderId.orEmpty()}"
}

/** Where each app-facing role lands once its token is active. */
private fun homeFor(role: String): String = when (role) {
    PortalRole.VENDOR -> Routes.VendorToday
    PortalRole.DRIVER -> Routes.DriverHome
    else -> Routes.CustomerHome
}

/**
 * After sign-in or on relaunch: a remembered role goes straight through the gate, several
 * roles get the picker, a single role goes through the gate (which is a no-op when the
 * token is already in that role).
 */
private fun entryRouteFor(tokenStore: TokenStore): String {
    val roles = PortalRole.of(tokenStore.grantedRoles)
    val pref = tokenStore.lastRolePreference?.takeIf { it in roles }
    return when {
        pref != null -> Routes.roleGate(pref, remember = true)
        roles.size > 1 -> Routes.RolePicker
        else -> Routes.roleGate(roles.firstOrNull() ?: PortalRole.CUSTOMER)
    }
}

/**
 * Bottom-tab navigation. Every tab pops back to Home rather than stacking, so
 * five taps around the bar leave one entry on the back stack, and `saveState` /
 * `restoreState` keep each tab's scroll position where the customer left it.
 */
/**
 * Seller tabs (board VTabBar). Jars and Shop land on the closest screens that exist today
 * (catalog stock, shop settings) until their own boards are built.
 */
/** A vendor tab's root screen with the tab bar under it, so every tab can reach the others. */
@Composable
private fun VendorTabRoot(tab: com.neerly.mobile.core.design.VendorTab, nav: NavHostController, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) { content() }
        com.neerly.mobile.core.design.VendorTabBar(current = tab, onSelect = nav::switchVendorTab)
    }
}

private fun NavHostController.switchVendorTab(tab: com.neerly.mobile.core.design.VendorTab) {
    val route = when (tab) {
        com.neerly.mobile.core.design.VendorTab.Today -> Routes.VendorToday
        com.neerly.mobile.core.design.VendorTab.Orders -> Routes.VendorOrders
        com.neerly.mobile.core.design.VendorTab.Jars -> Routes.VendorJars
        com.neerly.mobile.core.design.VendorTab.Earnings -> Routes.VendorEarnings
        com.neerly.mobile.core.design.VendorTab.Shop -> Routes.VendorShop
    }
    navigate(route) {
        popUpTo(Routes.VendorToday) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.switchTab(tab: CustomerTab) {
    val route = when (tab) {
        CustomerTab.Home -> Routes.CustomerHome
        CustomerTab.Orders -> Routes.OrderHistory
        CustomerTab.Subs -> Routes.Subscriptions
        CustomerTab.Wallet -> Routes.Wallet
        CustomerTab.Profile -> Routes.Profile
    }
    navigate(route) {
        popUpTo(Routes.CustomerHome) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun NeerlyNavHost(nav: NavHostController, cartStore: CartStore, tokenStore: TokenStore,
                  languageSync: com.neerly.mobile.core.util.LanguageSync? = null) {
    NavHost(navController = nav, startDestination = Routes.Splash) {
        composable(Routes.Splash) {
            SplashScreen(onDone = {
                if (tokenStore.hasSession()) {
                    nav.navigate(entryRouteFor(tokenStore)) { popUpTo(Routes.Splash) { inclusive = true } }
                } else {
                    nav.navigate(Routes.Welcome) { popUpTo(Routes.Splash) { inclusive = true } }
                }
            })
        }
        composable(Routes.Welcome) {
            WelcomeScreen(
                onGetStarted = { nav.navigate(Routes.Phone + "?role=CUSTOMER") },
                onVendorRegister = { nav.navigate(Routes.Phone + "?role=VENDOR") },
                onTerms = { nav.navigate(Routes.legal(com.neerly.mobile.feature.legal.LegalDoc.Terms)) },
                onPrivacy = { nav.navigate(Routes.legal(com.neerly.mobile.feature.legal.LegalDoc.Privacy)) }
            )
        }
        composable(Routes.Legal) { entry ->
            val doc = runCatching { com.neerly.mobile.feature.legal.LegalDoc.valueOf(entry.arguments?.getString("doc").orEmpty()) }
                .getOrDefault(com.neerly.mobile.feature.legal.LegalDoc.Terms)
            com.neerly.mobile.feature.legal.LegalScreen(initial = doc, onDone = { nav.popBackStack() })
        }
        composable(Routes.LocationDenied) {
            // The screen that sent us here reads "location_choice" from its saved state:
            // "granted" (try GPS again) or "typed" (show the typed-address form).
            fun back(choice: String) {
                nav.previousBackStackEntry?.savedStateHandle?.set("location_choice", choice)
                nav.popBackStack()
            }
            com.neerly.mobile.feature.auth.LocationDeniedScreen(
                onContinue = { back("granted") },
                onTypeAddress = { back("typed") },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Routes.Phone + "?role={role}",
            arguments = listOf(androidx.navigation.navArgument("role") { defaultValue = "CUSTOMER" })
        ) { entry ->
            val role = entry.arguments?.getString("role") ?: "CUSTOMER"
            PhoneScreen(onOtpSent = { phone -> nav.navigate(Routes.otp(phone, role)) })
        }
        composable(Routes.Otp) { entry ->
            val phone = entry.arguments?.getString("phone").orEmpty()
            val role = entry.arguments?.getString("role") ?: "CUSTOMER"
            OtpScreen(phone = phone, onVerified = { isNew: Boolean, _: List<String> ->
                when {
                    // "I sell water" — the registration wizard, not customer onboarding.
                    role == "VENDOR" && PortalRole.VENDOR !in tokenStore.grantedRoles ->
                        nav.navigate(Routes.VendorOnboarding) { popUpTo(Routes.Welcome) { inclusive = true } }
                    isNew -> nav.navigate(Routes.Name)
                    else -> nav.navigate(entryRouteFor(tokenStore)) { popUpTo(Routes.Welcome) { inclusive = true } }
                }
            }, onEditNumber = { nav.popBackStack() })
        }
        composable(Routes.RolePicker) {
            com.neerly.mobile.feature.auth.RolePickerScreen(
                grantedRoles = tokenStore.grantedRoles,
                onRoleSelected = { role: String, remember: Boolean ->
                    nav.navigate(Routes.roleGate(role, remember)) { popUpTo(Routes.RolePicker) { inclusive = true } }
                }
            )
        }
        composable(
            Routes.RoleGate,
            arguments = listOf(
                androidx.navigation.navArgument("role") { defaultValue = PortalRole.CUSTOMER },
                androidx.navigation.navArgument("remember") {
                    type = androidx.navigation.NavType.BoolType; defaultValue = false
                }
            )
        ) { entry ->
            val role = entry.arguments?.getString("role") ?: PortalRole.CUSTOMER
            val remember = entry.arguments?.getBoolean("remember") ?: false
            RoleGateScreen(
                role = role,
                remember = remember,
                onReady = {
                    nav.navigate(homeFor(role)) {
                        popUpTo(nav.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { if (!nav.popBackStack()) nav.navigate(Routes.RolePicker) }
            )
        }
        composable(Routes.AdminTotp) {
            com.neerly.mobile.feature.auth.AdminTotpScreen(
                onVerified = {
                    nav.navigate(Routes.CustomerHome) { // Placeholder for Admin Dashboard
                        popUpTo(Routes.RolePicker) { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.Name) { NameScreen(onContinue = { nav.navigate(Routes.Language) }) }
        composable(Routes.Language) { LanguageScreen(onPicked = { nav.navigate(Routes.ReferralCode) }, languageSync = languageSync) }
        composable(Routes.ReferralCode) {
            com.neerly.mobile.feature.auth.ReferralCodeScreen(onDone = { nav.navigate(Routes.Address) })
        }
        composable(Routes.Address) {
            // Step 6: the same form as Saved addresses — real GPS pin, not a placeholder.
            com.neerly.mobile.feature.address.AddressFormScreen(
                onSaved = { nav.navigate(Routes.CustomerHome) { popUpTo(Routes.Welcome) { inclusive = true } } },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.CustomerHome) {
            CustomerHomeScreen(
                onVendorClick = { id -> nav.navigate(Routes.vendorDetail(id)) },
                onOrderClick = { id -> nav.navigate(Routes.orderTracking(id)) },
                onOpenProfile = { nav.navigate(Routes.Profile) },
                onOpenSearch = { nav.navigate(Routes.CustomerSearch) },
                onSelectTab = nav::switchTab,
                onOpenCart = { nav.navigate(Routes.Cart) },
                onSubscription = { id -> nav.navigate(Routes.subscriptionDetail(id)) },
                onAddAddress = { nav.navigate(Routes.AddressNew) }
            )
        }
        composable(Routes.CustomerSearch) {
            CustomerSearchScreen(
                onBack = { nav.popBackStack() },
                onOpenVendor = { vendorId -> nav.navigate(Routes.vendorDetail(vendorId)) },
                onBrowseVendors = { nav.popBackStack(Routes.CustomerHome, inclusive = false) }
            )
        }
        composable(Routes.VendorDetail) { entry ->
            val vendorId = entry.arguments?.getString("vendorId").orEmpty()
            VendorDetailScreen(
                vendorId = vendorId,
                onBack = { nav.popBackStack() },
                onAddToCart = { },
                onViewCart = { nav.navigate(Routes.Cart) }
            )
        }
        composable(Routes.VendorOnboarding) {
            val session: SessionViewModel = hiltViewModel()
            VendorOnboardingWizard(
                onOpenBank = { nav.navigate(Routes.VendorBank) },
                onAddDrivers = { nav.navigate(Routes.VendorTeam) },
                onSignInAsSeller = { session.logout { nav.navigate(Routes.Phone + "?role=VENDOR") { popUpTo(nav.graph.id) { inclusive = true } } } },
                onSubmitted = { nav.popBackStack() },
                // Arriving from "I sell water" leaves nothing beneath the wizard.
                onExit = { if (!nav.popBackStack()) nav.navigate(entryRouteFor(tokenStore)) }
            )
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
        composable(
            Routes.Help,
            arguments = listOf(androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType; defaultValue = "" })
        ) {
            com.neerly.mobile.feature.help.HelpScreen(
                onBack = { nav.popBackStack() },
                onOpenThread = { id -> nav.navigate(Routes.complaintThread(id)) { popUpTo(Routes.Help) { inclusive = true } } },
                onAllRequests = { nav.navigate(Routes.Complaints) }
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
                onBack = { nav.popBackStack() },
                onSettings = { nav.navigate(Routes.NotificationPrefs) }
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
                onAddAddress = { nav.navigate(Routes.AddressNew) },
                // Reached only once the order is settled: cash/wallet on placement,
                // UPI/card after the backend captured the payment.
                onOrderPlaced = { order ->
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
                onRateOrder = { id, number -> nav.navigate(Routes.reviewSubmit(id, number)) },
                onFileComplaint = { id -> nav.navigate(Routes.help(id)) },
                onCancel = { id -> nav.navigate(Routes.cancelOrder(id)) }
            )
        }
        composable(Routes.OrderHistory) {
            OrderHistoryScreen(
                onBack = { nav.popBackStack() },
                onOpen = { id -> nav.navigate(Routes.orderTracking(id)) },
                onSelectTab = nav::switchTab,
                onRate = { id, name -> nav.navigate(Routes.reviewSubmit(id, name)) },
                onInvoice = { id -> nav.navigate(Routes.invoice(id)) },
                onCart = { nav.navigate(Routes.Cart) }
            )
        }
        // ---- Week 3 ----
        composable(Routes.Wallet) {
            WalletScreen(
                onBack = { nav.popBackStack() },
                onSelectTab = nav::switchTab,
                onWithdraw = { nav.navigate(Routes.WalletWithdraw) },
                onJars = { nav.navigate(Routes.Deposits) }
            )
        }
        composable(Routes.WalletWithdraw) {
            com.neerly.mobile.feature.wallet.WithdrawScreen(
                onBack = { nav.popBackStack() },
                onJars = { nav.navigate(Routes.Deposits) }
            )
        }
        composable(Routes.Subscriptions) {
            SubscriptionListScreen(
                onBack = { nav.popBackStack() },
                onOpen = { id -> nav.navigate(Routes.subscriptionDetail(id)) },
                onNew = { nav.navigate(Routes.SubscriptionNew) },
                onSelectTab = nav::switchTab
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
            SubscriptionDetailScreen(onBack = { nav.popBackStack() }, onNewSubscription = { nav.navigate(Routes.SubscriptionNew) })
        }
        composable(Routes.Deposits) {
            DepositsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.ComplaintThread) {
            ComplaintThreadScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.Complaints) {
            com.neerly.mobile.feature.complaint.ComplaintsScreen(
                onBack = { nav.popBackStack() },
                onOpen = { nav.navigate(Routes.complaintThread(it)) },
                onNew = { nav.navigate(Routes.complaintFile(null)) })
        }
        composable(Routes.CancelOrder) {
            com.neerly.mobile.feature.order.CancelOrderScreen(
                onBack = { nav.popBackStack() },
                onRefunds = { nav.navigate(Routes.Refunds) { popUpTo(Routes.CustomerHome) } },
                onRefundSetting = { nav.navigate(Routes.RefundSetting) })
        }
        composable(Routes.Refunds) {
            com.neerly.mobile.feature.account.RefundsScreen(
                onBack = { nav.popBackStack() },
                onOpen = { nav.navigate(Routes.refundDetail(it)) },
                onSetting = { nav.navigate(Routes.RefundSetting) })
        }
        composable(Routes.RefundDetail) {
            com.neerly.mobile.feature.account.RefundDetailScreen(
                onBack = { nav.popBackStack() },
                onSetting = { nav.navigate(Routes.RefundSetting) },
                onHelp = { nav.navigate(Routes.help(null)) })
        }
        composable(Routes.RefundSetting) { com.neerly.mobile.feature.account.RefundSettingScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.EditProfile) {
            com.neerly.mobile.feature.account.EditProfileScreen(
                onBack = { nav.popBackStack() },
                onChangePhone = { nav.navigate(Routes.ChangePhone) },
                onDelete = { nav.navigate(Routes.DeleteAccount) })
        }
        composable(Routes.ChangePhone) { com.neerly.mobile.feature.account.ChangePhoneScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DeleteAccount) {
            com.neerly.mobile.feature.account.DeleteAccountScreen(
                onBack = { nav.popBackStack() },
                onDeleted = { nav.navigate(Routes.Welcome) { popUpTo(0) } })
        }
        composable(Routes.ReferEarn) { com.neerly.mobile.feature.account.ReferEarnScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.Invoice) { com.neerly.mobile.feature.account.InvoiceScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.Profile) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onAddresses = { nav.navigate(Routes.AddressList) },
                onWallet = { nav.navigate(Routes.Wallet) },
                onOrders = { nav.navigate(Routes.OrderHistory) },
                onSubscriptions = { nav.navigate(Routes.Subscriptions) },
                onDeposits = { nav.navigate(Routes.Deposits) },
                onComplaints = { nav.navigate(Routes.Complaints) },
                onNotifications = { nav.navigate(Routes.NotificationFeed) },
                onNotificationPrefs = { nav.navigate(Routes.NotificationPrefs) },
                onVendorDashboard = { nav.navigate(Routes.roleGate(PortalRole.VENDOR)) },
                onBecomeVendor = { nav.navigate(Routes.VendorOnboarding) },
                onDriverMode = { nav.navigate(Routes.roleGate(PortalRole.DRIVER)) },
                onEventBooking = { nav.navigate(Routes.EventBookingNew) },
                onSwitchRole = { nav.navigate(Routes.RolePicker) },
                onSelectTab = nav::switchTab,
                onLogout = { nav.navigate(Routes.Welcome) { popUpTo(Routes.CustomerHome) { inclusive = true } } },
                onEdit = { nav.navigate(Routes.EditProfile) },
                onRefunds = { nav.navigate(Routes.Refunds) },
                onRefundSetting = { nav.navigate(Routes.RefundSetting) },
                onHelp = { nav.navigate(Routes.help(null)) },
                onReferEarn = { nav.navigate(Routes.ReferEarn) },
                onLanguage = { nav.navigate(Routes.Language) },
                onLegal = { nav.navigate(Routes.legal(com.neerly.mobile.feature.legal.LegalDoc.Terms)) }
            )
        }
        // ---- Week 4-5 vendor app ----
        composable(Routes.VendorToday) {
            VendorTodayScreen(
                onOpen = { id -> nav.navigate(Routes.vendorOrderDetail(id)) },
                onCatalog = { nav.navigate(Routes.VendorCatalog) },
                onEarnings = { nav.navigate(Routes.VendorEarnings) },
                onSettings = { nav.navigate(Routes.VendorSettings) },
                onNewOrder = { id -> nav.navigate(Routes.vendorNewOrder(id)) { launchSingleTop = true } },
                onSelectTab = nav::switchVendorTab,
                onCompliance = { nav.navigate(Routes.VendorCompliance) },
                onRunSheet = { nav.navigate(Routes.VendorSubscriptionsToday) },
                onEvents = { nav.navigate(Routes.VendorEvents) },
                onJars = { nav.navigate(Routes.VendorJarPickups) }
            )
        }
        composable(Routes.VendorOrders) {
            com.neerly.mobile.feature.vendor.orders.VendorOrdersScreen(
                onOpen = { o -> nav.navigate(if (o.status == "VENDOR_ASSIGNED") Routes.vendorNewOrder(o.id) else Routes.vendorOrderDetail(o.id)) },
                onSelectTab = nav::switchVendorTab,
                onEvents = { nav.navigate(Routes.VendorEvents) }
            )
        }
        composable(Routes.VendorNewOrder) {
            com.neerly.mobile.feature.vendor.orders.VendorNewOrderScreen(
                onDone = { nav.popBackStack() },
                onAccepted = { id -> nav.navigate(Routes.vendorOrderDetail(id)) { popUpTo(Routes.VendorNewOrder) { inclusive = true } } },
                onAlertSettings = { nav.navigate(Routes.VendorAlertSettings) },
                onCatalog = { nav.navigate(Routes.VendorCatalog) },
                onCloseForHours = { nav.navigate(Routes.VendorBusinessConfig) }
            )
        }
        composable(Routes.VendorAlertSettings) {
            com.neerly.mobile.feature.vendor.orders.VendorAlertSettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.VendorOrderDetail) { VendorOrderDetailScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorCatalog) {
            VendorCatalogScreen(onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate(Routes.vendorProductEdit(id)) },
                onAdd = { nav.navigate(Routes.VendorProductNew) })
        }
        composable(Routes.VendorProductEdit) {
            com.neerly.mobile.feature.vendor.catalog.VendorProductEditScreen(onBack = { nav.popBackStack() },
                onCountJars = { nav.navigate(Routes.VendorJarCount) })
        }
        composable(Routes.VendorProductNew) {
            com.neerly.mobile.feature.vendor.catalog.VendorAddProductScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.VendorJars) {
            VendorTabRoot(com.neerly.mobile.core.design.VendorTab.Jars, nav) {
                com.neerly.mobile.feature.vendor.jars.VendorJarsScreen(
                    onCount = { nav.navigate(Routes.VendorJarCount) },
                    onPickups = { nav.navigate(Routes.VendorJarPickups) },
                    onDamage = { nav.navigate(Routes.VendorClaims) })
            }
        }
        composable(Routes.VendorJarCount) {
            com.neerly.mobile.feature.vendor.jars.VendorStockAdjustScreen(onBack = { nav.popBackStack() },
                onFileClaim = { nav.navigate(Routes.VendorJarPickups) })
        }
        composable(Routes.VendorJarPickups) {
            com.neerly.mobile.feature.vendor.jars.VendorJarReturnsScreen(onBack = { nav.popBackStack() },
                onFileClaim = { id -> nav.navigate(Routes.vendorClaimNew(id)) })
        }
        composable(Routes.VendorClaims) {
            com.neerly.mobile.feature.vendor.jars.VendorClaimsScreen(onBack = { nav.popBackStack() },
                onOpen = { id -> nav.navigate(Routes.vendorClaimDetail(id)) },
                onPickups = { nav.navigate(Routes.VendorJarPickups) })
        }
        composable(Routes.VendorClaimNew) {
            com.neerly.mobile.feature.vendor.jars.VendorClaimNewScreen(onBack = { nav.popBackStack() },
                onFiled = { id -> nav.navigate(Routes.vendorClaimDetail(id)) { popUpTo(Routes.VendorClaimNew) { inclusive = true } } })
        }
        composable(Routes.VendorClaimDetail) {
            com.neerly.mobile.feature.vendor.jars.VendorClaimDetailScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.VendorEarnings) {
            VendorTabRoot(com.neerly.mobile.core.design.VendorTab.Earnings, nav) {
                VendorEarningsScreen(onBank = { nav.navigate(Routes.VendorBank) })
            }
        }
        composable(Routes.VendorShop) {
            VendorTabRoot(com.neerly.mobile.core.design.VendorTab.Shop, nav) {
                com.neerly.mobile.feature.vendor.shop.VendorShopTabScreen(
                    onReviews = { nav.navigate(Routes.VendorReviews) },
                    onComplaints = { nav.navigate(Routes.VendorComplaints) },
                    onEvents = { nav.navigate(Routes.VendorEvents) },
                    onLicences = { nav.navigate(Routes.VendorCompliance) },
                    onCatalog = { nav.navigate(Routes.VendorCatalog) },
                    onHolidays = { nav.navigate(Routes.VendorBusinessConfig) },
                    onTeam = { nav.navigate(Routes.VendorTeam) },
                    onSettings = { nav.navigate(Routes.VendorSettings) })
            }
        }
        composable(Routes.VendorArea) { com.neerly.mobile.feature.vendor.settings.VendorAreaScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorReviews) { com.neerly.mobile.feature.vendor.shop.VendorReviewsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorComplaints) { com.neerly.mobile.feature.vendor.shop.VendorComplaintsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorEvents) { com.neerly.mobile.feature.vendor.shop.VendorEventsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorCompliance) { VendorComplianceScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VendorSubscriptionsToday) {
            VendorSubscriptionsTodayScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.VendorSettings) {
            val session: SessionViewModel = hiltViewModel()
            VendorSettingsScreen(
                onBack = { nav.popBackStack() },
                onCompliance = { nav.navigate(Routes.VendorCompliance) },
                onSubscriptionsToday = { nav.navigate(Routes.VendorSubscriptionsToday) },
                onBusinessConfig = { nav.navigate(Routes.VendorBusinessConfig) },
                onTeam = { nav.navigate(Routes.VendorTeam) },
                onBank = { nav.navigate(Routes.VendorBank) },
                onHolidays = { nav.navigate(Routes.VendorBusinessConfig) },
                onShop = { nav.navigate(Routes.VendorShop) },
                onAlerts = { nav.navigate(Routes.VendorAlertSettings) },
                onArea = { nav.navigate(Routes.VendorArea) },
                onLogout = { session.logout { nav.navigate(Routes.Welcome) { popUpTo(nav.graph.id) { inclusive = true } } } },
                onSwitchRole = if (session.canSwitchRole) ({ nav.navigate(Routes.RolePicker) }) else null
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
            val session: SessionViewModel = hiltViewModel()
            DriverHomeScreen(
                onCodReconcile = { nav.navigate(Routes.DriverCod) },
                onSwitchRole = if (session.canSwitchRole) ({ nav.navigate(Routes.RolePicker) }) else null,
                onLogout = { session.logout { nav.navigate(Routes.Welcome) { popUpTo(nav.graph.id) { inclusive = true } } } },
                // The hold on the SOS button is the confirmation — the SOS screen sends straight away.
                onSos = { nav.navigate(Routes.driverSos(send = true)) },
                onVehicle = { nav.navigate(Routes.DriverVehicle) },
                onDocuments = { nav.navigate(Routes.DriverDocuments) },
                onPickups = { nav.navigate(Routes.DriverPickups) }
            )
        }
        composable(Routes.DriverCod) {
            DriverCodReconcileScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
        }
        composable(
            Routes.DriverSos,
            arguments = listOf(androidx.navigation.navArgument("send") { type = androidx.navigation.NavType.BoolType; defaultValue = false })
        ) { entry ->
            com.neerly.mobile.feature.driver.DriverSosScreen(onBack = { nav.popBackStack() },
                sendOnOpen = entry.arguments?.getBoolean("send") == true)
        }
        composable(Routes.DriverVehicle) { com.neerly.mobile.feature.driver.DriverVehicleScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DriverDocuments) { com.neerly.mobile.feature.driver.DriverDocumentsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DriverPickups) { com.neerly.mobile.feature.driver.DriverPickupsScreen(onBack = { nav.popBackStack() }) }
    }
}
