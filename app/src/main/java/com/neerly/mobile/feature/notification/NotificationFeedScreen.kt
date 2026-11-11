package com.neerly.mobile.feature.notification

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * In-app feed, powered by GET /api/v1/notifications (see NotificationController).
 * Each row calls POST /api/v1/notifications/{id}/read when tapped.
 */
data class FeedItem(
    val id: String,
    val category: String,
    val subject: String?,
    val body: String,
    val isRead: Boolean,
    val queuedAt: String
)

private val IST: ZoneId = ZoneId.of("Asia/Kolkata")

/** "Today" / "Yesterday" / "Fri 6 Nov" — the board's section headings. */
internal fun dayHeading(queuedAt: String, today: LocalDate = LocalDate.now(IST), s: com.neerly.mobile.core.util.Strings): String {
    val d = runCatching { Instant.parse(queuedAt).atZone(IST).toLocalDate() }.getOrNull() ?: return s.get(R.string.acct_ntf_earlier)
    return when (d) {
        today -> s.get(R.string.acct_ntf_today)
        today.minusDays(1) -> s.get(R.string.acct_ntf_yesterday)
        else -> d.format(DateTimeFormatter.ofPattern("EEE d MMM", com.neerly.mobile.core.util.AppLocale.locale))
    }
}

internal fun clockTime(queuedAt: String): String =
    runCatching { Instant.parse(queuedAt).atZone(IST).format(DateTimeFormatter.ofPattern("h:mm a", com.neerly.mobile.core.util.AppLocale.locale)) }.getOrDefault("")

/** Board 25 — updates grouped by day, unread marked, "Mark all read". */
@Composable
fun NotificationFeedScreen(
    items: List<FeedItem>,
    loading: Boolean,
    onOpenItem: (String) -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    // Same instance the NavHost reads `items` from (one back-stack entry) — for the error,
    // retry and mark-all, which the caller doesn't pass.
    vm: NotificationFeedViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::refresh)
    com.neerly.mobile.core.util.OnLiveChange(action = vm::refresh)
    val unread = items.count { !it.isRead }
    val strings = com.neerly.mobile.core.util.rememberStrings()
    // Board Notifications: chevron with "Mark all read" on the right, day groups, hairline
    // rows with a glyph that says what kind of update it is, red unread dot.
    BoondhPage(
        title = stringResource(R.string.acct_ntf_title),
        onBack = onBack,
        topAction = {
            if (unread > 0) TextButton(onClick = vm::markAllRead, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.acct_ntf_mark_all), color = NeerlyColors.CustomerPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                Icon(androidx.compose.material.icons.Icons.Outlined.Settings, contentDescription = stringResource(R.string.acct_ntf_settings), tint = NeerlyColors.Ink900) }
        }
    ) {
        state.markError?.let { m -> item { com.neerly.mobile.core.design.Notice(m, com.neerly.mobile.core.design.Tone.Bad) } }
        if ((loading && items.isEmpty()) || state.error != null || items.isEmpty()) {
            item { LoadState(loading, state.error, vm::refresh, stringResource(R.string.acct_ntf_empty),
                stringResource(R.string.acct_ntf_empty_body)) }
            return@BoondhPage
        }
        items.groupBy { dayHeading(it.queuedAt, s = strings) }.forEach { (day, rows) ->
            item { SectionLabel(day, Modifier.padding(top = 8.dp)) }
            items(rows, key = { it.id }) { n -> FeedRow(n, last = n == rows.last()) { onOpenItem(n.id) } }
        }
    }
}

/** What kind of update it is, as board Notifications draws it. */
private enum class FeedGlyph { PERSON, CLOCK, REPEAT, MONEY, ALERT }

internal fun feedGlyphOf(category: String): String = when (category.uppercase()) {
    "SUBSCRIPTION", "SUBS" -> "REPEAT"
    "JARS", "DEPOSIT" -> "CLOCK"
    "WALLET", "PAYMENT", "REFUND" -> "MONEY"
    "SAFETY", "SECURITY" -> "ALERT"
    else -> "PERSON"   // order, shop and driver updates: whoever it's from
}

@Composable
private fun FeedRow(item: FeedItem, last: Boolean, onOpen: () -> Unit) {
    val unreadLabel = stringResource(R.string.acct_ntf_unread)
    val glyph = FeedGlyph.valueOf(feedGlyphOf(item.category))
    com.neerly.mobile.core.design.ListRow(onClick = onOpen, divider = !last) {
        Box(Modifier.size(48.dp).background(when (glyph) {
            FeedGlyph.REPEAT -> NeerlyColors.CustomerSoft
            FeedGlyph.MONEY -> NeerlyColors.OkSoft
            FeedGlyph.ALERT -> NeerlyColors.ErrSoft
            else -> NeerlyColors.WarnSoft
        }, CircleShape).align(Alignment.Top), contentAlignment = Alignment.Center) {
            when (glyph) {
                FeedGlyph.PERSON -> Text((item.subject ?: item.body).trim().take(1).uppercase(), fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NeerlyColors.Warn)
                FeedGlyph.CLOCK -> Icon(androidx.compose.material.icons.Icons.Outlined.Schedule, null, tint = NeerlyColors.Warn)
                FeedGlyph.REPEAT -> Icon(androidx.compose.material.icons.Icons.Filled.Autorenew, null, tint = NeerlyColors.CustomerPrimary)
                FeedGlyph.MONEY -> Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = NeerlyColors.Ok)
                FeedGlyph.ALERT -> Icon(androidx.compose.material.icons.Icons.Outlined.Shield, null, tint = NeerlyColors.Err)
            }
        }
        Column(Modifier.weight(1f).semantics { if (!item.isRead) contentDescription = unreadLabel }, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.subject ?: item.body, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            if (item.subject != null) Text(item.body, fontSize = 15.sp, lineHeight = 21.sp, color = NeerlyColors.Ink600, maxLines = 3)
            Text(clockTime(item.queuedAt), fontSize = 14.sp, color = NeerlyColors.Ink700)
        }
        Box(Modifier.align(Alignment.Top).padding(top = 8.dp).size(10.dp)
            .background(if (item.isRead) androidx.compose.ui.graphics.Color.Transparent else NeerlyColors.Err, CircleShape))
    }
}
