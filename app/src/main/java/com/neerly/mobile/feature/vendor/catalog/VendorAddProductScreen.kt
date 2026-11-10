package com.neerly.mobile.feature.vendor.catalog

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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

data class AddProductUiState(
    val loading: Boolean = true,
    val templates: List<ShopTemplate> = emptyList(),
    /** Templates the shop already sells — offered last, marked. */
    val alreadySelling: Set<String> = emptySet(),
    val picked: ShopTemplate? = null,
    /** VProductNew "Start from blank": the kind is picked inside the form, from chips. */
    val blank: Boolean = false,
    val brand: String = "",
    val price: String = "",
    val dailyCapacity: Int = 50,
    val swap: Boolean = true,
    val keep: Boolean = false,
    val deposit: String = "",
    val retentionHours: Int = 72,
    val saving: Boolean = false,
    val created: ShopProduct? = null,
    val error: String? = null
) {
    val priceValue: BigDecimal? get() = price.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    val depositValue: BigDecimal? get() = deposit.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    /** At least one way to take the jar, and a deposit when keeping is allowed (server rule). */
    val valid: Boolean get() = picked != null && priceValue != null && dailyCapacity >= 1 &&
        (!returnable(picked) || swap || keep) && (!keep || depositValue != null)
}

/** How a product is counted — jar, case, trip, unit or bag — from its container type. */
enum class SellUnit { JAR, CASE, TRIP, UNIT, BAG }

fun sellUnit(t: ShopTemplate?): SellUnit = when (t?.containerType) {
    "BOTTLE" -> SellUnit.CASE
    "TANKER" -> SellUnit.TRIP
    "DISPENSER" -> SellUnit.UNIT
    "ICE_BAG" -> SellUnit.BAG
    else -> SellUnit.JAR
}

/** Jars and dispensers come back (swap or deposit); sealed cases, tankers and ice don't. */
fun returnable(t: ShopTemplate?): Boolean = t != null && (t.allowsDeposit || sellUnit(t) == SellUnit.JAR)

@HiltViewModel
class VendorAddProductViewModel @Inject constructor(
    private val api: VendorCatalogApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(AddProductUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.templates() to runCatching { api.products() }.getOrDefault(emptyList()) }
                .onSuccess { (t, mine) -> _state.value = _state.value.copy(loading = false, templates = t,
                    alreadySelling = mine.map { it.template.categoryCode }.toSet()) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("vendor templates", strings.get(R.string.vend_err_load_types))) }
        }
    }

    fun pick(t: ShopTemplate?) { _state.value = _state.value.copy(picked = t, keep = t?.allowsDeposit == true && _state.value.keep,
        swap = returnable(t) && _state.value.swap, blank = if (t == null) false else _state.value.blank, error = null) }

    /** "Start from blank" — open the form with nothing picked; the kind chips sit at the top. */
    fun startBlank() {
        val s = _state.value
        val first = s.templates.firstOrNull { it.categoryCode !in s.alreadySelling } ?: s.templates.firstOrNull()
        _state.value = s.copy(blank = true, picked = first, swap = returnable(first), keep = false, error = null)
    }
    fun update(f: AddProductUiState.() -> AddProductUiState) { _state.value = _state.value.f() }

    fun request(): NewProduct? {
        val s = _state.value
        if (!s.valid) return null
        val back = returnable(s.picked)
        return NewProduct(
            templateCategoryCode = s.picked!!.categoryCode, brand = s.brand.trim().ifBlank { null },
            price = s.priceValue!!, dailyCapacity = s.dailyCapacity,
            allowKeepContainer = back && s.keep, allowTransferAndReturn = back && s.swap,
            depositAmount = if (s.keep) s.depositValue else null, retentionHours = if (s.keep) s.retentionHours else null
        )
    }

    fun create() {
        val body = request() ?: return
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching { api.create(body) }
                .onSuccess { _state.value = _state.value.copy(saving = false, created = it) }
                .onFailure { _state.value = _state.value.copy(saving = false,
                    // Price bands are enforced server-side; its message says the allowed range.
                    error = it.userMessage("vendor add product", strings.get(R.string.vend_err_add_product))) }
        }
    }
}

/**
 * Board V29 — add a product from one of Boondh's product types. Board V40 ("start from blank")
 * isn't offered: every product needs a type, and only Boondh admins create types.
 */
@Composable
fun VendorAddProductScreen(onBack: () -> Unit, onDone: () -> Unit = onBack, vm: VendorAddProductViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.created) { if (s.created != null) onDone() }
    val t = s.picked
    if (t == null) {
        BoondhPage(title = stringResource(R.string.vend_what_adding), onBack = onBack, backLabel = stringResource(R.string.vend_what_you_sell),
            subtitle = stringResource(R.string.vend_what_adding_sub)) {
            if (s.templates.isEmpty()) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
            items(s.templates.sortedBy { it.categoryCode in s.alreadySelling }, key = { it.id }) { tpl ->
                BoondhCard(onClick = { vm.pick(tpl) }) {
                    InfoRow(tpl.displayName, listOfNotNull(tpl.sizeLabel, tpl.waterType?.lowercase()?.replaceFirstChar(Char::uppercase),
                        if (tpl.allowsDeposit) stringResource(R.string.vend_can_deposit) else null).joinToString(" · "),
                        if (tpl.categoryCode in s.alreadySelling) stringResource(R.string.vend_you_sell_this) else null, NeerlyColors.Ink600)
                }
            }
            item { SecondaryButton(stringResource(R.string.vend_start_blank), vm::startBlank, color = NeerlyColors.VendorPrimary) }
            item { Text(stringResource(R.string.vend_ask_add_type), fontSize = 13.sp, color = NeerlyColors.Ink600) }
        }
        return
    }
    val unit = sellUnit(t)
    val shown = listOfNotNull(s.brand.trim().ifBlank { null }, t.displayName).joinToString(" ")
    BoondhPage(
        title = stringResource(if (s.blank) R.string.vend_new_product else R.string.vend_setup_title), onBack = { vm.pick(null) },
        backLabel = stringResource(R.string.vend_types),
        subtitle = if (s.blank) stringResource(R.string.vend_customers_see, shown)
                   else stringResource(R.string.vend_setup_sub, t.displayName, shown),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            if (returnable(t) && !s.swap && !s.keep) Notice(stringResource(R.string.vend_pick_one_way), Tone.Warn)
            PrimaryButton(stringResource(if (s.saving) R.string.vend_adding else R.string.vend_put_on_sale), vm::create, enabled = s.valid && !s.saving, color = NeerlyColors.VendorPrimary)
        }
    ) {
        if (s.blank) item {
            BoondhCard {
                Text(stringResource(R.string.vend_what_is_it), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.templates.forEach { k ->
                        FilterChip(selected = k.id == t.id, onClick = { vm.pick(k) },
                            label = { Text(listOfNotNull(k.displayName, if (k.categoryCode in s.alreadySelling) stringResource(R.string.vend_you_sell_this) else null)
                                .joinToString(" · "), fontSize = 14.sp) },
                            modifier = Modifier.heightIn(min = 44.dp))
                    }
                }
            }
        }
        item {
            BoondhCard {
                OutlinedTextField(s.brand, { v -> vm.update { copy(brand = v.take(100)) } }, label = { Text(stringResource(R.string.vend_brand_optional)) },
                    singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(s.price, { v -> vm.update { copy(price = v.filter { it.isDigit() || it == '.' }.take(8)) } },
                    label = { Text(stringResource(when (unit) { SellUnit.JAR -> R.string.vend_price_per_jar; SellUnit.CASE -> R.string.vend_price_per_case
                        SellUnit.TRIP -> R.string.vend_price_per_trip; SellUnit.UNIT -> R.string.vend_price_per_unit; SellUnit.BAG -> R.string.vend_price_per_bag })) },
                    prefix = { Text("₹") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(stringResource(when (unit) { SellUnit.JAR -> R.string.vend_jars_per_day_label; SellUnit.CASE -> R.string.vend_cases_per_day_label
                    SellUnit.TRIP -> R.string.vend_trips_per_day_label; SellUnit.UNIT -> R.string.vend_units_per_day_label; SellUnit.BAG -> R.string.vend_bags_per_day_label }),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                Stepper(s.dailyCapacity, stringResource(when (unit) { SellUnit.JAR -> R.string.vend_jars_a_day; SellUnit.CASE -> R.string.vend_cases_a_day
                    SellUnit.TRIP -> R.string.vend_trips_a_day; SellUnit.UNIT -> R.string.vend_units_a_day; SellUnit.BAG -> R.string.vend_bags_a_day })) { by ->
                    vm.update { copy(dailyCapacity = (dailyCapacity + by).coerceIn(1, 9999)) } }
            }
        }
        if (!returnable(t)) { item { Notice(stringResource(R.string.vend_sealed_no_return), Tone.Info) }; return@BoondhPage }
        item { SectionLabel(stringResource(R.string.vend_how_take_jar)) }
        item {
            BoondhCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.vend_swap_empty), Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    Switch(s.swap, { v -> vm.update { copy(swap = v) } })
                }
                if (t.allowsDeposit) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.vend_keep_deposit), Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink900)
                        Switch(s.keep, { v -> vm.update { copy(keep = v) } })
                    }
                    if (s.keep) {
                        OutlinedTextField(s.deposit, { v -> vm.update { copy(deposit = v.filter { it.isDigit() || it == '.' }.take(8)) } },
                            label = { Text(stringResource(R.string.vend_deposit)) }, prefix = { Text("₹") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.vend_return_within), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RETURN_WINDOWS.forEach { (h, label) ->
                                FilterChip(selected = s.retentionHours == h, onClick = { vm.update { copy(retentionHours = h) } },
                                    label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = 44.dp))
                            }
                        }
                        Text(stringResource(R.string.vend_late_fee_rule),
                            fontSize = 13.sp, color = NeerlyColors.Ink600)
                    }
                }
            }
        }
    }
}
