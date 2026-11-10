package com.neerly.mobile.feature.order.redeliver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.R
import com.neerly.mobile.core.design.BoondhCard
import com.neerly.mobile.core.design.InfoRow
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.Notice
import com.neerly.mobile.core.design.Pill
import com.neerly.mobile.core.design.Tone
import com.neerly.mobile.core.design.rupees
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.FailedDeliveryView
import com.neerly.mobile.data.dto.FileComplaintRequest
import com.neerly.mobile.data.dto.OrderResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class FailedDeliveryUi(
    val working: Boolean = false,
    /** Board DeliveryFailed "Cancelled · ₹240 on its way". */
    val closed: CloseFailedResponse? = null,
    /** "Sent to Boondh support." */
    val reported: Boolean = false,
    val error: String? = null
)

/** The two quick choices on the card: cancel, and "I was home". Re-delivery has its own pages. */
@HiltViewModel
class FailedDeliveryViewModel @Inject constructor(
    private val redelivery: RedeliveryApi,
    private val api: NeerlyApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(FailedDeliveryUi())
    val state = _state.asStateFlow()

    fun cancel(orderId: String, onChanged: () -> Unit) {
        if (_state.value.working) return
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { redelivery.closeFailed(orderId) }
                .onSuccess { _state.value = _state.value.copy(working = false, closed = it); onChanged() }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage(context = "cancel order", fallback = strings.get(R.string.cust_fd_cancel_failed))) }
        }
    }

    /** "I was home — report this": a FAILED_DELIVERY complaint; the choice stays open. */
    fun report(order: OrderResponse, note: String, onChanged: () -> Unit) {
        if (_state.value.working || note.isBlank()) return
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching {
                api.fileComplaint(FileComplaintRequest(orderId = order.id, category = "FAILED_DELIVERY",
                    subject = strings.get(R.string.cust_fd_report_subject, order.orderNumber), description = note.trim().take(2000)))
            }
                .onSuccess { _state.value = _state.value.copy(working = false, reported = true); onChanged() }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage(context = "report", fallback = strings.get(R.string.cust_fd_report_failed))) }
        }
    }
}

/**
 * Board DeliveryFailed, inside tracking: the driver's note and gate photo, then try again
 * later, send it somewhere else, or cancel — and "I was home" if the driver is wrong.
 */
@Composable
fun FailedDeliveryCard(
    order: OrderResponse,
    view: FailedDeliveryView,
    shopName: String,
    onTryAgain: () -> Unit,
    onElsewhere: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onChanged: () -> Unit,
    vm: FailedDeliveryViewModel = hiltViewModel(key = "failed-${order.id}")
) {
    val ui by vm.state.collectAsState()
    var confirmCancel by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }
    val prepaid = order.paymentMethod != "COD"
    val driver = order.driverName?.substringBefore(' ') ?: stringResource(R.string.cust_fd_the_driver)
    val fee = view.newTripFee ?: view.keptIfCancelled
    val closed = ui.closed

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoondhCard {
            Pill(stringResource(R.string.cust_fd_not_delivered), Tone.Bad)
            Text(stringResource(R.string.cust_fd_title, driver), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            val said = view.note ?: reasonLabel(view.reason)
            Text("“$said”", fontSize = 15.sp, fontStyle = FontStyle.Italic, color = NeerlyColors.Ink700)
            Text(listOfNotNull(clock(view.failedAt), view.photoKey?.let { stringResource(R.string.cust_fd_photo_attached) }).joinToString(" · "),
                fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
        when {
            closed != null || view.state == "CLOSED" -> {
                val refund = closed?.refund ?: view.refundIfCancelled
                Notice(if (prepaid && refund.signum() > 0) stringResource(R.string.cust_fd_closed_refund, refund.rupees(), view.keptIfCancelled.rupees(), shopName)
                       else stringResource(R.string.cust_fd_closed_cash), Tone.Ok)
            }
            view.state == "REDELIVERED" -> {
                Notice(stringResource(R.string.cust_fd_redelivered), Tone.Ok)
                view.redeliveredAs?.let { id ->
                    com.neerly.mobile.core.design.SecondaryButton(stringResource(R.string.cust_fd_see_new_order), { onOpenOrder(id) })
                }
            }
            view.open -> {
                Text(if (prepaid) stringResource(R.string.cust_fd_body_paid, order.orderNumber, shopName, order.totalAmount.rupees())
                     else stringResource(R.string.cust_fd_body_cash, order.orderNumber, shopName),
                    fontSize = 14.sp, color = NeerlyColors.Ink700)
                if (view.state == "DISPUTED" || ui.reported) Notice(stringResource(R.string.cust_fd_reported, driver, fee.rupees()), Tone.Info)
                Choice(stringResource(R.string.cust_fd_try_later), stringResource(R.string.cust_fd_try_later_sub, fee.rupees()), !ui.working, onTryAgain)
                Choice(stringResource(R.string.cust_fd_elsewhere), stringResource(R.string.cust_fd_elsewhere_sub, fee.rupees()), !ui.working, onElsewhere)
                Choice(
                    if (prepaid) stringResource(R.string.cust_fd_cancel_refund, view.refundIfCancelled.rupees()) else stringResource(R.string.cust_fd_cancel_cash),
                    if (prepaid) stringResource(R.string.cust_fd_cancel_refund_sub, view.keptIfCancelled.rupees(), driver) else stringResource(R.string.cust_fd_cancel_cash_sub),
                    !ui.working
                ) { confirmCancel = true }
                deadline(view.decideBy)?.let { by ->
                    Text(stringResource(if (prepaid) R.string.cust_fd_decide_by_paid else R.string.cust_fd_decide_by_cash, by, view.keptIfCancelled.rupees()),
                        fontSize = 13.sp, color = NeerlyColors.Ink600)
                }
                ui.error?.let { Notice(it, Tone.Bad) }
                if (view.state == "AWAITING" && !ui.reported) {
                    TextButton(onClick = { reporting = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.cust_fd_i_was_home), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.cust_fd_cancel_q, order.orderNumber)) },
            text = {
                if (prepaid) Column {
                    InfoRow(stringResource(R.string.cust_fd_paid_online), trailing = order.totalAmount.rupees())
                    InfoRow(stringResource(R.string.cust_fd_trip_fee, driver), trailing = "−" + view.keptIfCancelled.rupees())
                    InfoRow(stringResource(R.string.cust_fd_back_to_you), trailing = view.refundIfCancelled.rupees(), trailingColor = NeerlyColors.Ok)
                } else Text(stringResource(R.string.cust_fd_cancel_cash_body))
            },
            confirmButton = { TextButton(onClick = { confirmCancel = false; vm.cancel(order.id, onChanged) }) {
                Text(if (prepaid) stringResource(R.string.cust_fd_cancel_get, view.refundIfCancelled.rupees()) else stringResource(R.string.cust_fd_cancel_cash),
                    color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.cust_fd_keep)) } }
        )
    }
    if (reporting) {
        AlertDialog(
            onDismissRequest = { reporting = false },
            title = { Text(stringResource(R.string.cust_fd_were_you_home)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cust_fd_were_you_home_body, driver, clock(view.failedAt) ?: ""), fontSize = 14.sp)
                    OutlinedTextField(value = reportText, onValueChange = { reportText = it.take(2000) },
                        label = { Text(stringResource(R.string.cust_fd_what_happened)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(enabled = reportText.isNotBlank(), onClick = {
                reporting = false; vm.report(order, reportText, onChanged) }) { Text(stringResource(R.string.cust_fd_send_support)) } },
            dismissButton = { TextButton(onClick = { reporting = false }) { Text(stringResource(R.string.cust_fd_not_now)) } }
        )
    }
}

@Composable
private fun Choice(title: String, sub: String, enabled: Boolean, onClick: () -> Unit) {
    BoondhCard(onClick = if (enabled) onClick else null) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.CustomerPrimary)
        Text(sub, fontSize = 13.sp, color = NeerlyColors.Ink600)
    }
}

@Composable
private fun reasonLabel(reason: String?): String = stringResource(when (reason) {
    "NOT_HOME" -> R.string.cust_fd_reason_not_home
    "WRONG_ADDRESS" -> R.string.cust_fd_reason_wrong_address
    "REFUSED" -> R.string.cust_fd_reason_refused
    "UNSAFE" -> R.string.cust_fd_reason_unsafe
    else -> R.string.cust_fd_reason_other
})

private val IST: ZoneId = ZoneId.of("Asia/Kolkata")

/** "9:41 AM" */
private fun clock(iso: String?): String? = iso?.let { runCatching {
    DateTimeFormatter.ofPattern("h:mm a", com.neerly.mobile.core.util.AppLocale.locale).withZone(IST).format(Instant.parse(it))
}.getOrNull() }

/** "Tue 11 Nov, 9:41 AM" */
private fun deadline(iso: String?): String? = iso?.let { runCatching {
    DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", com.neerly.mobile.core.util.AppLocale.locale).withZone(IST).format(Instant.parse(it))
}.getOrNull() }
