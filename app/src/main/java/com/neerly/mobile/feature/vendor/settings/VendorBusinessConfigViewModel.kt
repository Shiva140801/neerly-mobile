package com.neerly.mobile.feature.vendor.settings

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.VendorBusinessStatus
import com.neerly.mobile.data.dto.VendorHolidayRow
import com.neerly.mobile.data.dto.VendorHoursRow
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.neerly.mobile.core.util.userMessage
import javax.inject.Inject

/**
 * Backs the Vendor → Settings → Business config screen.
 *
 * Three resource sets: hours (per-DOW open/close), holidays (single dates), and
 * the live status (whether emergency-close is in effect). All flow through
 * VendorRepository → /api/v1/vendor/business endpoints.
 */
@HiltViewModel
class VendorBusinessConfigViewModel @Inject constructor(
    private val repo: VendorRepository,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    /** Dates in the app's language — the locale tag is itself a translated string. */
    private val locale get() = java.util.Locale.forLanguageTag(strings.get(R.string.vend_date_locale))

    private val _state = MutableStateFlow(VendorBusinessConfigUiState())
    val state: StateFlow<VendorBusinessConfigUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val s = async { runCatching { repo.businessStatus() }.getOrNull() }
                val h = async { runCatching { repo.hours() }.getOrDefault(emptyList()) }
                val d = async { runCatching { repo.holidays() }.getOrDefault(emptyList()) }
                Triple(s.await(), h.await(), d.await())
            }
                .onSuccess { (status, hours, holidays) ->
                    _state.value = _state.value.copy(
                        loading = false, error = null,
                        status = status,
                        hours = hours.sortedBy { it.dayOfWeek },
                        holidays = holidays
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false,
                        error = it.userMessage("vendor business config", strings.get(R.string.vend_err_load_hours)))
                }
        }
    }

    fun saveHours(rows: List<VendorHoursRow>) {
        rows.firstOrNull { !validSpan(it.openTime, it.closeTime) }?.let {
            _state.value = _state.value.copy(error = strings.get(R.string.vend_close_after_open, dayName(it.dayOfWeek, locale)))
            return
        }
        if (rows.isEmpty()) { _state.value = _state.value.copy(error = strings.get(R.string.vend_open_one_day)); return }
        _state.value = _state.value.copy(saving = true, error = null, saved = null)
        viewModelScope.launch {
            runCatching { repo.replaceHours(rows) }
                .onSuccess { saved -> _state.value = _state.value.copy(saving = false, hours = saved.sortedBy { it.dayOfWeek }, saved = strings.get(R.string.vend_hours_saved)) }
                .onFailure { _state.value = _state.value.copy(saving = false,
                    error = it.userMessage("vendor hours", strings.get(R.string.vend_err_save_hours))) }
        }
    }

    /**
     * Close for every day from [from] to [to] (board V30). Each day is its own holiday on the
     * server; subscription jars due on those days are skipped and those customers told.
     */
    fun addClosure(from: java.time.LocalDate, to: java.time.LocalDate, label: String?) {
        if (to.isBefore(from)) { _state.value = _state.value.copy(error = strings.get(R.string.vend_last_before_first)); return }
        val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
        if (days.size > 60) { _state.value = _state.value.copy(error = strings.get(R.string.vend_over_60)); return }
        _state.value = _state.value.copy(saving = true, error = null, saved = null)
        viewModelScope.launch {
            val existing = _state.value.holidays.map { it.date }.toSet()
            val failed = days.filter { it.toString() !in existing }.mapNotNull { d ->
                runCatching { repo.addHoliday(d.toString(), label?.trim()?.ifBlank { null }) }.exceptionOrNull()?.let { d }
            }
            _state.value = _state.value.copy(saving = false,
                error = if (failed.isEmpty()) null else strings.get(R.string.vend_couldnt_save_days, failed.size),
                saved = if (failed.isEmpty()) (if (days.size == 1) strings.get(R.string.vend_closed_one_day_saved, shortDate(to.plusDays(1), locale))
                    else strings.get(R.string.vend_closed_days_saved, days.size, shortDate(to.plusDays(1), locale))) else null)
            refresh()
        }
    }

    fun addHoliday(date: String, label: String?) {
        viewModelScope.launch {
            runCatching { repo.addHoliday(date, label) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage("vendor holiday", strings.get(R.string.vend_err_add_day))) }
        }
    }

    fun removeHoliday(date: String) {
        viewModelScope.launch {
            runCatching { repo.removeHoliday(date) }
                .onSuccess {
                    // The server doesn't bring back subscription jars it already skipped for this day.
                    _state.value = _state.value.copy(saved = strings.get(R.string.vend_open_again_on, shortDate(java.time.LocalDate.parse(date), locale)))
                    refresh()
                }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage("vendor holiday", strings.get(R.string.vend_err_remove_day))) }
        }
    }

    fun emergencyClose(hours: Int, reason: String?) {
        viewModelScope.launch {
            runCatching { repo.emergencyClose(hours, reason) }
                .onSuccess { _state.value = _state.value.copy(status = it) }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage("vendor close", strings.get(R.string.vend_err_close_shop))) }
        }
    }

    fun pause(choice: com.neerly.mobile.feature.vendor.dashboard.PauseChoice, reason: String?) {
        viewModelScope.launch {
            runCatching { repo.pause(choice, reason) }
                .onSuccess { _state.value = _state.value.copy(status = it) }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage("vendor close", strings.get(R.string.vend_err_close_shop))) }
        }
    }

    fun reopen() {
        viewModelScope.launch {
            runCatching { repo.reopen() }
                .onSuccess { _state.value = _state.value.copy(status = it) }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage("vendor reopen", strings.get(R.string.vend_err_reopen_shop))) }
        }
    }

    fun dismissSaved() { _state.value = _state.value.copy(saved = null) }
}

/** A weekday's name (1 = Monday) in [locale]. */
internal fun dayName(dayOfWeek: Int, locale: java.util.Locale, style: java.time.format.TextStyle = java.time.format.TextStyle.FULL): String =
    java.time.DayOfWeek.of(dayOfWeek).getDisplayName(style, locale)

internal fun shortDate(d: java.time.LocalDate, locale: java.util.Locale = java.util.Locale.ENGLISH): String =
    d.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", locale))

internal fun validSpan(open: String, close: String): Boolean = runCatching {
    java.time.LocalTime.parse(close.take(5)).isAfter(java.time.LocalTime.parse(open.take(5)))
}.getOrDefault(false)

data class VendorBusinessConfigUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val status: VendorBusinessStatus? = null,
    val hours: List<VendorHoursRow> = emptyList(),
    val holidays: List<VendorHolidayRow> = emptyList(),
    val saving: Boolean = false,
    val saved: String? = null
)
