package com.neerly.mobile.core.design

import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

/**
 * Boondh building blocks, taken from the redesign canvas: cream page, serif page title,
 * paper cards with a warm hairline, navy primary buttons, small-caps section labels.
 * Screens rebuilt to the redesign compose these instead of re-deriving the look.
 */
object Boondh {
    val Amber = Color(0xFFE6A321)
    val MutedOnNavy = Color(0xFFC9CEE6)
}

/**
 * A full page as the boards draw it: a chevron back control (its label is read aloud,
 * not shown), serif title — beside the chevron when [inlineTitle] (Cart, Checkout) or
 * under it (Complaints, Addresses) — an optional [topAction] at the top right ("New
 * issue", "Mark all read"), scrolling content and a footer on the canvas behind a hairline.
 */
@Composable
fun BoondhPage(
    title: String,
    onBack: (() -> Unit)? = null,
    backLabel: String = stringResource(R.string.core_back),
    subtitle: String? = null,
    bottomBar: @Composable () -> Unit = {},
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    /** Shown at the end of the title row — e.g. the notification bell on Home. */
    titleAction: (@Composable () -> Unit)? = null,
    /** Title next to the chevron, smaller (boards Cart, Checkout). */
    inlineTitle: Boolean = false,
    /** Top-right action on the chevron row (boards Complaints, Notifications). */
    topAction: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    Scaffold(containerColor = NeerlyColors.Canvas, bottomBar = {
        Column {
            if (footer != null) {
                HorizontalDivider(color = NeerlyColors.Ink200)
                Column(Modifier.fillMaxWidth().background(NeerlyColors.Canvas).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), content = footer)
            }
            bottomBar()
        }
    }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            val inline = inlineTitle && onBack != null
            if (onBack != null && !inline) item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BackChevron(onBack, backLabel)
                    Spacer(Modifier.weight(1f))
                    topAction?.invoke()
                }
            }
            item {
                Row(verticalAlignment = if (inline) Alignment.CenterVertically else Alignment.Top) {
                    if (inline) { BackChevron(onBack!!, backLabel); Spacer(Modifier.width(4.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (inline) 2.dp else 6.dp)) {
                        PageTitle(title, size = if (inline) 26 else 30)
                        subtitle?.let { Text(it, fontSize = if (inline) 14.sp else 15.sp, lineHeight = 21.sp, color = NeerlyColors.Ink600) }
                    }
                    titleAction?.invoke()
                    if (inline) topAction?.invoke()
                }
            }
            content()
        }
    }
}

/** The boards' back control: a bare chevron, 48 dp target, labelled for TalkBack. */
@Composable
fun BackChevron(onBack: () -> Unit, label: String) {
    IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp).size(48.dp)) {
        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = label, tint = NeerlyColors.Ink900, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun PageTitle(text: String, size: Int = 30) = Text(
    text, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = size.sp,
    lineHeight = (size + 4).sp, color = NeerlyColors.Ink900, modifier = Modifier.semantics { heading() }
)

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) = Text(
    text.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
    color = NeerlyColors.Ink600, modifier = modifier.semantics { heading() }
)

@Composable
fun BoondhCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val base = modifier.fillMaxWidth()
        .background(NeerlyColors.Paper, RoundedCornerShape(14.dp))
        .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(14.dp))
    Column(
        (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp), content = content
    )
}

/** Title + secondary line + optional trailing text, for lists inside or outside cards. */
@Composable
fun InfoRow(title: String, subtitle: String? = null, trailing: String? = null, trailingColor: Color = NeerlyColors.Ink900,
            onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            subtitle?.let { Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = NeerlyColors.Ink600) }
        }
        trailing?.let { Text(it, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = trailingColor) }
        if (onClick != null) Text("  ›", fontSize = 20.sp, color = NeerlyColors.Ink600)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier,
                  color: Color = NeerlyColors.CustomerPrimary, contentColor: Color = NeerlyColors.Paper) =
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier,
                    color: Color = NeerlyColors.CustomerPrimary) =
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, NeerlyColors.Ink300)) {
        // One line always: in a row of equal buttons a wrapped label made one taller than its neighbours.
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }

enum class Tone { Neutral, Ok, Warn, Bad, Info }

@Composable
fun Pill(text: String, tone: Tone = Tone.Neutral) {
    val (bg, fg) = when (tone) {
        Tone.Ok -> NeerlyColors.OkSoft to NeerlyColors.Ok
        Tone.Warn -> NeerlyColors.WarnSoft to NeerlyColors.Warn
        Tone.Bad -> NeerlyColors.ErrSoft to NeerlyColors.Err
        Tone.Info -> NeerlyColors.CustomerSoft to NeerlyColors.CustomerPrimary
        Tone.Neutral -> NeerlyColors.Ink100 to NeerlyColors.Ink700
    }
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** A soft panel for a note, warning or problem. */
@Composable
fun Notice(text: String, tone: Tone = Tone.Info) {
    val (bg, fg) = when (tone) {
        Tone.Bad -> NeerlyColors.ErrSoft to NeerlyColors.Err
        Tone.Warn -> NeerlyColors.WarnSoft to NeerlyColors.Warn
        Tone.Ok -> NeerlyColors.OkSoft to NeerlyColors.Ok
        else -> NeerlyColors.CustomerSofter to NeerlyColors.Ink700
    }
    Text(text, fontSize = 14.sp, lineHeight = 20.sp, color = fg,
        modifier = Modifier.fillMaxWidth().background(bg, RoundedCornerShape(12.dp)).padding(14.dp))
}

/** Loading / failed / empty, one way everywhere. A failure never pretends to be "nothing here". */
@Composable
fun LoadState(loading: Boolean, error: String?, onRetry: () -> Unit, emptyTitle: String? = null, emptyBody: String? = null) {
    when {
        loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeerlyColors.CustomerPrimary)
        }
        error != null -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Notice(error, Tone.Bad)
            SecondaryButton(stringResource(R.string.core_try_again), onRetry)
        }
        emptyTitle != null -> Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emptyTitle, fontFamily = FontFamily.Serif, fontSize = 22.sp, color = NeerlyColors.Ink900)
            emptyBody?.let { Text(it, fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600) }
        }
    }
}

/** "₹1,25,000" / "₹6,002.50" — Indian digit grouping; paise only when there are some. */
fun BigDecimal.rupees(): String {
    val v = setScale(2, java.math.RoundingMode.HALF_UP).abs()
    val digits = v.toBigInteger().toString()
    // Last three digits, then pairs: 12,34,567. Done by hand so every JVM/ICU agrees.
    val grouped = if (digits.length <= 3) digits else {
        val head = digits.dropLast(3); val tail = digits.takeLast(3)
        head.reversed().chunked(2).joinToString(",").reversed() + "," + tail
    }
    val paise = v.remainder(BigDecimal.ONE).movePointRight(2).toInt()
    val body = if (paise == 0) grouped else "$grouped.%02d".format(paise)
    return (if (signum() < 0) "−₹" else "₹") + body
}

/** Whole rupees for tight spots (stat tiles): "₹6,002". */
fun BigDecimal.rupeesWhole(): String = setScale(0, java.math.RoundingMode.HALF_UP).rupees()

/** "Sat 7 Nov" from an ISO instant or date, in IST — day and month names in the app's language. */
fun shortDay(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val fmt = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", com.neerly.mobile.core.util.AppLocale.locale)
    return runCatching { java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata")).format(fmt) }
        .recoverCatching { java.time.Instant.parse(iso).atZone(java.time.ZoneId.of("Asia/Kolkata")).format(fmt) }
        .recoverCatching { java.time.LocalDate.parse(iso.take(10)).format(fmt) }
        .getOrDefault(iso.take(10))
}

/**
 * Runs [action] each time the screen comes back to the foreground (back from a detail screen,
 * or the app returning) — not on first show, when the view model's own load already ran.
 * Lists use it so a change made elsewhere (pause, cancel, top-up) shows without a pull.
 */
@Composable
fun OnReturn(action: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val latest = androidx.compose.runtime.rememberUpdatedState(action)
    // The "already shown once" flag must live as long as the screen's view model, not the
    // composition: a tab switch or back navigation rebuilds the composable but keeps the
    // view model (its init load doesn't run again). With the flag in the effect, the first
    // resume after every rebuild was skipped and the screen showed stale data — e.g. an
    // order cancelled on tracking still read "Being packed" on the Orders tab.
    val tracker: ReturnTracker = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "OnReturn:" + androidx.compose.runtime.currentCompositeKeyHash)
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME && tracker.shouldRun()) latest.value()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

/** Per-screen memory for [OnReturn]: skip only the very first resume of this view-model scope. */
class ReturnTracker : androidx.lifecycle.ViewModel() {
    private var seen = false
    /** False exactly once (first show — the view model's own load covers it), true after. */
    fun shouldRun(): Boolean = if (seen) true else { seen = true; false }
}


/** A delivery window like "6–8 AM" (or "11 AM–1 PM") in the app's language, from "06:00"/"08:00". */
fun timeWindow(start: String, end: String): String = runCatching {
    val loc = com.neerly.mobile.core.util.AppLocale.locale
    val a = java.time.LocalTime.parse(start.take(5)); val b = java.time.LocalTime.parse(end.take(5))
    val full = java.time.format.DateTimeFormatter.ofPattern("h a", loc)
    // The day period once when both ends share it ("6–8 AM"); both when they don't.
    if ((a.hour < 12) == (b.hour < 12)) (if (a.hour % 12 == 0) 12 else a.hour % 12).toString() + "–" + b.format(full)
    else a.format(full) + "–" + b.format(full)
}.getOrDefault("$start–$end")
