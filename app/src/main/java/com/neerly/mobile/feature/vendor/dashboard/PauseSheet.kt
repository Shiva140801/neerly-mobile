package com.neerly.mobile.feature.vendor.dashboard

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.R
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.PrimaryButton
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** How long a shop stops new orders — the server works out "rest of today" from its hours. */
sealed interface PauseChoice {
    data class Hours(val hours: Int) : PauseChoice
    data object RestOfToday : PauseChoice
    data class Until(val at: Instant) : PauseChoice
}

/** The quick choices on the sheet, shortest first. */
val PAUSE_HOURS = listOf(1, 2, 3, 5, 10)

private val IST: ZoneId = ZoneId.of("Asia/Kolkata")

/**
 * "4:30 PM" today, "Wed 11 Nov, 6:00 AM" on another day — when a pause ends, in India time.
 * Used for "Paused till …" on Today and Settings.
 */
fun pauseEndLabel(iso: String, now: Instant = Instant.now()): String {
    val at = Instant.parse(iso).atZone(IST)
    val sameDay = at.toLocalDate() == now.atZone(IST).toLocalDate()
    return at.format(DateTimeFormatter.ofPattern(if (sameDay) "h:mm a" else "EEE d MMM, h:mm a", java.util.Locale.ENGLISH))
}

/**
 * Pause new orders: 1 / 2 / 3 / 5 / 10 hours, closed for the rest of today (back at the next
 * opening time), or until a date and time the shop picks. Booked slots after the pause and
 * orders already taken still go out; longer breaks belong in holidays.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PauseSheet(working: Boolean, onDismiss: () -> Unit, onPause: (PauseChoice, String?) -> Unit) {
    val context = LocalContext.current
    var choice by remember { mutableStateOf<PauseChoice>(PauseChoice.Hours(1)) }
    var reason by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.vend_pause_sheet_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text(stringResource(R.string.vend_pause_sheet_body), fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PAUSE_HOURS.forEach { h ->
                    FilterChip(selected = choice == PauseChoice.Hours(h), onClick = { choice = PauseChoice.Hours(h) },
                        label = { Text(pluralStringResource(R.plurals.vend_pause_n_hours, h, h)) }, modifier = Modifier.heightIn(min = 48.dp))
                }
                FilterChip(selected = choice == PauseChoice.RestOfToday, onClick = { choice = PauseChoice.RestOfToday },
                    label = { Text(stringResource(R.string.vend_pause_rest_today)) }, modifier = Modifier.heightIn(min = 48.dp))
                FilterChip(selected = choice is PauseChoice.Until, onClick = {
                    val start = LocalDateTime.now(IST).plusHours(1)
                    DatePickerDialog(context, { _, y, m, d ->
                        TimePickerDialog(context, { _, hh, mm ->
                            choice = PauseChoice.Until(LocalDate.of(y, m + 1, d).atTime(hh, mm).atZone(IST).toInstant())
                        }, start.hour, 0, false).show()
                    }, start.year, start.monthValue - 1, start.dayOfMonth).apply {
                        datePicker.minDate = System.currentTimeMillis()
                    }.show()
                }, label = { Text(stringResource(R.string.vend_pause_pick)) }, modifier = Modifier.heightIn(min = 48.dp))
            }
            Text(when (val c = choice) {
                is PauseChoice.Hours -> stringResource(R.string.vend_pause_back_at, pauseEndLabel(Instant.now().plusSeconds(c.hours * 3600L).toString()))
                PauseChoice.RestOfToday -> stringResource(R.string.vend_pause_back_opening)
                is PauseChoice.Until -> stringResource(R.string.vend_pause_back_at, pauseEndLabel(c.at.toString()))
            }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            OutlinedTextField(value = reason, onValueChange = { reason = it.take(200) }, singleLine = true,
                label = { Text(stringResource(R.string.vend_reason_customers_see)) }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.vend_pause_longer_holiday), fontSize = 13.sp, color = NeerlyColors.Ink600)
            PrimaryButton(stringResource(R.string.vend_pause_orders), { onPause(choice, reason.trim().ifBlank { null }) },
                enabled = !working, color = NeerlyColors.Warn)
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.vend_cancel), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
