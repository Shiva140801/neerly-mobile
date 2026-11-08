package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.auth.AuthRepository
import com.neerly.mobile.data.auth.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * The three app-facing roles, spelled exactly as the backend grants them. The app used
 * to test for "VENDOR", which the backend never issues, so a vendor owner could not
 * reach the vendor app at all.
 */
object PortalRole {
    const val CUSTOMER = "CUSTOMER"
    const val VENDOR = "VENDOR_OWNER"
    const val DRIVER = "DRIVER"
    val ALL = listOf(CUSTOMER, VENDOR, DRIVER)

    /** Granted roles this app has a home for — admin roles live in the web portal. */
    fun of(granted: List<String>): List<String> = ALL.filter { it in granted }
}

/**
 * Every way into a role goes through here: the access token carries one active role,
 * and the server checks it on every call. Navigating to driver screens without
 * switching left them running on a CUSTOMER token, so every request came back 403.
 */
@HiltViewModel
class RoleGateViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val tokens: TokenStore,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var started = false

    fun enter(role: String, remember: Boolean, onReady: () -> Unit) {
        if (started) return
        started = true
        _error.value = null
        if (tokens.activeRole == role) {
            if (remember) tokens.lastRolePreference = role
            onReady()
            return
        }
        viewModelScope.launch {
            runCatching { repo.switchRole(role, remember) }
                .onSuccess { onReady() }
                .onFailure {
                    started = false
                    _error.value = it.userMessage(
                        context = "switch role",
                        fallback = strings.get(R.string.cust_role_switch_failed)
                    )
                }
        }
    }
}

/**
 * Signing out from any role. Vendor "Log out" used to only navigate to Welcome, leaving
 * the tokens in place — the next launch signed straight back in.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val tokens: TokenStore
) : ViewModel() {
    /** More than one app role to move between — the "Switch role" affordance shows only then. */
    val canSwitchRole: Boolean get() = PortalRole.of(tokens.grantedRoles).size > 1

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onDone()
        }
    }
}

@Composable
fun RoleGateScreen(
    role: String,
    remember: Boolean,
    onReady: () -> Unit,
    onBack: () -> Unit,
    vm: RoleGateViewModel = hiltViewModel()
) {
    val error by vm.error.collectAsState()
    LaunchedEffect(role) { vm.enter(role, remember, onReady) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .padding(NeerlySpacing.x6),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (error == null) {
            CircularProgressIndicator(color = NeerlyColors.CustomerPrimary)
            Spacer(Modifier.height(NeerlySpacing.x4))
            Text(stringResource(R.string.cust_role_switching), fontSize = 14.sp, color = NeerlyColors.Ink600)
        } else {
            Text(error!!, fontSize = 14.sp, color = NeerlyColors.Err, textAlign = TextAlign.Center)
            Spacer(Modifier.height(NeerlySpacing.x4))
            Button(onClick = { vm.enter(role, remember, onReady) }) { Text(stringResource(R.string.cust_try_again)) }
            TextButton(onClick = onBack) { Text(stringResource(R.string.cust_go_back)) }
        }
    }
}
