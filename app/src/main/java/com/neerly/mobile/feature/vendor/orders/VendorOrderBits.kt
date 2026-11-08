package com.neerly.mobile.feature.vendor.orders

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.Pill
import com.neerly.mobile.core.design.Tone
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val IST = ZoneId.of("Asia/Kolkata")

/** "9:44 AM" in India time from a server instant. */
fun istClock(iso: String): String =
    Instant.parse(iso).atZone(IST).format(DateTimeFormatter.ofPattern("h:mm a"))

/** Seconds left to accept, never negative; null when there is no deadline. */
fun secondsLeft(deadlineIso: String?, now: Instant = Instant.now()): Long? =
    deadlineIso?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?.let { Duration.between(now, it).seconds.coerceAtLeast(0) }

fun mmss(sec: Long) = "%d:%02d".format(sec / 60, sec % 60)

/** Ticking state of seconds left, updated every second. */
@Composable
fun rememberSecondsLeft(deadlineIso: String?): State<Long?> {
    val left = remember(deadlineIso) { mutableStateOf(secondsLeft(deadlineIso)) }
    LaunchedEffect(deadlineIso) {
        while (true) { left.value = secondsLeft(deadlineIso); if ((left.value ?: 0) <= 0) break; delay(1_000) }
    }
    return left
}

/** The amber "ACCEPT IN 2:41" pill on Today and Orders. */
@Composable
fun AcceptClock(deadlineIso: String?) {
    val left by rememberSecondsLeft(deadlineIso)
    left?.let { Pill(if (it > 0) stringResource(R.string.vend_accept_in, mmss(it)) else stringResource(R.string.vend_times_up), if (it > 30) Tone.Warn else Tone.Bad) }
}

@Composable
fun Money(text: String, bold: Boolean = false) =
    Text(text, fontSize = if (bold) 17.sp else 15.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = NeerlyColors.Ink900)
