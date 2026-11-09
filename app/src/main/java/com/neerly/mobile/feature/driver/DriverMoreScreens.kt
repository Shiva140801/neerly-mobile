package com.neerly.mobile.feature.driver

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import kotlinx.coroutines.delay
import java.math.BigDecimal

private val BIG = Modifier.heightIn(min = 60.dp)
private val SOS_RED = Color(0xFF9E2F22)

// ---- D29 hold-to-send SOS ----------------------------------------------------------------

/**
 * Hold for two seconds to send — the hold is the confirmation, so a pocket tap can't fire it.
 * TalkBack users get a regular click action that opens the SOS screen, where they hold again.
 */
@Composable
fun SosHoldButton(onSend: () -> Unit, modifier: Modifier = Modifier, label: String = stringResource(R.string.drv_sos_hold_label)) {
    val sosCd = stringResource(R.string.drv_sos_cd)
    val sendLabel = stringResource(R.string.drv_sos_send)
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(holding) {
        progress = 0f
        if (!holding) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (holding) {
            progress = ((System.currentTimeMillis() - start) / HOLD_MS.toFloat()).coerceAtMost(1f)
            if (progress >= 1f) { holding = false; onSend(); break }
            delay(30)
        }
    }
    Box(modifier.fillMaxWidth().heightIn(min = 64.dp).background(SOS_RED, RoundedCornerShape(14.dp))
        .pointerInput(Unit) {
            awaitEachGesture { awaitFirstDown(); holding = true; waitForUpOrCancellation(); holding = false }
        }
        .semantics { role = Role.Button; contentDescription = sosCd
            onClick(label = sendLabel) { onSend(); true } },
        contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().padding(4.dp)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)))
        }
        Text(if (holding) stringResource(R.string.drv_sos_keep_holding) else stringResource(R.string.drv_sos_suffix, label), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** NPDCatalog D10: "SOS held 3 s" (decision A4). */
private const val HOLD_MS = 3_000L

/** D29 — SOS sent: who was told, live location sharing, "I'm safe". */
@Composable
fun DriverSosScreen(onBack: () -> Unit, sendOnOpen: Boolean = false, vm: DriverSosViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(sendOnOpen) { if (sendOnOpen) vm.send() }
    Column(Modifier.fillMaxSize().background(SOS_RED).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.drv_back), color = Color.White, fontSize = 16.sp) }
        when {
            s.resolved -> {
                Text(stringResource(R.string.drv_sos_glad), fontFamily = FontFamily.Serif, fontSize = 32.sp, color = Color.White)
                Text(stringResource(R.string.drv_sos_told_ok), fontSize = 16.sp, color = Color.White)
                PrimaryButton(stringResource(R.string.drv_back_to_work), onBack, modifier = BIG, color = Color.White, contentColor = SOS_RED)
            }
            s.sos != null -> {
                Text(stringResource(R.string.drv_sos_help_coming), fontFamily = FontFamily.Serif, fontSize = 32.sp, color = Color.White)
                Text(pluralStringResource(R.plurals.drv_sos_alerted, s.sos!!.notified, s.sos!!.notified), fontSize = 16.sp, lineHeight = 23.sp, color = Color.White)
                s.lastShared?.let { Text(stringResource(R.string.drv_sos_last_shared, ((System.currentTimeMillis() - it) / 1000).coerceAtLeast(0).toInt()),
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f)) }
                PrimaryButton(stringResource(R.string.drv_call_112_full), { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
                    modifier = BIG, color = Color.White, contentColor = SOS_RED)
                SecondaryButton(stringResource(R.string.drv_im_safe), vm::safe, modifier = BIG, color = Color.White)
            }
            else -> {
                Text(stringResource(R.string.drv_sos_danger), fontFamily = FontFamily.Serif, fontSize = 32.sp, color = Color.White)
                Text(stringResource(R.string.drv_sos_hold_intro),
                    fontSize = 16.sp, lineHeight = 23.sp, color = Color.White)
                if (s.sending) Text(stringResource(R.string.drv_sos_sending), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                else SosHoldButton(vm::send, Modifier.border(2.dp, Color.White, RoundedCornerShape(14.dp)), label = stringResource(R.string.drv_sos_hold_short))
                SecondaryButton(stringResource(R.string.drv_call_112), { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) }, modifier = BIG, color = Color.White)
            }
        }
        s.error?.let { Notice(it, Tone.Bad) }
    }
}

// ---- D17 Me · D31 verification ---------------------------------------------------------

@Composable
fun DriverMeContent(
    onDuty: Boolean,
    onEndDuty: () -> Unit,
    onVehicle: () -> Unit,
    onDocuments: () -> Unit,
    onPickups: () -> Unit,
    onSos: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    onLogout: () -> Unit,
    vm: DriverMeViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    OnReturn(vm::load)
    val me = s.me
    var upi by remember(me?.payoutUpi) { mutableStateOf(me?.payoutUpi.orEmpty()) }
    BoondhPage(title = me?.name?.ifBlank { null } ?: stringResource(R.string.drv_me), subtitle = listOfNotNull(me?.phoneMasked, if (onDuty) stringResource(R.string.drv_on_duty_short) else stringResource(R.string.drv_off_duty_short)).joinToString(" · ")) {
        if (s.loading) { item { LoadState(true, null, {}) }; return@BoondhPage }
        s.error?.let { item { Notice(it, Tone.Bad) } }
        me?.let { m -> item { VerificationCard(m.verification, onDocuments) } }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.drv_my_shops), me?.shops?.joinToString(" · ") { it.shopName }?.ifBlank { null } ?: stringResource(R.string.drv_no_team_yet))
                HorizontalDivider(color = NeerlyColors.Ink200)
                val typeLabel = me?.vehicle?.type?.let { t -> DriverVehicleViewModel.TYPES.toMap()[t] }?.let { stringResource(it) }
                InfoRow(stringResource(R.string.drv_vehicle), me?.vehicle?.let { v -> listOfNotNull(typeLabel, v.plateNumber).joinToString(" · ").ifBlank { null } }
                    ?: stringResource(R.string.drv_add_vehicle), onClick = onVehicle)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.drv_papers), when (me?.verification) { "VERIFIED" -> stringResource(R.string.drv_papers_all_verified); "IN_REVIEW" -> stringResource(R.string.drv_papers_being_checked); else -> stringResource(R.string.drv_papers_missing) }, onClick = onDocuments)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.drv_jar_pickups), stringResource(R.string.drv_empties_from_homes), onClick = onPickups)
            }
        }
        item {
            BoondhCard {
                SectionLabel(stringResource(R.string.drv_pay_goes_to))
                OutlinedTextField(value = upi, onValueChange = { upi = it.take(80) }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.drv_upi_ph)) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                if (s.upiSaved) Text(stringResource(R.string.drv_saved_tick), color = NeerlyColors.Ok, fontWeight = FontWeight.SemiBold)
                SecondaryButton(if (s.savingUpi) stringResource(R.string.drv_saving) else stringResource(R.string.drv_save_upi), { vm.saveUpi(upi) },
                    enabled = !s.savingUpi && upi.isNotBlank() && upi != me?.payoutUpi, modifier = BIG)
            }
        }
        if (onDuty) item { SecondaryButton(stringResource(R.string.drv_end_duty), onEndDuty, modifier = BIG, color = NeerlyColors.Err) }
        // Tasks ring full screen and SOS comes through Do Not Disturb only with these allowed (plan §e).
        item { com.neerly.mobile.push.RingReadinessCard(com.neerly.mobile.push.PushRole.DRIVER) }
        item { SosHoldButton(onSos) }
        item { com.neerly.mobile.push.PushLabEntry() }
        onSwitchRole?.let { item { SecondaryButton(stringResource(R.string.drv_switch_role), it, modifier = BIG) } }
        item { TextButton(onClick = vm::checkLogout, enabled = !s.checkingLogout, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (s.checkingLogout) stringResource(R.string.drv_checking) else stringResource(R.string.drv_log_out), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) } }
    }
    s.logoutCheck?.let { c ->
        if (c.canLogout) LaunchedEffect(c) { vm.dismissLogout(); onLogout() }
        else AlertDialog(onDismissRequest = vm::dismissLogout,
            title = { Text(stringResource(R.string.drv_not_yet)) },
            text = { Text(listOfNotNull(
                if (c.pendingDrops > 0) pluralStringResource(R.plurals.drv_logout_drops, c.pendingDrops, c.pendingDrops) else null,
                if (c.cashHeld.signum() > 0) stringResource(R.string.drv_logout_cash, c.cashHeld.rupees()) else null,
                if (c.onShift) stringResource(R.string.drv_logout_end_duty) else null
            ).joinToString(" ")) },
            confirmButton = { TextButton(onClick = vm::dismissLogout) { Text(stringResource(R.string.drv_ok)) } })
    }
}

/** D31 — the papers decide whether the shop sees "verified". */
@Composable
private fun VerificationCard(status: String, onDocuments: () -> Unit) {
    val (tone, title, body) = when (status) {
        "VERIFIED" -> Triple(Tone.Ok, stringResource(R.string.drv_verified), stringResource(R.string.drv_verified_body))
        "IN_REVIEW" -> Triple(Tone.Info, stringResource(R.string.drv_papers_checking_title), stringResource(R.string.drv_papers_checking_body))
        else -> Triple(Tone.Warn, stringResource(R.string.drv_add_papers), stringResource(R.string.drv_add_papers_body))
    }
    BoondhCard(onClick = if (status == "VERIFIED") null else onDocuments) {
        Pill(title, tone)
        Text(body, fontSize = 14.sp, color = NeerlyColors.Ink600)
    }
}

// ---- D15 / D30 vehicle ------------------------------------------------------------------

@Composable
fun DriverVehicleScreen(onBack: () -> Unit, vm: DriverVehicleViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var type by remember(s.vehicle.type) { mutableStateOf(s.vehicle.type ?: "BIKE") }
    var plate by remember(s.vehicle.plateNumber) { mutableStateOf(s.vehicle.plateNumber.orEmpty()) }
    var model by remember(s.vehicle.model) { mutableStateOf(s.vehicle.model.orEmpty()) }
    BoondhPage(title = stringResource(R.string.drv_your_vehicle), onBack = onBack, backLabel = stringResource(R.string.drv_me),
        subtitle = stringResource(R.string.drv_vehicle_sub),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            if (s.saved) Notice(stringResource(R.string.drv_saved), Tone.Ok)
            PrimaryButton(if (s.saving) stringResource(R.string.drv_saving) else stringResource(R.string.drv_save_vehicle), { vm.save(type, plate, model) }, enabled = !s.saving && !s.loading, modifier = BIG)
        }) {
        if (s.loading) { item { LoadState(true, null, {}) }; return@BoondhPage }
        item { SectionLabel(stringResource(R.string.drv_type)) }
        item {
            FlowRowCompat(DriverVehicleViewModel.TYPES) { (v, label) ->
                FilterChip(selected = type == v, onClick = { type = v }, label = { Text(stringResource(label), fontSize = 16.sp) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        if (type != "CYCLE") item {
            OutlinedTextField(value = plate, onValueChange = { plate = it.uppercase().take(13) }, singleLine = true,
                label = { Text(stringResource(R.string.drv_number_plate)) }, placeholder = { Text(stringResource(R.string.drv_plate_ph)) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = model, onValueChange = { model = it.take(60) }, singleLine = true,
                label = { Text(stringResource(R.string.drv_model_optional)) }, placeholder = { Text(stringResource(R.string.drv_model_ph)) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun <T> FlowRowCompat(items: List<T>, content: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { content(it) } } }
    }
}

// ---- D16 papers -------------------------------------------------------------------------

@Composable
fun DriverDocumentsScreen(onBack: () -> Unit, vm: DriverDocumentsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    var pendingType by remember { mutableStateOf<String?>(null) }
    var pendingFile by remember { mutableStateOf<java.io.File?>(null) }
    var numbers by remember { mutableStateOf(mapOf<String, String>()) }
    val camera = rememberCameraCapture { ok ->
        val t = pendingType; val f = pendingFile
        if (ok && t != null && f != null) vm.upload(t, f, numbers[t], null)
    }
    BoondhPage(title = stringResource(R.string.drv_your_papers), onBack = onBack, backLabel = stringResource(R.string.drv_me),
        subtitle = stringResource(R.string.drv_papers_sub)) {
        if (s.loading) { item { LoadState(true, null, {}) }; return@BoondhPage }
        s.error?.let { item { Notice(it, Tone.Bad) } }
        if (s.storageDown) item { Notice(stringResource(R.string.drv_storage_down_short), Tone.Warn) }
        s.docs?.documents.orEmpty().forEach { d -> item(key = d.type) {
            BoondhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(DriverDocumentsViewModel.LABELS[d.type]?.let { stringResource(it) } ?: d.type, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Pill(when (d.status) { "APPROVED" -> stringResource(R.string.drv_doc_verified); "SUBMITTED" -> stringResource(R.string.drv_doc_checking); "REJECTED" -> stringResource(R.string.drv_doc_fix); "EXPIRED" -> stringResource(R.string.drv_doc_expired); else -> if (d.required) stringResource(R.string.drv_doc_needed) else stringResource(R.string.drv_doc_optional) },
                        when (d.status) { "APPROVED" -> Tone.Ok; "SUBMITTED" -> Tone.Info; "REJECTED", "EXPIRED" -> Tone.Bad; else -> if (d.required) Tone.Warn else Tone.Neutral })
                }
                d.rejectionReason?.takeIf { d.status == "REJECTED" }?.let { Text(stringResource(R.string.drv_doc_why, it), fontSize = 14.sp, color = NeerlyColors.Err) }
                d.docNumber?.let { Text(it, fontSize = 14.sp, color = NeerlyColors.Ink600) }
                if (d.status != "APPROVED") {
                    if (d.type == "DRIVING_LICENSE" || d.type == "VEHICLE_RC" || d.type == "AADHAAR") OutlinedTextField(
                        value = numbers[d.type].orEmpty(), onValueChange = { numbers = numbers + (d.type to it.take(30)) }, singleLine = true,
                        label = { Text(if (d.type == "AADHAAR") stringResource(R.string.drv_aadhaar_number) else stringResource(R.string.drv_number_on_paper)) },
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    SecondaryButton(when { s.uploading == d.type -> stringResource(R.string.drv_sending); d.status == "MISSING" -> stringResource(R.string.drv_take_a_photo); else -> stringResource(R.string.drv_send_new_photo) }, {
                        val f = vm.newPhotoFile(); pendingType = d.type; pendingFile = f
                        camera(f)
                    }, enabled = s.uploading == null, modifier = BIG)
                }
            }
        } }
    }
}

// ---- D12 money: the week ----------------------------------------------------------------

@Composable
fun DriverWeekContent(vm: DriverWeekViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val w = s.week
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::previous, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.drv_earlier), fontSize = 15.sp) }
            Spacer(Modifier.weight(1f))
            Text(w?.let { stringResource(R.string.drv_week_range, shortDay(it.weekStart), shortDay(it.weekEnd)) } ?: "", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::next, enabled = !vm.isThisWeek, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.drv_later), fontSize = 15.sp) }
        }
        when {
            s.loading && w == null -> LoadState(true, null, {})
            s.error != null && w == null -> LoadState(false, s.error, { vm.load() })
            w != null -> {
                Column(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Text(if (vm.isThisWeek) stringResource(R.string.drv_this_week) else stringResource(R.string.drv_that_week), fontSize = 14.sp, color = Boondh.MutedOnNavy)
                    Text(w.totalEarned.rupees(), fontSize = 36.sp, fontFamily = FontFamily.Serif, color = NeerlyColors.Paper)
                    val dropsText = pluralStringResource(R.plurals.drv_drops_n, w.totalDrops, w.totalDrops)
                    Text(w.totalBonus.takeIf { it.signum() > 0 }?.let { stringResource(R.string.drv_with_bonus, dropsText, it.rupees()) } ?: dropsText,
                        fontSize = 14.sp, color = Boondh.MutedOnNavy)
                    // Honest: there's no driver payout run yet, so no "paid on" date is promised.
                    Text(if (w.payoutStatus == "NOT_PAID_YET") (w.payoutUpi?.let { stringResource(R.string.drv_not_paid_to, it) } ?: stringResource(R.string.drv_not_paid_add_upi))
                         else stringResource(R.string.drv_paid), fontSize = 14.sp, color = Boondh.MutedOnNavy)
                }
                WeekBars(w.days)
            }
        }
    }
}

/** One bar per day, scaled to the best day; amounts written out (the chart is decoration). */
@Composable
private fun WeekBars(days: List<WeekDay>) {
    val max = days.maxOfOrNull { it.earned }?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
    BoondhCard {
        days.forEach { d ->
            val frac = (d.earned.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(shortDay(d.date).substringBefore(' '), Modifier.width(40.dp), fontSize = 14.sp, color = NeerlyColors.Ink700)
                Box(Modifier.weight(1f).height(14.dp).background(NeerlyColors.Ink100, RoundedCornerShape(7.dp))) {
                    if (frac > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Boondh.Amber, RoundedCornerShape(7.dp)))
                }
                Text(d.earned.rupeesWhole(), Modifier.width(64.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text("${d.drops}", Modifier.width(28.dp), fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
        }
        Text(stringResource(R.string.drv_right_column), fontSize = 12.sp, color = NeerlyColors.Ink600)
    }
}

// ---- D10 jar pickups -------------------------------------------------------------------

@Composable
fun DriverPickupsScreen(onBack: () -> Unit, vm: DriverPickupsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Pair<String, java.io.File>?>(null) }
    val camera = rememberCameraCapture { ok ->
        pending?.let { (id, f) -> vm.photoTaken(id, f, ok) }
    }
    OnReturn(vm::load)
    val open = s.pickups.filter { it.status in setOf("REQUESTED", "SCHEDULED", "EN_ROUTE") }
    BoondhPage(title = stringResource(R.string.drv_jar_pickups), onBack = onBack, backLabel = stringResource(R.string.drv_me),
        subtitle = stringResource(R.string.drv_pickups_sub)) {
        if (s.loading) { item { LoadState(true, null, {}) }; return@BoondhPage }
        s.error?.let { item { Notice(it, Tone.Warn) } }
        if (open.isEmpty()) item { LoadState(false, null, vm::load, stringResource(R.string.drv_no_pickups), stringResource(R.string.drv_no_pickups_body)) }
        open.forEach { p -> item(key = p.id) {
            var note by remember(p.id) { mutableStateOf("") }
            BoondhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pluralStringResource(R.plurals.drv_jars_n, p.containerCount, p.containerCount), Modifier.weight(1f),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Pill(if (p.status == "EN_ROUTE") stringResource(R.string.drv_status_on_way) else stringResource(R.string.drv_to_collect), if (p.status == "EN_ROUTE") Tone.Info else Tone.Neutral)
                }
                p.customerName?.let { Text(it, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900) }
                p.addressLine?.let { Text(it, fontSize = 15.sp, color = NeerlyColors.Ink700) }
                p.scheduledFor?.let { Text(stringResource(R.string.drv_booked_for, shortDay(it)), fontSize = 14.sp, color = NeerlyColors.Ink600) }
                if (p.lat != null && p.lng != null) {
                    val ctx = LocalContext.current
                    val pickupLabel = p.customerName ?: stringResource(R.string.drv_pickup)
                    SecondaryButton(stringResource(R.string.drv_open_maps), { openMaps(ctx, p.lat.toDouble(), p.lng.toDouble(), pickupLabel) })
                } else if (p.addressLine == null) Text(stringResource(R.string.drv_ask_shop_address), fontSize = 13.sp, color = NeerlyColors.Ink600)
                if (p.status != "EN_ROUTE") PrimaryButton(if (s.working == p.id) stringResource(R.string.drv_starting) else stringResource(R.string.drv_start_pickup), { vm.start(p.id) },
                    enabled = s.working == null, modifier = BIG)
                else {
                    SecondaryButton(if (s.photoFor == p.id) stringResource(R.string.drv_photo_saved_again) else stringResource(R.string.drv_photo_of_jars), {
                        val f = vm.newPhotoFile(); pending = p.id to f
                        camera(f)
                    }, enabled = s.working == null, modifier = BIG)
                    OutlinedTextField(value = note, onValueChange = { note = it.take(300) }, label = { Text(stringResource(R.string.drv_jar_condition)) },
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    PrimaryButton(if (s.working == p.id) stringResource(R.string.drv_saving) else stringResource(R.string.drv_collected), { vm.complete(p.id, note) },
                        enabled = s.working == null, modifier = BIG, color = NeerlyColors.Ok)
                }
            }
        } }
    }
}

// ---- D19 location off ------------------------------------------------------------------

/** D19 — tasks and calls still work; only the location is off. */
@Composable
fun GpsProblemScreen(problem: GpsProblem, onAskPermission: () -> Unit, onSos: () -> Unit) {
    val context = LocalContext.current
    BoondhPage(title = stringResource(R.string.drv_gps_title), subtitle = stringResource(R.string.drv_gps_sub),
        footer = {
            PrimaryButton(if (problem == GpsProblem.NO_PERMISSION) stringResource(R.string.drv_allow_location) else stringResource(R.string.drv_turn_on_location), {
                if (problem == GpsProblem.NO_PERMISSION) onAskPermission()
                else runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            }, modifier = BIG, color = Boondh.Amber, contentColor = NeerlyColors.Ink900)
            SosHoldButton(onSos)
        }) {
        item { Notice(if (problem == GpsProblem.NO_PERMISSION) stringResource(R.string.drv_gps_need_permission)
                      else stringResource(R.string.drv_gps_switched_off), Tone.Warn) }
    }
}

@Suppress("unused") private val LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
