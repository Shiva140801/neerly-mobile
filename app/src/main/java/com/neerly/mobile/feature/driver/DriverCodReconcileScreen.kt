package com.neerly.mobile.feature.driver

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.repo.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class CashHandOverState(
    val loading: Boolean = true,
    /** What the server counted from today's cash drops — the figure the shop will check. */
    val collected: BigDecimal = BigDecimal.ZERO,
    val drops: Int = 0,
    /** Today's delivered cash orders — for "which one was short" (D24). */
    val cashDrops: List<com.neerly.mobile.data.dto.DriverAssignment> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DriverCodReconcileViewModel @Inject constructor(
    private val repo: DriverRepository,
    private val ops: DriverOpsApi,
    private val platform: DriverPlatformApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(CashHandOverState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { ops.today() }
                .onSuccess { t ->
                    val cash = runCatching { repo.assignments() }.getOrDefault(emptyList())
                        .filter { it.paymentMethod == "COD" && it.status == "DELIVERED" }
                    _state.value = _state.value.copy(loading = false, collected = t.cashInHand, drops = t.drops, cashDrops = cash)
                }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("driver cash", strings.get(R.string.drv_err_load_cash))) }
        }
    }

    /** Collected is the server's own total; the driver only says what they handed over. */
    fun submit(handedOver: BigDecimal, notes: String?, shortLines: Map<String, BigDecimal> = emptyMap()) {
        _state.value = _state.value.copy(saving = true, error = null)
        val lines = shortLines.map { (orderId, got) -> CodLine(orderId, got, "Customer paid less") }.ifEmpty { null }
        viewModelScope.launch {
            runCatching {
                val r = platform.reconcile(ReconcileBody(_state.value.collected, handedOver, notes, lines))
                if (!r.isSuccessful) throw retrofit2.HttpException(r)
            }
                .onSuccess { _state.value = _state.value.copy(saving = false, saved = true) }
                .onFailure { _state.value = _state.value.copy(saving = false,
                    error = it.userMessage("driver cash", strings.get(R.string.drv_err_not_saved))) }
        }
    }
}

/** D11 — close the day: give the shop the cash, record what you gave. The shop confirms it on its side. */
@Composable
fun DriverCodReconcileScreen(onBack: () -> Unit, onDone: () -> Unit, vm: DriverCodReconcileViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var handed by remember(s.collected) { mutableStateOf(s.collected.stripTrailingZeros().toPlainString()) }
    val amount = handed.toBigDecimalOrNull()
    var notes by remember { mutableStateOf("") }
    // D24 — per-customer: what each cash drop actually paid, when it wasn't the full amount.
    var paid by remember { mutableStateOf(mapOf<String, String>()) }
    val shortLines = paid.mapNotNull { (id, v) -> v.toBigDecimalOrNull()?.let { id to it } }
        .filter { (id, v) -> s.cashDrops.firstOrNull { it.orderId == id }?.codAmount?.let { v < it } == true }.toMap()
    val short = amount?.let { s.collected - it }?.takeIf { it.signum() > 0 }
    BoondhPage(title = if (s.saved) stringResource(R.string.drv_cash_recorded) else stringResource(R.string.drv_cash_title), onBack = onBack, backLabel = stringResource(R.string.drv_money_title),
        subtitle = if (s.saved) stringResource(R.string.drv_cash_saved_sub)
                   else stringResource(R.string.drv_cash_sub),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            if (s.saved) PrimaryButton(stringResource(R.string.drv_done), onDone, modifier = Modifier.heightIn(min = 60.dp))
            else PrimaryButton(if (s.saving) stringResource(R.string.drv_saving) else stringResource(R.string.drv_i_gave, amount?.rupees() ?: "—"),
                { amount?.let { vm.submit(it, notes.trim().ifBlank { null }, shortLines) } },
                enabled = amount != null && amount.signum() >= 0 && !s.saving && !s.loading, modifier = Modifier.heightIn(min = 60.dp))
        }) {
        if (s.loading) { item { LoadState(true, null, {}) }; return@BoondhPage }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.drv_cash_from_drops), pluralStringResource(R.plurals.drv_drops_counted, s.drops, s.drops), s.collected.rupees())
            }
        }
        if (!s.saved) {
            item {
                Text(stringResource(R.string.drv_you_gave_shop), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = handed, onValueChange = { v -> handed = v.filter { it.isDigit() || it == '.' }.take(9) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp))
            }
            short?.let { item { Notice(stringResource(R.string.drv_less_than_collected, it.rupees()), Tone.Warn) } }
            if (s.cashDrops.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.drv_customer_paid_less)) }
                s.cashDrops.forEach { d -> item(key = d.orderId) {
                    BoondhCard {
                        InfoRow(d.customerFirstName, d.orderNumber, (d.codAmount ?: BigDecimal.ZERO).rupees())
                        OutlinedTextField(value = paid[d.orderId].orEmpty(),
                            onValueChange = { v -> paid = paid + (d.orderId to v.filter { it.isDigit() || it == '.' }.take(8)) },
                            singleLine = true, placeholder = { Text(stringResource(R.string.drv_paid_in_full)) }, label = { Text(stringResource(R.string.drv_they_paid)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                } }
            }
            item {
                OutlinedTextField(value = notes, onValueChange = { notes = it.take(300) }, label = { Text(stringResource(R.string.drv_note_shop_optional)) },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
