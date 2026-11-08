package com.neerly.mobile.feature.vendor.subscriptions

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.VendorSubscriptionTodayRow
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.vendor.orders.VendorOpsApi
import com.neerly.mobile.feature.vendor.orders.VendorTeamLive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class VendorSubscriptionsTodayViewModel @Inject constructor(
    private val repo: VendorRepository,
    private val ops: VendorOpsApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(VendorSubsTodayUiState())
    val state: StateFlow<VendorSubsTodayUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.rows.isEmpty(), error = null)
        viewModelScope.launch {
            runCatching {
                supervisorScope {
                    val rows = async { repo.subscriptionsToday() }
                    val team = async { runCatching { ops.team() }.getOrDefault(emptyList()) }
                    rows.await() to team.await()
                }
            }.onSuccess { (rows, team) -> _state.value = VendorSubsTodayUiState(loading = false, rows = rows, team = team) }
             .onFailure { _state.value = _state.value.copy(loading = false,
                 error = it.userMessage(context = "run sheet", fallback = strings.get(R.string.vend_err_runsheet))) }
        }
    }
}

data class VendorSubsTodayUiState(
    val loading: Boolean = true,
    val rows: List<VendorSubscriptionTodayRow> = emptyList(),
    val team: List<VendorTeamLive> = emptyList(),
    val error: String? = null
) {
    val going get() = rows.filter { it.status != "SKIPPED" }
    val done get() = going.count { it.status == "DELIVERED" }
    val noDriver get() = going.filter { it.status == "PENDING" }
    val skipped get() = rows.filter { it.status == "SKIPPED" }
}

/** Board V12 — this morning's subscription homes, slot by slot. */
@Composable
fun VendorSubscriptionsTodayScreen(onBack: () -> Unit, vm: VendorSubscriptionsTodayViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    OnReturn(vm::load)
    val now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("EEE d MMM · h:mm a",
        androidx.compose.ui.platform.LocalConfiguration.current.locales[0]))
    BoondhPage(title = stringResource(R.string.vend_runsheet_title), onBack = onBack, subtitle = now) {
        if (s.rows.isEmpty()) {
            item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.vend_runsheet_empty), stringResource(R.string.vend_runsheet_empty_sub)) }
            return@BoondhPage
        }
        item {
            BoondhCard {
                Text(stringResource(R.string.vend_runsheet_progress, s.done, s.going.size), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                LinearProgressIndicator(progress = { if (s.going.isEmpty()) 0f else s.done / s.going.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(), color = NeerlyColors.Ok, trackColor = NeerlyColors.Ink200)
            }
        }
        s.team.filter { it.dropsToday > 0 || it.liveStatus != "OFF" }.forEach { d -> item(key = "t" + d.driverId) {
            InfoRow(d.name, pluralStringResource(R.plurals.vend_drops_today_n, d.dropsToday, d.dropsToday) + " · " +
                stringResource(when (d.liveStatus) { "ON_DROP" -> R.string.vend_rs_out_now; "ON_SHIFT" -> R.string.vend_rs_at_shop; else -> R.string.vend_rs_off }),
                if (d.cashHeld.signum() > 0) stringResource(R.string.vend_x_cash, d.cashHeld.rupees()) else null)
        } }
        if (s.noDriver.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.vend_not_sent, s.noDriver.size)) }
            s.noDriver.forEach { r -> item(key = "n" + r.subscriptionId) { RunRow(r) } }
        }
        s.going.filter { it.status != "PENDING" }.groupBy { it.slot }.forEach { (slot, list) ->
            item(key = "slot$slot") { SectionLabel(slot) }
            list.forEach { r -> item(key = r.subscriptionId) { RunRow(r) } }
        }
        if (s.skipped.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.vend_skipped_today, s.skipped.size)) }
            s.skipped.forEach { r -> item(key = "s" + r.subscriptionId) { RunRow(r) } }
        }
    }
}

@Composable
private fun RunRow(r: VendorSubscriptionTodayRow) {
    BoondhCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.customerName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(stringResource(R.string.vend_rs_line, r.quantity, r.productName, r.slot), fontSize = 14.sp, color = NeerlyColors.Ink700)
                Text(r.deliveryAddress, fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
            Pill(stringResource(when (r.status) { "DELIVERED" -> R.string.vend_rs_done; "DISPATCHED" -> R.string.vend_rs_on_way; "SKIPPED" -> R.string.vend_rs_skipped; else -> R.string.vend_rs_no_driver }),
                when (r.status) { "DELIVERED" -> Tone.Ok; "DISPATCHED" -> Tone.Info; "SKIPPED" -> Tone.Neutral; else -> Tone.Bad })
        }
    }
}
