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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
    onLegal: () -> Unit = {},
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
                androidx.compose.material3.OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(NeerlyRadius.pill), border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ink300)) {
                    Text(stringResource(R.string.acct_profile_edit), color = NeerlyColors.Ink900, fontWeight = FontWeight.Bold) }
            }
        }
        // Board Profile: three hairline-divided stats, then single-line rows with the value on the right.
        item {
            Column {
                HorizontalDivider(color = NeerlyColors.Ink200)
                Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                    Stat("${s.stats?.lifetimeOrders ?: 0}", pluralStringResource(R.plurals.acct_profile_stat_orders, (s.stats?.lifetimeOrders ?: 0).toInt()), Modifier.weight(1f), onOrders)
                    androidx.compose.material3.VerticalDivider(color = NeerlyColors.Ink200)
                    Stat((s.wallet ?: java.math.BigDecimal.ZERO).rupees(), stringResource(R.string.acct_profile_stat_wallet), Modifier.weight(1f), onWallet)
                    androidx.compose.material3.VerticalDivider(color = NeerlyColors.Ink200)
                    Stat("${s.stats?.jarsOut ?: 0}", pluralStringResource(R.plurals.acct_profile_stat_jars, (s.stats?.jarsOut ?: 0).toInt()), Modifier.weight(1f), onDeposits)
                }
                HorizontalDivider(color = NeerlyColors.Ink200)
            }
        }
        item {
            Column {
                Row_(stringResource(R.string.acct_profile_addresses),
                    s.addressLabels.map { com.neerly.mobile.feature.address.addressLabel(it) }.joinToString(", ").ifBlank { stringResource(R.string.acct_profile_addresses_sub) }, onAddresses)
                Row_(stringResource(R.string.acct_profile_subs), stringResource(R.string.acct_profile_subs_sub), onSubscriptions)
                Row_(stringResource(R.string.acct_profile_jars), stringResource(R.string.acct_profile_jars_sub, (s.stats?.jarsOut ?: 0).toInt()), onDeposits)
                Row_(stringResource(R.string.acct_profile_refunds), stringResource(R.string.acct_profile_refunds_sub), onRefunds)
                Row_(stringResource(R.string.acct_profile_refund_dest), if (u.refundDestination == "WALLET") stringResource(R.string.acct_refund_to_wallet) else stringResource(R.string.acct_refund_to_source), onRefundSetting)
                Row_(stringResource(R.string.acct_profile_language), when (u.preferredLanguage) { "te" -> "తెలుగు"; "hi" -> "हिन्दी"; else -> "English" }, onLanguage)
                Row_(stringResource(R.string.acct_profile_notifications), stringResource(R.string.acct_profile_notifications_sub), onNotificationPrefs)
                Row_(stringResource(R.string.acct_profile_requests), stringResource(R.string.acct_profile_requests_sub), onComplaints)
                Row_(stringResource(R.string.acct_profile_event), stringResource(R.string.acct_profile_event_sub), onEventBooking)
                Row_(stringResource(R.string.acct_profile_help), null, onHelp)
                Row_(stringResource(R.string.core_profile_legal), null, onLegal)
            }
        }
        item {
            com.neerly.mobile.core.design.StripCard(
                trailing = {
                    androidx.compose.material3.Button(onClick = onReferEarn, shape = RoundedCornerShape(NeerlyRadius.pill), modifier = Modifier.heightIn(min = 48.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeerlyColors.Ink900, contentColor = NeerlyColors.Paper)) {
                        Text(stringResource(R.string.acct_profile_share), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }) {
                Column {
                    Text(stringResource(R.string.acct_profile_refer_title, cfg.referrerReward.rupees()), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text(stringResource(R.string.acct_profile_refer_body, cfg.refereeReward.rupees(), u.referralCode ?: s.stats?.referralCode ?: "…"),
                        fontSize = 14.sp, color = NeerlyColors.Ink700)
                }
            }
        }
        item {
            val roles = s.grantedRoles
            if ("VENDOR_OWNER" in roles) TextButton(onClick = onVendorDashboard, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.acct_profile_to_seller), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            else TextButton(onClick = onBecomeVendor, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.acct_profile_become_seller), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
        }
        if ("DRIVER" in s.grantedRoles) item { TextButton(onClick = onDriverMode, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.acct_profile_to_driver), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) } }
        item { com.neerly.mobile.push.PushLabEntry() }
        item {
            var leaving by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
            com.neerly.mobile.core.design.LogoutButton(stringResource(R.string.acct_profile_logout),
                onConfirmed = { leaving = true; vm.logout(onLogout) },
                busy = leaving, busyLabel = stringResource(R.string.core_logging_out))
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, onClick: () -> Unit) =
    Column(modifier.clickable(onClick = onClick).padding(vertical = 14.dp).padding(start = 4.dp)) {
        Text(value, fontFamily = FontFamily.Serif, fontSize = 24.sp, color = NeerlyColors.Ink900)
        Text(label, fontSize = 14.sp, color = NeerlyColors.Ink600)
    }

/** Board Profile row: label left, value right (or a chevron when there's no value). */
@Composable
private fun Row_(label: String, value: String?, onClick: () -> Unit) = com.neerly.mobile.core.design.ListRow(onClick = onClick) {
    Text(label, fontSize = 17.sp, color = NeerlyColors.Ink900, maxLines = 1)
    if (value != null) Text(value, fontSize = 15.sp, color = NeerlyColors.Ink600, maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    else { Spacer(Modifier.weight(1f)); androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NeerlyColors.Ink600) }
}
