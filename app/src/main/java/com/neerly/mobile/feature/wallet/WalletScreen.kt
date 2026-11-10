package com.neerly.mobile.feature.wallet

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.CustomerBottomBar
import com.neerly.mobile.core.design.CustomerTab
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.rupees
import com.neerly.mobile.core.design.shortDay
import com.neerly.mobile.core.design.WalletSkeleton
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.dto.WalletTransaction
import com.neerly.mobile.feature.checkout.findActivity
import java.math.BigDecimal
import java.math.RoundingMode

/** Boondh board 21 — Wallet: navy balance card, jar deposits, this week's movements. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    onSelectTab: (CustomerTab) -> Unit = {},
    onWithdraw: () -> Unit = {},
    onJars: () -> Unit = {},
    onRefunds: () -> Unit = {},
    vm: WalletViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::refresh)
    val context = LocalContext.current
    var addOpen by remember { mutableStateOf(false) }
    state.added?.let { done ->
        AddMoneyDonePage(done, onBack = vm::dismissAdded, onOrderWater = { vm.dismissAdded(); onSelectTab(CustomerTab.Home) })
        return
    }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        bottomBar = { CustomerBottomBar(current = CustomerTab.Wallet, onSelect = onSelectTab) }
    ) { padding ->
        when {
            state.loading && state.balance == null -> Box(Modifier.fillMaxSize().padding(padding)) { WalletSkeleton() }
            // A balance that never loaded is shown as a failure, never as ₹0.
            state.balance == null -> Box(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                BalanceUnavailable(state.error ?: stringResource(R.string.acct_wallet_load_failed), vm::refresh)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item {
                    Text(stringResource(R.string.acct_wallet_title), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium,
                        fontSize = 32.sp, color = NeerlyColors.Ink900, modifier = Modifier.semantics { heading() })
                }
                item { BalanceCard(state.balance!!, onAdd = { addOpen = true }, onWithdraw = onWithdraw) }
                state.notice?.let { msg -> item { Text(msg, color = NeerlyColors.Ok, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) } }
                state.error?.let { msg -> item { Text(msg, color = NeerlyColors.Err, fontSize = 14.sp) } }
                if (state.jarsOut > 0) item { JarCard(state.jarsOut, state.depositHeld, state.jarsDueToday, onJars) }
                // Board Wallet: the list header carries a "Refunds" link.
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.acct_wallet_recent), Modifier.weight(1f).semantics { heading() }, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = NeerlyColors.Ink600)
                        androidx.compose.material3.TextButton(onClick = onRefunds, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.acct_profile_refunds), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                if (state.transactions.isEmpty()) {
                    item { Text(stringResource(R.string.acct_wallet_empty), fontSize = 14.sp, color = NeerlyColors.Ink600) }
                } else {
                    items(state.transactions, key = { it.id }) { TxnRow(it) }
                }
            }
        }
    }

    if (addOpen) {
        ModalBottomSheet(onDismissRequest = { addOpen = false }, containerColor = NeerlyColors.Paper) {
            AddMoneySheet(disabled = state.toppingUp) { amount ->
                addOpen = false
                context.findActivity()?.let { vm.topup(it, amount) }
            }
        }
    }
}

@Composable
private fun BalanceCard(b: WalletResponse, onAdd: () -> Unit, onWithdraw: () -> Unit) {
    val whole = b.availableAmount.setScale(0, RoundingMode.DOWN)
    val paise = b.availableAmount.setScale(2, RoundingMode.HALF_UP).toPlainString().substringAfter('.', "00")
    Column(
        Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(20.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.acct_wallet_available), fontSize = 13.sp, color = Muted)
            Text(buildAnnotatedString {
                append(whole.rupees())   // ₹12,500 — the paise follow in small type
                withStyle(SpanStyle(fontSize = 24.sp, color = Muted)) { append(".$paise") }
            }, fontFamily = FontFamily.Serif, fontSize = 44.sp, fontWeight = FontWeight.Medium, color = NeerlyColors.Paper)
            Text(
                if (b.availableAmount.signum() == 0)
                    stringResource(R.string.acct_wallet_add_once)
                else if (b.promoBalance.signum() > 0)
                    stringResource(R.string.acct_wallet_promo_part, b.promoBalance.rupees(), b.withdrawable.rupees())
                else stringResource(R.string.acct_wallet_all_withdrawable),
                fontSize = 13.sp, lineHeight = 19.sp, color = Muted
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onAdd, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = NeerlyColors.Ink900)
            ) { Text(stringResource(R.string.acct_wallet_add_money), fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onWithdraw, enabled = b.withdrawable.signum() > 0,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF5664A3)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeerlyColors.Paper, disabledContentColor = Muted)
            ) { Text(stringResource(R.string.acct_wallet_withdraw), fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun JarCard(jars: Int, held: BigDecimal, dueToday: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(NeerlyColors.Paper, RoundedCornerShape(14.dp))
            .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        com.neerly.mobile.core.design.JarGlyph(Modifier.size(width = 30.dp, height = 40.dp), body = NeerlyColors.Paper, outline = NeerlyColors.Ink900, cap = NeerlyColors.CustomerPrimary)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.acct_wallet_held, held.rupees()), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text(listOfNotNull(pluralStringResource(R.plurals.acct_wallet_jars_with_you, jars, jars),
                if (dueToday > 0) pluralStringResource(R.plurals.acct_wallet_due_today, dueToday, dueToday) else null).joinToString(" · "),
                fontSize = 14.sp, color = NeerlyColors.Ink600)
        }
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NeerlyColors.Ink600)
    }
}

@Composable
private fun AddMoneySheet(disabled: Boolean, onPick: (BigDecimal) -> Unit) {
    val cfg = LocalAppConfig.current
    val amounts = listOf("200", "500", "1000", "2000").map(::BigDecimal).filter { it in cfg.topupMin..cfg.topupMax }
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.acct_wallet_add_money), fontFamily = FontFamily.Serif, fontSize = 24.sp, color = NeerlyColors.Ink900)
        Text(stringResource(R.string.acct_wallet_topup_hint, cfg.topupMax.rupees()), fontSize = 14.sp, color = NeerlyColors.Ink600)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            amounts.forEach { a ->
                OutlinedButton(
                    onClick = { onPick(a) }, enabled = !disabled, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, NeerlyColors.Ink200)
                ) { Text(a.rupees(), fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900) }
            }
        }
    }
}

@Composable
private fun TxnRow(txn: WalletTransaction) {
    val out = txn.amount.signum() < 0
    val title = titleFor(txn)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Text(listOfNotNull(txn.description?.takeIf { it != title }, shortDate(txn.occurredAt)).joinToString(" · "),
                fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
        Text((if (out) "−" else "+") + txn.amount.abs().rupees(), fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = if (out) NeerlyColors.Ink900 else NeerlyColors.Ok)
    }
    HorizontalDivider(color = NeerlyColors.Ink200)
}

@Composable
private fun BalanceUnavailable(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(NeerlyColors.ErrSoft, RoundedCornerShape(16.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.acct_wallet_load_failed_title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Err)
        Text(message, fontSize = 14.sp, color = NeerlyColors.Ink700)
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)) { Text(stringResource(R.string.core_retry)) }
    }
}

private val Muted = Color(0xFFC9CEE6)
private val Amber = Color(0xFFE6A321)

@Composable
private fun titleFor(t: WalletTransaction): String = when (t.type) {
    "TOPUP" -> stringResource(R.string.acct_txn_topup)
    "ORDER_PAYMENT" -> stringResource(R.string.acct_txn_order)
    "ORDER_REFUND" -> stringResource(R.string.acct_txn_refund)
    "REFERRAL_CREDIT" -> stringResource(R.string.acct_txn_referral)
    "WITHDRAWAL" -> stringResource(R.string.acct_txn_withdrawal)
    "ADJUSTMENT_CREDIT" -> stringResource(R.string.acct_txn_adjustment)
    "CASHBACK" -> stringResource(R.string.acct_txn_cashback)
    else -> t.description ?: stringResource(R.string.acct_wallet_title)
}

private fun shortDate(iso: String): String = shortDay(iso)

/** Board AddMoneyDone: what was added, the new balance, and how far it goes. */
@Composable
private fun AddMoneyDonePage(done: TopUpDone, onBack: () -> Unit, onOrderWater: () -> Unit) {
    val at = remember(done.at) {
        java.time.format.DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", com.neerly.mobile.core.util.AppLocale.locale)
            .withZone(java.time.ZoneId.of("Asia/Kolkata")).format(done.at)
    }
    com.neerly.mobile.core.design.BoondhPage(
        title = stringResource(R.string.acct_added_title),
        footer = {
            com.neerly.mobile.core.design.PrimaryButton(stringResource(R.string.acct_added_back), onBack)
            com.neerly.mobile.core.design.SecondaryButton(stringResource(R.string.acct_added_order), onOrderWater)
        }
    ) {
        item {
            Text(done.amount.rupees(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium,
                fontSize = 44.sp, color = NeerlyColors.Ok)
        }
        item {
            com.neerly.mobile.core.design.BoondhCard {
                com.neerly.mobile.core.design.InfoRow(stringResource(R.string.acct_added_new_balance),
                    subtitle = at, trailing = done.newBalance?.rupees() ?: "…")
            }
        }
        done.coverage?.let { c ->
            val n = done.deliveriesCovered
            if (n != null) item {
                com.neerly.mobile.core.design.Notice(
                    androidx.compose.ui.res.pluralStringResource(R.plurals.acct_added_covers, n,
                        c.shopName ?: stringResource(R.string.cust_the_shop), c.perDelivery.rupees(),
                        done.newBalance!!.rupees(), n),
                    com.neerly.mobile.core.design.Tone.Info)
            }
        }
    }
}
