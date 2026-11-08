package com.neerly.mobile.feature.deposit

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.DepositResponse
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val IST = ZoneId.of("Asia/Kolkata")

/** Boards 22 (jars at your place) and 23 (return a jar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositsScreen(
    onBack: () -> Unit,
    vm: DepositsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::refresh)
    var returning by remember { mutableStateOf<DepositResponse?>(null) }
    val cfg = LocalAppConfig.current

    BoondhPage(
        title = stringResource(R.string.acct_dep_title), onBack = onBack,
        subtitle = stringResource(R.string.acct_dep_sub, stringResource(state.refundWhere))
    ) {
        if (state.loading && state.deposits.isEmpty() || state.error != null && state.deposits.isEmpty()) {
            item { LoadState(state.loading, state.error, vm::refresh) }; return@BoondhPage
        }
        state.error?.let { e -> item { Notice(e, Tone.Bad) } }
        if (state.held.isEmpty()) {
            item { LoadState(false, null, vm::refresh, stringResource(R.string.acct_dep_empty),
                stringResource(R.string.acct_dep_empty_body)) }
        } else {
            item { BoondhCard { InfoRow(stringResource(R.string.acct_dep_held), state.held.sumOf { it.containerCount }.let { pluralStringResource(R.plurals.acct_dep_jars, it, it) }, state.heldTotal.rupees()) } }
            items(state.held.size, key = { state.held[it].id }) { i ->
                val d = state.held[i]
                JarCard(d, state.requested[d.id]) { returning = d }
            }
            item {
                Text(if (cfg.depositForfeitDays > 0) pluralStringResource(R.plurals.acct_dep_late_rule_days, cfg.depositForfeitDays, cfg.depositForfeitDays)
                    else stringResource(R.string.acct_dep_late_rule), fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
            }
        }
        val past = state.returned + state.forfeited
        if (past.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.acct_dep_returned)) }
            items(past.size, key = { past[it].id }) { i ->
                val d = past[i]
                BoondhCard {
                    InfoRow(jarTitle(d), when (d.status) {
                        "RETURNED" -> d.returnedAt?.let { stringResource(R.string.acct_dep_returned_on, shortDay(it)) } ?: stringResource(R.string.acct_dep_returned_refunded)
                        else -> stringResource(R.string.acct_dep_forfeited)
                    }, (d.refundableAmount ?: d.amount).rupees())
                }
            }
        }
    }

    returning?.let { d ->
        ModalBottomSheet(onDismissRequest = { returning = null }, containerColor = NeerlyColors.Paper) {
            ReturnSheet(d, submitting = state.submitting, refundWhere = stringResource(state.refundWhere)) { mode, slot ->
                vm.scheduleReturn(d.id, mode, slot); returning = null
            }
        }
    }
}

@Composable
private fun JarCard(d: DepositResponse, requestedMode: String?, onReturn: () -> Unit) {
    val due = d.returnDeadline?.let { runCatching { Instant.parse(it).atZone(IST).toLocalDate() }.getOrNull() }
    val today = LocalDate.now(IST)
    val late = d.status == "LATE" || (due != null && due.isBefore(today))
    BoondhCard {
        Text(jarTitle(d), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        d.heldAt?.let { Text(stringResource(R.string.acct_dep_since, shortDay(it)), fontSize = 13.sp, color = NeerlyColors.Ink600) }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                when {
                    requestedMode != null -> Pill(if (requestedMode == "DROPOFF") stringResource(R.string.acct_dep_pill_dropoff) else stringResource(R.string.acct_dep_pill_pickup), Tone.Info)
                    late -> Pill(stringResource(R.string.acct_dep_pill_overdue), Tone.Bad)
                    due == today -> Pill(stringResource(R.string.acct_dep_pill_today, fmt(due)), Tone.Warn)
                    due != null -> Pill(stringResource(R.string.acct_dep_pill_by, fmt(due)), Tone.Neutral)
                }
            }
            Text((d.refundableAmount ?: d.amount).rupees(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        }
        if (d.lateFeeAccrued.signum() > 0) Text(stringResource(R.string.acct_dep_late_fee, d.lateFeeAccrued.rupees()), fontSize = 13.sp, color = NeerlyColors.Err)
        if (requestedMode == null) SecondaryButton(stringResource(R.string.acct_dep_return), onReturn)
    }
}

/** Board 23 — when and how; the money follows the customer's refund setting. */
@Composable
private fun ReturnSheet(d: DepositResponse, submitting: Boolean, refundWhere: String = stringResource(R.string.acct_dep_where_wallet), onConfirm: (mode: String, slotIso: String?) -> Unit) {
    var mode by remember { mutableStateOf("SCHEDULED_PICKUP") }
    val days = remember { (0..2).map { LocalDate.now(IST).plusDays(it.toLong()) } }
    var day by remember { mutableStateOf(days[1]) }
    val hourFmt = DateTimeFormatter.ofPattern("h a", com.neerly.mobile.core.util.AppLocale.locale)
    fun h(x: Int) = java.time.LocalTime.of(x, 0).format(hourFmt)
    val windows = listOf(9 to "${h(9)} – ${h(12)}", 12 to "${h(12)} – ${h(15)}", 15 to "${h(15)} – ${h(18)}")
    val nowHour = java.time.LocalTime.now(IST).hour
    var window by remember { mutableStateOf(windows[0].first) }
    val windowOk = day.isAfter(LocalDate.now(IST)) || window > nowHour
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.acct_dep_sheet_title, jarTitle(d), (d.refundableAmount ?: d.amount).rupees()), fontSize = 20.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, color = NeerlyColors.Ink900)
        SectionLabel(stringResource(R.string.acct_dep_when))
        listOf("SCHEDULED_PICKUP" to (stringResource(R.string.acct_dep_mode_pickup) to stringResource(R.string.acct_dep_mode_pickup_sub)),
               "DROPOFF" to (stringResource(R.string.acct_dep_mode_drop) to stringResource(R.string.acct_dep_mode_drop_sub, d.vendorName ?: stringResource(R.string.acct_dep_the_shop)))).forEach { (m, t) ->
            BoondhCard(onClick = { mode = m }) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = mode == m, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Column { Text(t.first, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                        Text(t.second, fontSize = 13.sp, color = NeerlyColors.Ink600) }
                }
            }
        }
        if (mode == "SCHEDULED_PICKUP") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { dd -> FilterChip(selected = day == dd, onClick = { day = dd }, label = { Text(fmt(dd)) }, modifier = Modifier.heightIn(min = 44.dp)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                windows.forEach { (h, l) -> FilterChip(selected = window == h, onClick = { window = h }, label = { Text(l, fontSize = 13.sp) }, modifier = Modifier.heightIn(min = 44.dp)) }
            }
            if (!windowOk) Notice(stringResource(R.string.acct_dep_window_started), Tone.Warn)
        }
        Text(stringResource(R.string.acct_dep_rinse, refundWhere),
            fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
        PrimaryButton(
            when { submitting -> stringResource(R.string.acct_dep_booking); mode == "DROPOFF" -> stringResource(R.string.acct_dep_drop_cta); else -> stringResource(R.string.acct_dep_book_cta, fmt(day)) },
            {
                val slot = if (mode == "SCHEDULED_PICKUP") day.atTime(window, 0).atZone(IST).toInstant().truncatedTo(ChronoUnit.SECONDS).toString() else null
                onConfirm(mode, slot)
            },
            enabled = !submitting && (mode == "DROPOFF" || windowOk)
        )
    }
}

@Composable
private fun jarTitle(d: DepositResponse) =
    listOfNotNull(if (d.containerCount > 1) pluralStringResource(R.plurals.acct_dep_jars, d.containerCount, d.containerCount)
        else (d.productName ?: stringResource(R.string.acct_dep_jar)), d.vendorName).joinToString(" · ")

private fun fmt(d: LocalDate) = d.format(DateTimeFormatter.ofPattern("EEE d MMM", com.neerly.mobile.core.util.AppLocale.locale))
