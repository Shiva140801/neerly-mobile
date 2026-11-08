package com.neerly.mobile.feature.subscription

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.SubscriptionResponse
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

private enum class Mode { Detail, Pause, Change, End }

/**
 * Boards 18 (detail), 61 (change), 62/63 (pause → paused), 78/79 (end → ended).
 * Every date offered starts at the cut-off-aware first changeable day; the server
 * refuses anything earlier, so we never offer it.
 */
@Composable
fun SubscriptionDetailScreen(
    onBack: () -> Unit,
    onNewSubscription: () -> Unit = {},
    vm: SubscriptionDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var mode by remember { mutableStateOf(Mode.Detail) }
    val s = state.subscription
    // A finished action returns to the detail behind its confirmation.
    LaunchedEffect(state.done) { if (state.done != null) mode = Mode.Detail }

    if (s == null) {
        BoondhPage(stringResource(R.string.acct_sub_one), onBack = onBack, backLabel = stringResource(R.string.acct_profile_subs)) {
            item { LoadState(state.loading, state.error ?: stringResource(R.string.acct_sub_not_found), vm::load) }
        }
        return
    }
    when (val done = state.done) {
        is Done.Paused -> return PausedDone(s, done, onDone = { vm.dismissDone() }, onChange = { vm.dismissDone(); mode = Mode.Pause })
        is Done.Ended -> return EndedDone(s, done.finalDelivery, onDone = { vm.dismissDone(); onBack() }, onNew = onNewSubscription)
        null -> Unit
    }
    when (mode) {
        Mode.Detail -> Detail(s, state, vm, onBack, onPause = { mode = Mode.Pause }, onChange = { mode = Mode.Change }, onEnd = { mode = Mode.End })
        Mode.Pause -> PauseSheet(s, state, onBack = { mode = Mode.Detail }) { from, until -> vm.pause(from.toString(), until?.toString(), null) }
        Mode.Change -> ChangeSheet(s, state, onBack = { mode = Mode.Detail }) { q, start, end -> vm.change(q, start, end); mode = Mode.Detail }
        Mode.End -> EndSheet(s, state, onBack = { mode = Mode.Detail }, onPause = { mode = Mode.Pause }) { vm.cancel(null) }
    }
}

@Composable
private fun Detail(
    s: SubscriptionResponse, state: SubscriptionDetailUiState, vm: SubscriptionDetailViewModel,
    onBack: () -> Unit, onPause: () -> Unit, onChange: () -> Unit, onEnd: () -> Unit
) {
    val cfg = LocalAppConfig.current
    val first = cfg.firstChangeableDate()
    val next = s.nextDeliveryDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val skipTarget = if (next == null || next.isBefore(first)) first else next
    // An ending plan (ended after the cut-off) has nothing left to pause, skip or change.
    val live = s.status in setOf("ACTIVE", "PAUSED") && !s.ending
    BoondhPage(
        title = s.what(), onBack = onBack, backLabel = stringResource(R.string.acct_profile_subs),
        subtitle = listOfNotNull(s.vendorName, s.howOften(), s.deliverySlot).joinToString(" · ")
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    s.ending -> Pill(stringResource(R.string.acct_sub_pill_ending_after, shortDay(s.finalDeliveryDate)), Tone.Warn)
                    else -> when (s.status) {
                    "ACTIVE" -> Pill(stringResource(R.string.acct_sub_pill_since, shortDay(s.startDate).substringAfter(' ')), Tone.Ok)
                    "PAUSED" -> Pill(s.pausedUntil?.let { stringResource(R.string.acct_sub_pill_paused_till, shortDay(it)) } ?: stringResource(R.string.acct_sub_paused_open), Tone.Warn)
                    "PENDING_MANDATE" -> Pill(stringResource(R.string.acct_sub_pill_autopay_wait), Tone.Warn)
                    else -> Pill(stringResource(R.string.acct_sub_ended), Tone.Neutral)
                    }
                }
            }
        }
        if (s.ending) item {
            Notice(stringResource(R.string.acct_sub_ending_note, shortDay(s.finalDeliveryDate)), Tone.Info)
        }
        state.error?.let { item { Notice(it, Tone.Bad) } }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.acct_sub_row_next), trailing = if (s.status == "ACTIVE") s.nextDeliveryDate?.let(::shortDay) ?: "—" else "—")
                s.eachTime()?.let { InfoRow(stringResource(R.string.acct_sub_row_each), trailing = it.rupees()) }
                InfoRow(stringResource(R.string.acct_sub_row_pays), if (s.paymentMethod == "WALLET" && cfg.upiAutopay) stringResource(R.string.acct_sub_autopay_if_short) else null, s.paysFrom())
            }
        }
        if (s.status == "PENDING_MANDATE") item {
            Notice(stringResource(R.string.acct_sub_autopay_unavailable), Tone.Warn)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(if (state.working) stringResource(R.string.acct_sub_switching) else stringResource(R.string.acct_sub_pay_wallet_instead), vm::payFromWallet, enabled = !state.working)
        }
        if (live) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.status == "ACTIVE") SecondaryButton(if (state.working) stringResource(R.string.acct_sub_skipping) else stringResource(R.string.acct_sub_skip_day, shortDay(skipTarget.toString()).substringAfter(' ')),
                    { vm.skip(skipTarget.toString()) }, enabled = !state.working, modifier = Modifier.weight(1f))
                if (s.status == "PAUSED") PrimaryButton(if (state.working) stringResource(R.string.acct_sub_resuming) else stringResource(R.string.acct_sub_resume_now), vm::resume,
                    enabled = !state.working, modifier = Modifier.weight(1f))
                else SecondaryButton(stringResource(R.string.acct_sub_pause), onPause, enabled = !state.working, modifier = Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.acct_sub_change), onChange, enabled = !state.working, modifier = Modifier.weight(1f))
            }
        }
        if (live) item {
            Text(stringResource(R.string.acct_sub_cutoff_note, cfg.clock(cfg.subscriptionCutoff)), fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
        val coming = state.comingUp(first)
        if (coming.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.acct_sub_coming_up)) }
            coming.take(5).forEach { i -> item(key = i.id) {
                BoondhCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(shortDay(i.deliveryDate), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                            Text(if (i.status == "SKIPPED") (i.skipReason?.let { stringResource(R.string.acct_sub_skipped_why, s.deliverySlot, it) } ?: stringResource(R.string.acct_sub_skipped, s.deliverySlot))
                                else pluralStringResource(R.plurals.acct_sub_slot_jars, s.quantity, s.deliverySlot, s.quantity),
                                fontSize = 13.sp, color = if (i.status == "SKIPPED") NeerlyColors.Warn else NeerlyColors.Ink600)
                        }
                        if (i.status == "SCHEDULED" && s.status == "ACTIVE" && !s.ending) TextButton(onClick = { vm.skip(i.deliveryDate.take(10)) },
                            enabled = !state.working, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.acct_sub_skip), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold) }
                    }
                }
            } }
        }
        if (state.lately.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.acct_sub_lately)) }
            state.lately.forEach { i -> item(key = "l" + i.id) {
                BoondhCard {
                    InfoRow(shortDay(i.deliveryDate), when (i.status) { "DELIVERED" -> stringResource(R.string.acct_sub_inst_delivered); "MISSED" -> stringResource(R.string.acct_sub_inst_missed); else -> stringResource(R.string.acct_sub_inst_failed) },
                        if (i.status == "DELIVERED") i.amount.rupees() else null)
                }
            } }
        }
        if (live || s.status == "PENDING_MANDATE") item {
            TextButton(onClick = onEnd, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.acct_sub_end_this), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Board 62 — pick the days you won't need water. */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun PauseSheet(s: SubscriptionResponse, state: SubscriptionDetailUiState, onBack: () -> Unit, onPause: (LocalDate, LocalDate?) -> Unit) {
    val first = LocalAppConfig.current.firstChangeableDate()
    val sat = first.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
    val presets = listOf(
        stringResource(R.string.acct_sub_pause_weekend) to (sat to sat.plusDays(1)),
        stringResource(R.string.acct_sub_pause_week) to (first to first.plusDays(6)),
        stringResource(R.string.acct_sub_pause_2weeks) to (first to first.plusDays(13)),
        stringResource(R.string.acct_sub_pause_open) to (first to null)
    )
    var pick by remember { mutableStateOf(1) }
    val (from, until) = presets[pick].second
    val skipped = state.instances.count { i ->
        i.status == "SCHEDULED" && SubscriptionDetailUiState.d(i)?.let { !it.isBefore(from) && (until == null || !it.isAfter(until)) } == true
    }
    BoondhPage(stringResource(R.string.acct_sub_pause_title), onBack = onBack, backLabel = stringResource(R.string.acct_sub_one),
        subtitle = stringResource(R.string.acct_sub_pause_sub),
        footer = {
            state.error?.let { Notice(it, Tone.Bad) }
            PrimaryButton(if (state.working) stringResource(R.string.acct_sub_pausing) else if (until == null) stringResource(R.string.acct_sub_pause_from, shortDay(from.toString()))
                else stringResource(R.string.acct_sub_pause_range, shortDay(from.toString()), shortDay(until.toString())),
                { onPause(from, until) }, enabled = !state.working)
        }) {
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEachIndexed { i, (label, _) ->
                    FilterChip(selected = pick == i, onClick = { pick = i }, label = { Text(label, fontSize = 15.sp) }, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.acct_sub_no_jars), trailing = if (until == null) stringResource(R.string.acct_sub_from_day, shortDay(from.toString())) else "${shortDay(from.toString())} – ${shortDay(until.toString())}")
                if (skipped > 0 && until != null) InfoRow(stringResource(R.string.acct_sub_skipped_count), trailing = "$skipped")
                InfoRow(stringResource(R.string.acct_sub_first_back), trailing = if (until == null) stringResource(R.string.acct_sub_when_resume)
                    else stringResource(R.string.acct_sub_back_on_or_after, shortDay(until.plusDays(1).toString())))
            }
        }
        item { Text(stringResource(R.string.acct_sub_pause_shop_told, s.vendorName ?: stringResource(R.string.acct_sub_your_shop)),
            fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600) }
    }
}

private fun slotLabel(start: String, end: String): String = com.neerly.mobile.core.design.timeWindow(start, end)
private val SLOTS get() = listOf("06:00" to "08:00", "07:00" to "09:00", "08:00" to "10:00", "17:00" to "19:00", "19:00" to "21:00")
    .map { (a, b) -> Triple(slotLabel(a, b), a, b) }

/** Board 61 — jars each time and the time window; Before/After so the bill change is plain. */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun ChangeSheet(s: SubscriptionResponse, state: SubscriptionDetailUiState, onBack: () -> Unit, onSave: (Int?, String?, String?) -> Unit) {
    var qty by remember { mutableStateOf(s.quantity) }
    val currentSlot = SLOTS.indexOfFirst { s.slotStart.startsWith(it.second) }
    var slot by remember { mutableStateOf(currentSlot) }
    val first = LocalAppConfig.current.firstChangeableDate()
    val changed = qty != s.quantity || slot != currentSlot
    val each = s.unitPrice
    BoondhPage(stringResource(R.string.acct_sub_change_title), onBack = onBack, backLabel = stringResource(R.string.acct_sub_one),
        footer = {
            state.error?.let { Notice(it, Tone.Bad) }
            PrimaryButton(stringResource(R.string.acct_sub_save_changes), {
                val sl = SLOTS.getOrNull(slot)?.takeIf { slot != currentSlot }
                onSave(qty.takeIf { it != s.quantity }, sl?.second, sl?.third)
            }, enabled = changed && !state.working)
        }) {
        item { SectionLabel(stringResource(R.string.acct_sub_jars_each)) }
        item {
            BoondhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(listOfNotNull(s.productName, each?.let { stringResource(R.string.acct_sub_price_each, it.rupees()) }).joinToString(" · "),
                        Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    TextButton(onClick = { qty = (qty - 1).coerceAtLeast(1) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Text("−", fontSize = 22.sp, color = NeerlyColors.CustomerPrimary) }
                    Text("$qty", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    TextButton(onClick = { qty = (qty + 1).coerceAtMost(20) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Text("+", fontSize = 22.sp, color = NeerlyColors.CustomerPrimary) }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.acct_sub_time)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SLOTS.forEachIndexed { i, (label, _, _) ->
                    FilterChip(selected = slot == i, onClick = { slot = i }, label = { Text(label, fontSize = 15.sp) }, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.acct_sub_before), trailing = listOfNotNull("${s.quantity} × ${s.howOften()}", s.eachTime()?.rupees()).joinToString(" · "))
                InfoRow(stringResource(R.string.acct_sub_after), trailing = listOfNotNull("$qty × ${SLOTS.getOrNull(slot)?.first ?: s.deliverySlot}",
                    each?.multiply(BigDecimal(qty))?.rupees()).joinToString(" · "))
            }
        }
        if (changed) item { Notice(stringResource(R.string.acct_sub_change_starts, shortDay(first.toString())), Tone.Info) }
        item { Text(stringResource(R.string.acct_sub_change_freq_note), fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}

/** Board 78 — say what ending really does, and offer a pause instead. */
@Composable
private fun EndSheet(s: SubscriptionResponse, state: SubscriptionDetailUiState, onBack: () -> Unit, onPause: () -> Unit, onEnd: () -> Unit) {
    val firstChangeable = LocalAppConfig.current.firstChangeableDate()
    BoondhPage(s.vendorName?.let { stringResource(R.string.acct_sub_end_title_named, it) } ?: stringResource(R.string.acct_sub_end_title), onBack = onBack, backLabel = stringResource(R.string.acct_sub_one),
        subtitle = s.what(),
        footer = {
            state.error?.let { Notice(it, Tone.Bad) }
            PrimaryButton(if (state.working) stringResource(R.string.acct_sub_ending) else stringResource(R.string.acct_sub_end), onEnd, enabled = !state.working, color = NeerlyColors.Err)
            SecondaryButton(stringResource(R.string.acct_sub_keep), onBack)
        }) {
        // After the cut-off the next jar is already being packed — the server keeps it and ends after.
        val packed = s.nextDeliveryDate?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            ?.isBefore(firstChangeable) == true
        item { Text(if (packed) stringResource(R.string.acct_sub_end_packed, s.nextDeliveryDate?.let(::shortDay) ?: "")
            else s.nextDeliveryDate?.let { stringResource(R.string.acct_sub_end_now_day, shortDay(it)) } ?: stringResource(R.string.acct_sub_end_now),
            fontSize = 16.sp, lineHeight = 23.sp, color = NeerlyColors.Ink900) }
        item { Text(stringResource(R.string.acct_sub_end_lose_slot, s.deliverySlot), fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink700) }
        item { Notice(stringResource(R.string.acct_sub_end_jars), Tone.Info) }
        if (s.status == "ACTIVE") item {
            BoondhCard(onClick = onPause) {
                InfoRow(stringResource(R.string.acct_sub_break_title), stringResource(R.string.acct_sub_break_sub), stringResource(R.string.acct_sub_pause))
            }
        }
    }
}

/** Board 63. */
@Composable
private fun PausedDone(s: SubscriptionResponse, d: Done.Paused, onDone: () -> Unit, onChange: () -> Unit) {
    val shop = s.vendorName ?: stringResource(R.string.acct_sub_your_shop)
    BoondhPage(stringResource(R.string.acct_sub_paused_title),
        subtitle = if (d.until == null) stringResource(R.string.acct_sub_paused_body_open, shortDay(d.from), shop)
            else stringResource(R.string.acct_sub_paused_body_range, shortDay(d.from), shortDay(d.until), shop),
        footer = { PrimaryButton(stringResource(R.string.acct_refunds_done), onDone); SecondaryButton(stringResource(R.string.acct_sub_change_dates), onChange) }) {
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.acct_sub_back_on), trailing = s.pausedUntil?.let { shortDay(LocalDate.parse(it.take(10)).plusDays(1).toString()) }
                    ?: d.until?.let { shortDay(LocalDate.parse(it).plusDays(1).toString()) } ?: stringResource(R.string.acct_sub_when_resume))
                InfoRow(stringResource(R.string.acct_sub_time), trailing = s.deliverySlot)
                InfoRow(stringResource(R.string.acct_sub_jars_with_you), trailing = stringResource(R.string.acct_sub_keep_them))
                InfoRow(stringResource(R.string.acct_sub_wallet_or_autopay), trailing = stringResource(R.string.acct_sub_nothing_taken))
            }
        }
    }
}

/** Board 79. */
@Composable
private fun EndedDone(s: SubscriptionResponse, finalDelivery: String?, onDone: () -> Unit, onNew: () -> Unit) {
    val shop = s.vendorName ?: stringResource(R.string.acct_sub_your_shop)
    BoondhPage(if (finalDelivery != null) stringResource(R.string.acct_sub_ends_after_title, shop, shortDay(finalDelivery))
        else stringResource(R.string.acct_sub_ended_title, shop),
        subtitle = if (finalDelivery != null) stringResource(R.string.acct_sub_ended_packed)
            else stringResource(R.string.acct_sub_ended_nothing),
        footer = { PrimaryButton(stringResource(R.string.acct_refunds_done), onDone); SecondaryButton(stringResource(R.string.acct_sub_start_different), onNew) }) {
        item { Notice(stringResource(R.string.acct_sub_end_jars), Tone.Info) }
    }
}
