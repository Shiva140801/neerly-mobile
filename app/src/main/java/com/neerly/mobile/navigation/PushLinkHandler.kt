package com.neerly.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.neerly.mobile.data.auth.TokenStore
import com.neerly.mobile.feature.auth.PortalRole
import com.neerly.mobile.push.DeepLinkBus
import com.neerly.mobile.push.DeepLinks
import com.neerly.mobile.push.PushRole

/** Routes that come before the user is inside a role — a notification link waits until they're past. */
private val ENTRY_ROUTES = setOf(Routes.Splash, Routes.Welcome, Routes.Phone, Routes.Otp, Routes.Name, Routes.Language,
    Routes.Address, Routes.ReferralCode, Routes.RolePicker, Routes.AdminTotp, Routes.RoleGate)

/**
 * Opens the screen a notification asked for. Waits out the splash/sign-in screens, and when the
 * link belongs to another face (a seller push tapped while in the customer app) it goes through
 * the role gate first; the gate lands on that role's home and this effect then opens the link.
 */
@Composable
fun PushLinkHandler(nav: NavHostController, links: DeepLinkBus, tokens: TokenStore) {
    val pending by links.pending.collectAsState()
    val entry by nav.currentBackStackEntryAsState()
    val here = entry?.destination?.route
    LaunchedEffect(pending, here) {
        val route = pending ?: return@LaunchedEffect
        if (here == null || here in ENTRY_ROUTES || !tokens.hasSession()) return@LaunchedEffect
        val want = DeepLinks.roleOf(route)
        val active = PushRole.of(tokens.activeRole)
        if (want != null && want != active) {
            val portal = when (want) {
                PushRole.SELLER -> PortalRole.VENDOR
                PushRole.DRIVER -> PortalRole.DRIVER
                PushRole.CUSTOMER -> PortalRole.CUSTOMER
            }
            if (portal !in tokens.grantedRoles) { links.consume(route); return@LaunchedEffect }
            nav.navigate(Routes.roleGate(portal)) { launchSingleTop = true }
            return@LaunchedEffect
        }
        links.consume(route)
        runCatching { nav.navigate(route) { launchSingleTop = true } }
    }
}
