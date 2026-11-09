package com.neerly.mobile.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/** What the on-duty card shows (NPDLock): drops done / total, time on duty, cash, next customer. */
data class DutyCard(
    val shopName: String?,
    val startedAtMs: Long?,
    val dropsDone: Int,
    val dropsTotal: Int,
    val cashInHand: BigDecimal,
    val nextCustomer: String?,
    val nextLat: Double? = null,
    val nextLng: Double? = null,
    val onBreak: Boolean = false
)

/**
 * Shared between the driver home screen (writes after every refresh) and the on-duty
 * foreground service (reads, updates the card silently). [onDuty] also gates task rings.
 */
@Singleton
class DriverDutyState @Inject constructor() {
    private val _card = MutableStateFlow<DutyCard?>(null)
    val card: StateFlow<DutyCard?> = _card.asStateFlow()

    private val _onDuty = MutableStateFlow<Boolean?>(null)
    val onDuty: StateFlow<Boolean?> = _onDuty.asStateFlow()

    fun update(onDuty: Boolean, card: DutyCard?) {
        _onDuty.value = onDuty
        _card.value = if (onDuty) card else null
    }

    fun markBreak(onBreak: Boolean) { _card.value = _card.value?.copy(onBreak = onBreak) }

    fun clear() { _onDuty.value = null; _card.value = null }
}
