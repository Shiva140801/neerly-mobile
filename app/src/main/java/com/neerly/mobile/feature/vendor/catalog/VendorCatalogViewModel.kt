package com.neerly.mobile.feature.vendor.catalog

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class VendorCatalogUiState(
    val loading: Boolean = true,
    val products: List<ShopProduct> = emptyList(),
    val error: String? = null,
    /** Product id whose on/off is being saved — its switch is disabled meanwhile. */
    val saving: String? = null
)

/** Board V13 — what the shop sells, switched on or off (optionally with a "back on" day). */
@HiltViewModel
class VendorCatalogViewModel @Inject constructor(
    private val api: VendorCatalogApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    /** Tests pin "today". */
    internal var clock: Clock = Clock.system(ZoneId.of("Asia/Kolkata"))

    private val _state = MutableStateFlow(VendorCatalogUiState())
    val state: StateFlow<VendorCatalogUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.products() }
                .onSuccess { _state.value = _state.value.copy(loading = false, products = it) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("vendor catalog", strings.get(R.string.vend_err_load_catalog))) }
        }
    }

    fun today(): LocalDate = LocalDate.now(clock)

    /** Switch off; [backOn] null means "until I switch it on". */
    fun switchOff(p: ShopProduct, backOn: LocalDate?) = save(p) { api.pause(p.id, PauseBody(true, backOn?.toString())) }

    fun switchOn(p: ShopProduct) = save(p) { api.pause(p.id, PauseBody(false)) }

    private fun save(p: ShopProduct, call: suspend () -> ShopProduct) {
        _state.value = _state.value.copy(saving = p.id, error = null)
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { updated -> _state.value = _state.value.copy(saving = null,
                    products = _state.value.products.map { if (it.id == updated.id) updated else it }) }
                .onFailure { _state.value = _state.value.copy(saving = null,
                    error = it.userMessage("vendor product switch", strings.get(R.string.vend_err_switch_product, p.name))) }
        }
    }
}
