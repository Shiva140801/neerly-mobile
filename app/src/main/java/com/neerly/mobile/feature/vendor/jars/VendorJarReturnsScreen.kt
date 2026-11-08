package com.neerly.mobile.feature.vendor.jars

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.NeerlyApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

private val IST: ZoneId = ZoneId.of("Asia/Kolkata")

/** Pickup windows a shop offers; the start hour is what's sent. */
internal val PICKUP_SLOTS = listOf(8 to R.string.vend_slot_8, 12 to R.string.vend_slot_12, 16 to R.string.vend_slot_16)

data class Driver(val id: String, val name: String)

data class PlanDraft(val day: LocalDate, val slotHour: Int?, val driverId: String?)

data class JarReturnsUiState(
    val loading: Boolean = true,
    val open: List<JarReturn> = emptyList(),
    val collected: List<JarReturn> = emptyList(),
    val drivers: List<Driver> = emptyList(),
    val planning: String? = null,
    val draft: PlanDraft? = null,
    val working: String? = null,
    val error: String? = null
)

@HiltViewModel
class VendorJarReturnsViewModel @Inject constructor(
    private val jars: VendorJarsApi,
    private val api: NeerlyApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    internal var clock: Clock = Clock.system(IST)
    private val _state = MutableStateFlow(JarReturnsUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val open = async { jars.returns() }
                    val done = async { runCatching { jars.returns(listOf("COMPLETED"), 20) }.getOrDefault(emptyList()) }
                    val team = async { runCatching { api.vendorTeam() }.getOrDefault(emptyList()) }
                    Triple(open.await(), done.await(), team.await().map { Driver(it.driverId, it.name ?: strings.get(R.string.vend_driver)) })
                }
            }.onSuccess { (o, d, t) -> _state.value = _state.value.copy(loading = false, open = o, collected = d, drivers = t) }
             .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage("vendor returns", strings.get(R.string.vend_err_load_pickups))) }
        }
    }

    fun now(): ZonedDateTime = ZonedDateTime.now(clock)

    /** Days offered: today (if a window is still ahead), tomorrow, the day after. */
    fun days(): List<LocalDate> {
        val n = now()
        val today = n.toLocalDate()
        val todayOpen = PICKUP_SLOTS.any { it.first > n.hour }
        return listOfNotNull(today.takeIf { todayOpen }, today.plusDays(1), today.plusDays(2))
    }

    fun slotOpen(day: LocalDate, hour: Int): Boolean = day.isAfter(now().toLocalDate()) || hour > now().hour

    fun startPlanning(r: JarReturn) {
        _state.value = _state.value.copy(planning = r.id, draft = PlanDraft(days().first(), null, _state.value.drivers.singleOrNull()?.id))
    }
    fun cancelPlanning() { _state.value = _state.value.copy(planning = null, draft = null) }
    fun edit(f: PlanDraft.() -> PlanDraft) { _state.value.draft?.let { _state.value = _state.value.copy(draft = it.f()) } }

    fun scheduledFor(d: PlanDraft): String? = d.slotHour?.takeIf { slotOpen(d.day, it) }
        ?.let { d.day.atTime(it, 0).atZone(IST).toInstant().toString() }

    fun schedule() {
        val s = _state.value; val id = s.planning ?: return; val at = s.draft?.let(::scheduledFor) ?: return
        act(id) { jars.schedule(id, ScheduleReturn(at, s.draft.driverId)) }
    }

    fun collected(r: JarReturn) = act(r.id) { jars.complete(r.id, CompleteReturn()) }

    private fun act(id: String, call: suspend () -> JarReturn) {
        _state.value = _state.value.copy(working = id, error = null)
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { r ->
                    val s = _state.value
                    _state.value = if (r.status == "COMPLETED") s.copy(working = null, planning = null, draft = null,
                        open = s.open.filterNot { it.id == r.id }, collected = listOf(r) + s.collected)
                    else s.copy(working = null, planning = null, draft = null, open = s.open.map { if (it.id == r.id) r else it })
                }
                .onFailure { _state.value = _state.value.copy(working = null, error = it.userMessage("vendor return", strings.get(R.string.vend_err_update_pickup))) }
        }
    }
}

/** Board V27 — jars customers asked the shop to collect, plus jars dropped at the shop. */
@Composable
fun VendorJarReturnsScreen(
    onBack: () -> Unit,
    onFileClaim: (returnId: String) -> Unit = {},
    vm: VendorJarReturnsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    OnReturn(vm::load)
    BoondhPage(title = stringResource(R.string.vend_jar_pickups), onBack = onBack, backLabel = stringResource(R.string.vend_tab_jars), subtitle = stringResource(R.string.vend_jar_pickups_sub)) {
        s.error?.let { item { Notice(it, Tone.Bad) } }
        if (s.open.isEmpty()) item { LoadState(s.loading, null, vm::load, stringResource(R.string.vend_no_pickups), stringResource(R.string.vend_no_pickups_sub)) }
        items(s.open, key = { it.id }) { r ->
            val pickup = r.returnMode == "SCHEDULED_PICKUP"
            BoondhCard {
                InfoRow(listOfNotNull(pluralStringResource(R.plurals.vend_n_jars, r.containerCount, r.containerCount), r.customerName).joinToString(" · "),
                    if (pickup) (r.addressLine ?: stringResource(R.string.vend_collect_from_customer)) else stringResource(R.string.vend_drops_at_shop))
                if (pickup && r.lat != null && r.lng != null) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val pickupLabel = stringResource(R.string.vend_pickup)
                    TextButton(onClick = { com.neerly.mobile.feature.driver.openMaps(ctx, r.lat.toDouble(), r.lng.toDouble(), r.customerName ?: pickupLabel) },
                        modifier = Modifier.heightIn(min = 44.dp)) {
                        Text(stringResource(R.string.vend_open_maps), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
                }
                when (r.status) {
                    "REQUESTED" -> Pill(stringResource(if (pickup) R.string.vend_needs_time else R.string.vend_waiting_dropoff), Tone.Warn)
                    "SCHEDULED" -> Pill(stringResource(R.string.vend_booked_at, r.scheduledFor?.let { shortDay(it) + " " + timeOf(it) } ?: ""), Tone.Info)
                    "EN_ROUTE" -> Pill(stringResource(R.string.vend_driver_on_way), Tone.Info)
                    else -> Pill(r.status.lowercase(), Tone.Neutral)
                }
                if (s.planning == r.id) s.draft?.let { d -> PlanPicker(vm, d, s.drivers) }
                else if (pickup && r.status == "REQUESTED")
                    SecondaryButton(stringResource(R.string.vend_plan_pickup), { vm.startPlanning(r) }, enabled = s.working == null, color = NeerlyColors.VendorPrimary)
                if (s.planning != r.id && r.status != "REQUESTED" || !pickup)
                    PrimaryButton(stringResource(if (s.working == r.id) R.string.vend_saving else if (pickup) R.string.vend_mark_collected else R.string.vend_mark_received),
                        { vm.collected(r) }, enabled = s.working == null, color = NeerlyColors.VendorPrimary)
            }
        }
        if (s.collected.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.vend_collected)) }
            items(s.collected.take(10), key = { "c" + it.id }) { r ->
                BoondhCard {
                    InfoRow(pluralStringResource(R.plurals.vend_n_jars_back, r.containerCount, r.containerCount), r.completedAt?.let { stringResource(R.string.vend_back_on_day, shortDay(it)) })
                    // A claim needs the deposit and at least one photo — the driver's pickup photo.
                    if (r.depositId != null && r.pickupPhotoUrl != null)
                        TextButton(onClick = { onFileClaim(r.id) }, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.vend_damaged_claim_link), color = NeerlyColors.VendorPrimary, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun PlanPicker(vm: VendorJarReturnsViewModel, d: PlanDraft, drivers: List<Driver>) {
    Text(stringResource(R.string.vend_day), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.days().forEach { day -> FilterChip(selected = d.day == day, onClick = { vm.edit { copy(day = day, slotHour = null) } },
            label = { Text(shortDay(day.toString())) }, modifier = Modifier.heightIn(min = 44.dp)) }
    }
    Text(stringResource(R.string.vend_slot), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PICKUP_SLOTS.forEach { (h, label) -> FilterChip(selected = d.slotHour == h, enabled = vm.slotOpen(d.day, h),
            onClick = { vm.edit { copy(slotHour = h) } }, label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = 44.dp)) }
    }
    if (drivers.isNotEmpty()) {
        Text(stringResource(R.string.vend_driver), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            drivers.forEach { dr -> FilterChip(selected = d.driverId == dr.id, onClick = { vm.edit { copy(driverId = if (driverId == dr.id) null else dr.id) } },
                label = { Text(dr.name.substringBefore(' ')) }, modifier = Modifier.heightIn(min = 44.dp)) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(stringResource(R.string.vend_cancel), vm::cancelPlanning, modifier = Modifier.weight(1f), color = NeerlyColors.VendorPrimary)
        PrimaryButton(stringResource(R.string.vend_book_pickup), vm::schedule, enabled = vm.scheduledFor(d) != null, modifier = Modifier.weight(1f), color = NeerlyColors.VendorPrimary)
    }
}

internal fun timeOf(iso: String): String = runCatching {
    java.time.Instant.parse(iso).atZone(IST).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
}.getOrDefault("")
