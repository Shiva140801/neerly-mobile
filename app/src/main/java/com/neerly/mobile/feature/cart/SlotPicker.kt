package com.neerly.mobile.feature.cart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.R
import com.neerly.mobile.core.design.BoondhPage
import com.neerly.mobile.core.design.LoadState
import com.neerly.mobile.core.design.Notice
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.PrimaryButton
import com.neerly.mobile.core.design.Tone
import com.neerly.mobile.core.design.timeWindow
import com.neerly.mobile.core.util.AppLocale
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.DiscoveryApi
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.cart.DeliverySlot
import com.neerly.mobile.data.dto.SlotDayDto
import com.neerly.mobile.data.dto.SlotOptionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Board SlotPicker: five days across the top, the day's windows below. Closed days and
 * windows stay visible but greyed with the reason ("Shop closed · Diwali", "Shop shuts
 * 1 PM"), so the customer sees why rather than wondering where Sunday evening went.
 */
@HiltViewModel
class SlotPickerViewModel @Inject constructor(
    private val api: DiscoveryApi,
    private val cart: CartStore,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(SlotPickerUiState(vendorName = cart.snapshot.vendorName))
    val state: StateFlow<SlotPickerUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val vendorId = cart.snapshot.vendorId ?: run {
            _state.value = _state.value.copy(loading = false, error = strings.get(R.string.cust_slot_need_shop)); return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.slotDays(vendorId, DAYS).days }
                .onSuccess { days ->
                    val current = cart.snapshot.slot
                    val dayIndex = SlotPickerLogic.initialDay(days, current)
                    val picked = current?.let { s ->
                        days.getOrNull(dayIndex)?.slots?.firstOrNull { SlotPickerLogic.time(it.start) == s.start && it.bookable }
                    }
                    _state.value = _state.value.copy(loading = false, days = days, dayIndex = dayIndex, picked = picked)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false,
                        error = it.userMessage(context = "slot picker", fallback = strings.get(R.string.cust_slot_load_failed)))
                }
        }
    }

    fun pickDay(index: Int) {
        if (index !in _state.value.days.indices) return
        _state.value = _state.value.copy(dayIndex = index, picked = null)
    }

    fun pickSlot(slot: SlotOptionDto) {
        if (!slot.bookable) return
        _state.value = _state.value.copy(picked = slot)
    }

    /** Puts the picked window on the cart; false when nothing bookable is picked. */
    fun confirm(): Boolean {
        val s = _state.value
        val day = s.days.getOrNull(s.dayIndex) ?: return false
        val slot = s.picked?.takeIf { it.bookable } ?: return false
        cart.setSlot(DeliverySlot(LocalDate.parse(day.date), SlotPickerLogic.time(slot.start), SlotPickerLogic.time(slot.end)))
        return true
    }

    companion object { const val DAYS = 5 }
}

data class SlotPickerUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val vendorName: String? = null,
    val days: List<SlotDayDto> = emptyList(),
    val dayIndex: Int = 0,
    val picked: SlotOptionDto? = null
) {
    val day: SlotDayDto? get() = days.getOrNull(dayIndex)
    val nothingBookable: Boolean get() = !loading && days.isNotEmpty() && days.none { d -> d.slots.any { it.bookable } }
}

/** The picker's rules, kept out of Compose so they can be tested. */
object SlotPickerLogic {
    fun time(s: String): LocalTime = LocalTime.parse(s.take(5))

    /** Reopen on the picked day; otherwise the first day with a window that can be booked. */
    fun initialDay(days: List<SlotDayDto>, current: DeliverySlot?): Int {
        current?.let { c -> days.indexOfFirst { it.date == c.date.toString() }.takeIf { it >= 0 }?.let { return it } }
        return days.indexOfFirst { d -> d.slots.any { it.bookable } }.coerceAtLeast(0)
    }

    enum class Part { EARLY, MORNING, AFTERNOON, EVENING }

    fun partOf(start: LocalTime): Part = when {
        start.hour < 9 -> Part.EARLY
        start.hour < 12 -> Part.MORNING
        start.hour < 17 -> Part.AFTERNOON
        else -> Part.EVENING
    }

    /** Why a closed window is closed. */
    sealed interface Note {
        data object Room : Note
        data object Filling : Note
        data object Full : Note
        data object Passed : Note
        data class ShopClosed(val label: String?) : Note
        data class Shuts(val at: LocalTime) : Note
        data class Opens(val at: LocalTime) : Note
    }

    fun noteOf(day: SlotDayDto, slot: SlotOptionDto): Note = when (slot.state) {
        "OPEN" -> Note.Room
        "FILLING" -> Note.Filling
        "FULL" -> Note.Full
        "PASSED" -> Note.Passed
        else -> {
            val closes = day.closesAt?.let(::time)
            val opens = day.opensAt?.let(::time)
            when {
                !day.open -> Note.ShopClosed(day.label)
                closes != null && time(slot.end) > closes -> Note.Shuts(closes)
                opens != null && time(slot.start) < opens -> Note.Opens(opens)
                else -> Note.ShopClosed(null)
            }
        }
    }

    /** "till 1 PM" on a day the shop closes before the evening window. */
    fun shortDayClose(day: SlotDayDto): LocalTime? =
        day.closesAt?.let(::time)?.takeIf { day.open && it.isBefore(LocalTime.of(17, 0)) }
}

@Composable
fun SlotPickerScreen(onBack: () -> Unit, onDone: () -> Unit, vm: SlotPickerViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val day = s.day
    val hourFmt = DateTimeFormatter.ofPattern("h a", AppLocale.locale)
    val weekday = DateTimeFormatter.ofPattern("EEE", AppLocale.locale)
    val dayMonth = DateTimeFormatter.ofPattern("EEE d MMM", AppLocale.locale)
    val today = stringResource(R.string.cust_slot_today)
    fun dayName(i: Int, d: SlotDayDto) = if (i == 0) today else LocalDate.parse(d.date).format(dayMonth)

    BoondhPage(
        title = stringResource(R.string.cust_slot_title),
        onBack = onBack,
        subtitle = s.vendorName?.let { stringResource(R.string.cust_slot_subtitle, it) },
        footer = if (s.days.isEmpty()) null else ({
            val picked = s.picked
            PrimaryButton(
                if (picked != null && day != null) stringResource(R.string.cust_slot_deliver, dayName(s.dayIndex, day), timeWindow(picked.start, picked.end))
                else stringResource(R.string.cust_slot_pick),
                { if (vm.confirm()) onDone() },
                enabled = picked != null
            )
        })
    ) {
        if (s.loading || s.error != null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(s.days) { i, d ->
                    val on = i == s.dayIndex
                    val closed = !d.open
                    val date = LocalDate.parse(d.date)
                    val tag = when {
                        closed -> stringResource(R.string.cust_slot_tag_closed)
                        else -> SlotPickerLogic.shortDayClose(d)?.let { stringResource(R.string.cust_slot_tag_till, it.format(hourFmt)) } ?: ""
                    }
                    Column(
                        Modifier.widthIn(min = 64.dp).heightIn(min = 72.dp)
                            .background(if (on) NeerlyColors.Ink900 else if (closed) NeerlyColors.Ink100 else NeerlyColors.Paper, RoundedCornerShape(14.dp))
                            .then(if (on) Modifier else Modifier.border(BorderStroke(1.dp, NeerlyColors.Ink200), RoundedCornerShape(14.dp)))
                            .clickable(role = Role.Tab) { vm.pickDay(i) }
                            .semantics { selected = on; contentDescription = listOf(dayName(i, d), tag).filter { it.isNotBlank() }.joinToString(", ") }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val fg = if (on) NeerlyColors.Paper else NeerlyColors.Ink900
                        Text(if (i == 0) today else date.format(weekday), fontSize = 13.sp, color = fg)
                        Text(date.dayOfMonth.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = fg)
                        Text(tag, fontSize = 11.sp, color = when { on -> NeerlyColors.Ink200; closed -> NeerlyColors.Err; else -> NeerlyColors.Ink600 })
                    }
                }
            }
        }
        if (s.nothingBookable) item { Notice(stringResource(R.string.cust_slot_none), Tone.Warn) }
        day?.slots?.forEach { slot ->
            item(key = "${day.date}-${slot.start}") {
                val note = when (val n = SlotPickerLogic.noteOf(day, slot)) {
                    SlotPickerLogic.Note.Room -> stringResource(R.string.cust_slot_note_room)
                    SlotPickerLogic.Note.Filling -> stringResource(R.string.cust_slot_note_filling)
                    SlotPickerLogic.Note.Full -> stringResource(R.string.cust_slot_note_full)
                    SlotPickerLogic.Note.Passed -> stringResource(R.string.cust_slot_note_passed)
                    is SlotPickerLogic.Note.ShopClosed -> n.label?.let { stringResource(R.string.cust_slot_note_closed_for, it) } ?: stringResource(R.string.cust_slot_note_closed)
                    is SlotPickerLogic.Note.Shuts -> stringResource(R.string.cust_slot_note_shuts, n.at.format(hourFmt))
                    is SlotPickerLogic.Note.Opens -> stringResource(R.string.cust_slot_note_opens, n.at.format(hourFmt))
                }
                val part = when (SlotPickerLogic.partOf(SlotPickerLogic.time(slot.start))) {
                    SlotPickerLogic.Part.EARLY -> stringResource(R.string.cust_slot_part_early)
                    SlotPickerLogic.Part.MORNING -> stringResource(R.string.cust_slot_part_morning)
                    SlotPickerLogic.Part.AFTERNOON -> stringResource(R.string.cust_slot_part_afternoon)
                    SlotPickerLogic.Part.EVENING -> stringResource(R.string.cust_slot_part_evening)
                }
                val on = s.picked == slot
                val window = timeWindow(slot.start, slot.end)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .background(if (slot.bookable) NeerlyColors.Paper else NeerlyColors.Ink100, RoundedCornerShape(14.dp))
                        .border(BorderStroke(if (on) 2.dp else 1.dp, if (on) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200), RoundedCornerShape(14.dp))
                        .clickable(enabled = slot.bookable, role = Role.RadioButton) { vm.pickSlot(slot) }
                        .semantics { selected = on; contentDescription = "$window, $part, $note" }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(window, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            color = if (slot.bookable) NeerlyColors.Ink900 else NeerlyColors.Ink600)
                        Text(part, fontSize = 13.sp, color = NeerlyColors.Ink600)
                    }
                    Text(note, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = when (slot.state) {
                        "OPEN" -> NeerlyColors.Ok; "FILLING" -> NeerlyColors.Warn; else -> NeerlyColors.Ink600 })
                }
            }
        }
        // "Closed Mon 9 and Tue 10 for Diwali." — the days the shop told us it's shut.
        val closed = s.days.filter { !it.open }
        if (closed.isNotEmpty()) item {
            val list = closed.joinToString(", ") { LocalDate.parse(it.date).format(dayMonth) }
            val label = closed.firstNotNullOfOrNull { it.label }
            Text(if (label != null) stringResource(R.string.cust_slot_closed_days_for, list, label) else stringResource(R.string.cust_slot_closed_days, list),
                fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
