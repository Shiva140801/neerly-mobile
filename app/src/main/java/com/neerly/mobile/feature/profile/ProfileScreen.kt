package com.neerly.mobile.feature.profile

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.config.LocalAppConfig

/** Boondh board 26 — Profile: who you are, your numbers, settings, referral banner. */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onAddresses: () -> Unit,
    onWallet: () -> Unit,
    onOrders: () -> Unit,
    onSubscriptions: () -> Unit,
    onDeposits: () -> Unit,
    onNotifications: () -> Unit,
    onNotificationPrefs: () -> Unit = {},
    onVendorDashboard: () -> Unit = {},
    onBecomeVendor: () -> Unit = {},
    onDriverMode: () -> Unit = {},
    onEventBooking: () -> Unit = {},
    onSwitchRole: () -> Unit = {},
    onSelectTab: (CustomerTab) -> Unit = {},
    onLogout: () -> Unit,
    onEdit: () -> Unit = {},
    onRefunds: () -> Unit = {},
    onRefundSetting: () -> Unit = {},
    onHelp: () -> Unit = {},
    onComplaints: () -> Unit = {},
    onReferEarn: () -> Unit = {},
    onLanguage: () -> Unit = {},
    vm: ProfileViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::load)
    val cfg = LocalAppConfig.current
    val u = s.user
    BoondhPage(title = stringResource(R.string.acct_profile_title), bottomBar = { CustomerBottomBar(current = CustomerTab.Profile, onSelect = onSelectTab) }) {
        if (u == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).background(NeerlyColors.CustomerPrimary, CircleShape), contentAlignment = Alignment.Center) {
                    Text(u.displayName.take(1).uppercase(), color = NeerlyColors.Paper, fontSize = 24.sp, fontFamily = FontFamily.Serif)
                }
                Column(Modifier.weight(1f)) {
                    Text(u.displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text(u.phoneMask, fontSize = 14.sp, color = NeerlyColors.Ink600)
                }
                TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.acct_profile_edit), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${s.stats?.lifetimeOrders ?: 0}", pluralStringResource(R.plurals.acct_profile_stat_orders, (s.stats?.lifetimeOrders ?: 0).toInt()), Modifier.weight(1f), onOrders)
                Stat((s.wallet ?: java.math.BigDecimal.ZERO).rupees(), stringResource(R.string.acct_profile_stat_wallet), Modifier.weight(1f), onWallet)
                Stat("${s.stats?.jarsOut ?: 0}", pluralStringResource(R.plurals.acct_profile_stat_jars, (s.stats?.jarsOut ?: 0).toInt()), Modifier.weight(1f), onDeposits)
            }
        }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.acct_profile_addresses), stringResource(R.string.acct_profile_addresses_sub), onClick = onAddresses)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.acct_profile_subs), stringResource(R.string.acct_profile_subs_sub), onClick = onSubscriptions)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.acct_profile_jars), stringResource(R.string.acct_profile_jars_sub, (s.stats?.jarsOut ?: 0).toInt()), onClick = onDeposits)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.acct_profile_refunds), stringResource(R.string.acct_profile_refunds_sub), onClick = onRefunds)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.acct_profile_refund_dest), if (u.refundDestination == "WALLET") stringResource(R.string.acct_refund_to_wallet) else stringResource(R.string.acct_refund_to_source), onClick = onRefundSetting)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.acct_profile_language), when (u.preferredLanguage) { "te" -> "తెలుగు"; "hi" -> "हिन्दी"; else -> "English" }, onClick = onLanguage)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.acct_profile_notifications), stringResource(R.string.acct_profile_notifications_sub), onClick = onNotificationPrefs)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.acct_profile_help), stringResource(R.string.acct_profile_help_sub, cfg.clock(cfg.supportOpens), cfg.clock(cfg.supportCloses)), onClick = onHelp)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.acct_profile_requests), stringResource(R.string.acct_profile_requests_sub), onClick = onComplaints)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.acct_profile_event), stringResource(R.string.acct_profile_event_sub), onClick = onEventBooking)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(16.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.acct_profile_refer_title, cfg.referrerReward.rupees()), fontFamily = FontFamily.Serif, fontSize = 22.sp, color = NeerlyColors.Paper)
                Text(stringResource(R.string.acct_profile_refer_body, cfg.refereeReward.rupees(), u.referralCode ?: s.stats?.referralCode ?: "…"),
                    fontSize = 14.sp, color = Boondh.MutedOnNavy)
                PrimaryButton(stringResource(R.string.acct_profile_share), onReferEarn, color = Boondh.Amber, contentColor = NeerlyColors.Ink900)
            }
        }
        item {
            val roles = s.grantedRoles
            if ("VENDOR_OWNER" in roles) SecondaryButton(stringResource(R.string.acct_profile_to_seller), onVendorDashboard)
            else TextButton(onClick = onBecomeVendor, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.acct_profile_become_seller), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
        }
        if ("DRIVER" in s.grantedRoles) item { SecondaryButton(stringResource(R.string.acct_profile_to_driver), onDriverMode) }
        item { TextButton(onClick = { vm.logout(onLogout) }, modifier = Modifier.heightIn(min = 44.dp)) {
            Text(stringResource(R.string.acct_profile_logout), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold) } }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, onClick: () -> Unit) = BoondhCard(modifier, onClick = onClick) {
    Text(value, fontFamily = FontFamily.Serif, fontSize = 22.sp, color = NeerlyColors.Ink900)
    Text(label, fontSize = 13.sp, color = NeerlyColors.Ink600)
}
