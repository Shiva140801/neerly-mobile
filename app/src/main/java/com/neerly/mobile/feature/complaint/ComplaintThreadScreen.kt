package com.neerly.mobile.feature.complaint

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.ComplaintResponse

/**
 * Board ComplaintThread — a chat about one problem: compact header ("subject /
 * NEE-… · with <shop>"), messages grouped by day with the time inside each bubble,
 * a green "Happy to close this now?" once someone has answered, and a pill composer.
 */
@Composable
fun ComplaintThreadScreen(onBack: () -> Unit, vm: ComplaintThreadViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val cfg = LocalAppConfig.current
    var draft by remember { mutableStateOf("") }
    var notYet by remember { mutableStateOf(false) }
    val c = s.complaint
    val open = c != null && c.status !in setOf("CLOSED", "WITHDRAWN")
    // Shop and support replies arrive from other phones: re-read on return, on a push for
    // this complaint, and every 15 s while it's still open.
    OnReturn(vm::refresh)
    com.neerly.mobile.core.util.OnLiveChange({ ch -> ch.complaintId == null || ch.complaintId == c?.id }, vm::refresh)
    com.neerly.mobile.core.util.LivePoll(open, THREAD_RECHECK_MS, vm::refresh)
    Scaffold(containerColor = NeerlyColors.Canvas, topBar = {
        Column(Modifier.background(NeerlyColors.Canvas).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                BackChevron(onBack, stringResource(R.string.core_back))
                Column(Modifier.weight(1f)) {
                    Text(c?.subject ?: stringResource(R.string.acct_cmp_one), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = NeerlyColors.Ink900, maxLines = 2)
                    c?.let { Text(threadSubtitle(it), fontSize = 14.sp, color = NeerlyColors.Ink600, maxLines = 1) }
                }
            }
            HorizontalDivider(color = NeerlyColors.Ink200)
        }
    }, bottomBar = {
        if (open) Column(Modifier.background(NeerlyColors.Canvas).navigationBarsPadding().imePadding()) {
            HorizontalDivider(color = NeerlyColors.Ink200)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(draft, { draft = it }, placeholder = { Text(stringResource(R.string.acct_cmp_write)) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(28.dp), maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = NeerlyColors.Paper, focusedContainerColor = NeerlyColors.Paper,
                        unfocusedBorderColor = NeerlyColors.Ink300))
                val canSend = draft.isNotBlank() && !s.sending
                FilledIconButton(onClick = { vm.appendMessage(draft); draft = "" }, enabled = canSend,
                    modifier = Modifier.size(56.dp), shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = NeerlyColors.CustomerPrimary,
                        disabledContainerColor = NeerlyColors.Ink200)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.acct_cmp_send),
                        tint = NeerlyColors.Paper)
                }
            }
        }
    }) { padding ->
        if (c == null) {
            Box(Modifier.padding(padding).padding(20.dp)) { LoadState(s.loading, s.error, vm::load) }
            return@Scaffold
        }
        val shown = c.messages.filter { !it.isInternal }
        val answered = shown.any { it.authorRole != "CUSTOMER" }
        // The opening description is the first message; replies follow in order.
        val entries = listOf(Triple("CUSTOMER", c.description, c.filedAt)) + shown.map { Triple(it.authorRole, it.message, it.createdAt) }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            var lastDay: java.time.LocalDate? = null
            entries.forEach { (role, text, at) ->
                val day = istTime(at)?.toLocalDate()
                if (day != null && day != lastDay) {
                    lastDay = day
                    item { Text(dayLabel(day, at), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        fontSize = 14.sp, color = NeerlyColors.Ink600) }
                }
                item {
                    val mine = role == "CUSTOMER"
                    val who = when (role) {
                        "CUSTOMER" -> null
                        "VENDOR_OWNER", "VENDOR_MANAGER" -> c.vendorName ?: stringResource(R.string.acct_dep_the_shop)
                        else -> stringResource(R.string.acct_cmp_support)
                    }
                    Bubble(who, text, at, mine)
                }
            }
            c.awardedAmount?.takeIf { it.signum() > 0 }?.let { a -> item { Notice(stringResource(R.string.acct_cmp_credited, a.rupees()), Tone.Ok) } }
            if (open) {
                if (answered && !notYet) item {
                    Column(Modifier.fillMaxWidth().background(NeerlyColors.OkSoft, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.acct_cmp_happy_close), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryButton(stringResource(R.string.acct_cmp_yes_sorted), vm::close, modifier = Modifier.weight(1f), color = NeerlyColors.Ok)
                            OutlinedButton(onClick = { notYet = true }, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ok)) {
                                Text(stringResource(R.string.acct_cmp_not_yet), color = NeerlyColors.Ok, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item {
                    Text(if (c.slaBreached) stringResource(R.string.acct_cmp_on_it)
                        else pluralStringResource(R.plurals.acct_cmp_support_joins, cfg.complaintFirstReplyHours, cfg.complaintFirstReplyHours),
                        Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600)
                }
            } else item {
                Notice(stringResource(if (s.closedNow) R.string.acct_cmp_thanks else when (c.status) {
                    "WITHDRAWN" -> R.string.acct_cmp_withdrawn; else -> R.string.acct_cmp_sorted }), Tone.Ok)
            }
            s.error?.let { e -> item { Notice(e, Tone.Bad) } }
        }
    }
}

@Composable
private fun threadSubtitle(c: ComplaintResponse): String {
    val num = c.orderNumber
    val shop = c.vendorName
    return when {
        num != null && shop != null -> stringResource(R.string.acct_cmp_sub_order_shop, num, shop)
        num != null -> num
        else -> stringResource(R.string.acct_cmp_opened, shortDay(c.filedAt))
    }
}

@Composable
private fun dayLabel(day: java.time.LocalDate, iso: String): String {
    val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))
    return when (day) {
        today -> stringResource(R.string.acct_cmp_today)
        today.minusDays(1) -> stringResource(R.string.acct_cmp_yesterday)
        else -> shortDay(iso)
    }
}

/** Mine: navy, right, time inside bottom-right. Theirs: paper with a hairline, sender line on top. */
@Composable
private fun Bubble(who: String?, text: String, at: String, mine: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (mine) 20.dp else 4.dp, bottomEnd = if (mine) 4.dp else 20.dp)
        Column(Modifier.widthIn(max = 300.dp)
            .background(if (mine) NeerlyColors.CustomerPrimary else NeerlyColors.Paper, shape)
            .let { if (mine) it else it.border(1.dp, NeerlyColors.Ink200, shape) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            who?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink600) }
            Text(text, fontSize = 16.sp, lineHeight = 22.sp, color = if (mine) NeerlyColors.Paper else NeerlyColors.Ink900)
            Text(clockTime(at), Modifier.align(if (mine) Alignment.End else Alignment.Start), fontSize = 12.sp,
                color = if (mine) NeerlyColors.Paper.copy(alpha = 0.75f) else NeerlyColors.Ink600)
        }
    }
}

private const val THREAD_RECHECK_MS = 15_000L
