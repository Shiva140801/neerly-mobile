package com.neerly.mobile.feature.vendor.settings

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.feature.vendor.shop.docLabel

/**
 * Board V18 — the seller's settings hub. Each row says what's set right now (next closure,
 * hours, bank check, the licence nearest to lapsing) so the owner rarely has to open it.
 */
@Composable
fun VendorSettingsScreen(
    onBack: () -> Unit,
    onCompliance: () -> Unit,
    onSubscriptionsToday: () -> Unit,
    onBusinessConfig: () -> Unit = {},
    onTeam: () -> Unit = {},
    onBank: () -> Unit = {},
    onLogout: () -> Unit,
    onSwitchRole: (() -> Unit)? = null,
    onHolidays: () -> Unit = onBusinessConfig,
    onShop: (() -> Unit)? = null,
    onAlerts: (() -> Unit)? = null,
    onArea: () -> Unit = {},
    vm: VendorSettingsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    OnReturn(vm::load)
    var closeOpen by remember { mutableStateOf(false) }
    var closeReason by remember { mutableStateOf("") }
    var closeHours by remember { mutableStateOf(24) }
    val closed = s.status?.isOpen == false

    BoondhPage(stringResource(R.string.vend_settings), onBack = onBack) {
        if (closed) item {
            Notice(listOfNotNull(s.status?.pausedUntil?.let { stringResource(R.string.vend_closed_orders_until, shortDay(it)) }
                ?: stringResource(R.string.vend_closed_orders), s.status?.pausedReason).joinToString(" · "), Tone.Warn)
        }
        s.error?.let { item { Notice(it, Tone.Bad) } }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.vend_holidays),
                    s.nextHoliday?.let { stringResource(R.string.vend_closed_on, listOfNotNull(shortDay(it.date), it.label).joinToString(" · ")) } ?: stringResource(R.string.vend_no_closures),
                    onClick = onHolidays)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_delivery_area),
                    if (s.pincodes > 0) pluralStringResource(R.plurals.vend_n_pincodes, s.pincodes, s.pincodes) else stringResource(R.string.vend_set_where_deliver),
                    onClick = onArea)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_opening_hours), hoursSummary(s.hours, androidx.compose.ui.platform.LocalConfiguration.current.locales[0],
                    stringResource(R.string.vend_hours_not_set)), onClick = onBusinessConfig)
                HorizontalDivider(color = NeerlyColors.Ink200)
                val b = s.bank
                val bankWord = stringResource(R.string.vend_bank)
                InfoRow(stringResource(R.string.vend_bank_account),
                    when {
                        b == null -> stringResource(R.string.vend_add_payout_bank)
                        b.verifiedAt == null -> stringResource(R.string.vend_bank_checking, "${b.bankName ?: bankWord} ${b.accountNumberMasked}")
                        else -> "${b.bankName ?: bankWord} ${b.accountNumberMasked}"
                    }, onClick = onBank)
                HorizontalDivider(color = NeerlyColors.Ink200)
                val r = s.soonestRenewal
                val days = r?.expiresInDays
                InfoRow(stringResource(R.string.vend_licences),
                    when {
                        r == null || days == null -> stringResource(R.string.vend_licences_list)
                        days < 0 -> pluralStringResource(R.plurals.vend_doc_lapsed, -days.toInt(), docLabel(r.docType), -days.toInt())
                        days <= 60 -> pluralStringResource(R.plurals.vend_doc_renews, days.toInt(), docLabel(r.docType), days.toInt())
                        else -> stringResource(R.string.vend_all_valid)
                    }, onClick = onCompliance)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_drivers), s.drivers?.let { stringResource(R.string.vend_n_on_team, it) } ?: stringResource(R.string.vend_delivery_staff), onClick = onTeam)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_sub_run), stringResource(R.string.vend_sub_run_sub), onClick = onSubscriptionsToday)
                onShop?.let {
                    HorizontalDivider(color = NeerlyColors.Ink200)
                    InfoRow(stringResource(R.string.vend_your_shop), stringResource(R.string.vend_shop_row_sub), onClick = it)
                }
                onAlerts?.let {
                    HorizontalDivider(color = NeerlyColors.Ink200)
                    InfoRow(stringResource(R.string.vend_new_order_alert), stringResource(R.string.vend_sound_repeat), onClick = it)
                }
            }
        }
        item {
            if (closed) PrimaryButton(stringResource(if (s.working) R.string.vend_opening else R.string.vend_open_again), vm::reopen, enabled = !s.working)
            else SecondaryButton(stringResource(R.string.vend_close_for_now), { closeOpen = true }, color = NeerlyColors.Warn)
        }
        item { Text(stringResource(R.string.vend_close_note),
            fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600) }
        onSwitchRole?.let { item { SecondaryButton(stringResource(R.string.vend_switch_role), it) } }
        item { TextButton(onClick = onLogout, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.vend_log_out), color = NeerlyColors.Err) } }
    }

    if (closeOpen) {
        AlertDialog(
            onDismissRequest = { closeOpen = false },
            title = { Text(stringResource(R.string.vend_close_q)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.vend_close_body), fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2 to R.string.vend_2_hours, 24 to R.string.vend_ep_today, 72 to R.string.vend_3_days).forEach { (h, l) ->
                            FilterChip(selected = closeHours == h, onClick = { closeHours = h }, label = { Text(stringResource(l)) },
                                modifier = Modifier.heightIn(min = 44.dp))
                        }
                    }
                    OutlinedTextField(value = closeReason, onValueChange = { closeReason = it.take(200) },
                        label = { Text(stringResource(R.string.vend_reason_customers_see)) }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = {
                vm.emergencyClose(closeHours, closeReason.trim().ifBlank { null }); closeOpen = false; closeReason = ""
            }) { Text(stringResource(R.string.vend_close), color = NeerlyColors.Warn) } },
            dismissButton = { TextButton(onClick = { closeOpen = false }) { Text(stringResource(R.string.vend_cancel)) } }
        )
    }
}
