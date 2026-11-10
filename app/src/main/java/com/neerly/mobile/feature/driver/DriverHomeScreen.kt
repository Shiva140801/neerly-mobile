package com.neerly.mobile.feature.driver

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.dto.DriverAssignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.semantics.heading
import java.math.BigDecimal

/** Primary actions on the driver app: taller than the customer app's, one per screen. */
private val BIG = Modifier.heightIn(min = 60.dp)

@Composable
fun DriverHomeScreen(
    onCodReconcile: () -> Unit,
    onSwitchRole: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    onSos: () -> Unit = {},
    onVehicle: () -> Unit = {},
    onDocuments: () -> Unit = {},
    onPickups: () -> Unit = {},
    vm: DriverHomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e ->
            when (e) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> vm.setVisible(true)
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> vm.setVisible(false)
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); vm.setVisible(false) }
    }
    // Live tracking needs a fix stream the moment the shift starts, so ask when the driver
    // goes on duty rather than burying it in onboarding.
    val context = LocalContext.current
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.onLocationPermission(it)
        if (it && vm.state.value.isOnDuty) com.neerly.mobile.push.DriverDutyService.start(context)
    }
    LaunchedEffect(state.isOnDuty) {
        if (!state.isOnDuty) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) vm.onLocationPermission(true) else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    // Push: ask for notifications when duty starts (plan §e), and keep the on-duty card — a
    // location foreground service — up for the whole shift (NPDLock).
    com.neerly.mobile.push.AskNotificationsOnce("driver", state.isOnDuty)
    var wasOnDuty by remember { mutableStateOf(false) }
    LaunchedEffect(state.isOnDuty, state.loading) {
        if (state.loading) return@LaunchedEffect
        val located = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (state.isOnDuty && located) { com.neerly.mobile.push.DriverDutyService.start(context); wasOnDuty = true }
        else if (!state.isOnDuty && wasOnDuty) { com.neerly.mobile.push.DriverDutyService.end(context, state.today?.cashInHand); wasOnDuty = false }
    }
    // D19: re-check on every return to the app — the driver may have switched GPS off meanwhile.
    OnReturn { reportGps(context, vm) }
    LaunchedEffect(state.isOnDuty) { reportGps(context, vm) }

    var tab by rememberSaveable { mutableStateOf(DriverTab.Tasks) }
    // A new task always wins the screen — it has a 45-second clock.
    LaunchedEffect(state.offer?.orderId) { if (state.offer != null) tab = DriverTab.Tasks }

    Column(Modifier.fillMaxSize().background(NeerlyColors.Canvas)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                DriverTab.Tasks -> TasksTab(state, vm, onSos, onAskLocation = { locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
                DriverTab.Round -> RoundTab(state, onOpen = { tab = DriverTab.Tasks })
                DriverTab.Money -> MoneyTab(state, onCodReconcile)
                DriverTab.Me -> DriverMeContent(onDuty = state.isOnDuty, onEndDuty = vm::askEndShift, onVehicle = onVehicle,
                    onDocuments = onDocuments, onPickups = onPickups, onSos = onSos, onSwitchRole = onSwitchRole, onLogout = onLogout)
            }
        }
        DriverTabBar(current = tab, onSelect = { tab = it }, offerWaiting = state.offer != null && tab != DriverTab.Tasks)
    }
    state.endCheck?.let { EndDutyDialog(it, state, vm) }
}

private fun reportGps(context: android.content.Context, vm: DriverHomeViewModel) {
    val permitted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
    val on = lm?.let { androidx.core.location.LocationManagerCompat.isLocationEnabled(it) } ?: true
    vm.onGpsStatus(permitted, on)
}

/** End duty: what's still out (drops, cash) and the hand-back choice (D18 end / 409 DRIVER_DROPS_PENDING). */
@Composable
private fun EndDutyDialog(c: EndCheck, s: DriverHomeUiState, vm: DriverHomeViewModel) {
    AlertDialog(onDismissRequest = vm::cancelEndShift,
        title = { Text(if (c.pendingDrops.isEmpty()) stringResource(R.string.drv_end_duty_q) else stringResource(R.string.drv_still_drops)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (c.pendingDrops.isNotEmpty()) {
                    Text(stringResource(R.string.drv_drops_back_queue))
                    c.pendingDrops.forEach { Text(it.shopName?.let { n -> stringResource(R.string.drv_bullet_shop, it.orderNumber, n) } ?: stringResource(R.string.drv_bullet, it.orderNumber), fontWeight = FontWeight.SemiBold) }
                }
                if (c.cashHeld.signum() > 0) Text(stringResource(R.string.drv_holding_cash_money, c.cashHeld.rupees()))
                if (c.pendingDrops.isEmpty() && c.cashHeld.signum() == 0) Text(stringResource(R.string.drv_no_tasks_until_start))
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.finishShift(handBack = c.pendingDrops.isNotEmpty()) }, enabled = !s.ending) {
                Text(if (s.ending) stringResource(R.string.drv_ending) else if (c.pendingDrops.isEmpty()) stringResource(R.string.drv_end_duty) else stringResource(R.string.drv_hand_back_end), color = NeerlyColors.Err)
            }
        },
        dismissButton = { TextButton(onClick = vm::cancelEndShift) { Text(stringResource(R.string.drv_keep_driving)) } })
}

// ---------------------------------------------------------------------------------------------
// Tasks tab — one of the loop's screens at a time.

@Composable
private fun TasksTab(s: DriverHomeUiState, vm: DriverHomeViewModel, onSos: () -> Unit, onAskLocation: () -> Unit) {
    val job = s.activeAssignment
    when {
        s.loading -> LoadState(true, null, {})
        s.failed != null -> FailedDropScreen(s.failed, vm)
        s.done != null -> DoneScreen(s.done, s, vm)
        s.notHome != null && job != null -> NotHomeScreen(job, s.notHome, vm, onSos)
        s.offer != null -> OfferScreen(s, vm)
        s.missed != null -> MissedScreen(s.missed, vm::dismissMissed)
        s.choosingShop -> ChooseShopScreen(s, vm)
        !s.isOnDuty -> OffDutyScreen(s, vm)
        s.gps != null -> GpsProblemScreen(s.gps, onAskLocation, onSos)
        s.onBreak -> OnBreakScreen(s, vm, onSos)
        job != null && s.step == DropStep.LOAD -> PickupScreen(job, s, vm, onSos)
        job != null && s.step == DropStep.RIDE -> RideScreen(job, s, vm, onSos)
        job != null && s.step == DropStep.HAND_OVER -> HandOverScreen(job, s, vm, onSos)
        else -> OnDutyScreen(s, vm, onSos, onAskLocation)
    }
}

/** D01 — off duty. */
@Composable
private fun OffDutyScreen(s: DriverHomeUiState, vm: DriverHomeViewModel) {
    BoondhPage(title = stringResource(R.string.drv_off_title), subtitle = stringResource(R.string.drv_off_sub),
        footer = {
            PrimaryButton(if (s.starting) stringResource(R.string.drv_starting) else stringResource(R.string.drv_start_duty), { vm.startShift() },
                enabled = !s.starting, modifier = BIG, color = Boondh.Amber, contentColor = NeerlyColors.Ink900)
        }) {
        s.error?.let { item { Notice(it, Tone.Bad) } }
        if (s.shops.isNotEmpty()) item {
            BoondhCard {
                SectionLabel(if (s.shops.size == 1) stringResource(R.string.drv_your_shop) else stringResource(R.string.drv_shops_you_drive))
                s.shops.forEach { InfoRow(it.shopName, it.address) }
            }
        }
        if (!s.loading && s.shops.isEmpty()) item { Notice(stringResource(R.string.drv_no_shop_yet), Tone.Info) }
        s.today?.takeIf { it.drops > 0 }?.let { t -> item {
            BoondhCard {
                SectionLabel(stringResource(R.string.drv_today_so_far))
                InfoRow(pluralStringResource(R.plurals.drv_drops_n, t.drops, t.drops), trailing = t.earned.rupees())
            }
        } }
        // DOff: "Yesterday · 15 drops · you earned ₹610".
        s.today?.takeIf { it.yesterdayDrops > 0 }?.let { t -> item {
            BoondhCard {
                SectionLabel(stringResource(R.string.drv_yesterday))
                InfoRow(pluralStringResource(R.plurals.drv_drops_n, t.yesterdayDrops, t.yesterdayDrops),
                    trailing = t.yesterdayEarned.rupees())
            }
        } }
    }
}

/** D14 — which shop today (only when the driver has more than one). */
@Composable
private fun ChooseShopScreen(s: DriverHomeUiState, vm: DriverHomeViewModel) {
    BoondhPage(title = stringResource(R.string.drv_which_shop), onBack = vm::cancelChooseShop,
        subtitle = stringResource(R.string.drv_which_shop_sub)) {
        s.shops.forEach { shop -> item(key = shop.shopId) {
            BoondhCard(onClick = { vm.startShift(shop.shopId) }) {
                Text(shop.shopName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                shop.address?.let { Text(it, fontSize = 14.sp, color = NeerlyColors.Ink600) }
            }
        } }
    }
}

/** D02 / DOnline — on duty: duty clock, the day's numbers, the round, alert preview, signal. */
@Composable
private fun OnDutyScreen(s: DriverHomeUiState, vm: DriverHomeViewModel, onSos: () -> Unit, onSignal: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val started = s.shift?.startedAt?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
    val dutyFor = started?.let { java.time.Duration.ofMillis((now - it.toEpochMilli()).coerceAtLeast(0)) }
    val shopName = s.shops.firstOrNull { it.shopId == s.shift?.vendorId }?.shopName ?: s.shops.firstOrNull()?.shopName
    val zone = java.time.ZoneId.systemDefault()
    // A shift left open from an earlier day says which day, instead of "since 5:59 PM".
    val sinceLabel = started?.let {
        val at = it.atZone(zone)
        if (at.toLocalDate() == java.time.LocalDate.now(zone)) clock(s.shift!!.startedAt)
        else stringResource(R.string.drv_since_day, at.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH)), clock(s.shift!!.startedAt))
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending = s.assignments.filter { it.status in setOf("VENDOR_ACCEPTED", "PREPARING", "DISPATCHED", "ARRIVING") }
    LazyColumn(Modifier.fillMaxSize().background(NeerlyColors.Canvas), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(NeerlyColors.Ok, RoundedCornerShape(6.dp)))
                        Spacer(Modifier.width(8.dp))
                        Text(dutyFor?.let { stringResource(R.string.drv_on_duty_for, hoursMinutes(it)) } ?: stringResource(R.string.drv_on_duty),
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ok,
                            modifier = Modifier.semantics { heading() })
                    }
                    if (shopName != null && sinceLabel != null) Text(stringResource(R.string.drv_shop_since, shopName, sinceLabel),
                        fontSize = 15.sp, color = NeerlyColors.Ink600)
                    else sinceLabel?.let { Text(stringResource(R.string.drv_on_duty_since, it), fontSize = 15.sp, color = NeerlyColors.Ink600) }
                }
                SosChip(onSos)
            }
        }
        s.error?.let { item { Notice(it, Tone.Bad) } }
        item {
            // One card, three columns, like DOnline.
            Row(Modifier.fillMaxWidth().background(NeerlyColors.Paper, RoundedCornerShape(16.dp))
                .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(16.dp)).height(IntrinsicSize.Min)) {
                DutyStat("${s.today?.drops ?: 0}", stringResource(R.string.drv_drops_done_stat), Modifier.weight(1f))
                VerticalDivider(color = NeerlyColors.Ink200)
                DutyStat((s.today?.earned ?: BigDecimal.ZERO).rupeesWhole(), stringResource(R.string.drv_stat_you_earned), Modifier.weight(1f))
                VerticalDivider(color = NeerlyColors.Ink200)
                DutyStat((s.today?.cashInHand ?: BigDecimal.ZERO).rupeesWhole(), stringResource(R.string.drv_stat_cash_with_you), Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.drv_your_tasks), fontFamily = FontFamily.Serif, fontSize = 26.sp, color = NeerlyColors.Ink900,
                    modifier = Modifier.weight(1f).semantics { heading() })
            }
        }
        if (pending.isNotEmpty()) item {
            val cash = pending.filter { it.paymentMethod == "COD" }.fold(BigDecimal.ZERO) { a, j -> a + (j.codAmount ?: BigDecimal.ZERO) }
            val next = pending.first()
            Column(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(16.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.drv_round_caps).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Boondh.Amber)
                Text(stringResource(R.string.drv_drops_left_n, pending.size), fontFamily = FontFamily.Serif, fontSize = 28.sp, color = NeerlyColors.Paper)
                if (cash.signum() > 0) Text(stringResource(R.string.drv_round_cash, cash.rupees()), fontSize = 15.sp, color = Boondh.MutedOnNavy)
                HorizontalDivider(color = Boondh.MutedOnNavy.copy(alpha = 0.3f))
                Text(stringResource(R.string.drv_round_next, next.customerFirstName, next.deliveryAddress.substringBefore(',')),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Paper)
            }
        } else item {
            BoondhCard {
                Text(stringResource(R.string.drv_waiting_next), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text(stringResource(R.string.drv_keep_app_open), fontSize = 14.sp, color = NeerlyColors.Ink600)
            }
        }
        item { BreakPicker(s.working, vm::startBreak) }
        item {
            // DOnline: hear what a task sounds like before one rings for real.
            Row(Modifier.fillMaxWidth().border(1.dp, NeerlyColors.Ink400, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.drv_ring_here), fontSize = 15.sp, color = NeerlyColors.Ink600, modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { previewRing() } }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.drv_preview_alert), fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().background(NeerlyColors.OkSoft, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(NeerlyColors.Ok, RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.drv_gps_ok), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ok, modifier = Modifier.weight(1f))
                TextButton(onClick = onSignal, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.drv_signal_trouble), fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary)
                }
            }
        }
    }
}

@Composable
private fun DutyStat(value: String, label: String, modifier: Modifier) {
    Column(modifier.padding(16.dp)) {
        Text(value, fontFamily = FontFamily.Serif, fontSize = 26.sp, color = NeerlyColors.Ink900, maxLines = 1)
        Text(label, fontSize = 14.sp, color = NeerlyColors.Ink600)
    }
}

private fun hoursMinutes(d: java.time.Duration): String {
    val h = d.toHours(); val m = d.toMinutes() % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** The same three beeps a real task rings with (RingActivities), once. */
private suspend fun previewRing() {
    val tone = runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_RING, 100) }.getOrNull() ?: return
    repeat(3) { tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 400); delay(650) }
    tone.release()
}

/** DOnBreak: the driver says how long — 15, 30 or 45 minutes (admin default pre-picked). */
@Composable
private fun BreakPicker(working: Boolean, onStart: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val default = com.neerly.mobile.data.config.LocalAppConfig.current.driverBreakMinutes
    var pick by remember(default) { mutableIntStateOf(DriverHomeViewModel.BREAK_CHOICES.minByOrNull { kotlin.math.abs(it - default) } ?: 30) }
    if (!open) { SecondaryButton(if (working) "…" else stringResource(R.string.drv_take_break), { open = true }, enabled = !working, modifier = BIG); return }
    BoondhCard {
        Text(stringResource(R.string.drv_break_how_long), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
        // Equal-width choices across the card, picked one in turmeric (not the stock lavender chip).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DriverHomeViewModel.BREAK_CHOICES.forEach { m ->
                val on = pick == m
                Surface(onClick = { pick = m }, selected = on, shape = RoundedCornerShape(12.dp),
                    color = if (on) Boondh.Amber else NeerlyColors.Paper,
                    border = androidx.compose.foundation.BorderStroke(if (on) 2.dp else 1.dp, if (on) Boondh.Amber else NeerlyColors.Ink200),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.drv_break_n_min, m), fontSize = 16.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = NeerlyColors.Ink900)
                    }
                }
            }
        }
        PrimaryButton(if (working) "…" else stringResource(R.string.drv_take_break), { onStart(pick) }, enabled = !working, modifier = BIG,
            color = Boondh.Amber, contentColor = NeerlyColors.Ink900)
    }
}

/** D28 — on break: no new tasks until the driver comes back. */
@Composable
private fun OnBreakScreen(s: DriverHomeUiState, vm: DriverHomeViewModel, onSos: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val since = s.shiftState?.onBreakSince?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
    val mins = since?.let { ((now - it) / 60000).coerceAtLeast(0) }
    val endsAt = s.shiftState?.breakEndsAt
    val left = secondsUntil(endsAt, now)
    // The server closes the break at its end time; pick that up so tasks show again.
    LaunchedEffect(left == 0L) { if (left == 0L) { delay(2000); vm.refresh() } }
    // "We buzz you 2 minutes before" — once, while the break screen is up.
    val context = LocalContext.current
    var buzzed by remember(endsAt) { mutableStateOf(false) }
    LaunchedEffect(left != null && left in 1L..BREAK_BUZZ_SECONDS) {
        if (left != null && left in 1L..BREAK_BUZZ_SECONDS && !buzzed) { buzzed = true; buzz(context) }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A2238)).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.drv_on_break_caps), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Boondh.Amber,
                letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            SosChip(onSos)
        }
        breakWindow(s.shiftState?.onBreakSince, endsAt)?.let {
            Text(stringResource(R.string.drv_break_window, it), fontSize = 16.sp, color = Boondh.MutedOnNavy)
        }
        Text(left?.let { minSec(it) }
            ?: mins?.let { pluralStringResource(R.plurals.drv_on_break_minutes, it.toInt(), it.toInt()) } ?: stringResource(R.string.drv_on_break),
            fontFamily = FontFamily.Serif, fontSize = 30.sp, color = NeerlyColors.Paper,
            modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
        Text(stringResource(R.string.drv_break_now, java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)
            .format(java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()))), fontSize = 14.sp, color = Boondh.MutedOnNavy)
        Text(clockOf(endsAt)?.let { stringResource(R.string.drv_break_till, it) } ?: stringResource(R.string.drv_no_tasks_break),
            fontSize = 16.sp, color = Boondh.MutedOnNavy)
        BoondhCard {
            InfoRow(stringResource(R.string.drv_drops_done_today), trailing = "${s.today?.drops ?: 0}")
            InfoRow(stringResource(R.string.drv_cash_with_you), trailing = (s.today?.cashInHand ?: BigDecimal.ZERO).rupees())
        }
        // DOnBreak: what's waiting when the break ends.
        s.assignments.firstOrNull { it.status in setOf("VENDOR_ACCEPTED", "PREPARING", "DISPATCHED", "ARRIVING") }?.let { next ->
            BoondhCard {
                SectionLabel(stringResource(R.string.drv_break_next_drop))
                Text(next.customerFirstName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text(next.deliveryAddress, fontSize = 14.sp, color = NeerlyColors.Ink600)
                next.codAmount?.takeIf { it.signum() > 0 }?.let {
                    Text(stringResource(R.string.drv_break_next_cash, it.rupees()), fontSize = 14.sp, color = NeerlyColors.Ink600)
                }
            }
        }
        s.error?.let { Notice(it, Tone.Bad) }
        Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
        PrimaryButton(if (s.working) "…" else stringResource(R.string.drv_break_im_back_start), vm::endBreak, enabled = !s.working, modifier = BIG,
            color = Boondh.Amber, contentColor = NeerlyColors.Ink900)
        TextButton(onClick = vm::askEndShift, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.drv_break_end_shift), color = NeerlyColors.Paper, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    BoondhCard(modifier) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, maxLines = 1)
        Text(label, fontSize = 13.sp, color = NeerlyColors.Ink600)
    }
}

/** D03 — a task, with the clock. */
@Composable
private fun OfferScreen(s: DriverHomeUiState, vm: DriverHomeViewModel) {
    val offer = s.offer ?: return
    val t = offer.task
    var left by remember(offer.orderId) { mutableLongStateOf(offer.secondsRemaining) }
    LaunchedEffect(offer.orderId) {
        while (true) {
            left = (offer.secondsRemaining - (System.currentTimeMillis() - s.offerShownAtMs) / 1000).coerceAtLeast(0)
            if (left <= 0) { vm.offerExpired(); break }
            delay(1000)
        }
    }
    Column(Modifier.fillMaxSize().background(Boondh.Amber).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.drv_new_task_caps), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, letterSpacing = 1.sp)
        val leftCd = stringResource(R.string.drv_seconds_left_cd, left.toInt())
        Text("0:%02d".format(left), fontSize = 56.sp, fontFamily = FontFamily.Serif, color = NeerlyColors.Ink900,
            modifier = Modifier.semantics { contentDescription = leftCd })
        Text(stringResource(R.string.drv_left_to_take), fontSize = 15.sp, color = NeerlyColors.Ink900)
        BoondhCard {
            Text(stringResource(R.string.drv_you_earn, offer.driverFee.rupees()), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            t?.let {
                HorizontalDivider(color = NeerlyColors.Ink200)
                SectionLabel(stringResource(R.string.drv_pick_up))
                InfoRow(it.vendorName, it.pickupAddress)
                Text("${pluralStringResource(R.plurals.drv_jars_n, it.jars, it.jars)} · ${it.loadKg.stripTrailingZeros().toPlainString()} kg",
                    fontSize = 14.sp, color = NeerlyColors.Ink600)
                SectionLabel(it.dropDistanceKm?.let { d -> stringResource(R.string.drv_drop_km, "%.1f".format(d)) } ?: stringResource(R.string.drv_drop))
                InfoRow(it.customerFirstName, it.dropAddress)
                val extras = listOfNotNull(
                    it.emptiesExpected.takeIf { e -> e > 0 }?.let { e -> pluralStringResource(R.plurals.drv_empties_to_bring, e, e) },
                    if (it.paymentMethod == "COD") stringResource(R.string.drv_collect_cash, (it.codAmount ?: BigDecimal.ZERO).rupees()) else stringResource(R.string.drv_paid_online))
                Text(extras.joinToString(" · "), fontSize = 14.sp, color = NeerlyColors.Ink900)
            }
        }
        PrimaryButton(if (s.offerBusy) stringResource(R.string.drv_taking_it) else stringResource(R.string.drv_take_it), vm::acceptOffer, enabled = !s.offerBusy,
            modifier = BIG, color = NeerlyColors.Ink900, contentColor = NeerlyColors.Paper)
        SecondaryButton(stringResource(R.string.drv_pass), vm::passOffer, enabled = !s.offerBusy, modifier = BIG, color = NeerlyColors.Ink900)
    }
}

/** D20 — it timed out. (No auto-accept: the server doesn't have it.) */
@Composable
private fun MissedScreen(m: MissedTask, onOk: () -> Unit) {
    BoondhPage(title = stringResource(R.string.drv_missed_title),
        subtitle = stringResource(R.string.drv_missed_sub, m.fee.rupees(), m.where, m.shop),
        footer = { PrimaryButton(stringResource(R.string.drv_missed_ok), onOk, modifier = BIG) }) {
        item { Text(stringResource(R.string.drv_went_other), fontSize = 15.sp, color = NeerlyColors.Ink600) }
        item { Notice(stringResource(R.string.drv_missed_note), Tone.Info) }
    }
}

/** D04 — load at the shop; every line ticked before riding. */
@Composable
private fun PickupScreen(job: DriverAssignment, s: DriverHomeUiState, vm: DriverHomeViewModel, onSos: () -> Unit) {
    val card = s.taskCards[job.orderId]
    val lines = buildList {
        add(job.product to stringResource(R.string.drv_check_seal))
        if (job.emptiesExpected > 0) add(pluralStringResource(R.plurals.drv_space_empties, job.emptiesExpected, job.emptiesExpected) to
            stringResource(R.string.drv_bring_back_home))
        if (job.paymentMethod == "COD") add(stringResource(R.string.drv_collect_cash, (job.codAmount ?: BigDecimal.ZERO).rupees()) to
            stringResource(R.string.drv_customer_pays_door))
    }
    var ticked by remember(job.orderId) { mutableStateOf(setOf<Int>()) }
    val ready = job.status == "DISPATCHED"
    val allTicked = ticked.size == lines.size
    DriverStepPage(eyebrow = stringResource(R.string.drv_step1_label),
        eyebrowSub = stringResource(R.string.drv_order_customer, job.orderNumber, job.customerFirstName),
        title = stringResource(R.string.drv_load_title), onSos = onSos,
        footer = {
            if (!ready) Notice(stringResource(R.string.drv_still_packing), Tone.Warn)
            // DPickup: the button says what's missing until every line is ticked.
            PrimaryButton(when { s.working -> stringResource(R.string.drv_starting); !allTicked -> stringResource(R.string.drv_tick_each_cta)
                    else -> stringResource(R.string.drv_loaded_start) }, { vm.loaded(job.orderId) },
                enabled = ready && allTicked && !s.working, modifier = BIG)
        }) {
        s.error?.let { item { Notice(it, Tone.Bad) } }
        lines.forEachIndexed { i, (title, sub) -> item(key = "line$i") {
            com.neerly.mobile.core.design.CheckCard(title, sub, checked = i in ticked,
                onToggle = { ticked = if (i in ticked) ticked - i else ticked + i })
        } }
        if (job.emptiesExpected > 0) item {
            Notice(pluralStringResource(R.plurals.drv_bring_empties_note, job.emptiesExpected, job.emptiesExpected), Tone.Warn)
        }
        job.notes?.let { item { Notice(stringResource(R.string.drv_customer_note, it), Tone.Info) } }
        // Where to pick up — the shop's name and address come on the assignment itself.
        val shopName = job.shopName ?: card?.vendorName
        val shopAddress = job.pickupAddress ?: card?.pickupAddress
        if (shopName != null || shopAddress != null || job.shopPhone != null) item {
            BoondhCard {
                InfoRow(shopName ?: stringResource(R.string.drv_the_shop), shopAddress)
                job.shopPhone?.let { CallButton(it, stringResource(R.string.drv_call_shop_btn), color = NeerlyColors.CustomerDark) }
            }
        }
    }
}

/** D05 / D32 — ride to the door. No Maps key, so the route opens in Google Maps. */
@Composable
private fun RideScreen(job: DriverAssignment, s: DriverHomeUiState, vm: DriverHomeViewModel, onSos: () -> Unit) {
    val context = LocalContext.current
    val km = s.here?.let { kmBetween(it, job.deliveryLat to job.deliveryLng) }
    val index = s.assignments.indexOf(job) + 1
    val who = if (job.paymentMethod == "COD") stringResource(R.string.drv_cash_suffix, job.customerFirstName, (job.codAmount ?: BigDecimal.ZERO).rupees())
        else job.customerFirstName
    // DNavigate: the address is the title (that's what the driver looks for), the person
    // and the cash are on the step line, SOS sits top-right.
    val (firstLine, rest) = splitAddress(job.deliveryAddress)
    DriverStepPage(
        eyebrow = if (s.assignments.size > 1) stringResource(R.string.drv_step2_label_n, index, s.assignments.size, who)
            else stringResource(R.string.drv_step2_label, who),
        title = firstLine, eyebrowSub = rest, onSos = onSos,
        footer = {
            PrimaryButton(stringResource(R.string.drv_open_maps), { openMaps(context, job.deliveryLat, job.deliveryLng, job.customerFirstName) },
                modifier = BIG, color = NeerlyColors.CustomerDark)
            PrimaryButton(if (s.working) stringResource(R.string.drv_telling) else stringResource(R.string.drv_at_door), { vm.markArrived(job.orderId) },
                enabled = !s.working, modifier = BIG, color = Boondh.Amber, contentColor = NeerlyColors.Ink900)
        }) {
        s.error?.let { item { Notice(it, Tone.Bad) } }
        item {
            BoondhCard {
                Text(job.emptiesExpected.takeIf { it > 0 }?.let { stringResource(R.string.drv_join2, job.product, pluralStringResource(R.plurals.drv_empties_back, it, it)) } ?: job.product,
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text(km?.let { stringResource(R.string.drv_km_from_you, "%.1f".format(it)) } ?: stringResource(R.string.drv_location_to_see),
                    fontSize = 15.sp, color = NeerlyColors.Ink600)
            }
        }
        job.notes?.let { item { Notice(stringResource(R.string.drv_quote, it), Tone.Info) } }
        if (job.customerPhone != null || job.shopPhone != null) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                job.customerPhone?.let { CallButton(it, stringResource(R.string.drv_call_name, job.customerFirstName),
                    color = NeerlyColors.CustomerDark, modifier = Modifier.fillMaxWidth()) }
                job.shopPhone?.let { CallButton(it, stringResource(R.string.drv_call_shop_btn), color = NeerlyColors.CustomerDark,
                    modifier = Modifier.fillMaxWidth()) }
            }
        }
        item { Text(stringResource(R.string.drv_customer_call, job.customerPhoneMask), fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}

/** D06 + D07 — code, empties, door photo, then cash for cash orders. */
@Composable
private fun HandOverScreen(job: DriverAssignment, s: DriverHomeUiState, vm: DriverHomeViewModel, onSos: () -> Unit) {
    if (s.codeLocked) { CodeLockedScreen(job, s, vm, onSos); return }
    var shortReason by remember(job.orderId) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var code by remember(job.orderId) { mutableStateOf("") }
    var empties by remember(job.orderId) { mutableIntStateOf(job.emptiesExpected) }
    val isCash = job.paymentMethod == "COD"
    var settlement by remember(job.orderId) { mutableStateOf("CASH") }
    var partGot by remember(job.orderId) { mutableStateOf("") }
    var pending by remember { mutableStateOf<java.io.File?>(null) }
    val camera = rememberCameraCapture { ok ->
        pending?.let { vm.photoTaken(it, ok) }
    }
    val due = job.codAmount ?: BigDecimal.ZERO
    val photoNeeded = com.neerly.mobile.data.config.LocalAppConfig.current.deliveryPhotoRequired
    // DPayment "Paid part of it": more than nothing, less than the bill.
    val partAmount = partGot.toBigDecimalOrNull()?.takeIf { it.signum() > 0 && it < due }
    val cashTaken = when { !isCash || settlement == "NOT_COLLECTED" -> null; settlement == "PARTIAL" -> partAmount; else -> due }
    DriverStepPage(eyebrow = stringResource(R.string.drv_step3_label),
        eyebrowSub = stringResource(R.string.drv_order_customer, job.orderNumber, job.customerFirstName),
        title = stringResource(R.string.drv_ask_code_title, job.customerFirstName), onSos = onSos,
        footer = {
            s.deliverError?.let { Notice(it, Tone.Bad) }
            PrimaryButton(when { s.delivering -> stringResource(R.string.drv_saving); isCash && settlement == "CASH" -> stringResource(R.string.drv_got_cash_hand, due.rupees())
                    isCash && settlement == "PARTIAL" -> stringResource(R.string.drv_got_part); else -> stringResource(R.string.drv_hand_over_done) },
                { vm.deliver(code, empties, if (isCash) settlement else null, cashTaken, shortReason, photoNeeded) },
                enabled = !s.delivering && code.length == DriverHomeViewModel.CODE_LENGTH && (s.photo is DoorPhoto.Ready || !photoNeeded) &&
                    (!isCash || settlement != "PARTIAL" || partAmount != null) &&
                    (empties >= job.emptiesExpected || shortReason != null),
                modifier = BIG, color = NeerlyColors.Ok)
            TextButton(onClick = vm::openNotHome, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.drv_nobody_door), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
        }) {
        s.codeAttemptsLeft?.takeIf { it > 0 }?.let { left -> item { Notice(pluralStringResource(R.plurals.drv_tries_left, left, left), Tone.Warn) } }
        item { Text(stringResource(R.string.drv_ask_digit_code, DriverHomeViewModel.CODE_LENGTH), fontSize = 15.sp, color = NeerlyColors.Ink600) }
        item {
            OutlinedTextField(value = code, onValueChange = { v -> code = v.filter(Char::isDigit).take(DriverHomeViewModel.CODE_LENGTH) },
                singleLine = true, placeholder = { Text(stringResource(R.string.drv_digit_code_ph, DriverHomeViewModel.CODE_LENGTH), fontSize = 22.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 30.sp, letterSpacing = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp))
        }
        item {
            BoondhCard {
                SectionLabel(stringResource(R.string.drv_empties_expected, job.emptiesExpected))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { if (empties > 0) empties-- }, modifier = Modifier.size(60.dp),
                        contentPadding = PaddingValues(0.dp)) { Text("−", fontSize = 28.sp) }
                    val emptiesCd = pluralStringResource(R.plurals.drv_empties_collected_cd, empties, empties)
                    Text("$empties", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                        modifier = Modifier.semantics { contentDescription = emptiesCd })
                    OutlinedButton(onClick = { empties++ }, modifier = Modifier.size(60.dp),
                        contentPadding = PaddingValues(0.dp)) { Text("+", fontSize = 28.sp) }
                }
                if (empties < job.emptiesExpected) {
                    // D23 — why fewer empties came back; the shop sees the reason.
                    Text(stringResource(R.string.drv_short_why, job.emptiesExpected - empties), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Warn)
                    FlowRowLite(SHORT_REASONS) { (v, label) ->
                        FilterChip(selected = shortReason == v, onClick = { shortReason = v }, label = { Text(stringResource(label), fontSize = 15.sp) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }
        }
        item {
            BoondhCard {
                SectionLabel(stringResource(R.string.drv_photo_door))
                when (val p = s.photo) {
                    DoorPhoto.None -> Text(if (photoNeeded) stringResource(R.string.drv_photo_needed) else stringResource(R.string.drv_photo_optional),
                        fontSize = 14.sp, color = NeerlyColors.Ink600)
                    DoorPhoto.Uploading -> Text(stringResource(R.string.drv_photo_sending), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    is DoorPhoto.Ready -> Text(stringResource(R.string.drv_photo_saved), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ok)
                    DoorPhoto.StorageDown -> Notice(stringResource(R.string.drv_photo_storage_down), Tone.Warn)
                    is DoorPhoto.Failed -> Notice(p.message, Tone.Bad)
                }
                SecondaryButton(if (s.photo is DoorPhoto.Ready) stringResource(R.string.drv_take_again) else stringResource(R.string.drv_take_photo), {
                    val f = vm.newPhotoFile(); pending = f
                    camera(f)
                }, enabled = s.photo !is DoorPhoto.Uploading, modifier = BIG)
            }
        }
        if (isCash) {
            item { SectionLabel(stringResource(R.string.drv_collect_cash, due.rupees())) }
            listOf("CASH" to (R.string.drv_settle_cash to R.string.drv_settle_cash_sub),
                   "UPI_AT_DOOR" to (R.string.drv_settle_upi to R.string.drv_settle_upi_sub),
                   "PARTIAL" to (R.string.drv_settle_part to R.string.drv_settle_part_sub),
                   "NOT_COLLECTED" to (R.string.drv_settle_none to R.string.drv_settle_none_sub)).forEach { (v, t) ->
                item(key = v) {
                    val on = settlement == v
                    BoondhCard(Modifier.border(if (on) 2.dp else 0.dp, if (on) NeerlyColors.Ink900 else Color.Transparent, RoundedCornerShape(14.dp))
                        .selectable(selected = on, role = Role.RadioButton) { settlement = v }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = on, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column { Text(stringResource(t.first), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                                Text(stringResource(t.second), fontSize = 13.sp, color = NeerlyColors.Ink600) }
                        }
                    }
                }
            }
            if (settlement == "PARTIAL") item(key = "partial-amount") {
                BoondhCard {
                    OutlinedTextField(value = partGot, onValueChange = { v -> partGot = v.filter { it.isDigit() || it == '.' }.take(8) },
                        singleLine = true, prefix = { Text("₹") }, label = { Text(stringResource(R.string.drv_part_they_gave)) },
                        supportingText = { Text(partAmount?.let { stringResource(R.string.drv_part_rest, (due - it).rupees()) }
                            ?: stringResource(R.string.drv_part_amount_hint)) },
                        isError = partGot.isNotEmpty() && partAmount == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private val SHORT_REASONS = listOf("NOT_READY" to R.string.drv_short_not_ready, "KEEPING_FOR_LATER" to R.string.drv_short_keeping,
    "DAMAGED" to R.string.drv_short_damaged, "LOST" to R.string.drv_short_lost, "REFUSED" to R.string.drv_reason_refused, "OTHER" to R.string.drv_reason_other)

@Composable
private fun <T> FlowRowLite(items: List<T>, content: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { content(it) } } }
    }
}

/** D26 — too many wrong codes. Keep the jars; the shop can confirm it from their app. */
@Composable
private fun CodeLockedScreen(job: DriverAssignment, s: DriverHomeUiState, vm: DriverHomeViewModel, onSos: () -> Unit) {
    val until = s.codeLockedUntil?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
    var now by remember { mutableStateOf(java.time.Instant.now()) }
    LaunchedEffect(until) { while (true) { delay(1000); now = java.time.Instant.now(); if (until != null && !now.isBefore(until)) { vm.refresh(); break } } }
    val left = until?.let { java.time.Duration.between(now, it).seconds.coerceAtLeast(0) } ?: 0
    BoondhPage(title = stringResource(R.string.drv_locked_title), subtitle = stringResource(R.string.drv_join2, job.customerFirstName, job.orderNumber),
        footer = {
            TextButton(onClick = vm::openNotHome, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.drv_not_here_wont), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
            SosHoldButton(onSos)
        }) {
        item {
            val unlockCd = stringResource(R.string.drv_unlock_cd, (left / 60).toInt(), (left % 60).toInt())
            Text("%d:%02d".format(left / 60, left % 60), fontSize = 56.sp, fontFamily = FontFamily.Serif, color = NeerlyColors.Ink900,
                modifier = Modifier.semantics { contentDescription = unlockCd })
        }
        item { Text(until?.let { stringResource(R.string.drv_opens_at, clock(it.toString())) } ?: stringResource(R.string.drv_too_many_codes), fontSize = 16.sp, color = NeerlyColors.Ink600) }
        item { Notice(stringResource(R.string.drv_keep_jars), Tone.Warn) }
        item { BoondhCard {
            SectionLabel(stringResource(R.string.drv_ask_shop))
            Text(stringResource(R.string.drv_call_shop, job.shopName ?: stringResource(R.string.drv_your_shop_lower)),
                fontSize = 15.sp, color = NeerlyColors.Ink900)
            CallButton(job.shopPhone, stringResource(R.string.drv_call_shop_btn), color = NeerlyColors.CustomerDark)
        } }
    }
}

/** D22 — nobody answering at the door: try these, then mark it not delivered. */
@Composable
private fun NotHomeScreen(job: DriverAssignment, n: NotHome, vm: DriverHomeViewModel, onSos: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val waited = ((now - n.arrivedAtMs) / 1000).coerceAtLeast(0)
    var reason by remember { mutableStateOf("NOT_HOME") }
    var note by remember { mutableStateOf("") }
    BoondhPage(title = stringResource(R.string.drv_nothome_title), onBack = vm::closeNotHome, backLabel = stringResource(R.string.drv_back_hand_over),
        subtitle = stringResource(R.string.drv_join2, job.customerFirstName, job.orderNumber),
        footer = {
            n.problem?.let { Notice(it, Tone.Warn) }
            PrimaryButton(if (n.sending) stringResource(R.string.drv_saving) else stringResource(R.string.drv_cant_deliver), { vm.failDrop(reason, note) },
                enabled = !n.sending && (reason != "OTHER" || note.isNotBlank()), modifier = BIG, color = NeerlyColors.Err)
            SosHoldButton(onSos)
        }) {
        item { BoondhCard {
            SectionLabel(stringResource(R.string.drv_try_these))
            InfoRow(stringResource(R.string.drv_waiting), trailing = "%d:%02d".format(waited / 60, waited % 60))
            InfoRow(stringResource(R.string.drv_calls_tried), job.customerPhone?.let { stringResource(R.string.drv_calls_via_shop) }, "${n.calls}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CallButton(job.customerPhone, stringResource(R.string.drv_call_customer), color = NeerlyColors.CustomerDark, onDialed = vm::recordCall)
                CallButton(job.shopPhone, stringResource(R.string.drv_call_shop_btn), color = NeerlyColors.CustomerDark)
            }
            SecondaryButton(stringResource(R.string.drv_tried_calling), vm::recordCall, modifier = BIG)
        } }
        item { SectionLabel(stringResource(R.string.drv_what_happened)) }
        item {
            FlowRowLite(listOf("NOT_HOME" to R.string.drv_reason_not_home, "WRONG_ADDRESS" to R.string.drv_reason_wrong_address, "REFUSED" to R.string.drv_reason_refused,
                "UNSAFE" to R.string.drv_reason_unsafe, "OTHER" to R.string.drv_reason_other)) { (v, label) ->
                FilterChip(selected = reason == v, onClick = { reason = v }, label = { Text(stringResource(label), fontSize = 15.sp) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        item {
            OutlinedTextField(value = note, onValueChange = { note = it.take(300) },
                label = { Text(if (reason == "OTHER") stringResource(R.string.drv_what_happened_needed) else stringResource(R.string.drv_note_shop_optional)) },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        }
        item { Text(stringResource(R.string.drv_refund_trip), fontSize = 14.sp, color = NeerlyColors.Ink600) }
    }
}

/** D27 — not delivered: take the jars back to the shop. */
@Composable
private fun FailedDropScreen(f: FailedDrop, vm: DriverHomeViewModel) {
    val context = LocalContext.current
    BoondhPage(title = stringResource(R.string.drv_failed_title), subtitle = stringResource(R.string.drv_failed_sub, f.customer, f.orderNumber),
        footer = {
            val shopLabel = f.shopName ?: stringResource(R.string.drv_the_shop)
            if (f.shopLat != null && f.shopLng != null) PrimaryButton(stringResource(R.string.drv_directions_to, f.shopName ?: stringResource(R.string.drv_the_shop_lower)),
                { openMaps(context, f.shopLat, f.shopLng, shopLabel) }, modifier = BIG, color = NeerlyColors.CustomerDark)
            SecondaryButton(stringResource(R.string.drv_ok), vm::dismissFailed, modifier = BIG)
        }) {
        item { BoondhCard {
            InfoRow(stringResource(R.string.drv_trip_pay), stringResource(R.string.drv_ride_out), f.result.tripPay.rupees())
            if (f.result.jarsToShop > 0) InfoRow(stringResource(R.string.drv_jars_to_return), trailing = "${f.result.jarsToShop}")
        } }
        item { Notice(stringResource(R.string.drv_told_shop), Tone.Info) }
    }
}

/** D08 — delivered; this drop, today, and where next (D32 / D33). */
@Composable
private fun DoneScreen(d: DropDone, s: DriverHomeUiState, vm: DriverHomeViewModel) {
    val next = s.activeAssignment
    val theShop = stringResource(R.string.drv_the_shop_lower)
    BoondhPage(title = stringResource(R.string.drv_done_title), subtitle = stringResource(R.string.drv_join2, d.customer, d.orderNumber),
        footer = { PrimaryButton(if (next != null) stringResource(R.string.drv_next_drop, next.customerFirstName) else stringResource(R.string.drv_back_tasks), vm::dismissDone, modifier = BIG) }) {
        item {
            BoondhCard {
                val drop = d.earning?.total ?: d.offeredFee
                InfoRow(stringResource(R.string.drv_this_drop), trailing = drop?.rupees() ?: stringResource(R.string.drv_shows_on_money))
                d.earning?.bonus?.takeIf { it.signum() > 0 }?.let { InfoRow(d.earning.bonusReason ?: stringResource(R.string.drv_bonus), trailing = stringResource(R.string.drv_plus_amount, it.rupees())) }
                if (d.emptiesInAuto > 0) InfoRow(stringResource(R.string.drv_empties_in_auto), trailing = "${d.emptiesInAuto}")
                d.cashTaken?.let { InfoRow(stringResource(R.string.drv_cash_you_took), trailing = it.rupees()) }
                d.earning?.let { InfoRow(stringResource(R.string.drv_today_so_far), pluralStringResource(R.plurals.drv_drops_n, it.todayDrops, it.todayDrops), it.todayEarned.rupees()) }
            }
        }
        if (next != null) item {
            BoondhCard {
                SectionLabel(stringResource(R.string.drv_next_on_round))
                InfoRow(next.customerFirstName, next.deliveryAddress)
                Text(next.product, fontSize = 14.sp, color = NeerlyColors.Ink600)
            }
        } else {
            item { Notice(stringResource(R.string.drv_no_more_drops), Tone.Info) }
            // D33: the shop's pin comes from the offer card of a task taken this session.
            val shop: Triple<String, Double, Double>? =
                s.assignments.firstOrNull { it.pickupLat != null && it.pickupLng != null }?.let { Triple(it.shopName ?: theShop, it.pickupLat!!, it.pickupLng!!) }
                    ?: s.taskCards.values.firstOrNull { it.pickupLat != null && it.pickupLng != null }?.let { Triple(it.vendorName, it.pickupLat!!, it.pickupLng!!) }
            shop?.let { (name, lat, lng) -> item {
                val context = LocalContext.current
                SecondaryButton(stringResource(R.string.drv_directions_to, name), { openMaps(context, lat, lng, name) }, modifier = BIG)
            } }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Round (D09), Money (D11 entry), Me.

@Composable
private fun RoundTab(s: DriverHomeUiState, onOpen: () -> Unit) {
    val open = s.assignments.filter { it.status != "DELIVERED" }
    val empties = open.sumOf { it.emptiesExpected }
    // DRoute: "Next · 0.6 km · about 4 min" — chained from where the driver is (or the shop).
    val speed = com.neerly.mobile.data.config.LocalAppConfig.current.driverAvgSpeedKmh
    val start = s.here ?: open.firstOrNull()?.let { j -> j.pickupLat?.let { lat -> j.pickupLng?.let { lat to it } } }
    val etas = roundEtas(start, open.map { j -> (j.deliveryLat to j.deliveryLng).takeIf { it.first != 0.0 || it.second != 0.0 } }, speed)
    BoondhPage(title = stringResource(R.string.drv_round_title),
        subtitle = if (open.isEmpty()) stringResource(R.string.drv_round_nothing) else stringResource(R.string.drv_round_sub, open.size, s.today?.drops ?: 0)) {
        if (open.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("$empties", stringResource(R.string.drv_empties_to_collect), Modifier.weight(1f))
                Stat(s.cashToCollect.rupeesWhole(), stringResource(R.string.drv_cash_to_collect), Modifier.weight(1f))
            }
        }
        if (open.isEmpty()) item { LoadState(false, null, {}, stringResource(R.string.drv_no_drops_yet), stringResource(R.string.drv_accepted_line_up)) }
        if (etas.any { it != null }) item { Text(stringResource(R.string.drv_round_eta_note), fontSize = 12.sp, color = NeerlyColors.Ink600) }
        open.forEachIndexed { i, j -> item(key = j.orderId) {
            BoondhCard(onClick = onOpen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, modifier = Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text(j.customerFirstName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(j.deliveryAddress, fontSize = 14.sp, color = NeerlyColors.Ink600)
                        Text(stringResource(R.string.drv_join2, j.product, if (j.paymentMethod == "COD") stringResource(R.string.drv_cash_amount_lower, (j.codAmount ?: BigDecimal.ZERO).rupees()) else stringResource(R.string.drv_paid_online_lower)),
                            fontSize = 14.sp, color = NeerlyColors.Ink900)
                        etas.getOrNull(i)?.let { e ->
                            val km = "%.1f".format(java.util.Locale.ENGLISH, e.km)
                            Text(if (i == 0) stringResource(R.string.drv_round_next, km, e.minutes) else stringResource(R.string.drv_round_stop, km, e.minutes),
                                fontSize = 14.sp, fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (i == 0) NeerlyColors.Ink900 else NeerlyColors.Ink600)
                        }
                    }
                    Pill(when (j.status) { "ARRIVING" -> stringResource(R.string.drv_status_at_door); "DISPATCHED" -> if (j.orderId in s.loadedOrders) stringResource(R.string.drv_status_on_way) else stringResource(R.string.drv_status_load); else -> stringResource(R.string.drv_status_packed) },
                        if (i == 0) Tone.Info else Tone.Neutral)
                }
            }
        } }
    }
}

@Composable
private fun MoneyTab(s: DriverHomeUiState, onCodReconcile: () -> Unit) {
    val t = s.today
    BoondhPage(title = stringResource(R.string.drv_money_title), subtitle = stringResource(R.string.drv_money_sub)) {
        item {
            Column(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(16.dp)).padding(20.dp)) {
                Text(stringResource(R.string.drv_earned_today), fontSize = 14.sp, color = Boondh.MutedOnNavy)
                Text((t?.earned ?: BigDecimal.ZERO).rupees(), fontSize = 36.sp, fontFamily = FontFamily.Serif, color = NeerlyColors.Paper)
                Text(pluralStringResource(R.plurals.drv_drops_weekly, t?.drops ?: 0, t?.drops ?: 0), fontSize = 14.sp, color = Boondh.MutedOnNavy)
            }
        }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.drv_cash_with_you), stringResource(R.string.drv_from_cash_orders), (t?.cashInHand ?: BigDecimal.ZERO).rupees())
                Text(stringResource(R.string.drv_give_shop),
                    fontSize = 14.sp, color = NeerlyColors.Ink600)
                // Nothing to hand over → nothing to press (it opened an empty form).
                SecondaryButton(stringResource(R.string.drv_hand_cash_shop), onCodReconcile, modifier = BIG,
                    enabled = (t?.cashInHand ?: BigDecimal.ZERO).signum() > 0)
            }
        }
        item { SectionLabel(stringResource(R.string.drv_by_week)) }
        item { DriverWeekContent() }
    }
}

// ---------------------------------------------------------------------------------------------

internal fun clock(iso: String): String = runCatching {
    java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata"))
        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
}.getOrDefault("")

/** Two-wheeler turn-by-turn in Google Maps; any maps app as a fallback. */
internal fun openMaps(context: android.content.Context, lat: Double, lng: Double, label: String) {
    val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=l"))
    try { context.startActivity(nav) } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})"))) }
    }
}

/** Seconds before the break ends at which the phone buzzes (DOnBreak: "We buzz you 2 minutes before"). */
internal const val BREAK_BUZZ_SECONDS = 120L

private fun buzz(context: android.content.Context) {
    runCatching {
        val v = if (android.os.Build.VERSION.SDK_INT >= 31)
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator)
        v.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
    }
}

/**
 * "204, Lakshmi Residency, Gachibowli 500032" → title "204, Lakshmi Residency", sub "Gachibowli 500032".
 * A bare flat number alone is no use as a heading, so a short first part takes the next one with it.
 */
internal fun splitAddress(address: String): Pair<String, String?> {
    val parts = address.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size <= 1) return address.trim() to null
    val take = if (parts[0].length < 8 && parts.size > 2) 2 else 1
    return parts.take(take).joinToString(", ") to parts.drop(take).joinToString(", ").ifBlank { null }
}
