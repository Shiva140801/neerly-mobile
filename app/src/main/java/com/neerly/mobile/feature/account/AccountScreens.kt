package com.neerly.mobile.feature.account

import android.content.Intent
import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

// ============================== Refunds (boards 51–53) ==============================

data class RefundsUiState(val loading: Boolean = true, val refunds: List<RefundResponse> = emptyList(),
                          val me: UserSummary? = null, val error: String? = null)

@HiltViewModel
class RefundsViewModel @Inject constructor(private val api: NeerlyApi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val _s = MutableStateFlow(RefundsUiState()); val state = _s.asStateFlow()
    init { load() }
    fun load() {
        _s.value = _s.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.myRefunds().content to runCatching { api.me() }.getOrNull() }
                .onSuccess { (r, me) -> _s.value = RefundsUiState(false, r, me) }
                .onFailure { _s.value = RefundsUiState(false, error = it.userMessage("refunds", R.string.acct_refunds_load_failed, strings)) }
        }
    }
}

private fun refundDone(r: RefundResponse) = r.status == "COMPLETED"
@Composable
private fun refundTitle(r: RefundResponse) = when {
    r.reason.startsWith("Automatic: order cancelled") -> stringResource(R.string.acct_refund_cancelled_order)
    r.reason.startsWith("Automatic") -> stringResource(R.string.acct_refund_undelivered_order)
    else -> r.reason.replaceFirstChar { it.uppercase() }
}
@Composable
private fun refundTo(mode: String) = if (mode == "WALLET") stringResource(R.string.acct_refund_to_wallet) else stringResource(R.string.acct_refund_to_how_paid)

@Composable
fun RefundsScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onSetting: () -> Unit, vm: RefundsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val dest = s.me?.let { if (it.refundDestination == "WALLET") stringResource(R.string.acct_refunds_go_wallet) else stringResource(R.string.acct_refunds_go_source) }
    BoondhPage(stringResource(R.string.acct_profile_refunds), onBack = onBack, subtitle = dest) {
        item { TextButton(onClick = onSetting, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(R.string.acct_refunds_change_dest), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) } }
        if (s.loading || s.error != null || s.refunds.isEmpty()) {
            item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.acct_refunds_empty), stringResource(R.string.acct_refunds_empty_body)) }
            return@BoondhPage
        }
        val (onWay, done) = s.refunds.partition { !refundDone(it) }
        if (onWay.isNotEmpty()) { item { SectionLabel(stringResource(R.string.acct_refunds_on_way)) }
            items(onWay, key = { it.id }) { r -> BoondhCard(onClick = { onOpen(r.id) }) {
                InfoRow(refundTitle(r), stringResource(R.string.acct_refunds_row_asked, refundTo(r.refundMode), shortDay(r.requestedAt)), r.amount.rupees())
                Pill(if (r.status == "FAILED") stringResource(R.string.acct_refunds_attention) else stringResource(R.string.acct_refunds_on_way), if (r.status == "FAILED") Tone.Bad else Tone.Warn)
            } } }
        if (done.isNotEmpty()) { item { SectionLabel(stringResource(R.string.acct_refunds_done)) }
            items(done, key = { it.id }) { r -> BoondhCard(onClick = { onOpen(r.id) }) {
                InfoRow(refundTitle(r), stringResource(R.string.acct_refunds_row_done, refundTo(r.refundMode), shortDay(r.completedAt ?: r.requestedAt)), r.amount.rupees(), NeerlyColors.Ok)
            } } }
    }
}

data class RefundDetailUiState(val loading: Boolean = true, val refund: RefundResponse? = null, val error: String? = null)

@HiltViewModel
class RefundDetailViewModel @Inject constructor(private val api: NeerlyApi, saved: SavedStateHandle, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val id: String = checkNotNull(saved["refundId"])
    private val _s = MutableStateFlow(RefundDetailUiState()); val state = _s.asStateFlow()
    init { load() }
    fun load() { viewModelScope.launch {
        _s.value = RefundDetailUiState(loading = true)
        runCatching { api.refund(id) }.onSuccess { _s.value = RefundDetailUiState(false, it) }
            .onFailure { _s.value = RefundDetailUiState(false, error = it.userMessage("refund", R.string.acct_refund_load_failed, strings)) }
    } }
}

@Composable
fun RefundDetailScreen(onBack: () -> Unit, onSetting: () -> Unit, onHelp: () -> Unit, vm: RefundDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val cfg = LocalAppConfig.current
    val r = s.refund
    BoondhPage(r?.let { refundTitle(it) } ?: stringResource(R.string.acct_refund), onBack = onBack, backLabel = stringResource(R.string.acct_profile_refunds)) {
        if (r == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        item { PageTitle(stringResource(R.string.acct_refund_amount_to, r.amount.rupees(), refundTo(r.refundMode)), 24) }
        item { BoondhCard {
            InfoRow(stringResource(R.string.acct_refund_started), shortDay(r.requestedAt))
            InfoRow(if (refundDone(r)) stringResource(R.string.acct_refund_sent) else stringResource(R.string.acct_refund_being_sent),
                (r.completedAt ?: r.approvedAt)?.let(::shortDay) ?: stringResource(R.string.acct_refund_in_progress))
            InfoRow(stringResource(R.string.acct_refund_shows),
                if (r.refundMode == "WALLET") stringResource(R.string.acct_refund_instantly) else stringResource(R.string.acct_working_days, cfg.refundDaysMin, cfg.refundDaysMax))
        } }
        when {
            r.status == "FAILED" -> item { Notice(stringResource(R.string.acct_refund_failed, r.failureReason ?: stringResource(R.string.acct_refund_bank_declined)), Tone.Bad) }
            r.refundMode != "WALLET" -> item {
                Notice(stringResource(R.string.acct_refund_bank_slow), Tone.Info)
            }
        }
        item { SecondaryButton(stringResource(R.string.acct_refunds_change_dest), onSetting) }
        item { TextButton(onClick = onHelp, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(R.string.acct_refund_help, cfg.refundDaysMax), color = NeerlyColors.CustomerPrimary) } }
    }
}

@HiltViewModel
class RefundSettingViewModel @Inject constructor(private val api: NeerlyApi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    val choice = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    init { viewModelScope.launch { choice.value = runCatching { api.me().refundDestination }.getOrNull() ?: "WALLET" } }
    fun save(onDone: () -> Unit) { val c = choice.value ?: return; viewModelScope.launch {
        saving.value = true
        runCatching { api.setRefundDestination(RefundDestinationRequest(c)) }
            .onSuccess { saving.value = false; onDone() }
            .onFailure { saving.value = false; message.value = it.userMessage("refund setting", R.string.acct_save_failed, strings) }
    } }
}

@Composable
fun RefundSettingScreen(onBack: () -> Unit, vm: RefundSettingViewModel = hiltViewModel()) {
    val choice by vm.choice.collectAsState(); val saving by vm.saving.collectAsState(); val msg by vm.message.collectAsState()
    val cfg = LocalAppConfig.current
    val options = listOf(
        Triple("WALLET", stringResource(R.string.acct_refund_to_wallet), stringResource(R.string.acct_refund_opt_wallet_sub)),
        Triple("ORIGINAL", stringResource(R.string.acct_refund_opt_source), stringResource(R.string.acct_refund_opt_source_sub, cfg.refundDaysMin, cfg.refundDaysMax))
    )
    BoondhPage(stringResource(R.string.acct_refund_setting_title), onBack = onBack, subtitle = stringResource(R.string.acct_refund_setting_sub),
        footer = { PrimaryButton(if (saving) stringResource(R.string.acct_saving) else stringResource(R.string.acct_save), { vm.save(onBack) }, enabled = choice != null && !saving) }) {
        items(options) { (key, title, sub) ->
            BoondhCard(Modifier.selectable(selected = choice == key, onClick = { vm.choice.value = key }, role = Role.RadioButton)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = choice == key, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = NeerlyColors.CustomerPrimary))
                    Spacer(Modifier.width(8.dp)); Column { Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                        Text(sub, fontSize = 13.sp, color = NeerlyColors.Ink600) }
                }
            }
        }
        item { Text(stringResource(R.string.acct_refund_setting_note),
            fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600) }
        msg?.let { m -> item { Notice(m, Tone.Bad) } }
    }
}

// ============================== Edit profile, change number (58, 76) ==============================

@HiltViewModel
class EditProfileViewModel @Inject constructor(private val api: NeerlyApi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    val me = MutableStateFlow<UserSummary?>(null)
    val saving = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    init { viewModelScope.launch { me.value = runCatching { api.me() }.getOrNull() } }
    fun save(name: String, email: String, onDone: () -> Unit) { viewModelScope.launch {
        saving.value = true; message.value = null
        runCatching { api.updateProfile(UpdateProfileRequest(displayName = name.trim(), email = email.trim().ifBlank { null })) }
            .onSuccess { saving.value = false; onDone() }
            .onFailure { saving.value = false; message.value = it.userMessage("profile", R.string.acct_details_save_failed, strings) }
    } }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit, onChangePhone: () -> Unit, onDelete: () -> Unit, vm: EditProfileViewModel = hiltViewModel()) {
    val me by vm.me.collectAsState(); val saving by vm.saving.collectAsState(); val msg by vm.message.collectAsState()
    var name by remember(me) { mutableStateOf(me?.displayName.orEmpty()) }
    var email by remember(me) { mutableStateOf(me?.email.orEmpty()) }
    BoondhPage(stringResource(R.string.acct_details_title), onBack = onBack, backLabel = stringResource(R.string.acct_profile_title),
        footer = { PrimaryButton(if (saving) stringResource(R.string.acct_saving) else stringResource(R.string.acct_save), { vm.save(name, email, onBack) }, enabled = name.trim().length >= 2 && !saving) }) {
        if (me == null) { item { LoadState(true, null, {}) }; return@BoondhPage }
        item { OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.acct_details_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { BoondhCard {
            InfoRow(stringResource(R.string.acct_details_mobile), me!!.phoneMask)
            TextButton(onClick = onChangePhone, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.acct_details_change_number), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
            Text(stringResource(R.string.acct_details_change_note), fontSize = 13.sp, color = NeerlyColors.Ink600)
        } }
        item { OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.acct_details_email)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()) }
        msg?.let { m -> item { Notice(m, Tone.Bad) } }
        item { TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 44.dp)) {
            Text(stringResource(R.string.acct_delete_cta), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold) } }
    }
}

@HiltViewModel
class ChangePhoneViewModel @Inject constructor(private val api: NeerlyApi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    val step = MutableStateFlow(1)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val done = MutableStateFlow<String?>(null)
    fun sendCode(phone: String) { message.value = null; step.value = 2 }   // dev: the OTP is the dev code
    fun verify(phone: String, code: String) { viewModelScope.launch {
        busy.value = true; message.value = null
        runCatching { api.changePhoneDev(DevChangePhoneRequest(phone, code)) }
            .onSuccess { busy.value = false; done.value = it.phoneNumber; step.value = 3 }
            .onFailure { e -> busy.value = false
                message.value = if ((e as? HttpException)?.code() == 409) strings.get(R.string.acct_phone_taken)
                                else e.userMessage("change number", R.string.acct_phone_verify_failed, strings) }
    } }
}

/** Board 76. Production verifies the new number with Firebase; debug builds use the dev OTP. */
@Composable
fun ChangePhoneScreen(onBack: () -> Unit, vm: ChangePhoneViewModel = hiltViewModel()) {
    val step by vm.step.collectAsState(); val busy by vm.busy.collectAsState()
    val msg by vm.message.collectAsState(); val done by vm.done.collectAsState()
    var phone by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }
    BoondhPage(when (step) { 1 -> stringResource(R.string.acct_phone_step1); 2 -> stringResource(R.string.acct_phone_step2); else -> stringResource(R.string.acct_phone_step3) },
        onBack = onBack, subtitle = stringResource(R.string.acct_step_of, step, 3),
        footer = { when (step) {
            1 -> PrimaryButton(stringResource(R.string.acct_phone_send), { vm.sendCode(phone) }, enabled = phone.length == 10)
            2 -> PrimaryButton(if (busy) stringResource(R.string.acct_phone_verifying) else stringResource(R.string.acct_phone_verify), { vm.verify(phone, code) }, enabled = code.length == 6 && !busy)
            else -> PrimaryButton(stringResource(R.string.acct_phone_back), onBack)
        } }) {
        when (step) {
            1 -> {
                item { OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, label = { Text(stringResource(R.string.acct_phone_new)) },
                    prefix = { Text("+91 ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()) }
                item { Text(stringResource(R.string.acct_phone_note),
                    fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600) }
            }
            2 -> {
                item { Text(stringResource(R.string.acct_phone_sent, "+91 $phone"), fontSize = 15.sp, color = NeerlyColors.Ink700) }
                item { OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text(stringResource(R.string.acct_phone_code)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth()) }
            }
            else -> item { Notice(stringResource(R.string.acct_phone_done, done ?: ""), Tone.Ok) }
        }
        msg?.let { m -> item { Notice(m, Tone.Bad) } }
    }
}

// ============================== Delete account (60) ==============================

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(private val api: NeerlyApi, private val moshi: com.squareup.moshi.Moshi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    val blocks = MutableStateFlow<List<String>>(emptyList())
    val busy = MutableStateFlow(false)
    val deleted = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    fun delete() { viewModelScope.launch {
        busy.value = true; message.value = null; blocks.value = emptyList()
        runCatching { api.deleteAccount() }
            .onSuccess { busy.value = false; deleted.value = true }
            .onFailure { e -> busy.value = false
                // The server lists what must be settled first; show each as a line.
                val body = (e as? HttpException)?.response()?.errorBody()?.string()
                val parsed = body?.let { runCatching { org.json.JSONObject(it).getJSONObject("error") }.getOrNull() }
                val list = parsed?.optJSONObject("details")?.optJSONArray("blocks")
                if (list != null) blocks.value = (0 until list.length()).map { list.getJSONObject(it).optString("message") }
                else message.value = e.userMessage("delete account", R.string.acct_delete_failed, strings)
            }
    } }
}

@Composable
fun DeleteAccountScreen(onBack: () -> Unit, onDeleted: () -> Unit, vm: DeleteAccountViewModel = hiltViewModel()) {
    val blocks by vm.blocks.collectAsState(); val busy by vm.busy.collectAsState()
    val deleted by vm.deleted.collectAsState(); val msg by vm.message.collectAsState()
    var sure by remember { mutableStateOf(false) }
    BoondhPage(if (deleted) stringResource(R.string.acct_delete_done_title) else stringResource(R.string.acct_delete_title), onBack = onBack,
        subtitle = if (deleted) null else stringResource(R.string.acct_delete_sub),
        footer = { if (deleted) PrimaryButton(stringResource(R.string.acct_ok), onDeleted) else {
            PrimaryButton(if (busy) stringResource(R.string.acct_checking) else stringResource(R.string.acct_delete_cta), vm::delete, enabled = sure && !busy, color = NeerlyColors.Err)
            SecondaryButton(stringResource(R.string.acct_delete_keep), onBack)
        } }) {
        if (deleted) { item { Notice(stringResource(R.string.acct_delete_done_body), Tone.Ok) }; return@BoondhPage }
        item { BoondhCard {
            InfoRow(stringResource(R.string.acct_delete_orders), stringResource(R.string.acct_delete_orders_sub))
            InfoRow(stringResource(R.string.acct_delete_jars), stringResource(R.string.acct_delete_jars_sub))
            InfoRow(stringResource(R.string.acct_delete_wallet), stringResource(R.string.acct_delete_wallet_sub))
            InfoRow(stringResource(R.string.acct_profile_subs), stringResource(R.string.acct_delete_subs_sub))
        } }
        if (blocks.isNotEmpty()) { item { SectionLabel(stringResource(R.string.acct_delete_still)) }; items(blocks) { Notice(it, Tone.Warn) } }
        msg?.let { m -> item { Notice(m, Tone.Bad) } }
        item { Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = sure, onClick = { sure = !sure }, role = Role.Checkbox),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sure, onCheckedChange = null)
            Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.acct_delete_confirm), fontSize = 14.sp, color = NeerlyColors.Ink700)
        } }
    }
}

// ============================== Refer & earn (55) ==============================

@HiltViewModel
class ReferEarnViewModel @Inject constructor(private val api: NeerlyApi) : ViewModel() {
    val me = MutableStateFlow<UserSummary?>(null)
    val rows = MutableStateFlow<List<ReferralRow>>(emptyList())
    init { viewModelScope.launch {
        me.value = runCatching { api.me() }.getOrNull()
        rows.value = runCatching { api.myReferrals() }.getOrDefault(emptyList())
    } }
}

@Composable
fun ReferEarnScreen(onBack: () -> Unit, vm: ReferEarnViewModel = hiltViewModel()) {
    val me by vm.me.collectAsState(); val rows by vm.rows.collectAsState()
    val cfg = LocalAppConfig.current; val context = LocalContext.current
    val code = me?.referralCode
    val earned = rows.filter { it.status == "REWARDED" }.mapNotNull { it.referrerReward }.fold(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
        val shareText = stringResource(R.string.acct_refer_share_text, code ?: "", cfg.refereeReward.rupees())
    val shareTitle = stringResource(R.string.acct_profile_share)
    BoondhPage(stringResource(R.string.acct_refer_title), onBack = onBack, backLabel = stringResource(R.string.acct_profile_title),
        subtitle = stringResource(R.string.acct_refer_sub, cfg.refereeReward.rupees(), cfg.referrerReward.rupees()),
        footer = { PrimaryButton(stringResource(R.string.acct_refer_share), {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText), shareTitle))
        }, enabled = code != null) }) {
        item { BoondhCard {
            SectionLabel(stringResource(R.string.acct_refer_code))
            Text(code ?: "…", fontFamily = FontFamily.Serif, fontSize = 34.sp, color = NeerlyColors.Ink900)
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${rows.size}", stringResource(R.string.acct_refer_joined), Modifier.weight(1f))
            Stat("${rows.count { it.status == "REWARDED" }}", stringResource(R.string.acct_refer_ordered), Modifier.weight(1f))
            Stat(earned.rupees(), stringResource(R.string.acct_refer_earned), Modifier.weight(1f))
        } }
        if (rows.isNotEmpty()) { item { SectionLabel(stringResource(R.string.acct_refer_friends)) }
            items(rows, key = { it.id }) { r -> InfoRow(if (r.status == "REWARDED") stringResource(R.string.acct_refer_row_ordered) else stringResource(R.string.acct_refer_row_signed),
                stringResource(R.string.acct_refer_row_joined, shortDay(r.createdAt)), r.referrerReward?.takeIf { r.status == "REWARDED" }?.let { "+${it.rupees()}" }, NeerlyColors.Ok) } }
        item { Text(stringResource(R.string.acct_refer_note), fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) = BoondhCard(modifier) {
    Text(value, fontFamily = FontFamily.Serif, fontSize = 24.sp, color = NeerlyColors.Ink900)
    Text(label, fontSize = 13.sp, color = NeerlyColors.Ink600)
}

// ============================== Invoice (57) ==============================

@HiltViewModel
class InvoiceViewModel @Inject constructor(private val api: NeerlyApi, saved: SavedStateHandle, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val orderId: String = checkNotNull(saved["orderId"])
    val html = MutableStateFlow<String?>(null)
    val error = MutableStateFlow<String?>(null)
    init { load() }
    fun load() { viewModelScope.launch {
        error.value = null
        runCatching { api.invoiceHtml(orderId).string() }.onSuccess { html.value = it }
            .onFailure { error.value = it.userMessage("invoice", R.string.acct_invoice_not_ready, strings) }
    } }
}

/** Board 57 — the backend renders the GST tax invoice; the app shows it as-is. */
@Composable
fun InvoiceScreen(onBack: () -> Unit, vm: InvoiceViewModel = hiltViewModel()) {
    val html by vm.html.collectAsState(); val error by vm.error.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 12.dp).heightIn(min = 44.dp)) {
            Text("‹ " + stringResource(R.string.acct_order), color = NeerlyColors.CustomerPrimary, fontSize = 15.sp) }
        when {
            html != null -> AndroidView(factory = { ctx -> android.webkit.WebView(ctx).apply { settings.javaScriptEnabled = false } },
                update = { it.loadDataWithBaseURL(null, html!!, "text/html", "utf-8", null) }, modifier = Modifier.fillMaxSize())
            error != null -> Box(Modifier.padding(20.dp)) { LoadState(false, error, vm::load) }
            else -> LoadState(true, null, {})
        }
    }
}
