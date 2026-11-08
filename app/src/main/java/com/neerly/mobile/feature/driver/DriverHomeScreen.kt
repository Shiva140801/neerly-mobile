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
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.onLocationPermission(it) }
    LaunchedEffect(state.isOnDuty) {
        if (!state.isOnDuty) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) vm.onLocationPermission(true) else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
        s.onBreak -> OnBreakScreen(s, vm)
        job != null && s.step == DropStep.LOAD -> PickupScreen(job, s, vm)
        job != null && s.step == DropStep.RIDE -> RideScreen(job, s, vm, onSos)
        job != null && s.step == DropStep.HAND_OVER -> HandOverScreen(job, s, vm, onSos)
        else -> OnDutyScreen(s, vm)
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
        s.today?.takeIf { it.drops > 0 }?.let { t -> item {
            BoondhCard {
                SectionLabel(stringResource(R.string.drv_today_so_far))
                InfoRow(pluralStringResource(R.plurals.drv_drops_n, t.drops, t.drops), trailing = t.earned.rupees())
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

/** D02 — on duty, waiting for a task. */
@Composable
private fun OnDutyScreen(s: DriverHomeUiState, vm: DriverHomeViewModel) {
    BoondhPage(title = stringResource(R.string.drv_on_duty), subtitle = s.shift?.startedAt?.let { stringResource(R.string.drv_on_duty_since, clock(it)) }) {
        s.error?.let { item { Notice(it, Tone.Bad) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${s.today?.drops ?: 0}", stringResource(R.string.drv_stat_drops_today), Modifier.weight(1f))
                Stat((s.today?.earned ?: BigDecimal.ZERO).rupeesWhole(), stringResource(R.string.drv_stat_you_earned), Modifier.weight(1f))
                Stat((s.today?.cashInHand ?: BigDecimal.ZERO).rupeesWhole(), stringResource(R.string.drv_stat_cash_with_you), Modifier.weight(1f))
            }
        }
        item {
            BoondhCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(12.dp).background(NeerlyColors.Ok, RoundedCornerShape(6.dp)))
                    Text(stringResource(R.string.drv_waiting_next), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                }
                Text(stringResource(R.string.drv_keep_app_open), fontSize = 14.sp, color = NeerlyColors.Ink600)
            }
        }
        item { SecondaryButton(if (s.working) "…" else stringResource(R.string.drv_take_break), vm::startBreak, enabled = !s.working, modifier = BIG) }
    }
}

/** D28 — on break: no new tasks until the driver comes back. */
@Composable
private fun OnBreakScreen(s: DriverHomeUiState, vm: DriverHomeViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val since = s.shiftState?.onBreakSince?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
    val mins = since?.let { ((now - it) / 60000).coerceAtLeast(0) }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A2238)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.drv_on_break_caps), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Boondh.Amber, letterSpacing = 1.sp)
        Text(mins?.let { pluralStringResource(R.plurals.drv_on_break_minutes, it.toInt(), it.toInt()) } ?: stringResource(R.string.drv_on_break),
            fontFamily = FontFamily.Serif, fontSize = 30.sp, color = NeerlyColors.Paper)
        Text(stringResource(R.string.drv_no_tasks_break), fontSize = 16.sp, color = Boondh.MutedOnNavy)
        BoondhCard {
            InfoRow(stringResource(R.string.drv_drops_done_today), trailing = "${s.today?.drops ?: 0}")
            InfoRow(stringResource(R.string.drv_cash_with_you), trailing = (s.today?.cashInHand ?: BigDecimal.ZERO).rupees())
        }
        s.error?.let { Notice(it, Tone.Bad) }
        Spacer(Modifier.weight(1f))
        PrimaryButton(if (s.working) "…" else stringResource(R.string.drv_im_back), vm::endBreak, enabled = !s.working, modifier = BIG,
            color = Boondh.Amber, contentColor = NeerlyColors.Ink900)
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
private fun PickupScreen(job: DriverAssignment, s: DriverHomeUiState, vm: DriverHomeViewModel) {
    val card = s.taskCards[job.orderId]
    val sealLine = job.product to stringResource(R.string.drv_check_seal)
    val emptiesLine = pluralStringResource(R.plurals.drv_space_empties, job.emptiesExpected, job.emptiesExpected) to stringResource(R.string.drv_bring_back_home)
    val cashLine = stringResource(R.string.drv_collect_cash, (job.codAmount ?: BigDecimal.ZERO).rupees()) to stringResource(R.string.drv_customer_pays_door)
    val lines = buildList {
        add(sealLine)
        if (job.emptiesExpected > 0) add(emptiesLine)
        if (job.paymentMethod == "COD") add(cashLine)
    }
    var ticked by remember(job.orderId) { mutableStateOf(setOf<Int>()) }
    val ready = job.status == "DISPATCHED"
    BoondhPage(title = stringResource(R.string.drv_load_title),
        subtitle = stringResource(R.string.drv_step1, job.orderNumber),
        footer = {
            if (!ready) Notice(stringResource(R.string.drv_still_packing), Tone.Warn)
            PrimaryButton(if (s.working) stringResource(R.string.drv_starting) else stringResource(R.string.drv_loaded_start), { vm.loaded(job.orderId) },
                enabled = ready && ticked.size == lines.size && !s.working, modifier = BIG)
        }) {
        item { Text(stringResource(R.string.drv_tick_each), fontSize = 15.sp, color = NeerlyColors.Ink600) }
        s.error?.let { item { Notice(it, Tone.Bad) } }
        lines.forEachIndexed { i, (title, sub) -> item(key = i) {
            val on = i in ticked
            BoondhCard(Modifier.toggleable(value = on, role = Role.Checkbox) { ticked = if (on) ticked - i else ticked + i }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = on, onCheckedChange = null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(sub, fontSize = 14.sp, color = NeerlyColors.Ink600) }
                }
            }
        } }
        item { BoondhCard { InfoRow(card?.vendorName ?: stringResource(R.string.drv_the_shop), card?.pickupAddress) } }
        job.notes?.let { item { Notice(stringResource(R.string.drv_customer_note, it), Tone.Info) } }
    }
}

/** D05 / D32 — ride to the door. No Maps key, so the route opens in Google Maps. */
@Composable
private fun RideScreen(job: DriverAssignment, s: DriverHomeUiState, vm: DriverHomeViewModel, onSos: () -> Unit) {
    val context = LocalContext.current
    val km = s.here?.let { kmBetween(it, job.deliveryLat to job.deliveryLng) }
    val index = s.assignments.indexOf(job) + 1
    BoondhPage(title = job.customerFirstName,
        subtitle = stringResource(R.string.drv_join2,
            if (s.assignments.size > 1) stringResource(R.string.drv_step2_drop_n, index, s.assignments.size) else stringResource(R.string.drv_step2_drop),
            if (job.paymentMethod == "COD") stringResource(R.string.drv_cash_caps, (job.codAmount ?: BigDecimal.ZERO).rupees()) else stringResource(R.string.drv_paid_online_caps)),
        footer = {
            PrimaryButton(stringResource(R.string.drv_open_maps), { openMaps(context, job.deliveryLat, job.deliveryLng, job.customerFirstName) },
                modifier = BIG, color = NeerlyColors.CustomerDark)
            PrimaryButton(if (s.working) stringResource(R.string.drv_telling) else stringResource(R.string.drv_at_door), { vm.markArrived(job.orderId) },
                enabled = !s.working, modifier = BIG, color = Boondh.Amber, contentColor = NeerlyColors.Ink900)
        }) {
        s.error?.let { item { Notice(it, Tone.Bad) } }
        item {
            BoondhCard {
                Text(job.deliveryAddress, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(km?.let { stringResource(R.string.drv_km_from_you, "%.1f".format(it)) } ?: stringResource(R.string.drv_location_to_see),
                    fontSize = 15.sp, color = NeerlyColors.Ink600)
                Text(job.emptiesExpected.takeIf { it > 0 }?.let { stringResource(R.string.drv_join2, job.product, pluralStringResource(R.plurals.drv_empties_back, it, it)) } ?: job.product,
                    fontSize = 15.sp, color = NeerlyColors.Ink900)
            }
        }
        job.notes?.let { item { Notice(stringResource(R.string.drv_quote, it), Tone.Info) } }
        item { Text(stringResource(R.string.drv_customer_call, job.customerPhoneMask),
            fontSize = 13.sp, color = NeerlyColors.Ink600) }
        item { SosHoldButton(onSos) }
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
    var pending by remember { mutableStateOf<java.io.File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        pending?.let { vm.photoTaken(it, ok) }
    }
    val due = job.codAmount ?: BigDecimal.ZERO
    val photoNeeded = com.neerly.mobile.data.config.LocalAppConfig.current.deliveryPhotoRequired
    BoondhPage(title = stringResource(R.string.drv_ask_code_title, job.customerFirstName),
        subtitle = stringResource(R.string.drv_step3),
        footer = {
            s.deliverError?.let { Notice(it, Tone.Bad) }
            PrimaryButton(when { s.delivering -> stringResource(R.string.drv_saving); isCash && settlement == "CASH" -> stringResource(R.string.drv_got_cash_hand, due.rupees()); else -> stringResource(R.string.drv_hand_over_done) },
                { vm.deliver(code, empties, if (isCash) settlement else null, if (isCash && settlement != "NOT_COLLECTED") due else null, shortReason, photoNeeded) },
                enabled = !s.delivering && code.length == DriverHomeViewModel.CODE_LENGTH && (s.photo is DoorPhoto.Ready || !photoNeeded) &&
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
                    runCatching { camera.launch(photoUri(context, f)) }
                }, enabled = s.photo !is DoorPhoto.Uploading, modifier = BIG)
            }
        }
        if (isCash) {
            item { SectionLabel(stringResource(R.string.drv_collect_cash, due.rupees())) }
            listOf("CASH" to (R.string.drv_settle_cash to R.string.drv_settle_cash_sub),
                   "UPI_AT_DOOR" to (R.string.drv_settle_upi to R.string.drv_settle_upi_sub),
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
            InfoRow(stringResource(R.string.drv_calls_tried), stringResource(R.string.drv_calls_via_shop), "${n.calls}")
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
    BoondhPage(title = stringResource(R.string.drv_round_title),
        subtitle = if (open.isEmpty()) stringResource(R.string.drv_round_nothing) else stringResource(R.string.drv_round_sub, open.size, s.today?.drops ?: 0)) {
        if (open.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("$empties", stringResource(R.string.drv_empties_to_collect), Modifier.weight(1f))
                Stat(s.cashToCollect.rupeesWhole(), stringResource(R.string.drv_cash_to_collect), Modifier.weight(1f))
            }
        }
        if (open.isEmpty()) item { LoadState(false, null, {}, stringResource(R.string.drv_no_drops_yet), stringResource(R.string.drv_accepted_line_up)) }
        open.forEachIndexed { i, j -> item(key = j.orderId) {
            BoondhCard(onClick = onOpen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, modifier = Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text(j.customerFirstName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(j.deliveryAddress, fontSize = 14.sp, color = NeerlyColors.Ink600)
                        Text(stringResource(R.string.drv_join2, j.product, if (j.paymentMethod == "COD") stringResource(R.string.drv_cash_amount_lower, (j.codAmount ?: BigDecimal.ZERO).rupees()) else stringResource(R.string.drv_paid_online_lower)),
                            fontSize = 14.sp, color = NeerlyColors.Ink900)
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
                SecondaryButton(stringResource(R.string.drv_hand_cash_shop), onCodReconcile, modifier = BIG)
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
