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
            // Board Jars: the money held, big, with one jar glyph per jar on a shelf.
            item {
                BoondhCard {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(stringResource(R.string.acct_dep_held), Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink600)
                        Text(state.heldTotal.rupees(), fontSize = 34.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, color = NeerlyColors.Ink900)
                    }
                    val n = state.held.sumOf { it.containerCount }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterHorizontally)) {
                        repeat(n.coerceAtMost(6)) { com.neerly.mobile.core.design.JarGlyph(Modifier.size(width = 40.dp, height = 56.dp), body = NeerlyColors.Paper, water = NeerlyColors.Water100, outline = NeerlyColors.Ink900) }
                    }
                    Text(pluralStringResource(R.plurals.acct_dep_jars, n, n), fontSize = 13.sp, color = NeerlyColors.Ink600,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
                }
            }
            // Due first — the board lists "Return today" above "Return by".
            val held = state.held.sortedBy { it.returnDeadline ?: "~" }
            items(held.size, key = { held[it].id }) { i ->
                val d = held[i]
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

/**
 * Board Jars row: title, "With you since …", the due tag, and the Return button on the
 * right — filled when it's due today or late, outlined otherwise. The old card stacked a
 * full-width button under each jar, so two jars filled the screen (user report).
 */
@Composable
private fun JarCard(d: DepositResponse, requestedMode: String?, onReturn: () -> Unit) {
    val due = d.returnDeadline?.let { runCatching { Instant.parse(it).atZone(IST).toLocalDate() }.getOrNull() }
    val today = LocalDate.now(IST)
    val late = d.status == "LATE" || (due != null && due.isBefore(today))
    val urgent = late || due == today
    com.neerly.mobile.core.design.ListRow {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(jarTitle(d), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            d.heldAt?.let { Text(stringResource(R.string.acct_dep_since, shortDay(it)), fontSize = 14.sp, color = NeerlyColors.Ink600) }
            when {
                requestedMode != null -> Pill(if (requestedMode == "DROPOFF") stringResource(R.string.acct_dep_pill_dropoff) else stringResource(R.string.acct_dep_pill_pickup), Tone.Info)
                late -> Pill(stringResource(R.string.acct_dep_pill_overdue), Tone.Bad)
                due == today -> Pill(stringResource(R.string.acct_dep_pill_today, fmt(due)), Tone.Warn)
                due != null -> Pill(stringResource(R.string.acct_dep_pill_by, fmt(due)), Tone.Neutral)
            }
            if (d.lateFeeAccrued.signum() > 0) Text(stringResource(R.string.acct_dep_late_fee, d.lateFeeAccrued.rupees()), fontSize = 13.sp, color = NeerlyColors.Err)
        }
        if (requestedMode == null) {
            ReturnButton(stringResource(R.string.acct_dep_return), filled = urgent, onClick = onReturn)
        }
    }
}

/** Compact row button (board Jars "Return"): content-width, 52 dp tall. */
@Composable
private fun ReturnButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    if (filled) androidx.compose.material3.Button(onClick = onClick, shape = shape, modifier = Modifier.heightIn(min = 52.dp).widthIn(min = 112.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary, contentColor = NeerlyColors.Paper)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    } else androidx.compose.material3.OutlinedButton(onClick = onClick, shape = shape, modifier = Modifier.heightIn(min = 52.dp).widthIn(min = 112.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ink300)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900, maxLines = 1)
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
            // Board JarReturn: equal-width days, then windows two to a row — three windows in
            // one row ran off the right edge of a phone.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { dd -> SlotChip(fmt(dd), day == dd, { day = dd }, Modifier.weight(1f)) }
            }
            windows.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (h, l) -> SlotChip(l, window == h, { window = h }, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
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

/** A full-width choice chip: label centred, at least 48 dp tall, one line. */
@Composable
private fun SlotChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(selected = selected, onClick = onClick, modifier = modifier.heightIn(min = 48.dp),
        label = { Text(label, fontSize = 14.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()) })
}

@Composable
private fun jarTitle(d: DepositResponse) =
    listOfNotNull(if (d.containerCount > 1) pluralStringResource(R.plurals.acct_dep_jars, d.containerCount, d.containerCount)
        else (d.productName ?: stringResource(R.string.acct_dep_jar)), d.vendorName).joinToString(" · ")

private fun fmt(d: LocalDate) = d.format(DateTimeFormatter.ofPattern("EEE d MMM", com.neerly.mobile.core.util.AppLocale.locale))
