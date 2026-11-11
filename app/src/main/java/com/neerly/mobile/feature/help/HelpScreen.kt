package com.neerly.mobile.feature.help

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.ComplaintResponse
import com.neerly.mobile.data.dto.FileComplaintRequest
import com.neerly.mobile.data.dto.OrderResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A reason on board 27, the complaint category it files under, and the hint shown in the box. */
enum class HelpReason(@StringRes val labelRes: Int, val category: String, @StringRes val hintRes: Int, val whileOnTheWay: Boolean, val afterDelivery: Boolean) {
    LATE(R.string.acct_help_r_late, "LATE_DELIVERY", R.string.acct_help_h_late, true, false),
    WRONG_ADDRESS(R.string.acct_help_r_address, "OTHER", R.string.acct_help_h_address, true, false),
    CANT_FIND(R.string.acct_help_r_cant_find, "OTHER", R.string.acct_help_h_cant_find, true, false),
    NOT_HOME(R.string.acct_help_r_not_home, "OTHER", R.string.acct_help_h_not_home, true, false),
    // After the jars arrived — the complaint categories the backend already has.
    WATER(R.string.acct_help_r_water, "WATER_QUALITY", R.string.acct_help_h_water, false, true),
    JAR(R.string.acct_help_r_jar, "DAMAGED_CONTAINER", R.string.acct_help_h_jar, false, true),
    SHORT(R.string.acct_help_r_short, "SHORT_QUANTITY", R.string.acct_help_h_short, false, true),
    WRONG_ITEM(R.string.acct_help_r_wrong_item, "WRONG_PRODUCT", R.string.acct_help_h_wrong_item, false, true),
    DRIVER(R.string.acct_help_r_driver, "DRIVER_BEHAVIOUR", R.string.acct_help_h_driver, false, true),
    CHARGED(R.string.acct_help_r_charged, "OVERCHARGED", R.string.acct_help_h_charged, true, true),
    OTHER(R.string.acct_help_r_other, "OTHER", R.string.acct_help_h_other, true, true);

    companion object {
        /** The reasons that make sense for where this order is. */
        fun forOrder(status: String?): List<HelpReason> {
            val delivered = status in setOf("DELIVERED", "CANCELLED", "FAILED")
            return entries.filter { if (delivered) it.afterDelivery else it.whileOnTheWay }
        }
    }
}

data class HelpUiState(
    val order: OrderResponse? = null,
    val reason: HelpReason? = null,
    val details: String = "",
    val sending: Boolean = false,
    val sent: ComplaintResponse? = null,
    val error: String? = null
) {
    val canSend get() = reason != null && !sending && sent == null &&
        (reason != HelpReason.OTHER || details.isNotBlank())
}

@HiltViewModel
class HelpViewModel @Inject constructor(private val api: NeerlyApi, saved: SavedStateHandle, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val orderId: String? = saved.get<String>("orderId")?.takeIf { it.isNotBlank() }
    private val _state = MutableStateFlow(HelpUiState())
    val state: StateFlow<HelpUiState> = _state.asStateFlow()

    init {
        orderId?.let { id -> viewModelScope.launch {
            runCatching { api.order(id) }.onSuccess { o -> _state.value = _state.value.copy(order = o) }
        } }
    }

    fun pick(r: HelpReason) { _state.value = _state.value.copy(reason = r, error = null) }
    fun details(v: String) { _state.value = _state.value.copy(details = v.take(2000)) }

    /** Files it as a complaint on the order, so it lands in the support desk and the thread. */
    fun send() {
        val s = _state.value
        val r = s.reason ?: return
        if (!s.canSend) return
        _state.value = s.copy(sending = true, error = null)
        viewModelScope.launch {
            runCatching {
                api.fileComplaint(FileComplaintRequest(
                    orderId = orderId, category = r.category, subject = strings.get(r.labelRes),
                    description = s.details.trim().ifBlank { strings.get(r.labelRes) }
                ))
            }.onSuccess { _state.value = _state.value.copy(sending = false, sent = it) }
             .onFailure { _state.value = _state.value.copy(sending = false,
                 error = it.userMessage("help request", R.string.acct_help_send_failed, strings)) }
        }
    }
}

/** Board 27 — help with an order: pick what's wrong, add a line, it goes to Boondh support. */
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onOpenThread: (String) -> Unit = {},
    onAllRequests: () -> Unit = {},
    vm: HelpViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val cfg = LocalAppConfig.current
    val o = s.order
    val driver = o?.driverName?.substringBefore(' ')
    BoondhPage(
        title = if (s.sent != null) stringResource(R.string.acct_help_sent_title) else stringResource(R.string.acct_help_title),
        onBack = onBack,
        subtitle = o?.let { listOfNotNull(it.orderNumber, driver?.let { d -> stringResource(R.string.acct_help_with, d) }).joinToString(" · ") },
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            if (s.sent == null) PrimaryButton(when { s.sending -> stringResource(R.string.acct_evt_sending); s.reason == null -> stringResource(R.string.acct_help_pick_what); else -> stringResource(R.string.acct_help_send) }, vm::send, enabled = s.canSend)
            else {
                PrimaryButton(stringResource(R.string.acct_help_open_thread), { onOpenThread(s.sent!!.id) })
                SecondaryButton(stringResource(R.string.acct_help_see_all), onAllRequests)
            }
        }
    ) {
        val sent = s.sent
        if (sent != null) {
            item { BoondhCard {
                Text("“${s.reason?.let { reasonLabel(it, driver) }}”", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                if (s.details.isNotBlank()) Text(s.details, fontSize = 14.sp, color = NeerlyColors.Ink700)
                val hrs = cfg.complaintFirstReplyHours
                val reply = if (driver != null) pluralStringResource(R.plurals.acct_help_reply_driver, hrs, hrs, driver)
                    else pluralStringResource(R.plurals.acct_help_reply, hrs, hrs)
                val done = s.order?.status in setOf("DELIVERED", "CANCELLED", "FAILED")
                Text(if (done) reply else stringResource(R.string.acct_help_reply_keeps_coming, reply),
                    fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600)
            } }
            return@BoondhPage
        }
        val reasons = HelpReason.forOrder(s.order?.status)
        // Board Help: reasons as hairline choice rows; the note field is always there and
        // says what to do first, instead of appearing out of nowhere after a pick.
        items(reasons.size) { i ->
            val r = reasons[i]
            com.neerly.mobile.core.design.ChoiceRow(reasonLabel(r, driver), s.reason == r, { vm.pick(r) }, divider = true)
        }
        item {
            val r = s.reason
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (r == HelpReason.OTHER) stringResource(R.string.acct_help_more) else stringResource(R.string.acct_help_more_optional),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                OutlinedTextField(value = s.details, onValueChange = vm::details, enabled = r != null,
                    modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(14.dp),
                    placeholder = { Text(if (r == null) stringResource(R.string.acct_help_pick_first) else stringResource(r.hintRes)) },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = NeerlyColors.Paper, focusedContainerColor = NeerlyColors.Paper,
                        disabledContainerColor = NeerlyColors.Paper, unfocusedBorderColor = NeerlyColors.Ink300, disabledBorderColor = NeerlyColors.Ink200))
            }
        }
        if (s.order?.status != "DELIVERED") item {
            Text(stringResource(R.string.acct_help_after_arrive), fontSize = 13.sp, color = NeerlyColors.Ink600) }
        // "Rather talk? Call Boondh support, 6 AM – 10 PM" when a number is set; otherwise just the hours.
        item {
            val hours = stringResource(R.string.acct_help_hours, cfg.clock(cfg.supportOpens), cfg.clock(cfg.supportCloses))
            if (cfg.supportPhone.isNotBlank()) CallSupport(cfg.supportPhone,
                stringResource(R.string.acct_help_rather_talk, cfg.clock(cfg.supportOpens), cfg.clock(cfg.supportCloses)))
            else Text(hours, fontSize = 14.sp, color = NeerlyColors.Ink600)
        }
    }
}

/** Shown only when admins have set `support.phone`; opens the dialler, never calls by itself. */
@Composable
private fun CallSupport(phone: String, label: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    TextButton(onClick = {
        runCatching {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone")))
        }
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, color = NeerlyColors.CustomerPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** "Ravi can't find my place" when we know the driver's name. */
@Composable
private fun reasonLabel(r: HelpReason, driver: String?): String =
    if (r == HelpReason.CANT_FIND && driver != null) stringResource(R.string.acct_help_r_cant_find_named, driver) else stringResource(r.labelRes)
