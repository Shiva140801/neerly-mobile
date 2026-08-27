package com.neerly.mobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * Profile hub — entry point linking out to addresses, wallet, orders, subscriptions,
 * deposits, language, notification prefs, referral share, account deletion, logout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onAddresses: () -> Unit,
    onWallet: () -> Unit,
    onOrders: () -> Unit,
    onSubscriptions: () -> Unit,
    onDeposits: () -> Unit,
    onNotifications: () -> Unit,
    onLogout: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var deleteOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(NeerlySpacing.x4),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Surface(
                color = NeerlyColors.Paper,
                shape = RoundedCornerShape(NeerlyRadius.md),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(NeerlySpacing.x4), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape)
                            .background(NeerlyColors.CustomerSofter, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (state.user?.displayName ?: "?").take(1).uppercase(),
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = NeerlyColors.CustomerDark
                        )
                    }
                    Spacer(Modifier.width(NeerlySpacing.x4))
                    Column(Modifier.weight(1f)) {
                        Text(state.user?.displayName ?: "—",
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(state.user?.phoneMask ?: "—",
                            fontSize = 12.sp, color = NeerlyColors.Ink500)
                        state.user?.email?.let {
                            Text(it, fontSize = 12.sp, color = NeerlyColors.Ink500)
                        }
                    }
                }
            }

            ProfileLink("Addresses", onAddresses)
            ProfileLink("Wallet", onWallet)
            ProfileLink("Orders", onOrders)
            ProfileLink("Subscriptions", onSubscriptions)
            ProfileLink("Returns & Deposits", onDeposits)
            ProfileLink("Notifications", onNotifications)
            LanguageRow(state.user?.preferredLanguage ?: "en", vm::updateLanguage)

            Spacer(Modifier.height(NeerlySpacing.x4))

            OutlinedButton(
                onClick = { vm.logout(onLogout) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeerlyColors.Ink700)
            ) { Text("Log out") }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { deleteOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete account", color = NeerlyColors.Err) }
        }
    }

    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Delete account?") },
            text = {
                Text("Your account will be marked for deletion. You have 30 days to log back in to cancel. After 30 days, personal data is wiped per DPDP rules.")
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteOpen = false
                    vm.deleteAccount(onLogout)
                }) { Text("Delete", color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { deleteOpen = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileLink(label: String, onClick: () -> Unit) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(NeerlySpacing.x4), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Text(">", fontSize = 18.sp, color = NeerlyColors.Ink400)
        }
    }
}

@Composable
private fun LanguageRow(current: String, onPick: (String) -> Unit) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Text("Language", fontSize = 13.sp, color = NeerlyColors.Ink500, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("en" to "English", "te" to "తెలుగు", "hi" to "हिंदी").forEach { (code, label) ->
                    val on = code == current
                    Surface(
                        color = if (on) NeerlyColors.CustomerPrimary else NeerlyColors.CustomerSofter,
                        shape = RoundedCornerShape(NeerlyRadius.pill),
                        modifier = Modifier.clickable { onPick(code) }
                    ) {
                        Text(label,
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = if (on) NeerlyColors.Paper else NeerlyColors.CustomerDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
