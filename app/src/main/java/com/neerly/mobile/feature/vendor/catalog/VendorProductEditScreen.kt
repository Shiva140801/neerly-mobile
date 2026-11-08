package com.neerly.mobile.feature.vendor.catalog

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** How long a kept jar may stay out before late fees — board V14's "must come back within". */
internal val RETURN_WINDOWS = listOf(48 to R.string.vend_rw_2d, 72 to R.string.vend_rw_3d, 120 to R.string.vend_rw_5d, 168 to R.string.vend_rw_week)

data class ProductEditUiState(
    val loading: Boolean = true,
    val product: ShopProduct? = null,
    val price: String = "",
    val dailyCapacity: Int = 0,
    val deposit: String = "",
    val retentionHours: Int? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
) {
    val priceValue: BigDecimal? get() = price.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    val depositValue: BigDecimal? get() = deposit.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
    val valid: Boolean get() = priceValue != null && dailyCapacity >= 1 &&
        (product?.allowKeepContainer != true || depositValue != null)
}

@HiltViewModel
class VendorProductEditViewModel @Inject constructor(
    private val api: VendorCatalogApi,
    saved: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val productId: String = saved["productId"] ?: error("productId required")
    private val _state = MutableStateFlow(ProductEditUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            // There is no single-product GET; the list is small (a shop sells a handful of jars).
            runCatching { api.products().first { it.id == productId } }
                .onSuccess { p -> _state.value = ProductEditUiState(loading = false, product = p,
                    price = p.price.stripTrailingZeros().toPlainString(), dailyCapacity = p.dailyCapacity,
                    deposit = p.depositAmount?.stripTrailingZeros()?.toPlainString().orEmpty(), retentionHours = p.retentionHours) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("vendor product", strings.get(R.string.vend_err_load_product))) }
        }
    }

    fun setPrice(v: String) { _state.value = _state.value.copy(price = v.filter { it.isDigit() || it == '.' }.take(8), saved = false) }
    fun setDeposit(v: String) { _state.value = _state.value.copy(deposit = v.filter { it.isDigit() || it == '.' }.take(8), saved = false) }
    fun stepCapacity(by: Int) { _state.value = _state.value.copy(dailyCapacity = (_state.value.dailyCapacity + by).coerceIn(1, 9999), saved = false) }
    fun setWindow(hours: Int) { _state.value = _state.value.copy(retentionHours = hours, saved = false) }

    /** Sends only what changed, so a stale screen can't overwrite someone else's edit of another field. */
    fun patch(): ProductPatch? {
        val s = _state.value; val p = s.product ?: return null
        val price = s.priceValue?.takeIf { it.compareTo(p.price) != 0 }
        val cap = s.dailyCapacity.takeIf { it != p.dailyCapacity }
        val dep = if (p.allowKeepContainer) s.depositValue?.takeIf { p.depositAmount == null || it.compareTo(p.depositAmount) != 0 } else null
        val ret = if (p.allowKeepContainer) s.retentionHours?.takeIf { it != p.retentionHours } else null
        return ProductPatch(price = price, dailyCapacity = cap, depositAmount = dep, retentionHours = ret)
            .takeIf { price != null || cap != null || dep != null || ret != null }
    }

    fun save() {
        val body = patch() ?: run { _state.value = _state.value.copy(saved = true); return }
        if (!_state.value.valid) return
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching { api.update(productId, body) }
                .onSuccess { p -> _state.value = _state.value.copy(saving = false, saved = true, product = p) }
                .onFailure { _state.value = _state.value.copy(saving = false,
                    error = it.userMessage("vendor product save", strings.get(R.string.vend_err_save_product))) }
        }
    }
}

/** Board V14 — price, how many a day, and the jar rules for one product. */
@Composable
fun VendorProductEditScreen(onBack: () -> Unit, onCountJars: () -> Unit = {}, vm: VendorProductEditViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val p = s.product
    BoondhPage(
        title = p?.name ?: stringResource(R.string.vend_edit_product), onBack = onBack, backLabel = stringResource(R.string.vend_what_you_sell),
        footer = if (p == null) null else ({
            s.error?.let { Notice(it, Tone.Bad) }
            if (s.saved) Notice(stringResource(R.string.vend_product_saved), Tone.Ok)
            PrimaryButton(stringResource(if (s.saving) R.string.vend_saving else R.string.vend_save_changes), vm::save, enabled = s.valid && !s.saving,
                color = NeerlyColors.VendorPrimary)
        })
    ) {
        if (p == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        item {
            BoondhCard {
                Text(stringResource(R.string.vend_price_per_jar), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                OutlinedTextField(value = s.price, onValueChange = vm::setPrice, singleLine = true, prefix = { Text("₹") },
                    isError = s.priceValue == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.vend_jars_per_day_label), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                Stepper(s.dailyCapacity, stringResource(R.string.vend_jars_a_day), vm::stepCapacity)
                p.containersInStock?.let {
                    InfoRow(stringResource(R.string.vend_full_jars_shop), stringResource(R.string.vend_change_by_count), "$it", onClick = onCountJars)
                }
            }
        }
        item { SectionLabel(stringResource(R.string.vend_how_take_jar)) }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.vend_swap_empty), stringResource(R.string.vend_swap_empty_sub, p.template.displayName),
                    stringResource(if (p.allowTransferAndReturn) R.string.vend_on else R.string.vend_off), if (p.allowTransferAndReturn) NeerlyColors.Ok else NeerlyColors.Ink600)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_keep_deposit), stringResource(R.string.vend_keep_deposit_sub),
                    stringResource(if (p.allowKeepContainer) R.string.vend_on else R.string.vend_off), if (p.allowKeepContainer) NeerlyColors.Ok else NeerlyColors.Ink600)
                if (p.allowKeepContainer) {
                    OutlinedTextField(value = s.deposit, onValueChange = vm::setDeposit, singleLine = true, prefix = { Text("₹") },
                        label = { Text(stringResource(R.string.vend_deposit)) }, isError = s.depositValue == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.vend_jar_back_within), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RETURN_WINDOWS.forEach { (h, label) ->
                            FilterChip(selected = s.retentionHours == h, onClick = { vm.setWindow(h) }, label = { Text(stringResource(label)) },
                                modifier = Modifier.heightIn(min = 44.dp))
                        }
                    }
                }
                // The update API can't switch these two on or off yet — say so rather than show a dead toggle.
                Text(stringResource(R.string.vend_ways_ask_support), fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
        }
    }
}

@Composable
internal fun Stepper(value: Int, unit: String, onStep: (Int) -> Unit) {
    val desc = stringResource(R.string.vend_stepper_desc, value, unit)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onStep(-1) }, enabled = value > 1, modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(0.dp)) { Text("−", fontSize = 20.sp) }
        Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
            modifier = Modifier.padding(horizontal = 16.dp).semantics { contentDescription = desc })
        OutlinedButton(onClick = { onStep(1) }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("+", fontSize = 20.sp) }
        Text(unit, fontSize = 14.sp, color = NeerlyColors.Ink600, modifier = Modifier.padding(start = 12.dp))
    }
}
