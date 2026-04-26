package com.neerly.mobile.feature.vendor.settings

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
    private val repo: VendorRepository
) : ViewModel() {

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
                    _state.value = VendorBusinessConfigUiState(
                        loading = false,
                        status = status,
                        hours = hours.sortedBy { it.dayOfWeek },
                        holidays = holidays
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }

    fun saveHours(rows: List<VendorHoursRow>) {
        viewModelScope.launch {
            runCatching { repo.replaceHours(rows) }
                .onSuccess { saved -> _state.value = _state.value.copy(hours = saved.sortedBy { it.dayOfWeek }) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun addHoliday(date: String, label: String?) {
        viewModelScope.launch {
            runCatching { repo.addHoliday(date, label) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun removeHoliday(date: String) {
        viewModelScope.launch {
            runCatching { repo.removeHoliday(date) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun emergencyClose(hours: Int, reason: String?) {
        viewModelScope.launch {
            runCatching { repo.emergencyClose(hours, reason) }
                .onSuccess { _state.value = _state.value.copy(status = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun reopen() {
        viewModelScope.launch {
            runCatching { repo.reopen() }
                .onSuccess { _state.value = _state.value.copy(status = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}

data class VendorBusinessConfigUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val status: VendorBusinessStatus? = null,
    val hours: List<VendorHoursRow> = emptyList(),
    val holidays: List<VendorHolidayRow> = emptyList()
)
