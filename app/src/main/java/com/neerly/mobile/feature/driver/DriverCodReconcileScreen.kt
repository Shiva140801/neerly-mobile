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
    /** Cash drops still with the driver — for "which one was short" (D24). */
    val cashDrops: List<CashHeldLine> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    /** DCash: the shop has counted and confirmed — the cash in hand dropped to zero. */
    val confirmed: Boolean = false,
    /** The shop's own figure once it has counted (DCashMismatch). */
    val handover: HandoverCount? = null,
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
                    // The delivered cash drops come from the server; active assignments never held them.
                    val cash = runCatching { platform.cashHeld() }.getOrDefault(emptyList())
                    _state.value = _state.value.copy(loading = false, collected = t.cashInHand, drops = cash.size, cashDrops = cash,
                        handover = t.lastHandover)
                }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("driver cash", strings.get(R.string.drv_err_load_cash))) }
        }
    }

    /** DCash: "Waiting for the shop to count…" until its confirmation clears the cash in hand. */
    private fun waitForShop() = viewModelScope.launch {
        repeat(120) {
            kotlinx.coroutines.delay(SHOP_COUNT_POLL_MS)
            val t = runCatching { ops.today() }.getOrNull() ?: return@repeat
            if (t.cashInHand.signum() == 0) { _state.value = _state.value.copy(confirmed = true, handover = t.lastHandover); return@launch }
        }
    }

    /** Collected is the server's own total; the driver only says what they handed over. */
    fun submit(handedOver: BigDecimal, notes: String?, shortLines: Map<String, BigDecimal> = emptyMap(),
               reasons: Map<String, String> = emptyMap()) {
        _state.value = _state.value.copy(saving = true, error = null)
        val lines = codLines(shortLines, reasons)
        viewModelScope.launch {
            runCatching {
                val r = platform.reconcile(ReconcileBody(_state.value.collected, handedOver, notes, lines))
                if (!r.isSuccessful) throw retrofit2.HttpException(r)
            }
                .onSuccess { _state.value = _state.value.copy(saving = false, saved = true); waitForShop() }
                .onFailure { _state.value = _state.value.copy(saving = false,
                    error = it.userMessage("driver cash", strings.get(R.string.drv_err_not_saved))) }
        }
    }
}

/** Seconds between checks while the shop counts. */
internal const val SHOP_COUNT_POLL_MS = 5_000L

/** DCashMismatch: one line per customer who paid less, with the reason the driver picked. */
internal fun codLines(shortLines: Map<String, BigDecimal>, reasons: Map<String, String>): List<CodLine>? =
    shortLines.map { (orderId, got) ->
        CodLine(orderId, got, "Customer paid less", reasons[orderId]?.takeIf { it in CASH_SHORT_REASONS })
    }.ifEmpty { null }

private val SHORT_REASON_LABELS = mapOf("PAYS_LATER" to R.string.drv_short_pays_later,
    "GAVE_LESS" to R.string.drv_short_gave_less, "DISPUTES_BILL" to R.string.drv_short_disputes)

/** D11 — close the day: give the shop the cash, record what you gave. The shop confirms it on its side. */
@Composable
fun DriverCodReconcileScreen(onBack: () -> Unit, onDone: () -> Unit, vm: DriverCodReconcileViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var handed by remember(s.collected) { mutableStateOf(s.collected.stripTrailingZeros().toPlainString()) }
    val amount = handed.toBigDecimalOrNull()
    var notes by remember { mutableStateOf("") }
    // D24 — per-customer: what each cash drop actually paid, when it wasn't the full amount.
    var paid by remember { mutableStateOf(mapOf<String, String>()) }
    var why by remember { mutableStateOf(mapOf<String, String>()) }
    val shortLines = paid.mapNotNull { (id, v) -> v.toBigDecimalOrNull()?.let { id to it } }
        .filter { (id, v) -> s.cashDrops.firstOrNull { it.orderId == id }?.collected?.let { v < it } == true }.toMap()
    val short = amount?.let { s.collected - it }?.takeIf { it.signum() > 0 }
    val explained = shortExplained(s.cashDrops.associate { it.orderId to it.collected }, shortLines)
    val reasonsMissing = shortLines.keys.any { it !in why }
    BoondhPage(title = when { s.confirmed -> stringResource(R.string.drv_cash_confirmed); s.saved -> stringResource(R.string.drv_cash_recorded)
            else -> stringResource(R.string.drv_cash_title) }, onBack = onBack, backLabel = stringResource(R.string.drv_money_title),
        subtitle = when { s.confirmed -> stringResource(R.string.drv_cash_confirmed_sub)
                   s.saved -> stringResource(R.string.drv_cash_waiting_shop, (amount ?: s.collected).rupees())
                   else -> stringResource(R.string.drv_cash_sub) },
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            if (s.saved) PrimaryButton(stringResource(R.string.drv_done), onDone, modifier = Modifier.heightIn(min = 60.dp))
            else PrimaryButton(if (s.saving) stringResource(R.string.drv_saving) else stringResource(R.string.drv_i_gave, amount?.rupees() ?: "—"),
                { amount?.let { vm.submit(it, notes.trim().ifBlank { null }, shortLines, why) } },
                enabled = amount != null && amount.signum() >= 0 && !s.saving && !s.loading && !reasonsMissing, modifier = Modifier.heightIn(min = 60.dp))
            if (reasonsMissing) Text(stringResource(R.string.drv_short_pick_reason), fontSize = 13.sp, color = NeerlyColors.Err)
        }) {
        if (s.loading) { item { LoadState(true, null, {}) }; return@BoondhPage }
        // DCashMismatch: what the shop itself counted, next to what it expected.
        if (s.confirmed || (s.collected.signum() == 0 && s.handover != null)) s.handover?.let { h -> item {
            BoondhCard {
                SectionLabel(h.shopName?.let { stringResource(R.string.drv_shop_counted_named, it) } ?: stringResource(R.string.drv_shop_counted))
                InfoRow(stringResource(R.string.drv_shop_expected), trailing = h.expected.rupees())
                InfoRow(stringResource(R.string.drv_shop_got), trailing = h.counted.rupees())
                if (h.short.signum() > 0) InfoRow(stringResource(R.string.drv_shop_short), trailing = h.short.rupees(), trailingColor = NeerlyColors.Err)
            }
        } }
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
                short?.takeIf { explained.signum() > 0 }?.let { gap -> item {
                    Text(stringResource(R.string.drv_short_explained, explained.rupees(), gap.rupees()), fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                } }
                s.cashDrops.forEach { d -> item(key = d.orderId) {
                    BoondhCard {
                        InfoRow(d.customerFirstName, d.deliveredAt?.let { "${d.orderNumber} · ${clock(it)}" } ?: d.orderNumber, d.collected.rupees())
                        OutlinedTextField(value = paid[d.orderId].orEmpty(),
                            onValueChange = { v -> paid = paid + (d.orderId to v.filter { it.isDigit() || it == '.' }.take(8)) },
                            singleLine = true, placeholder = { Text(stringResource(R.string.drv_paid_in_full)) }, label = { Text(stringResource(R.string.drv_they_paid)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        if (d.orderId in shortLines) {
                            Text(stringResource(R.string.drv_short_why), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SHORT_REASON_LABELS.forEach { (code, label) ->
                                    androidx.compose.material3.FilterChip(selected = why[d.orderId] == code,
                                        onClick = { why = why + (d.orderId to code) },
                                        label = { Text(stringResource(label), fontSize = 14.sp) }, modifier = Modifier.heightIn(min = 44.dp))
                                }
                            }
                        }
                    }
                } }
                if (shortLines.isNotEmpty()) item { Text(stringResource(R.string.drv_short_check_note), fontSize = 13.sp, color = NeerlyColors.Ink600) }
            }
            item {
                OutlinedTextField(value = notes, onValueChange = { notes = it.take(300) }, label = { Text(stringResource(R.string.drv_note_shop_optional)) },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
