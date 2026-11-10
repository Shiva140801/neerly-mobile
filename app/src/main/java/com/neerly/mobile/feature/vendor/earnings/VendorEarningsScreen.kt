package com.neerly.mobile.feature.vendor.earnings

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.feature.vendor.money.VendorPayout
import com.neerly.mobile.feature.vendor.money.payoutStatus
import com.neerly.mobile.feature.vendor.money.periodText

/**
 * Board V16 — Earnings, with the payouts list (V39), one payout (V31) and a payout the bank
 * sent back (V32) shown inside this screen, so the existing route keeps working.
 */
@Composable
fun VendorEarningsScreen(
    onBack: (() -> Unit)? = null,     // null when it's the Earnings tab root
    onBank: () -> Unit = {},
    vm: VendorEarningsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var showPayouts by rememberSaveable { mutableStateOf(false) }
    OnReturn(vm::load)
    when {
        state.openPayout != null && state.invoiceHtml != null -> {
            BackHandler { vm.closeInvoice() }
            FeeInvoice(state.openPayout!!, state.invoiceHtml!!, onBack = vm::closeInvoice)
        }
        state.openPayout != null -> {
            BackHandler { vm.closePayout() }
            val p = state.openPayout!!
            val context = androidx.compose.ui.platform.LocalContext.current
            val chooser = stringResource(R.string.vend_payout_open_with)
            LaunchedEffect(state.csv) {
                state.csv?.let { PayoutFiles.openCsv(context, it.fileName, it.text, chooser); vm.csvHandled() }
            }
            // The payout's own account first; older payouts without one fall back to today's.
            val bankWord = stringResource(R.string.vend_bank)
            PayoutDetail(p, p.accountMasked?.let { "${p.bankName ?: bankWord} $it" }
                ?: state.bank?.let { "${it.bankName ?: bankWord} ${it.accountNumberMasked}" },
                state.payoutError, onBack = vm::closePayout, onBank = onBank,
                busy = state.fileBusy, onCsv = vm::downloadCsv, onInvoice = vm::openInvoice)
        }
        showPayouts -> {
            BackHandler { showPayouts = false }
            PayoutList(state, onBack = { showPayouts = false }, onOpen = vm::openPayout, onBank = onBank)
        }
        else -> Overview(state, onBack, vm, onPayouts = { showPayouts = true }, onOpen = vm::openPayout)
    }
}

@Composable
private fun Overview(state: VendorEarningsUiState, onBack: (() -> Unit)?, vm: VendorEarningsViewModel,
                     onPayouts: () -> Unit, onOpen: (String) -> Unit) {
    BoondhPage(stringResource(R.string.vend_earnings), onBack = onBack) {
        if (state.summary == null) { item { LoadState(state.loading, state.error, vm::load) }; return@BoondhPage }
        val s = state.summary
        item {
            Column(Modifier.fillMaxWidth().background(NeerlyColors.VendorDark, RoundedCornerShape(16.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val period = state.unpaid?.let { u -> periodText(u.periodStart, u.periodEnd).takeIf { it.isNotBlank() } }
                Text(listOfNotNull(stringResource(R.string.vend_next_payout_label), s.nextPayoutAt?.let(::shortDay),
                    period?.let { stringResource(R.string.vend_for_so_far, it) })
                    .joinToString(" · "), fontSize = 13.sp, color = Boondh.MutedOnNavy)
                Text((s.nextPayoutAmount ?: state.unpaid?.unpaidNet)?.rupees() ?: "—", fontFamily = FontFamily.Serif,
                    fontSize = 34.sp, color = NeerlyColors.Paper)
                state.unpaid?.let { Text(pluralStringResource(R.plurals.vend_orders_not_paid, it.unpaidCount.toInt(), it.unpaidCount.toInt()), fontSize = 13.sp, color = Boondh.MutedOnNavy) }
            }
        }
        state.stuck.forEach { p -> item {
            BoondhCard(onClick = { onOpen(p.id) }) {
                Pill(payoutStatus(p.status).first, payoutStatus(p.status).second)
                Text(stringResource(R.string.vend_not_reached, p.netTotal.rupees(), periodText(p.periodStart, p.periodEnd)),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                (p.failureReason ?: p.holdReason)?.let { Text(it, fontSize = 14.sp, color = NeerlyColors.Ink600) }
            }
        } }
        item {
            TextButton(onClick = onPayouts, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.vend_all_payouts), color = NeerlyColors.VendorPrimary, fontWeight = FontWeight.Bold) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EarningsPeriod.entries.forEach { p ->
                    FilterChip(selected = state.period == p, onClick = { vm.select(p) }, label = { Text(stringResource(p.label), fontSize = 15.sp) },
                        modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        state.bucket?.let { b -> item {
            BoondhCard {
                InfoRow(pluralStringResource(R.plurals.vend_n_orders_count, b.orders.toInt(), b.orders.toInt()), trailing = b.gross.rupees())
                InfoRow(stringResource(R.string.vend_boondh_fee), trailing = "−" + b.commission.rupees(), trailingColor = NeerlyColors.Ink600)
                InfoRow(stringResource(R.string.vend_gst_fee), trailing = "−" + b.tax.rupees(), trailingColor = NeerlyColors.Ink600)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_yours), trailing = b.net.rupees())
            }
        } }
        if (state.tripPayLines.isNotEmpty()) item {
            BoondhCard {
                InfoRow(pluralStringResource(R.plurals.vend_trip_pay_n, state.tripPayLines.size, state.tripPayLines.size), trailing = "+" + state.tripPayTotal.rupees(),
                    trailingColor = NeerlyColors.Ok)
                Text(stringResource(R.string.vend_trip_pay_note),
                    fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
        }
        if (state.lines.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.vend_recent)) }
            state.lines.take(15).forEach { l -> item(key = l.id) {
                InfoRow(stringResource(if (l.isTripPay) R.string.vend_trip_pay_line else R.string.vend_order_word),
                    listOfNotNull(shortDay(l.deliveryDate), stringResource(if (l.paidOut) R.string.vend_paid_out else R.string.vend_in_next_payout)).joinToString(" · "),
                    (if (l.isTripPay) "+" else "") + l.netPayable.rupees(),
                    if (l.isTripPay) NeerlyColors.Ok else NeerlyColors.Ink900)
            } }
        }
    }
}

@Composable
private fun PayoutList(state: VendorEarningsUiState, onBack: () -> Unit, onOpen: (String) -> Unit, onBank: () -> Unit) {
    val bank = state.bank
    BoondhPage(stringResource(R.string.vend_payouts), onBack = onBack, backLabel = stringResource(R.string.vend_earnings),
        subtitle = stringResource(R.string.vend_payouts_sub)) {
        item {
            BoondhCard {
                val period = state.unpaid?.let { periodText(it.periodStart, it.periodEnd).takeIf { s -> s.isNotBlank() } }
                InfoRow(listOfNotNull(stringResource(R.string.vend_next), state.summary?.nextPayoutAt?.let(::shortDay)).joinToString(" · "),
                    period?.let { stringResource(R.string.vend_for_so_far, it) },
                    (state.summary?.nextPayoutAmount ?: state.unpaid?.unpaidNet)?.rupees())
                Text(bank?.let { stringResource(R.string.vend_goes_to, it.bankName ?: stringResource(R.string.vend_your_bank), it.accountNumberMasked) } ?: stringResource(R.string.vend_add_bank_paid),
                    fontSize = 13.sp, color = if (bank == null) NeerlyColors.Warn else NeerlyColors.Ink600)
            }
        }
        if (state.payouts.isEmpty()) item {
            LoadState(false, null, {}, stringResource(R.string.vend_no_payouts), stringResource(R.string.vend_no_payouts_sub))
        } else {
            item { SectionLabel(stringResource(R.string.vend_past_payouts)) }
            state.payouts.forEach { p -> item(key = p.id) {
                val (label, tone) = payoutStatus(p.status)
                BoondhCard(onClick = { onOpen(p.id) }) {
                    Row { Column(Modifier.weight(1f)) {
                            Text(shortDay(p.completedAt ?: p.createdAt), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                            Text(periodText(p.periodStart, p.periodEnd), fontSize = 13.sp, color = NeerlyColors.Ink600) }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(p.netTotal.rupees(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                            Pill(label, tone) } }
                }
            } }
        }
        item { InfoRow(stringResource(R.string.vend_bank_account), bank?.let { "${it.bankName ?: stringResource(R.string.vend_bank)} ${it.accountNumberMasked}" } ?: stringResource(R.string.vend_not_added), onClick = onBank) }
    }
}

@Composable
private fun PayoutDetail(p: VendorPayout, bankLabel: String?, error: String?, onBack: () -> Unit, onBank: () -> Unit,
                         busy: Boolean = false, onCsv: () -> Unit = {}, onInvoice: () -> Unit = {}) {
    val (label, tone) = payoutStatus(p.status)
    BoondhPage(
        title = stringResource(if (p.stuck) R.string.vend_hasnt_reached else R.string.vend_payout_x, p.netTotal.rupees()),
        onBack = onBack, backLabel = stringResource(R.string.vend_payouts),
        subtitle = when {
            p.status == "COMPLETED" -> listOfNotNull(stringResource(R.string.vend_paid_on, shortDay(p.completedAt)), p.utr?.let { stringResource(R.string.vend_utr, it) }).joinToString(" · ")
            p.stuck -> stringResource(R.string.vend_money_safe)
            else -> stringResource(R.string.vend_for_period, periodText(p.periodStart, p.periodEnd))
        }
    ) {
        error?.let { item { Notice(it, Tone.Bad) } }
        item { Pill(label, tone) }
        if (p.stuck) item {
            BoondhCard {
                (p.failureReason ?: p.holdReason)?.let { InfoRow(stringResource(R.string.vend_why), trailing = it) }
                InfoRow(stringResource(R.string.vend_for), trailing = periodText(p.periodStart, p.periodEnd))
                InfoRow(stringResource(R.string.vend_tried), trailing = shortDay(p.createdAt))
                bankLabel?.let { InfoRow(stringResource(R.string.vend_account_on_file), trailing = it) }
            }
        }
        if (p.stuck) item {
            Text(stringResource(R.string.vend_held_release),
                fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600)
        }
        if (p.stuck) item { PrimaryButton(stringResource(R.string.vend_check_bank), onBank, color = NeerlyColors.VendorPrimary) }
        item {
            BoondhCard {
                InfoRow(periodText(p.periodStart, p.periodEnd), trailing = p.grossTotal.rupees())
                InfoRow(stringResource(R.string.vend_boondh_fee), trailing = "−" + p.commissionTotal.rupees(), trailingColor = NeerlyColors.Ink600)
                InfoRow(stringResource(R.string.vend_gst_fee), trailing = "−" + p.taxTotal.rupees(), trailingColor = NeerlyColors.Ink600)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(if (p.status == "COMPLETED") R.string.vend_sent_to_bank else R.string.vend_to_send), trailing = p.netTotal.rupees())
            }
        }
        // VPayoutDetail: the shop's records — the orders behind this payout and Boondh's fee invoice.
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(if (busy) stringResource(R.string.vend_payout_downloading) else stringResource(R.string.vend_payout_csv), onCsv,
                    enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 50.dp))
                SecondaryButton(stringResource(R.string.vend_payout_invoice), onInvoice,
                    enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 50.dp))
            }
        }
    }
}

/** The fee invoice as the server renders it, with a way to send it on (accountant, Drive). */
@Composable
private fun FeeInvoice(p: VendorPayout, html: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val shareTitle = stringResource(R.string.vend_payout_share_invoice)
    Column(Modifier.fillMaxSize().background(NeerlyColors.Canvas)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("‹ " + stringResource(R.string.vend_payouts), color = NeerlyColors.VendorPrimary, fontSize = 15.sp) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { PayoutFiles.shareInvoice(context, "boondh-fee-invoice-${p.periodEnd}.html", html, shareTitle) },
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text(shareTitle, color = NeerlyColors.VendorPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx -> android.webkit.WebView(ctx).apply { settings.javaScriptEnabled = false } },
            update = { it.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) },
            modifier = Modifier.fillMaxSize())
    }
}
