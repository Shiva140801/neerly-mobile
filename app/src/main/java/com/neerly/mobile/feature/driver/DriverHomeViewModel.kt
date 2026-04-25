package com.neerly.mobile.feature.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.CompleteDeliveryRequest
import com.neerly.mobile.data.dto.DriverAssignment
import com.neerly.mobile.data.dto.DriverShiftResponse
import com.neerly.mobile.data.repo.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Driver home — three states:
 *   off-duty  → big "Start shift" button
 *   on-duty, idle → "Available" badge + empty assignment list
 *   on-duty, with assignments → list of jobs, top one is the active one
 *
 * Tapping an assignment moves it through DISPATCHED → EN_ROUTE → ARRIVED →
 * DELIVERED. The driver app deliberately keeps UI minimal — large taps,
 * Telugu-friendly labels, low literacy support.
 */
@HiltViewModel
class DriverHomeViewModel @Inject constructor(
    private val repo: DriverRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DriverHomeUiState())
    val state: StateFlow<DriverHomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val shift = repo.currentShift()
                val jobs = if (shift?.endedAt == null && shift != null) repo.assignments() else emptyList()
                shift to jobs
            }
                .onSuccess { (shift, jobs) ->
                    _state.value = DriverHomeUiState(loading = false, shift = shift, assignments = jobs)
                }
                .onFailure { _state.value = DriverHomeUiState(loading = false, error = it.message) }
        }
    }

    fun startShift() {
        viewModelScope.launch {
            runCatching { repo.startShift() }
                .onSuccess { _state.value = _state.value.copy(shift = it); refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun endShift(codHandedOver: BigDecimal?) {
        val id = _state.value.shift?.id ?: return
        viewModelScope.launch {
            runCatching { repo.endShift(id, codHandedOver) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun startDelivery(orderId: String) {
        viewModelScope.launch {
            runCatching { repo.startDelivery(orderId) }.onSuccess { refresh() }
        }
    }

    fun markArrived(orderId: String) {
        viewModelScope.launch {
            runCatching { repo.markArrived(orderId) }.onSuccess { refresh() }
        }
    }

    fun completeDelivery(orderId: String, otp: String, photoS3Key: String, codCollected: BigDecimal?) {
        viewModelScope.launch {
            runCatching { repo.complete(orderId, otp, photoS3Key, codCollected) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}

data class DriverHomeUiState(
    val loading: Boolean = true,
    val shift: DriverShiftResponse? = null,
    val assignments: List<DriverAssignment> = emptyList(),
    val error: String? = null
) {
    val isOnDuty: Boolean get() = shift != null && shift.endedAt == null
    val activeAssignment: DriverAssignment? get() = assignments.firstOrNull {
        it.status in setOf("DISPATCHED", "EN_ROUTE", "ARRIVED")
    }
}
