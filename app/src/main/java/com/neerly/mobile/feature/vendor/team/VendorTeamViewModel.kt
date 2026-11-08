package com.neerly.mobile.feature.vendor.team

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Boards V17 (drivers, live), V34 (add a driver: look up the number first) and V35 (the
 * evening count of cash and empty jars a driver hands over).
 */
@HiltViewModel
class VendorTeamViewModel @Inject constructor(
    private val repo: VendorRepository,
    private val api: VendorTeamApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(VendorTeamUiState())
    val state: StateFlow<VendorTeamUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val team = async { api.team() }
                val cash = async { runCatching { api.cashToday() }.getOrDefault(emptyList()) }
                team.await() to cash.await()
            }.onSuccess { (team, cash) -> _state.value = _state.value.copy(loading = false, members = team, cash = cash) }
             .onFailure { _state.value = _state.value.copy(loading = false,
                 error = it.userMessage("vendor team", strings.get(R.string.vend_err_load_drivers))) }
        }
    }

    fun remove(driverId: String) {
        viewModelScope.launch {
            runCatching { repo.removeDriver(driverId) }
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage("remove driver", strings.get(R.string.vend_err_remove_driver))) }
        }
    }

    // ---- Add a driver (V34) ----

    fun startInvite() { _state.value = _state.value.copy(invite = InviteState()) }
    fun closeInvite() { _state.value = _state.value.copy(invite = null) }

    fun typePhone(raw: String) {
        val digits = raw.filter(Char::isDigit).takeLast(10)
        _state.value = _state.value.copy(invite = (_state.value.invite ?: InviteState()).copy(phone = digits, found = null, problem = null))
    }

    fun findDriver() {
        val inv = _state.value.invite ?: return
        if (!inv.phone.matches(Regex("^[6-9]\\d{9}$"))) {
            _state.value = _state.value.copy(invite = inv.copy(problem = strings.get(R.string.vend_type_driver_mobile)))
            return
        }
        _state.value = _state.value.copy(invite = inv.copy(looking = true, problem = null))
        viewModelScope.launch {
            runCatching { api.lookup(inv.phone) }
                .onSuccess { r -> _state.value = _state.value.copy(invite = _state.value.invite?.copy(looking = false, found = r)) }
                .onFailure { e -> _state.value = _state.value.copy(invite = _state.value.invite?.copy(looking = false,
                    problem = e.userMessage("driver lookup", strings.get(R.string.vend_err_check_number)))) }
        }
    }

    fun addFound(note: String?) {
        val inv = _state.value.invite ?: return
        if (inv.found?.exists != true || inv.found.alreadyOnTeam) return
        _state.value = _state.value.copy(invite = inv.copy(adding = true, problem = null))
        viewModelScope.launch {
            runCatching { repo.addDriver("+91${inv.phone}", note?.trim()?.ifBlank { null }) }
                .onSuccess { _state.value = _state.value.copy(invite = _state.value.invite?.copy(adding = false, added = true)); refresh() }
                .onFailure { e -> _state.value = _state.value.copy(invite = _state.value.invite?.copy(adding = false,
                    problem = e.userMessage("add driver", strings.get(R.string.vend_err_add_driver)))) }
        }
    }

    // ---- Evening count (V35) ----

    fun startCount(driverId: String) {
        val row = _state.value.cash.firstOrNull { it.driverId == driverId } ?: return
        _state.value = _state.value.copy(count = CountState(row,
            cash = row.confirmation?.cashReceived?.toPlainString() ?: "",
            empties = row.confirmation?.emptiesReceived ?: row.emptiesExpected))
    }
    fun closeCount() { _state.value = _state.value.copy(count = null) }
    fun typeCash(raw: String) { _state.value.count?.let { c ->
        _state.value = _state.value.copy(count = c.copy(cash = raw.filter { it.isDigit() || it == '.' }.take(9), problem = null)) } }
    fun empties(delta: Int) { _state.value.count?.let { c ->
        _state.value = _state.value.copy(count = c.copy(empties = (c.empties + delta).coerceAtLeast(0))) } }
    fun typeNote(raw: String) { _state.value.count?.let { c -> _state.value = _state.value.copy(count = c.copy(note = raw.take(300))) } }

    fun confirmCount() {
        val c = _state.value.count ?: return
        val amount = c.cash.toBigDecimalOrNull()
        if (amount == null || amount.signum() < 0) {
            _state.value = _state.value.copy(count = c.copy(problem = strings.get(R.string.vend_type_cash)))
            return
        }
        _state.value = _state.value.copy(count = c.copy(saving = true, problem = null))
        viewModelScope.launch {
            runCatching { api.confirmCash(c.row.driverId, CashConfirmRequest(amount, c.empties, c.note.trim().ifBlank { null })) }
                .onSuccess { r -> _state.value = _state.value.copy(count = _state.value.count?.copy(saving = false, result = r)); refresh() }
                .onFailure { e -> _state.value = _state.value.copy(count = _state.value.count?.copy(saving = false,
                    problem = e.userMessage("cash confirm", strings.get(R.string.vend_err_save_count)))) }
        }
    }
}

data class InviteState(
    val phone: String = "",
    val looking: Boolean = false,
    val found: DriverLookup? = null,
    val adding: Boolean = false,
    val added: Boolean = false,
    val problem: String? = null
)

data class CountState(
    val row: CashToday,
    val cash: String = "",
    val empties: Int = 0,
    val note: String = "",
    val saving: Boolean = false,
    val result: CashConfirmation? = null,
    val problem: String? = null
) {
    /** Live difference as the owner types: positive means less cash than the day's cash orders. */
    val cashShort: BigDecimal? get() = cash.toBigDecimalOrNull()?.let { row.cashExpected - it }
    val emptiesShort: Int get() = row.emptiesExpected - empties
}

data class VendorTeamUiState(
    val loading: Boolean = true,
    val members: List<TeamDriver> = emptyList(),
    val cash: List<CashToday> = emptyList(),
    val invite: InviteState? = null,
    val count: CountState? = null,
    val error: String? = null
)
