package com.neerly.mobile.feature.vendor.jars

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One jar type with its name, for screens that show stock. */
data class JarLine(val stock: JarStock, val name: String)

data class JarsUiState(
    val loading: Boolean = true,
    val lines: List<JarLine> = emptyList(),
    val openReturns: Int = 0,
    val openClaims: Int = 0,
    val error: String? = null
) {
    val owned get() = lines.sumOf { it.stock.totalOwned }
    val inShop get() = lines.sumOf { it.stock.totalInStock }
    val atHomes get() = lines.sumOf { it.stock.outWithCustomers }
    val damaged get() = lines.sumOf { it.stock.damagedCount }
    val lost get() = lines.sumOf { it.stock.lostCount }
}

/** Names come from the catalogue; a jar row for a product no longer listed is still shown. */
internal suspend fun jarLines(jars: VendorJarsApi, catalog: VendorCatalogApi, unnamed: String): List<JarLine> = coroutineScope {
    val stock = async { jars.stock() }
    val names = async { runCatching { catalog.products() }.getOrDefault(emptyList()).associate { it.id to it.name } }
    val n = names.await()
    stock.await().map { JarLine(it, n[it.productId] ?: unnamed) }.sortedByDescending { it.stock.totalOwned }
}

@HiltViewModel
class VendorJarsViewModel @Inject constructor(
    private val jars: VendorJarsApi,
    private val catalog: VendorCatalogApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(JarsUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val lines = async { jarLines(jars, catalog, strings.get(R.string.vend_jar)) }
                    val returns = async { runCatching { jars.returns().size }.getOrDefault(0) }
                    val claims = async { runCatching { jars.claims().content.count { it.open } }.getOrDefault(0) }
                    JarsUiState(loading = false, lines = lines.await(), openReturns = returns.await(), openClaims = claims.await())
                }
            }.onSuccess { _state.value = it }
             .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage("vendor jars", strings.get(R.string.vend_err_load_jars))) }
        }
    }
}

/** Board V15 — where the shop's jars are, with the jobs that move them. */
@Composable
fun VendorJarsScreen(
    onBack: (() -> Unit)? = null,     // null when it's the Jars tab root — no Back there
    onCount: () -> Unit = {},
    onPickups: () -> Unit = {},
    onDamage: () -> Unit = {},
    vm: VendorJarsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    OnReturn(vm::load)
    BoondhPage(
        title = stringResource(R.string.vend_your_jars), onBack = onBack,
        subtitle = if (s.lines.isEmpty()) null else pluralStringResource(R.plurals.vend_jars_carry_name, s.owned, s.owned)
    ) {
        if (s.lines.isEmpty()) {
            item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.vend_jars_empty), stringResource(R.string.vend_jars_empty_sub)) }
            item { PrimaryButton(stringResource(R.string.vend_count_the_shop), onCount, color = NeerlyColors.VendorPrimary) }
            return@BoondhPage
        }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.vend_full_in_shop), trailing = "${s.inShop}")
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_at_homes), trailing = "${s.atHomes}")
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_damaged), trailing = "${s.damaged}", trailingColor = if (s.damaged > 0) NeerlyColors.Warn else NeerlyColors.Ink900)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_lost), trailing = "${s.lost}", trailingColor = if (s.lost > 0) NeerlyColors.Err else NeerlyColors.Ink900)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(stringResource(R.string.vend_count_shop_short), onCount, modifier = Modifier.weight(1f), color = NeerlyColors.VendorPrimary)
                SecondaryButton(stringResource(R.string.vend_pickups_n, s.openReturns), onPickups, modifier = Modifier.weight(1f), color = NeerlyColors.VendorPrimary)
                SecondaryButton(if (s.openClaims > 0) stringResource(R.string.vend_damage_n, s.openClaims) else stringResource(R.string.vend_damage), onDamage, modifier = Modifier.weight(1f), color = NeerlyColors.VendorPrimary)
            }
        }
        item { SectionLabel(stringResource(R.string.vend_by_jar_type)) }
        items(s.lines, key = { it.stock.id }) { l ->
            BoondhCard {
                Text(l.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(listOfNotNull(stringResource(R.string.vend_in_shop_at_homes, l.stock.totalInStock, l.stock.outWithCustomers),
                    l.stock.damagedCount.takeIf { it > 0 }?.let { stringResource(R.string.vend_n_damaged, it) },
                    l.stock.lostCount.takeIf { it > 0 }?.let { stringResource(R.string.vend_n_lost, it) }).joinToString(" · "),
                    fontSize = 13.sp, color = NeerlyColors.Ink600)
                l.stock.lastAdjustedAt?.let { Text(stringResource(R.string.vend_last_changed, shortDay(it)), fontSize = 12.sp, color = NeerlyColors.Ink600) }
            }
        }
    }
}
