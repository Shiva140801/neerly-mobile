package com.neerly.mobile.feature.vendor.settings

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.dto.VendorHoursRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Board V30 (holidays & closures) and the hours half of V06. Closing days skips the
 * subscription jars due then and tells those customers; opening a day again brings those jars
 * back and tells them. The emergency close in Settings also skips jars on days it fully covers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorBusinessConfigScreen(onBack: () -> Unit, vm: VendorBusinessConfigViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var pausing by remember { mutableStateOf(false) }
    if (pausing) com.neerly.mobile.feature.vendor.dashboard.PauseSheet(working = false, onDismiss = { pausing = false },
        onPause = { choice, reason -> vm.pause(choice, reason); pausing = false })
    var label by remember { mutableStateOf("") }
    var range by remember { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    val today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))

    BoondhPage(stringResource(R.string.vend_holidays_hours), onBack = onBack,
        subtitle = stringResource(R.string.vend_holidays_sub)) {
        if (s.loading && s.hours.isEmpty() && s.holidays.isEmpty()) { item { LoadState(true, null, vm::refresh) }; return@BoondhPage }
        s.error?.let { item { Notice(it, Tone.Bad) } }
        s.saved?.let { item { Notice(it, Tone.Ok) } }
        s.status?.takeIf { !it.isOpen }?.let { st -> item {
            BoondhCard {
                Pill(stringResource(R.string.vend_closed_orders), Tone.Warn)
                Text(listOfNotNull(st.pausedUntil?.let { stringResource(R.string.vend_closed_orders_until, com.neerly.mobile.feature.vendor.dashboard.pauseEndLabel(it)) }, st.pausedReason).joinToString(" · ")
                    .ifBlank { stringResource(R.string.vend_until_reopen) },
                    fontSize = 14.sp, color = NeerlyColors.Ink700)
                PrimaryButton(stringResource(R.string.vend_open_again), vm::reopen)
            }
        } }

        // Board VHoliday: "Days off" and "A few hours" live together.
        if (s.status?.isOpen != false) item {
            BoondhCard { InfoRow(stringResource(R.string.vend_pause_few_hours), stringResource(R.string.vend_pause_few_hours_sub), onClick = { pausing = true }) }
        }
        item { SectionLabel(stringResource(R.string.vend_closed_days)) }
        val upcoming = s.holidays.filter { it.date >= today.toString() }.sortedBy { it.date }
        item {
            BoondhCard {
                if (upcoming.isEmpty()) Text(stringResource(R.string.vend_no_closures_dot), fontSize = 15.sp, color = NeerlyColors.Ink600)
                upcoming.forEachIndexed { i, h ->
                    if (i > 0) HorizontalDivider(color = NeerlyColors.Ink200)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text(shortDay(h.date), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                            h.label?.let { Text(it, fontSize = 13.sp, color = NeerlyColors.Ink600) }
                        }
                        TextButton(onClick = { vm.removeHoliday(h.date) }, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.vend_open_this_day), color = NeerlyColors.CustomerPrimary) }
                    }
                }
            }
        }
        item {
            BoondhCard {
                Text(stringResource(R.string.vend_close_some_days), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                val loc = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                SecondaryButton(range?.let { (a, b) -> if (a == b) shortDate(a, loc) else stringResource(R.string.vend_date_range, shortDate(a, loc), shortDate(b, loc)) }
                    ?: stringResource(R.string.vend_pick_days),
                    { picking = true })
                OutlinedTextField(value = label, onValueChange = { label = it.take(120) }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, label = { Text(stringResource(R.string.vend_reason_customers)) }, placeholder = { Text(stringResource(R.string.vend_reason_placeholder)) })
                Text(stringResource(R.string.vend_closure_note), fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
                PrimaryButton(stringResource(if (s.saving) R.string.vend_saving else R.string.vend_save_tell_subs), {
                    range?.let { (a, b) -> vm.addClosure(a, b, label); range = null; label = "" }
                }, enabled = range != null && !s.saving)
            }
        }

        item { SectionLabel(stringResource(R.string.vend_opening_hours)) }
        item { HoursEditor(s.hours, s.saving, vm::saveHours) }
    }

    if (picking) {
        val state = rememberDateRangePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
            })
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    val a = state.selectedStartDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    val b = state.selectedEndDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: a
                    if (a != null && b != null) range = a to b
                    picking = false
                }, enabled = state.selectedStartDateMillis != null) { Text(stringResource(R.string.vend_done)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.vend_cancel)) } }
        ) {
            DateRangePicker(state = state, modifier = Modifier.weight(1f),
                title = { Text(stringResource(R.string.vend_days_closed), Modifier.padding(16.dp)) })
        }
    }
}

@Composable
private fun HoursEditor(hours: List<VendorHoursRow>, saving: Boolean, onSave: (List<VendorHoursRow>) -> Unit) {
    val open = remember(hours) { mutableStateMapOf<Int, String>().apply { (1..7).forEach { d -> put(d, hours.firstOrNull { it.dayOfWeek == d }?.openTime?.take(5).orEmpty()) } } }
    val close = remember(hours) { mutableStateMapOf<Int, String>().apply { (1..7).forEach { d -> put(d, hours.firstOrNull { it.dayOfWeek == d }?.closeTime?.take(5).orEmpty()) } } }
    val loc = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    BoondhCard {
        Text(stringResource(R.string.vend_hours_hint), fontSize = 13.sp, color = NeerlyColors.Ink600)
        (1..7).forEach { d ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dayName(d, loc, java.time.format.TextStyle.SHORT), Modifier.width(44.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = open[d].orEmpty(), onValueChange = { open[d] = it.take(5) }, modifier = Modifier.weight(1f),
                    singleLine = true, placeholder = { Text("06:00") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("–")
                OutlinedTextField(value = close[d].orEmpty(), onValueChange = { close[d] = it.take(5) }, modifier = Modifier.weight(1f),
                    singleLine = true, placeholder = { Text("21:00") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        PrimaryButton(stringResource(if (saving) R.string.vend_saving else R.string.vend_save_hours), {
            onSave((1..7).mapNotNull { d ->
                val o = open[d]?.trim().orEmpty(); val c = close[d]?.trim().orEmpty()
                if (o.isBlank() || c.isBlank()) null else VendorHoursRow(d, padTime(o), padTime(c))
            })
        }, enabled = !saving)
    }
}

/** "6" / "6:0" / "06:00" → "06:00:00", which the backend parses. */
internal fun padTime(input: String): String {
    val parts = input.replace('.', ':').split(":")
    val h = parts.getOrNull(0)?.filter(Char::isDigit)?.padStart(2, '0')?.take(2) ?: "00"
    val m = parts.getOrNull(1)?.filter(Char::isDigit)?.padEnd(2, '0')?.take(2) ?: "00"
    return "$h:$m:00"
}
