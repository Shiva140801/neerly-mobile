package com.neerly.mobile.feature.vendor.money

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import com.neerly.mobile.core.design.Tone
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** "2–8 Nov", or "26 Oct – 1 Nov" across months. */
fun periodLabel(start: String?, end: String?, locale: java.util.Locale = java.util.Locale.ENGLISH): String {
    val f = DateTimeFormatter.ofPattern("d MMM", locale)
    val s = start?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: return ""
    val e = end?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: return s.format(f)
    return if (s.month == e.month) "${s.dayOfMonth}–${e.format(f)}"
    else "${s.format(f)} – ${e.format(f)}"
}

/** [periodLabel] with the month in the app's language. */
@androidx.compose.runtime.Composable
fun periodText(start: String?, end: String?): String =
    periodLabel(start, end, androidx.compose.ui.platform.LocalConfiguration.current.locales[0])

@androidx.compose.runtime.Composable
fun payoutStatus(status: String): Pair<String, Tone> = when (status) {
    "COMPLETED" -> stringResource(R.string.vend_ps_paid) to Tone.Ok
    "PROCESSING" -> stringResource(R.string.vend_ps_sending) to Tone.Info
    "PENDING" -> stringResource(R.string.vend_ps_coming) to Tone.Neutral
    "ON_HOLD" -> stringResource(R.string.vend_ps_hold) to Tone.Warn
    "FAILED" -> stringResource(R.string.vend_ps_failed) to Tone.Bad
    else -> status to Tone.Neutral
}
