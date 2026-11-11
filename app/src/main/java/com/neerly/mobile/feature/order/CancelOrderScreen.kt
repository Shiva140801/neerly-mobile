package com.neerly.mobile.feature.order

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.CancelOrderRequest
import com.neerly.mobile.data.dto.CancelQuote
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.UserSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CancelUiState(
    val loading: Boolean = true,
    val order: OrderResponse? = null,
    val quote: CancelQuote? = null,
    val me: UserSummary? = null,
    val cancelling: Boolean = false,
    val cancelled: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CancelOrderViewModel @Inject constructor(
    private val api: NeerlyApi,
    saved: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val orderId: String = checkNotNull(saved["orderId"])
    private val _state = MutableStateFlow(CancelUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { Triple(api.order(orderId), api.cancelQuote(orderId), runCatching { api.me() }.getOrNull()) }
                .onSuccess { (o, q, me) -> _state.value = CancelUiState(loading = false, order = o, quote = q, me = me) }
                .onFailure { _state.value = CancelUiState(loading = false, error = it.userMessage("cancel quote", strings.get(R.string.cust_cx_quote_failed))) }
        }
    }

    fun cancel(reason: String) {
        _state.value = _state.value.copy(cancelling = true, error = null)
        viewModelScope.launch {
            runCatching { api.cancelOrder(orderId, CancelOrderRequest(reason)) }
                .onSuccess { _state.value = _state.value.copy(cancelling = false, cancelled = true, order = it) }
                .onFailure { _state.value = _state.value.copy(cancelling = false,
                    error = it.userMessage("cancel", strings.get(R.string.cust_cx_failed))) }
        }
    }
}

private val REASONS = listOf(R.string.cust_cx_r_late, R.string.cust_cx_r_mistake, R.string.cust_cx_r_address,
    R.string.cust_cx_r_elsewhere, R.string.cust_cx_r_other)

/**
 * Boards 44 (free cancel) and 68 (after pickup). The backend's quote decides which: before
 * the driver leaves it's free; after, a prepaid order keeps the trip fee. The refund line
 * says where the money goes (the customer's refund setting) and by when.
 */
@Composable
fun CancelOrderScreen(onBack: () -> Unit, onRefunds: () -> Unit, onRefundSetting: () -> Unit,
                      vm: CancelOrderViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val cfg = LocalAppConfig.current
    var reason by remember { mutableStateOf<Int?>(null) }
    val reasonText = reason?.let { stringResource(it) }
    val q = s.quote
    val o = s.order
    val toWallet = s.me?.refundDestination == "WALLET"
    val prepaid = o?.paymentMethod in setOf("UPI", "CARD", "WALLET")
    val notAllowed = stringResource(R.string.cust_cx_not_allowed)
    val destination = when {
        o?.paymentMethod == "WALLET" || toWallet -> stringResource(R.string.cust_cx_dest_wallet)
        o?.paymentMethod == "CARD" -> stringResource(R.string.cust_cx_dest_card, cfg.refundDaysMin, cfg.refundDaysMax)
        else -> stringResource(R.string.cust_cx_dest_upi, cfg.refundDaysMin, cfg.refundDaysMax)
    }

    BoondhPage(
        title = when { s.cancelled -> stringResource(R.string.cust_cx_title_done); q?.late == true -> stringResource(R.string.cust_cx_title_late); else -> stringResource(R.string.cust_cx_title) },
        onBack = onBack, backLabel = stringResource(R.string.cust_cx_back_order),
        subtitle = o?.let { if (q?.late == true) stringResource(R.string.cust_cx_sub_late, it.orderNumber) else stringResource(R.string.cust_cx_sub_early, it.orderNumber) },
        footer = if (s.loading || o == null || q == null) null else ({
            if (s.cancelled) {
                if (prepaid && q.refund.signum() > 0) PrimaryButton(stringResource(R.string.cust_cx_see_refunds), onRefunds)
                SecondaryButton(stringResource(R.string.cust_cx_back_orders), onBack)
            } else if (q.allowed) {
                PrimaryButton(
                    when { s.cancelling -> stringResource(R.string.cust_cx_cancelling); reason == null -> stringResource(R.string.cust_cx_pick_reason)
                           prepaid -> stringResource(R.string.cust_cx_cancel_refund, q.refund.rupees()); else -> stringResource(R.string.cust_tr_cancel) },
                    onClick = { reasonText?.let(vm::cancel) }, enabled = reason != null && !s.cancelling,
                    color = NeerlyColors.Err)
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.cust_cx_keep), color = NeerlyColors.CustomerPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            } else SecondaryButton(stringResource(R.string.cust_back), onBack)
        })
    ) {
        if (s.loading || o == null || q == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        if (s.cancelled) {
            item {
                Notice(if (prepaid && q.refund.signum() > 0) stringResource(R.string.cust_cx_refund_on_way, q.refund.rupees(), destination)
                       else stringResource(R.string.cust_cx_done_cash), Tone.Ok)
            }
            return@BoondhPage
        }
        if (!q.allowed) { item { Notice(q.reason ?: notAllowed, Tone.Warn) }; return@BoondhPage }
        if (q.late) item {
            Text(stringResource(R.string.cust_cx_late_body),
                fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink700)
        }
        // Board CancelOrder: "**What happened?** — helps the shop" in sentence case, then
        // hairline-separated reason rows (one list item, so rows sit tight like the board).
        item {
            val why = stringResource(R.string.cust_cx_why)
            Column {
                Text(androidx.compose.ui.text.buildAnnotatedString {
                    val cut = why.indexOf(" — ").takeIf { it > 0 } ?: why.length
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)); append(why.substring(0, cut)); pop()
                    append(why.substring(cut))
                }, fontSize = 16.sp, color = NeerlyColors.Ink600, modifier = Modifier.padding(bottom = 4.dp))
                REASONS.forEach { r ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .selectable(selected = reason == r, onClick = { reason = r }, role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = reason == r, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = NeerlyColors.CustomerPrimary, unselectedColor = NeerlyColors.Ink300))
                        Spacer(Modifier.width(8.dp)); Text(stringResource(r), fontSize = 17.sp, fontWeight = FontWeight.Medium, color = NeerlyColors.Ink900)
                    }
                    HorizontalDivider(color = NeerlyColors.Ink200)
                }
            }
        }
        if (prepaid) item {
            BoondhCard {
                SectionLabel(stringResource(R.string.cust_cx_your_refund))
                InfoRow(stringResource(R.string.cust_paid), trailing = o.totalAmount.rupees())
                InfoRow(if (q.late) stringResource(R.string.cust_cx_trip_fee) else stringResource(R.string.cust_cx_fee),
                    trailing = if (q.fee.signum() > 0) stringResource(R.string.cust_minus_amount, q.fee.rupees()) else stringResource(R.string.cust_cx_zero))
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.cust_cx_back_to_you), trailing = q.refund.rupees(), trailingColor = NeerlyColors.Ok)
                Text(stringResource(R.string.cust_cx_goes_to, destination), fontSize = 13.sp, color = NeerlyColors.Ink600)
                if (o.paymentMethod != "WALLET") TextButton(onClick = onRefundSetting, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.cust_cx_change_dest), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        } else item { Notice(stringResource(R.string.cust_cx_cash_nothing), Tone.Info) }
        if (q.late) item { Notice(stringResource(R.string.cust_cx_late_tip), Tone.Warn) }
        s.error?.let { e -> item { Notice(e, Tone.Bad) } }
    }
}
