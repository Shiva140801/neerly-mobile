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
import com.neerly.mobile.core.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderPlacedViewModel @Inject constructor(
    private val api: NeerlyApi,
    private val codes: OrderLocalStore,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    val order = MutableStateFlow<OrderResponse?>(null)
    /** Board OrderPlacedCash: cancelling, done, or why it couldn't. */
    val cancelling = MutableStateFlow(false)
    val cancelled = MutableStateFlow(false)
    val cancelError = MutableStateFlow<String?>(null)
    fun load(id: String) { if (order.value == null) viewModelScope.launch { order.value = runCatching { api.order(id) }.getOrNull() } }
    fun code(id: String) = codes.deliveryCode(id)

    /** Free for a cash order until the driver leaves the shop — nothing was paid, nothing to refund. */
    fun cancelCash(id: String) {
        if (cancelling.value) return
        cancelling.value = true; cancelError.value = null
        viewModelScope.launch {
            runCatching { api.cancelOrder(id, com.neerly.mobile.data.dto.CancelOrderRequest("CHANGED_MIND")) }
                .onSuccess { order.value = it; cancelled.value = true; codes.forgetDeliveryCode(id) }
                .onFailure { cancelError.value = it.userMessage(context = "cancel order",
                    fallback = strings.get(com.neerly.mobile.R.string.cust_placed_cancel_failed)) }
            cancelling.value = false
        }
    }

    companion object {
        /** Before the driver leaves the shop. */
        val CASH_CANCEL_FREE = setOf("PLACED", "VENDOR_ASSIGNED", "VENDOR_ACCEPTED", "PREPARING")
    }
}

/** Boards 14 (paid) and 69 (cash) — done, what happens next, what to keep ready. */
@Composable
fun OrderPlacedScreen(orderId: String, orderNumber: String, onTrack: () -> Unit, onHome: () -> Unit,
                      vm: OrderPlacedViewModel = hiltViewModel()) {
    LaunchedEffect(orderId) { vm.load(orderId) }
    // The moment the customer wants updates: right after placing an order (plan §e), not at install.
    com.neerly.mobile.push.AskNotificationsOnce("customer", true)
    val o by vm.order.collectAsState()
    val cancelling by vm.cancelling.collectAsState()
    val cancelled by vm.cancelled.collectAsState()
    val cancelError by vm.cancelError.collectAsState()
    var confirmCancel by remember { mutableStateOf(false) }
    val cash = o?.paymentMethod == "COD"
    val canCancelFree = cash && o?.status in OrderPlacedViewModel.CASH_CANCEL_FREE
    if (cancelled) {
        BoondhPage(title = stringResource(R.string.cust_placed_cancelled_title, orderNumber),
            footer = { PrimaryButton(stringResource(R.string.cust_placed_home), onHome) }) {
            item { Notice(stringResource(R.string.cust_placed_cancelled_cash), Tone.Ok) }
        }
        return
    }
    if (confirmCancel) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.cust_placed_cancel_q, orderNumber)) },
            text = { Text(stringResource(R.string.cust_placed_cancel_free_body)) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmCancel = false; vm.cancelCash(orderId) }) {
                Text(stringResource(R.string.cust_placed_cancel_yes), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmCancel = false }) {
                Text(stringResource(R.string.cust_placed_cancel_keep)) } }
        )
    }
    val swaps = o?.items?.filter { it.containerMode == "TRANSFER_AND_RETURN" }?.sumOf { it.quantity } ?: 0
    BoondhPage(
        title = stringResource(R.string.cust_placed_title),
        subtitle = stringResource(R.string.cust_placed_subtitle, o?.vendorName ?: stringResource(R.string.cust_the_shop)),
        footer = {
            cancelError?.let { Notice(it, Tone.Bad) }
            PrimaryButton(stringResource(R.string.cust_placed_track), onTrack)
            if (canCancelFree) SecondaryButton(if (cancelling) stringResource(R.string.cust_placed_cancelling) else stringResource(R.string.cust_placed_cancel),
                { confirmCancel = true }, enabled = !cancelling, color = NeerlyColors.Err)
            else SecondaryButton(stringResource(R.string.cust_placed_home), onHome)
        }
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
        if (canCancelFree) item { Text(stringResource(R.string.cust_placed_cancel_free_note), fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}
