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
    val unread = items.count { !it.isRead }
    val strings = com.neerly.mobile.core.util.rememberStrings()
    BoondhPage(
        title = stringResource(R.string.acct_ntf_title),
        onBack = onBack,
        subtitle = if (unread > 0) stringResource(R.string.acct_ntf_new, unread) else null
    ) {
        if (loading || state.error != null || items.isEmpty()) {
            item { LoadState(loading, state.error, vm::refresh, stringResource(R.string.acct_ntf_empty),
                stringResource(R.string.acct_ntf_empty_body)) }
            item { TextButton(onClick = onSettings, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.acct_ntf_settings_link), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) } }
            return@BoondhPage
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::markAllRead, enabled = unread > 0, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(R.string.acct_ntf_mark_all), color = if (unread > 0) NeerlyColors.CustomerPrimary else NeerlyColors.Ink600, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSettings, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(R.string.acct_ntf_settings), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
            }
        }
        items.groupBy { dayHeading(it.queuedAt, s = strings) }.forEach { (day, rows) ->
            item { SectionLabel(day) }
            items(rows, key = { it.id }) { n -> FeedRow(n) { onOpenItem(n.id) } }
        }
    }
}

@Composable
private fun FeedRow(item: FeedItem, onOpen: () -> Unit) {
    val unreadLabel = stringResource(R.string.acct_ntf_unread)
    BoondhCard(Modifier.semantics { if (!item.isRead) contentDescription = unreadLabel }, onClick = onOpen) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(if (item.isRead) NeerlyColors.Ink100 else NeerlyColors.CustomerSoft, CircleShape),
                contentAlignment = Alignment.Center) {
                Text(item.category.take(1).uppercase(), fontWeight = FontWeight.Bold,
                    color = if (item.isRead) NeerlyColors.Ink600 else NeerlyColors.CustomerPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.subject ?: item.body, fontSize = 16.sp,
                    fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold, color = NeerlyColors.Ink900)
                if (item.subject != null) Text(item.body, fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink700, maxLines = 3)
                Text(clockTime(item.queuedAt), fontSize = 12.sp, color = NeerlyColors.Ink600)
            }
            if (!item.isRead) Box(Modifier.padding(top = 6.dp).size(10.dp).background(NeerlyColors.CustomerPrimary, CircleShape))
        }
    }
}
