package com.neerly.mobile.feature.vendor.jars

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.feature.vendor.catalog.VendorCatalogApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What happened to the jars — maps to the backend's manual change types. */
enum class StockReason(val code: String, @androidx.annotation.StringRes val label: Int, @androidx.annotation.StringRes val detail: Int) {
    RECOUNT("RECOUNT_ADJUST", R.string.vend_sr_recount, R.string.vend_sr_recount_sub),
    ADD("STOCK_ADD", R.string.vend_sr_add, R.string.vend_sr_add_sub),
    REMOVE("STOCK_REMOVE", R.string.vend_sr_remove, R.string.vend_sr_remove_sub),
    DAMAGED("MARK_DAMAGED", R.string.vend_sr_damaged, R.string.vend_sr_damaged_sub),
    LOST("MARK_LOST", R.string.vend_sr_lost, R.string.vend_sr_lost_sub)
}

data class StockAdjustUiState(
    val loading: Boolean = true,
    val lines: List<JarLine> = emptyList(),
    val pickedId: String? = null,
    val reason: StockReason = StockReason.RECOUNT,
    val value: Int = 0,
    val note: String = "",
    val saving: Boolean = false,
    val done: String? = null,
    val error: String? = null
) {
    val picked: JarLine? get() = lines.firstOrNull { it.stock.productId == pickedId }
    /** The most the chosen change can move — the server refuses more. */
    val max: Int get() = when (reason) {
        StockReason.REMOVE, StockReason.DAMAGED -> picked?.stock?.totalInStock ?: 0
        StockReason.LOST -> picked?.stock?.outWithCustomers ?: 0
        else -> 9999
    }
    val min: Int get() = if (reason == StockReason.RECOUNT) 0 else 1
    /** "Say why" on the board: a note is required for anything that isn't a plain delivery of new jars. */
    val valid: Boolean get() = picked != null && value in min..max && (reason == StockReason.ADD || note.isNotBlank()) &&
        !(reason == StockReason.RECOUNT && value == picked?.stock?.totalInStock)
}

@HiltViewModel
class VendorStockAdjustViewModel @Inject constructor(
    private val jars: VendorJarsApi,
    private val catalog: VendorCatalogApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(StockAdjustUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            runCatching { jarLines(jars, catalog, strings.get(R.string.vend_jar)) }
                .onSuccess { l -> _state.value = _state.value.copy(loading = false, lines = l,
                    pickedId = _state.value.pickedId ?: l.firstOrNull()?.stock?.productId).resetValue() }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage("vendor stock", strings.get(R.string.vend_err_load_jars))) }
        }
    }

    private fun StockAdjustUiState.resetValue() = copy(value = if (reason == StockReason.RECOUNT) picked?.stock?.totalInStock ?: 0 else 1)

    fun pick(productId: String) { _state.value = _state.value.copy(pickedId = productId, done = null).resetValue() }
    fun reason(r: StockReason) { _state.value = _state.value.copy(reason = r, done = null).resetValue() }
    fun step(by: Int) { val s = _state.value; _state.value = s.copy(value = (s.value + by).coerceIn(s.min, s.max), done = null) }
    fun note(v: String) { _state.value = _state.value.copy(note = v.take(500)) }

    fun save() {
        val s = _state.value
        if (!s.valid) return
        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching { jars.adjust(AdjustStock(s.pickedId!!, s.value, s.reason.code, s.note.trim().ifBlank { null })) }
                .onSuccess { snap ->
                    val lines = _state.value.lines.map { if (it.stock.productId == snap.productId) it.copy(stock = snap) else it }
                    _state.value = _state.value.copy(saving = false, lines = lines, note = "",
                        done = strings.get(R.string.vend_jar_log_saved, snap.totalInStock)).resetValue()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.userMessage("vendor stock adjust", strings.get(R.string.vend_err_save_change))) }
        }
    }
}

/** Board V28 — pick the jar type, change a number, say why. */
@Composable
fun VendorStockAdjustScreen(onBack: () -> Unit, onFileClaim: () -> Unit = {}, vm: VendorStockAdjustViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    BoondhPage(
        title = stringResource(R.string.vend_count_the_shop), onBack = onBack, backLabel = stringResource(R.string.vend_tab_jars),
        subtitle = stringResource(R.string.vend_count_sub),
        footer = if (s.lines.isEmpty()) null else ({
            s.error?.let { Notice(it, Tone.Bad) }
            s.done?.let { Notice(it, Tone.Ok) }
            PrimaryButton(stringResource(if (s.saving) R.string.vend_saving else R.string.vend_save), vm::save, enabled = s.valid && !s.saving, color = NeerlyColors.VendorPrimary)
        })
    ) {
        if (s.lines.isEmpty()) { item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.vend_no_jar_types), stringResource(R.string.vend_no_jar_types_sub)) }; return@BoondhPage }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                s.lines.forEach { l -> FilterChip(selected = l.stock.productId == s.pickedId, onClick = { vm.pick(l.stock.productId) },
                    label = { Text(l.name) }, modifier = Modifier.heightIn(min = 44.dp)) }
            }
        }
        s.picked?.let { l -> item { Text(stringResource(R.string.vend_in_shop_at_homes, l.stock.totalInStock, l.stock.outWithCustomers), fontSize = 14.sp, color = NeerlyColors.Ink600) } }
        item { SectionLabel(stringResource(R.string.vend_what_happened_q)) }
        StockReason.entries.forEach { r ->
            item(key = r.name) {
                val on = s.reason == r
                val jarsDesc = pluralStringResource(R.plurals.vend_n_jars_desc, s.value, s.value)
                BoondhCard(Modifier.border(if (on) 2.dp else 0.dp, if (on) NeerlyColors.VendorPrimary else Color.Transparent, RoundedCornerShape(14.dp)),
                    onClick = { vm.reason(r) }) {
                    InfoRow(stringResource(r.label), stringResource(r.detail))
                    if (on) Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { vm.step(-1) }, enabled = s.value > s.min, modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)) { Text("−", fontSize = 20.sp) }
                        Text("${s.value}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                            modifier = Modifier.padding(horizontal = 16.dp).semantics { contentDescription = jarsDesc })
                        OutlinedButton(onClick = { vm.step(1) }, enabled = s.value < s.max, modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)) { Text("+", fontSize = 20.sp) }
                        Text(stringResource(if (r == StockReason.RECOUNT) R.string.vend_full_jars_now else R.string.vend_jars_word), fontSize = 14.sp, color = NeerlyColors.Ink600,
                            modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
        item {
            OutlinedTextField(s.note, vm::note, label = { Text(stringResource(if (s.reason == StockReason.ADD) R.string.vend_note_optional else R.string.vend_why_needed)) },
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        }
        if (s.reason == StockReason.DAMAGED || s.reason == StockReason.LOST) item {
            Notice(stringResource(R.string.vend_lost_damaged_info), Tone.Info)
            TextButton(onClick = onFileClaim, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.vend_file_claim), color = NeerlyColors.VendorPrimary, fontWeight = FontWeight.SemiBold) }
        }
    }
}
