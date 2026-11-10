package com.neerly.mobile.feature.vendor.team

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*

/** Board V17 — your drivers, live; V34 add a driver and V35 the evening count open in place. */
@Composable
fun VendorTeamScreen(
    onBack: () -> Unit,
    vm: VendorTeamViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    OnReturn(vm::refresh)
    when {
        s.count != null -> { BackHandler { vm.closeCount() }; CashCount(s.count!!, vm) }
        s.invite != null -> { BackHandler { vm.closeInvite() }; Invite(s.invite!!, vm) }
        else -> Drivers(s, onBack, vm)
    }
}

@Composable
private fun Drivers(s: VendorTeamUiState, onBack: () -> Unit, vm: VendorTeamViewModel) {
    var confirmRemove by remember { mutableStateOf<TeamDriver?>(null) }
    BoondhPage(stringResource(R.string.vend_your_drivers), onBack = onBack,
        subtitle = stringResource(R.string.vend_drivers_sub),
        footer = { PrimaryButton(stringResource(R.string.vend_add_driver), vm::startInvite, color = NeerlyColors.VendorPrimary) }) {
        s.error?.let { item { Notice(it, Tone.Bad) } }
        if (s.members.isEmpty()) {
            item { LoadState(s.loading, null, vm::refresh, stringResource(R.string.vend_no_drivers),
                stringResource(R.string.vend_no_drivers_sub)) }
            return@BoondhPage
        }
        // Cash still to count tonight first, then whoever is out, then the busiest — the
        // driver at the counter shouldn't be under a page of idle drivers.
        val toCount = s.cash.filter { it.drops > 0 && it.confirmation == null && it.cashExpected.signum() > 0 }
        if (toCount.isNotEmpty()) item {
            Notice(pluralStringResource(R.plurals.vend_to_count_tonight, toCount.size, toCount.size,
                toCount.fold(java.math.BigDecimal.ZERO) { a, c -> a + c.cashExpected }.rupees()), Tone.Warn)
        }
        teamOrder(s.members, s.cash).forEach { d -> item(key = d.membershipId) {
            val cash = s.cash.firstOrNull { it.driverId == d.driverId }
            BoondhCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).background(NeerlyColors.VendorSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text((d.name ?: "?").split(' ').take(2).joinToString("") { it.take(1) }.uppercase(),
                            fontWeight = FontWeight.Bold, color = NeerlyColors.VendorPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(d.name ?: stringResource(R.string.vend_driver), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(listOfNotNull(d.phoneMasked, d.notes).joinToString(" · "), fontSize = 13.sp, color = NeerlyColors.Ink600)
                    }
                    when (d.liveStatus) {
                        "ON_DROP" -> Pill(stringResource(R.string.vend_on_a_drop), Tone.Info)
                        "ON_SHIFT" -> Pill(stringResource(R.string.vend_on_shift), Tone.Ok)
                        else -> Pill(stringResource(R.string.vend_off), Tone.Neutral)
                    }
                }
                InfoRow(pluralStringResource(R.plurals.vend_drops_today_n2, d.dropsToday, d.dropsToday),
                    trailing = if (d.cashHeld.signum() > 0) stringResource(R.string.vend_holding_cash, d.cashHeld.rupees()) else null)
                if (cash != null && (cash.drops > 0 || cash.confirmation != null)) {
                    val done = cash.confirmation
                    if (done != null) Text(when {
                            !done.shortfall -> stringResource(R.string.vend_counted_ok)
                            done.emptiesShortfall > 0 -> pluralStringResource(R.plurals.vend_counted_short_jars, done.emptiesShortfall,
                                shortDay(done.confirmedAt), done.cashShortfall.rupees(), done.emptiesShortfall)
                            else -> stringResource(R.string.vend_counted_short, shortDay(done.confirmedAt), done.cashShortfall.rupees())
                        }, fontSize = 13.sp, color = if (done.shortfall) NeerlyColors.Warn else NeerlyColors.Ok)
                    SecondaryButton(if (done == null) stringResource(R.string.vend_tonight_count_with, (d.name ?: stringResource(R.string.vend_driver_lc)).substringBefore(' ')) else stringResource(R.string.vend_count_again),
                        { vm.startCount(d.driverId) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CallButton(d.phone, color = NeerlyColors.VendorPrimary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { confirmRemove = d }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.vend_remove_from_team), color = NeerlyColors.Err) }
                }
            }
        } }
    }
    confirmRemove?.let { d ->
        AlertDialog(onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.vend_remove_q, d.name ?: stringResource(R.string.vend_this_driver))) },
            text = { Text(stringResource(R.string.vend_remove_body)) },
            confirmButton = { TextButton(onClick = { vm.remove(d.driverId); confirmRemove = null }) { Text(stringResource(R.string.vend_remove), color = NeerlyColors.Err) } },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text(stringResource(R.string.vend_keep_them)) } })
    }
}

@Composable
private fun Invite(i: InviteState, vm: VendorTeamViewModel) {
    var note by rememberSaveable { mutableStateOf("") }
    val f = i.found
    BoondhPage(
        title = if (i.added) (f?.nameMasked?.let { stringResource(R.string.vend_named_on_team, it) } ?: stringResource(R.string.vend_they_on_team)) else stringResource(R.string.vend_add_driver),
        onBack = vm::closeInvite, backLabel = stringResource(R.string.vend_drivers),
        subtitle = stringResource(if (i.added) R.string.vend_added_sub else R.string.vend_invite_sub),
        footer = {
            when {
                i.added -> PrimaryButton(stringResource(R.string.vend_back_to_drivers), vm::closeInvite, color = NeerlyColors.VendorPrimary)
                f?.exists == true && !f.alreadyOnTeam -> PrimaryButton(if (i.adding) stringResource(R.string.vend_adding) else stringResource(R.string.vend_add_named, f.nameMasked ?: stringResource(R.string.vend_driver_lc)),
                    { vm.addFound(note) }, enabled = !i.adding, color = NeerlyColors.VendorPrimary)
                else -> PrimaryButton(stringResource(if (i.looking) R.string.vend_looking else R.string.vend_find), vm::findDriver, enabled = !i.looking, color = NeerlyColors.VendorPrimary)
            }
        }
    ) {
        if (i.added) return@BoondhPage
        item {
            Text(stringResource(R.string.vend_driver_mobile), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(i.phone, vm::typePhone, singleLine = true, prefix = { Text("+91 ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth())
        }
        i.problem?.let { item { Notice(it, Tone.Bad) } }
        when {
            f == null -> {}
            !f.exists -> item { Notice(stringResource(R.string.vend_no_driver_number), Tone.Warn) }
            f.alreadyOnTeam -> item { Notice(stringResource(R.string.vend_already_on_team, f.nameMasked ?: stringResource(R.string.vend_boondh_driver)), Tone.Info) }
            else -> {
                item { BoondhCard { InfoRow(f.nameMasked ?: stringResource(R.string.vend_boondh_driver), stringResource(R.string.vend_boondh_driver_phone, i.phone.take(5))) } }
                item {
                    Text(stringResource(R.string.vend_note_self), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(note, { note = it.take(200) }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Text(stringResource(R.string.vend_boondh_pays_drivers), fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
    }
}

@Composable
private fun CashCount(c: CountState, vm: VendorTeamViewModel) {
    val first = (c.row.name ?: stringResource(R.string.vend_driver)).substringBefore(' ')
    val r = c.result
    BoondhPage(
        title = if (r != null) stringResource(if (r.shortfall) R.string.vend_saved_short else R.string.vend_all_there) else stringResource(R.string.vend_count_with, first),
        onBack = vm::closeCount, backLabel = stringResource(R.string.vend_drivers),
        subtitle = pluralStringResource(R.plurals.vend_drops_done, c.row.drops, shortDay(c.row.day), c.row.drops),
        footer = {
            if (r != null) PrimaryButton(stringResource(R.string.vend_back_to_drivers), vm::closeCount, color = NeerlyColors.VendorPrimary)
            else PrimaryButton(stringResource(if (c.saving) R.string.vend_saving else R.string.vend_confirm_count), vm::confirmCount, enabled = !c.saving, color = NeerlyColors.VendorPrimary)
        }
    ) {
        if (r != null) {
            item { BoondhCard {
                InfoRow(stringResource(R.string.vend_cash_expected), trailing = r.expectedCash.rupees())
                InfoRow(stringResource(R.string.vend_you_counted), trailing = r.cashReceived.rupees())
                if (r.cashShortfall.signum() > 0) InfoRow(stringResource(R.string.vend_short), trailing = r.cashShortfall.rupees(), trailingColor = NeerlyColors.Err)
                InfoRow(stringResource(R.string.vend_empty_jars), trailing = stringResource(R.string.vend_x_of_y, r.emptiesReceived, r.expectedEmpties))
                if (r.emptiesShortfall > 0) InfoRow(stringResource(R.string.vend_jars_short), trailing = "${r.emptiesShortfall}", trailingColor = NeerlyColors.Err)
            } }
            if (r.shortfall) item { Notice(stringResource(R.string.vend_shortfall_saved, first), Tone.Warn) }
            return@BoondhPage
        }
        item { BoondhCard {
            InfoRow(stringResource(R.string.vend_cash_should_bring), trailing = c.row.cashExpected.rupees())
            c.row.driverDeclaredCash?.let { InfoRow(stringResource(R.string.vend_their_app_says), trailing = it.rupees()) }
            InfoRow(stringResource(R.string.vend_empties_due), trailing = "${c.row.emptiesExpected}")
        } }
        item {
            Text(stringResource(R.string.vend_you_counted), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(c.cash, vm::typeCash, singleLine = true, prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth())
            c.cashShort?.let { short -> Text(when {
                short.signum() > 0 -> stringResource(R.string.vend_less_than_cash, short.rupees())
                short.signum() < 0 -> stringResource(R.string.vend_more_than_cash, short.negate().rupees())
                else -> stringResource(R.string.vend_matches_cash)
            }, fontSize = 14.sp, color = if (short.signum() > 0) NeerlyColors.Warn else NeerlyColors.Ok) }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.vend_empties_back), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                    if (c.emptiesShort > 0) Text(stringResource(R.string.vend_n_short, c.emptiesShort), fontSize = 13.sp, color = NeerlyColors.Warn)
                }
                val fewer = stringResource(R.string.vend_one_fewer_jar)
                val more = stringResource(R.string.vend_one_more_jar)
                OutlinedButton(onClick = { vm.empties(-1) }, modifier = Modifier.size(48.dp).semantics { contentDescription = fewer },
                    contentPadding = PaddingValues(0.dp)) { Text("−", fontSize = 20.sp) }
                Text("${c.empties}", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp))
                OutlinedButton(onClick = { vm.empties(1) }, modifier = Modifier.size(48.dp).semantics { contentDescription = more },
                    contentPadding = PaddingValues(0.dp)) { Text("+", fontSize = 20.sp) }
            }
        }
        item {
            OutlinedTextField(c.note, vm::typeNote, label = { Text(stringResource(R.string.vend_note_optional2)) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        }
        c.problem?.let { item { Notice(it, Tone.Bad) } }
    }
}

/** Drivers with cash to count tonight first, then on a drop / on shift, then by drops today, then name. */
internal fun teamOrder(members: List<TeamDriver>, cash: List<CashToday>): List<TeamDriver> {
    val byId = cash.associateBy { it.driverId }
    fun toCount(d: TeamDriver) = byId[d.driverId]?.let { it.drops > 0 && it.confirmation == null && it.cashExpected.signum() > 0 } == true
    fun live(d: TeamDriver) = when (d.liveStatus) { "ON_DROP" -> 0; "ON_SHIFT" -> 1; else -> 2 }
    return members.sortedWith(compareBy<TeamDriver>({ !toCount(it) }, { live(it) }, { -it.dropsToday }, { it.name ?: "" }))
}
