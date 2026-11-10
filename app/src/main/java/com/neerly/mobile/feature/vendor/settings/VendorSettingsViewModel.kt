package com.neerly.mobile.feature.vendor.settings

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.VendorBankAccountResponse
import com.neerly.mobile.data.dto.VendorBusinessStatus
import com.neerly.mobile.data.dto.VendorHolidayRow
import com.neerly.mobile.data.dto.VendorHoursRow
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.vendor.compliance.DocRow
import com.neerly.mobile.feature.vendor.compliance.VendorDocsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Board V18 — each settings row shows its live state, not just a label. */
data class VendorSettingsUiState(
    val loading: Boolean = true,
    val status: VendorBusinessStatus? = null,
    val holidays: List<VendorHolidayRow> = emptyList(),
    val hours: List<VendorHoursRow> = emptyList(),
    val pincodes: Int = 0,
    val bank: VendorBankAccountResponse? = null,
    val docs: List<DocRow> = emptyList(),
    val drivers: Int? = null,
    val working: Boolean = false,
    val error: String? = null
) {
    val nextHoliday: VendorHolidayRow? get() {
        val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()
        return holidays.filter { it.date >= today }.minByOrNull { it.date }
    }
    /** The licence closest to lapsing, among the latest copy of each document type. */
    val soonestRenewal: DocRow? get() = docs.groupBy { it.docType }.values.map { v -> v.maxBy { it.uploadedAt } }
        .filter { it.expiresInDays != null && it.status in setOf("APPROVED", "EXPIRED") }.minByOrNull { it.expiresInDays!! }
}

@HiltViewModel
class VendorSettingsViewModel @Inject constructor(
    private val repo: VendorRepository,
    private val docsApi: VendorDocsApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(VendorSettingsUiState())
    val state: StateFlow<VendorSettingsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val status = async { runCatching { repo.businessStatus() }.getOrNull() }
            val holidays = async { runCatching { repo.holidays() }.getOrDefault(emptyList()) }
            val hours = async { runCatching { repo.hours() }.getOrDefault(emptyList()) }
            val me = async { runCatching { repo.me() }.getOrNull() }
            val bank = async { runCatching { repo.bank() }.getOrNull() }
            val docs = async { runCatching { docsApi.docs() }.getOrDefault(emptyList()) }
            val team = async { runCatching { repo.team().size }.getOrNull() }
            _state.value = _state.value.copy(loading = false, status = status.await(), holidays = holidays.await(),
                hours = hours.await().sortedBy { it.dayOfWeek }, pincodes = me.await()?.servicedPincodes?.size ?: 0,
                bank = bank.await(), docs = docs.await(), drivers = team.await())
        }
    }

    fun emergencyClose(hours: Int, reason: String?) = act { repo.emergencyClose(hours, reason) }
    fun pause(choice: com.neerly.mobile.feature.vendor.dashboard.PauseChoice, reason: String?) = act { repo.pause(choice, reason) }
    fun reopen() = act { repo.reopen() }

    private fun act(call: suspend () -> VendorBusinessStatus) {
        if (_state.value.working) return
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { _state.value = _state.value.copy(working = false, status = it) }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage("vendor open/close", strings.get(R.string.vend_err_change))) }
        }
    }

    /** Legacy one-arg call (24 h). */
    fun emergencyClose(reason: String?) = emergencyClose(24, reason)
}

/** "Mon–Sat 6 AM–9 PM · Sun 6 AM–1 PM" from the weekly rows (dayOfWeek 1 = Monday). */
fun hoursSummary(rows: List<VendorHoursRow>, locale: java.util.Locale = java.util.Locale.ENGLISH, notSet: String = "Hours not set"): String {
    if (rows.isEmpty()) return notSet
    val names = java.time.DayOfWeek.entries.map { it.getDisplayName(java.time.format.TextStyle.SHORT, locale) }
    fun t(s: String) = runCatching {
        java.time.LocalTime.parse(s).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", locale)).replace(":00", "")
    }.getOrDefault(s)
    val groups = mutableListOf<Triple<Int, Int, String>>()
    rows.sortedBy { it.dayOfWeek }.forEach { r ->
        val span = "${t(r.openTime)}–${t(r.closeTime)}"
        val last = groups.lastOrNull()
        if (last != null && last.third == span && last.second == r.dayOfWeek - 1) groups[groups.lastIndex] = Triple(last.first, r.dayOfWeek, span)
        else groups += Triple(r.dayOfWeek, r.dayOfWeek, span)
    }
    return groups.joinToString(" · ") { (a, b, span) ->
        val da = names.getOrElse(a - 1) { "?" }; val db = names.getOrElse(b - 1) { "?" }
        (if (a == b) da else "$da–$db") + " " + span
    }
}
