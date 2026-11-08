package com.neerly.mobile.feature.subscription

import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.SubscriptionResponse

/** Boondh board 17 (and its empty state) — every plan, with skip/pause/resume one tap away. */
@Composable
fun SubscriptionListScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onNew: () -> Unit = {},
    onSelectTab: (CustomerTab) -> Unit = {},
    vm: SubscriptionListViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::refresh)
    val cfg = LocalAppConfig.current
    val cutoff = cfg.clock(cfg.subscriptionCutoff)
    val live = state.active + state.paused + state.pending
    val empty = !state.loading && state.error == null && live.isEmpty()
    BoondhPage(
        title = stringResource(R.string.acct_profile_subs),
        subtitle = if (empty) null else stringResource(R.string.acct_sub_list_sub, cutoff),
        bottomBar = { CustomerBottomBar(current = CustomerTab.Subs, onSelect = onSelectTab) },
        footer = if ((state.loading || state.error != null) && state.subscriptions.isEmpty()) null else ({
            state.error?.let { Notice(it, Tone.Bad) }
            if (empty) PrimaryButton(stringResource(R.string.acct_sub_first), onNew) else SecondaryButton(stringResource(R.string.acct_sub_new), onNew)
        })
    ) {
        if ((state.loading || state.error != null) && state.subscriptions.isEmpty()) {
            item { LoadState(state.loading, state.error, vm::refresh) }
            return@BoondhPage
        }
        if (empty) {
            item { EmptyPitch() }
            if (state.ended.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.acct_sub_ended)) }
                items(state.ended, key = { it.id }) { s -> EndedRow(s) { onOpen(s.id) } }
            }
            return@BoondhPage
        }
        items(live, key = { it.id }) { s ->
            PlanCard(s, resuming = state.resuming == s.id, onOpen = { onOpen(s.id) }, onResume = { vm.resume(s.id) })
        }
        if (state.ended.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.acct_sub_ended)) }
            items(state.ended, key = { it.id }) { s -> EndedRow(s) { onOpen(s.id) } }
        }
    }
}

@Composable
private fun PlanCard(s: SubscriptionResponse, resuming: Boolean, onOpen: () -> Unit, onResume: () -> Unit) {
    BoondhCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).background(NeerlyColors.CustomerSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(initials(s.vendorName), fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(s.what(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(listOfNotNull(s.vendorName, s.howOften(), s.deliverySlot).joinToString(" · "), fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
            when {
                s.ending -> Pill(stringResource(R.string.acct_sub_pill_ending), Tone.Warn)
                else -> when (s.status) {
                "ACTIVE" -> Pill(stringResource(R.string.acct_sub_pill_active), Tone.Ok)
                "PAUSED" -> Pill(stringResource(R.string.acct_sub_pill_paused), Tone.Warn)
                else -> Pill(stringResource(R.string.acct_sub_pill_setup), Tone.Warn)
                }
            }
        }
        if (s.ending) {
            Text(stringResource(R.string.acct_sub_ending_after, shortDay(s.finalDeliveryDate)), fontSize = 15.sp, color = NeerlyColors.Ink900)
        } else when (s.status) {
            "ACTIVE" -> {
                s.nextDeliveryDate?.let { Text(stringResource(R.string.acct_sub_next, shortDay(it), s.deliverySlot), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900) }
                s.eachTime()?.let { Text(stringResource(R.string.acct_sub_per_delivery_from, it.rupees(), s.paysFrom()), fontSize = 13.sp, color = NeerlyColors.Ink600) }
                SecondaryButton(stringResource(R.string.acct_sub_skip_or_pause), onOpen)
            }
            "PAUSED" -> {
                Text(s.pausedUntil?.let { stringResource(R.string.acct_sub_away_till, shortDay(it)) } ?: stringResource(R.string.acct_sub_paused_open), fontSize = 15.sp, color = NeerlyColors.Ink900)
                PrimaryButton(if (resuming) stringResource(R.string.acct_sub_resuming) else stringResource(R.string.acct_sub_resume_now), onResume, enabled = !resuming)
            }
            else -> Text(stringResource(R.string.acct_sub_not_started), fontSize = 13.sp, color = NeerlyColors.Warn)
        }
    }
}

@Composable
private fun EndedRow(s: SubscriptionResponse, onOpen: () -> Unit) {
    BoondhCard(onClick = onOpen) {
        InfoRow(s.what(), listOfNotNull(s.vendorName, s.lastDeliveryDate?.let { stringResource(R.string.acct_sub_last, shortDay(it)) }).joinToString(" · "))
    }
}

@Composable
private fun EmptyPitch() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MON", "WED", "FRI").map(::dayName).forEach {
                Box(Modifier.background(NeerlyColors.CustomerSoft, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(it, fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary)
                }
            }
        }
        Text(stringResource(R.string.acct_sub_pitch_title), fontFamily = FontFamily.Serif, fontSize = 26.sp, color = NeerlyColors.Ink900)
        Text(stringResource(R.string.acct_sub_pitch_body),
            fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink700)
        listOf(R.string.acct_sub_pitch_1, R.string.acct_sub_pitch_2, R.string.acct_sub_pitch_3).forEach {
            Text("✓  " + stringResource(it), fontSize = 15.sp, color = NeerlyColors.Ink900)
        }
    }
}
