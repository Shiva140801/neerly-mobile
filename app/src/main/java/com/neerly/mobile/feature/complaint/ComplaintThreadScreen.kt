package com.neerly.mobile.feature.complaint

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.config.LocalAppConfig

/** Boondh board 50 — the conversation about one problem, and the customer's "Yes, all sorted". */
@Composable
fun ComplaintThreadScreen(onBack: () -> Unit, vm: ComplaintThreadViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val cfg = LocalAppConfig.current
    var draft by remember { mutableStateOf("") }
    val c = s.complaint
    val open = c != null && c.status !in setOf("CLOSED", "WITHDRAWN")
    BoondhPage(c?.subject ?: stringResource(R.string.acct_cmp_one), onBack = onBack, backLabel = stringResource(R.string.acct_cmp_title),
        subtitle = c?.let { listOfNotNull(it.orderId?.let { stringResource(R.string.acct_order) }, stringResource(R.string.acct_cmp_opened, shortDay(it.filedAt))).joinToString(" · ") },
        footer = if (!open) null else ({
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft, { draft = it }, placeholder = { Text(stringResource(R.string.acct_cmp_write)) }, modifier = Modifier.weight(1f))
                Button(onClick = { vm.appendMessage(draft); draft = "" }, enabled = draft.isNotBlank() && !s.sending,
                    modifier = Modifier.heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)) { Text(stringResource(R.string.acct_cmp_send)) }
            }
        })
    ) {
        if (c == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(stringResource(when (c.status) { "CLOSED" -> R.string.acct_cmp_sorted; "WITHDRAWN" -> R.string.acct_cmp_withdrawn; "RESOLVED" -> R.string.acct_cmp_resolved; else -> R.string.acct_cmp_open }),
                if (open) Tone.Warn else Tone.Ok)
            if (c.slaBreached) Pill(stringResource(R.string.acct_cmp_on_it), Tone.Bad)
        } }
        item { Bubble(stringResource(R.string.acct_cmp_you), c.description, c.filedAt, mine = true) }
        items(c.messages.filter { !it.isInternal }, key = { it.id }) { m ->
            Bubble(stringResource(when (m.authorRole) { "CUSTOMER" -> R.string.acct_cmp_you; "VENDOR_OWNER" -> R.string.acct_dep_the_shop; else -> R.string.acct_cmp_support }),
                m.message, m.createdAt, mine = m.authorRole == "CUSTOMER")
        }
        c.awardedAmount?.takeIf { it.signum() > 0 }?.let { a -> item { Notice(stringResource(R.string.acct_cmp_credited, a.rupees()), Tone.Ok) } }
        if (open) {
            item { Text(pluralStringResource(R.plurals.acct_cmp_support_joins, cfg.complaintFirstReplyHours, cfg.complaintFirstReplyHours), fontSize = 13.sp, color = NeerlyColors.Ink600) }
            item { BoondhCard {
                Text(stringResource(R.string.acct_cmp_is_sorted), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(stringResource(R.string.acct_cmp_yes_sorted), vm::close, modifier = Modifier.weight(1f))
                }
            } }
        } else if (s.closedNow) item { Notice(stringResource(R.string.acct_cmp_thanks), Tone.Ok) }
        s.error?.let { e -> item { Notice(e, Tone.Bad) } }
    }
}

@Composable
private fun Bubble(who: String, text: String, at: String, mine: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Text("$who · ${shortDay(at)}", fontSize = 12.sp, color = NeerlyColors.Ink600)
        Text(text, fontSize = 15.sp, lineHeight = 21.sp, color = if (mine) NeerlyColors.Paper else NeerlyColors.Ink900,
            modifier = Modifier.widthIn(max = 300.dp)
                .background(if (mine) NeerlyColors.CustomerPrimary else NeerlyColors.Paper, RoundedCornerShape(14.dp))
                .padding(12.dp))
    }
}
