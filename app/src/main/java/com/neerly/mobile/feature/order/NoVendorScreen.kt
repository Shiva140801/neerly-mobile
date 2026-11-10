package com.neerly.mobile.feature.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.R
import com.neerly.mobile.core.design.BoondhCard
import com.neerly.mobile.core.design.BoondhPage
import com.neerly.mobile.core.design.LoadState
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.Notice
import com.neerly.mobile.core.design.PrimaryButton
import com.neerly.mobile.core.design.SectionLabel
import com.neerly.mobile.core.design.SecondaryButton
import com.neerly.mobile.core.design.Tone
import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.DiscoveryApi
import com.neerly.mobile.data.cart.CartItem
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.AlternativeShopDto
import com.neerly.mobile.data.dto.OrderAlternativesResponse
import com.neerly.mobile.data.dto.RerouteRequest
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Board NoVendor: no shop took the order. Nothing was charged, so the customer can send
 * the same basket to another open shop in one tap, wait for their shop with a later slot,
 * or drop it.
 */
@HiltViewModel
class NoVendorViewModel @Inject constructor(
    private val api: DiscoveryApi,
    private val repo: CustomerRepository,
    private val cart: CartStore,
    private val strings: com.neerly.mobile.core.util.Strings,
    saved: SavedStateHandle
) : ViewModel() {
    private val orderId: String = saved["orderId"] ?: error("orderId required")

    private val _state = MutableStateFlow(NoVendorUiState())
    val state: StateFlow<NoVendorUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.alternatives(orderId) }
                .onSuccess { _state.value = _state.value.copy(loading = false, data = it) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage(context = "no-vendor alternatives", fallback = strings.get(R.string.cust_nv_load_failed))) }
        }
    }

    /** Same basket to [shop]; the new order's id lands in [NoVendorUiState.rerouted]. */
    fun sendTo(shop: AlternativeShopDto) {
        if (_state.value.sending != null) return
        _state.value = _state.value.copy(sending = shop.vendorId, error = null)
        viewModelScope.launch {
            runCatching { api.reroute(orderId, RerouteRequest(shop.vendorId)) }
                .onSuccess { _state.value = _state.value.copy(sending = null, rerouted = it.id) }
                .onFailure { _state.value = _state.value.copy(sending = null,
                    error = it.userMessage(context = "reroute", fallback = strings.get(R.string.cust_nv_failed))) }
        }
    }

    /**
     * "Wait for Sri Balaji · pick a later slot": the same lines back in the cart for the
     * original shop, so the slot picker opens on that shop. True when the cart is ready.
     */
    suspend fun refillCartForOriginalShop(): Boolean {
        val shopId = _state.value.data?.originalShopId ?: return false
        val shopName = _state.value.data?.originalShopName ?: ""
        val order = runCatching { repo.order(orderId) }.getOrNull() ?: return false
        val lines = order.items.mapNotNull { it ->
            runCatching {
                CartItem(it.productId, it.productName, it.unitPrice, it.quantity.coerceIn(1, 10),
                    keepContainer = it.containerMode != "TRANSFER_AND_RETURN")
            }.getOrNull()
        }
        if (lines.isEmpty()) return false
        cart.replaceWithNewVendor(shopId, shopName, lines.first())
        lines.drop(1).forEach { cart.addItem(shopId, shopName, it) }
        return true
    }
}

data class NoVendorUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: OrderAlternativesResponse? = null,
    /** vendorId being sent to, while the request runs. */
    val sending: String? = null,
    /** The new order once rerouted — the screen moves to its tracking. */
    val rerouted: String? = null
)

@Composable
fun NoVendorScreen(
    onBack: () -> Unit,
    onRerouted: (newOrderId: String) -> Unit,
    onPickLaterSlot: () -> Unit,
    onDrop: () -> Unit,
    vm: NoVendorViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.rerouted) { s.rerouted?.let(onRerouted) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val shop = s.data?.originalShopName
    BoondhPage(
        title = shop?.let { stringResource(R.string.cust_nv_title, it) } ?: stringResource(R.string.cust_nv_title_generic),
        onBack = onBack,
        subtitle = stringResource(R.string.cust_nv_body),
        footer = {
            if (shop != null) SecondaryButton(stringResource(R.string.cust_nv_wait, shop), {
                scope.launch { if (vm.refillCartForOriginalShop()) onPickLaterSlot() }
            })
            TextButton(onClick = onDrop, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cust_nv_drop), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold)
            }
        }
    ) {
        if (s.loading) { item { LoadState(true, null, vm::load) }; return@BoondhPage }
        s.error?.let { e -> item { Notice(e, Tone.Bad) } }
        val alts = s.data?.alternatives.orEmpty()
        item { SectionLabel(stringResource(R.string.cust_nv_others)) }
        if (alts.isEmpty() && s.data != null) item { Text(stringResource(R.string.cust_nv_none), fontSize = 14.sp, color = NeerlyColors.Ink600) }
        alts.forEach { a -> item(key = a.vendorId) { AlternativeCard(a, shop, sending = s.sending == a.vendorId, enabled = s.sending == null) { vm.sendTo(a) } } }
    }
}

@Composable
private fun AlternativeCard(a: AlternativeShopDto, originalShop: String?, sending: Boolean, enabled: Boolean, onSend: () -> Unit) {
    BoondhCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(a.businessName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                val lines = a.lines.map {
                    stringResource(R.string.cust_qty_item, it.quantity, it.name) + " " + it.lineTotal.asRupees()
                }.joinToString(" + ")
                val extras = listOfNotNull(
                    stringResource(R.string.cust_home_eta, a.etaMinutesMin, a.etaMinutesMax),
                    lines,
                    a.deliveryFee.takeIf { it.signum() > 0 }?.let { stringResource(R.string.cust_nv_plus_delivery, it.asRupees()) },
                    *a.missing.map { stringResource(R.string.cust_nv_missing, it) }.toTypedArray(),
                    if (a.promoDropped && originalShop != null) stringResource(R.string.cust_nv_promo_dropped, originalShop) else null
                )
                Text(extras.joinToString(" · "), fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
            }
            Spacer(Modifier.width(10.dp))
            Text(a.totalAmount.asRupees(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        }
        PrimaryButton(
            if (sending) stringResource(R.string.cust_nv_sending)
            else stringResource(R.string.cust_nv_send_here, a.businessName, a.totalAmount.asRupees()),
            onSend, enabled = enabled
        )
    }
}
