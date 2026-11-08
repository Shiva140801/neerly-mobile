package com.neerly.mobile.feature.order

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.order.OrderLocalStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderPlacedViewModel @Inject constructor(private val api: NeerlyApi, private val codes: OrderLocalStore) : ViewModel() {
    val order = MutableStateFlow<OrderResponse?>(null)
    fun load(id: String) { if (order.value == null) viewModelScope.launch { order.value = runCatching { api.order(id) }.getOrNull() } }
    fun code(id: String) = codes.deliveryCode(id)
}

/** Boards 14 (paid) and 69 (cash) — done, what happens next, what to keep ready. */
@Composable
fun OrderPlacedScreen(orderId: String, orderNumber: String, onTrack: () -> Unit, onHome: () -> Unit,
                      vm: OrderPlacedViewModel = hiltViewModel()) {
    LaunchedEffect(orderId) { vm.load(orderId) }
    val o by vm.order.collectAsState()
    val cash = o?.paymentMethod == "COD"
    val swaps = o?.items?.filter { it.containerMode == "TRANSFER_AND_RETURN" }?.sumOf { it.quantity } ?: 0
    BoondhPage(
        title = stringResource(R.string.cust_placed_title),
        subtitle = stringResource(R.string.cust_placed_subtitle, o?.vendorName ?: stringResource(R.string.cust_the_shop)),
        footer = { PrimaryButton(stringResource(R.string.cust_placed_track), onTrack); SecondaryButton(stringResource(R.string.cust_placed_home), onHome) }
    ) {
        item { BoondhCard {
            InfoRow(stringResource(R.string.cust_order), trailing = orderNumber)
            o?.let { InfoRow(if (cash) stringResource(R.string.cust_pay_at_door) else stringResource(R.string.cust_paid), trailing = it.totalAmount.rupees()) }
            o?.paymentMethod?.let { InfoRow(stringResource(R.string.cust_how), trailing = when (it) { "COD" -> stringResource(R.string.cust_method_cash); "WALLET" -> stringResource(R.string.cust_method_wallet); "CARD" -> stringResource(R.string.cust_method_card); else -> stringResource(R.string.cust_method_upi) }) }
        } }
        vm.code(orderId)?.let { c -> item { BoondhCard {
            SectionLabel(stringResource(R.string.cust_delivery_code))
            Text(c.toCharArray().joinToString(" "), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text(stringResource(R.string.cust_placed_code_tell), fontSize = 13.sp, color = NeerlyColors.Ink600)
        } } }
        if (swaps > 0) item { Notice(androidx.compose.ui.res.pluralStringResource(R.plurals.cust_placed_keep_empties, swaps, swaps), Tone.Info) }
        if (cash) item { Notice(stringResource(R.string.cust_placed_keep_cash, o!!.totalAmount.rupees()), Tone.Warn) }
    }
}
