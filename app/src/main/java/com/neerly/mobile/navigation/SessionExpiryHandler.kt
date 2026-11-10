package com.neerly.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.neerly.mobile.data.auth.SessionExpiry

/**
 * The server ended this session (refresh refused). Whatever role or screen the person
 * was on, go to Welcome with nothing behind it; Welcome says "Please sign in again".
 */
@Composable
fun SessionExpiryHandler(nav: NavHostController, expiry: SessionExpiry) {
    val expired by expiry.expired.collectAsState()
    LaunchedEffect(expired) {
        if (!expired) return@LaunchedEffect
        expiry.handled()
        if (nav.currentDestination?.route == Routes.Welcome) return@LaunchedEffect
        runCatching { nav.navigate(Routes.Welcome) { popUpTo(nav.graph.id) { inclusive = true } } }
    }
}
