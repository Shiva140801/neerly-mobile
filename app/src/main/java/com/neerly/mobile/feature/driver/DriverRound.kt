package com.neerly.mobile.feature.driver

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One stop on the round: straight-line km from the previous point, and minutes until we get there. */
data class StopEta(val km: Double, val minutes: Int)

/**
 * DRoute: estimate each stop in order, chaining from where the driver is (or the shop when
 * there's no fix) — distance ÷ the admin's average round speed, plus a few minutes at each
 * door already passed. Straight-line, so it's a guide, not a promise.
 */
fun roundEtas(from: Pair<Double, Double>?, stops: List<Pair<Double, Double>?>, speedKmh: Int,
              minutesPerStop: Int = ROUND_MINUTES_PER_STOP): List<StopEta?> {
    var at = from
    var minutes = 0.0
    val speed = speedKmh.coerceAtLeast(1)
    return stops.mapIndexed { i, stop ->
        val prev = at
        if (stop == null || prev == null) { if (stop != null) at = stop; return@mapIndexed null }
        val km = kmBetween(prev, stop)
        minutes += km / speed * 60 + if (i > 0) minutesPerStop else 0
        at = stop
        StopEta(km, Math.round(minutes).toInt())
    }
}

const val ROUND_MINUTES_PER_STOP = 3

private val SHORT_TIME = DateTimeFormatter.ofPattern("h:mm", Locale.ENGLISH)
private val AMPM = DateTimeFormatter.ofPattern("a", Locale.ENGLISH)
private val IST: ZoneId = ZoneId.of("Asia/Kolkata")

/** DOnBreak "1:15 – 1:45 PM": the period is said once when both ends share it. */
fun breakWindow(startIso: String?, endIso: String?): String? {
    val a = startIso?.let { runCatching { Instant.parse(it).atZone(IST) }.getOrNull() } ?: return null
    val b = endIso?.let { runCatching { Instant.parse(it).atZone(IST) }.getOrNull() } ?: return null
    val pa = a.format(AMPM).uppercase(); val pb = b.format(AMPM).uppercase()
    return if (pa == pb) "${a.format(SHORT_TIME)} – ${b.format(SHORT_TIME)} $pb"
    else "${a.format(SHORT_TIME)} $pa – ${b.format(SHORT_TIME)} $pb"
}

/** "h:mm AM" for an instant, in India time. */
fun clockOf(iso: String?): String? = iso?.let { runCatching {
    Instant.parse(it).atZone(IST).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)).uppercase()
}.getOrNull() }

/** Seconds left until [endIso], never negative; null when there's no end. */
fun secondsUntil(endIso: String?, nowMs: Long): Long? =
    endIso?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }?.let { ((it - nowMs) / 1000).coerceAtLeast(0) }

/** "14:58" countdown text. */
fun minSec(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)

/**
 * DCashMismatch: how much of the gap the driver's per-customer answers explain — the sum of
 * (bill − what they paid) over the customers marked as having paid less.
 */
fun shortExplained(bills: Map<String, BigDecimal>, paid: Map<String, BigDecimal>): BigDecimal =
    paid.entries.fold(BigDecimal.ZERO) { acc, (id, got) ->
        val bill = bills[id] ?: return@fold acc
        acc + (bill - got).max(BigDecimal.ZERO)
    }

/** Reasons a customer paid less (server values) — DCashMismatch. */
val CASH_SHORT_REASONS = listOf("PAYS_LATER", "GAVE_LESS", "DISPUTES_BILL")
