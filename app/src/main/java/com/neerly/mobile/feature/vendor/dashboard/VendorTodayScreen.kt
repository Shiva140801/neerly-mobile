package com.neerly.mobile.feature.vendor.dashboard

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.neerly.mobile.core.design.*
import com.neerly.mobile.feature.vendor.orders.AcceptClock
import com.neerly.mobile.feature.vendor.orders.istClock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Board V08 — Today. Counts, the pause pill, and "Needs you now". */
@Composable
fun VendorTodayScreen(
    onOpen: (String) -> Unit,
    onCatalog: () -> Unit,
    onEarnings: () -> Unit,
    onSettings: () -> Unit,
    onNewOrder: (String) -> Unit = onOpen,
    onSelectTab: (VendorTab) -> Unit = {},
    onEvents: () -> Unit = {},
    onCompliance: () -> Unit = {},
    onRunSheet: () -> Unit = {},
    onJars: () -> Unit = {},
    vm: VendorTodayViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    // New orders ring like a call — ask for notifications the first time the shop opens Today (plan §e).
    com.neerly.mobile.push.AskNotificationsOnce("seller", true)
    // Poll only while this screen is in front; the alert screen takes over when an offer lands.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> { vm.refresh(quiet = true); vm.startPolling() }
                Lifecycle.Event.ON_PAUSE -> vm.stopPolling()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs); vm.stopPolling() }
    }
    LaunchedEffect(s.alertFor) { s.alertFor?.let { vm.alertShown(it); onNewOrder(it) } }

    val now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
    val d = s.dashboard
    BoondhPage(
        title = d?.businessName ?: stringResource(R.string.vend_tab_today),
        subtitle = now.format(DateTimeFormatter.ofPattern("EEE d MMM · h:mm a", androidx.compose.ui.platform.LocalConfiguration.current.locales[0])),
        bottomBar = { VendorTabBar(VendorTab.Today, onSelectTab, ordersBadge = s.pending.size) }
    ) {
        if (d == null) { item { LoadState(s.loading, s.error, { vm.refresh() }) }; return@BoondhPage }
        item { PausePill(s, vm::togglePause) }
        s.error?.let { item { Notice(it, Tone.Bad) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Count("${d.ordersToday}", stringResource(R.string.vend_cnt_orders_today), Modifier.weight(1f))
                Count("${d.deliveredToday}", stringResource(R.string.vend_cnt_delivered), Modifier.weight(1f))
                Count(d.earningToday.rupeesWhole(), stringResource(R.string.vend_cnt_yours), Modifier.weight(1f), onEarnings)
            }
        }
        if (d.jarsOut > 0) item {
            Text(listOfNotNull(pluralStringResource(R.plurals.vend_jars_at_homes, d.jarsOut, d.jarsOut),
                d.jarsLate.takeIf { it > 0 }?.let { stringResource(R.string.vend_n_late, it) }).joinToString(" · "), fontSize = 14.sp, color = NeerlyColors.Ink600)
        }
        val nothing = s.pending.isEmpty() && s.needDriver.isEmpty() && d.pendingEvents == 0 &&
            s.subsWithoutDriver.isEmpty() && d.licencesToRenew == 0 && s.soldOut.isEmpty() && s.lateJars == 0
        item { SectionLabel(stringResource(R.string.vend_needs_you_now)) }
        if (nothing) item { Notice(stringResource(R.string.vend_all_caught_up), Tone.Ok) }
        items(s.pending, key = { "p" + it.id }) { o ->
            BoondhCard(onClick = { onNewOrder(o.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.vend_new_order_from, o.customerFirstName), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    AcceptClock(o.acceptDeadlineAt)
                }
                val km = o.distanceKm?.let { stringResource(R.string.vend_km, it.stripTrailingZeros().toPlainString()) }
                Text(listOfNotNull(o.itemsLine.ifBlank { null }, km,
                    stringResource(R.string.vend_you_earn_x, o.yourEarning.rupees())).joinToString(" · "), fontSize = 14.sp, color = NeerlyColors.Ink700)
            }
        }
        if (d.pendingEvents > 0) item {
            BoondhCard(onClick = onEvents) {
                InfoRow(pluralStringResource(R.plurals.vend_events_to_answer, d.pendingEvents.toInt(), d.pendingEvents.toInt()),
                    stringResource(R.string.vend_events_answer_sub))
            }
        }
        items(s.needDriver, key = { "d" + it.id }) { o ->
            BoondhCard(onClick = { onOpen(o.id) }) {
                Pill(stringResource(if (o.status == "PREPARING") R.string.vend_status_packed_needs_driver else R.string.vend_accepted_pack), Tone.Bad)
                InfoRow(stringResource(R.string.vend_name_order, o.customerFirstName, o.orderNumber), o.itemsLine)
            }
        }
        if (s.subsWithoutDriver.isNotEmpty()) item {
            BoondhCard(onClick = onRunSheet) {
                InfoRow(pluralStringResource(R.plurals.vend_subs_no_driver, s.subsWithoutDriver.size, s.subsWithoutDriver.size),
                    stringResource(R.string.vend_subs_no_driver_sub, s.subsWithoutDriver.sumOf { it.quantity }.let { q -> pluralStringResource(R.plurals.vend_n_jars, q, q) }))
            }
        }
        if (s.lateJars > 0) item {
            BoondhCard(onClick = onJars) {
                InfoRow(pluralStringResource(R.plurals.vend_jars_late, s.lateJars, s.lateJars),
                    stringResource(R.string.vend_jars_late_sub))
            }
        }
        if (d.licencesToRenew > 0) item {
            BoondhCard(onClick = onCompliance) {
                InfoRow(pluralStringResource(R.plurals.vend_licences_renew, d.licencesToRenew.toInt(), d.licencesToRenew.toInt()),
                    stringResource(R.string.vend_licences_renew_sub))
            }
        }
        if (s.soldOut.isNotEmpty()) item {
            BoondhCard(onClick = onCatalog) {
                InfoRow(pluralStringResource(R.plurals.vend_products_off, s.soldOut.size, s.soldOut.joinToString { it.name }),
                    pluralStringResource(R.plurals.vend_products_off_sub, s.soldOut.size))
            }
        }
        if (s.active.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.vend_on_the_go, s.active.size)) }
            items(s.active.filter { it.driverId != null || it.status !in setOf("VENDOR_ACCEPTED", "PREPARING") }, key = { "a" + it.id }) { o ->
                BoondhCard(onClick = { onOpen(o.id) }) {
                    InfoRow(stringResource(R.string.vend_name_order, o.customerFirstName, o.orderNumber), "${statusWord(o.status)} · ${o.itemsLine}", o.orderValue.rupees())
                }
            }
        }
        item { TextButton(onClick = onSettings, modifier = Modifier.heightIn(min = 44.dp)) {
            Text(stringResource(R.string.vend_shop_settings), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) } }
    }
}

@Composable
private fun PausePill(s: VendorTodayUiState, onToggle: () -> Unit) {
    val b = s.business
    val open = b?.isOpen != false
    val until = b?.pausedUntil?.let { runCatching { istClock(it) }.getOrNull() }
    BoondhCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Pill(when { open -> stringResource(R.string.vend_taking_orders); until != null -> stringResource(R.string.vend_paused_till, until)
                    else -> stringResource(R.string.vend_paused_short) }, if (open) Tone.Ok else Tone.Warn)
                if (!open) Text(until?.let { stringResource(R.string.vend_paused_body_until, it) } ?: stringResource(R.string.vend_paused_body),
                    fontSize = 13.sp, color = NeerlyColors.Ink600, modifier = Modifier.padding(top = 6.dp))
            }
            TextButton(onClick = onToggle, enabled = !s.working, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(if (open) R.string.vend_pause_hour else R.string.vend_take_orders), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Count(value: String, label: String, modifier: Modifier, onClick: (() -> Unit)? = null) {
    BoondhCard(modifier, onClick) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        Text(label, fontSize = 13.sp, color = NeerlyColors.Ink600)
    }
}

/** An order status in the shop's own words (and language). */
@Composable
internal fun statusWord(status: String): String = when (status) {
    "VENDOR_ASSIGNED" -> stringResource(R.string.vend_sw_new)
    "VENDOR_ACCEPTED" -> stringResource(R.string.vend_sw_accepted)
    "PREPARING" -> stringResource(R.string.vend_sw_packing)
    "DISPATCHED" -> stringResource(R.string.vend_sw_on_way)
    "ARRIVING" -> stringResource(R.string.vend_sw_door)
    "DELIVERED" -> stringResource(R.string.vend_sw_delivered)
    "CANCELLED" -> stringResource(R.string.vend_sw_cancelled)
    "FAILED" -> stringResource(R.string.vend_sw_failed)
    else -> status.lowercase().replace('_', ' ')
}
