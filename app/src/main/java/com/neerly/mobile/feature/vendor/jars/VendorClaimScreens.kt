package com.neerly.mobile.feature.vendor.jars

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
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

@Composable
internal fun claimPill(c: DamageClaim): Pair<String, Tone> = when (c.status) {
    "FILED" -> stringResource(R.string.vend_cl_sent) to Tone.Info
    "UNDER_REVIEW" -> stringResource(R.string.vend_cl_review) to Tone.Info
    "APPROVED" -> (c.awardedAmount?.let { stringResource(R.string.vend_cl_approved_x, it.rupees()) } ?: stringResource(R.string.vend_cl_approved)) to Tone.Ok
    "REJECTED" -> stringResource(R.string.vend_cl_rejected) to Tone.Bad
    "WITHDRAWN" -> stringResource(R.string.vend_cl_withdrawn) to Tone.Neutral
    else -> c.status.lowercase() to Tone.Neutral
}

// ---------------- V26 list ----------------

data class ClaimsUiState(val loading: Boolean = true, val claims: List<DamageClaim> = emptyList(), val error: String? = null)

@HiltViewModel
class VendorClaimsViewModel @Inject constructor(
    private val jars: VendorJarsApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(ClaimsUiState())
    val state = _state.asStateFlow()
    init { load() }
    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { jars.claims().content.sortedWith(compareByDescending<DamageClaim> { it.open }.thenByDescending { it.filedAt }) }
                .onSuccess { _state.value = ClaimsUiState(loading = false, claims = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage("vendor claims", strings.get(R.string.vend_err_load_claims))) }
        }
    }
}

/** Board V26 — damage claims. Boondh decides; the shop asks. */
@Composable
fun VendorClaimsScreen(onBack: () -> Unit, onOpen: (claimId: String) -> Unit = {}, onPickups: () -> Unit = {}, vm: VendorClaimsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    OnReturn(vm::load)
    BoondhPage(title = stringResource(R.string.vend_damaged_jars), onBack = onBack, backLabel = stringResource(R.string.vend_tab_jars),
        subtitle = stringResource(R.string.vend_damaged_jars_sub)) {
        if (s.claims.isEmpty()) {
            item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.vend_no_claims), stringResource(R.string.vend_no_claims_sub)) }
            item { SecondaryButton(stringResource(R.string.vend_go_pickups), onPickups, color = NeerlyColors.VendorPrimary) }
            return@BoondhPage
        }
        items(s.claims, key = { it.id }) { c ->
            val (label, tone) = claimPill(c)
            BoondhCard(onClick = { onOpen(c.id) }) {
                InfoRow(c.reason, listOfNotNull(stringResource(R.string.vend_filed_on, shortDay(c.filedAt)),
                    c.resolvedAt?.let { stringResource(R.string.vend_decided_on, shortDay(it)) }).joinToString(" · "), c.claimAmount.rupees())
                Pill(label, tone)
                c.adminNote?.takeIf { !c.open }?.let { Text(stringResource(R.string.vend_boondh_says, it), fontSize = 13.sp, color = NeerlyColors.Ink700) }
            }
        }
    }
}

// ---------------- V41 file ----------------

/** What went wrong — the English word is what Boondh's reviewers read; the chip shows the shop's language. */
internal val CLAIM_REASONS = listOf("Cracked", "Leaking", "Cap or handle broken", "Dirty or smells", "Other")
internal val CLAIM_REASON_LABELS = mapOf("Cracked" to R.string.vend_cr_cracked, "Leaking" to R.string.vend_cr_leaking,
    "Cap or handle broken" to R.string.vend_cr_cap, "Dirty or smells" to R.string.vend_cr_dirty, "Other" to R.string.vend_cr_other)

data class ClaimNewUiState(
    val loading: Boolean = true,
    val ret: JarReturn? = null,
    val amount: String = "",
    val kind: String? = null,
    val note: String = "",
    val sending: Boolean = false,
    val filed: DamageClaim? = null,
    val error: String? = null
) {
    val amountValue: BigDecimal? get() = amount.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    val photos: List<String> get() = listOfNotNull(ret?.pickupPhotoUrl)
    /** Board V41: a reason, an amount and at least one photo. The server caps the amount at the deposit. */
    val valid: Boolean get() = ret?.depositId != null && photos.isNotEmpty() && amountValue != null && kind != null &&
        (kind != "Other" || note.isNotBlank())
    val reason: String get() = listOfNotNull(kind, note.trim().ifBlank { null }).joinToString(" — ").take(500)
}

@HiltViewModel
class VendorClaimNewViewModel @Inject constructor(
    private val jars: VendorJarsApi,
    saved: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val returnId: String = saved["returnId"] ?: error("returnId required")
    private val _state = MutableStateFlow(ClaimNewUiState())
    val state = _state.asStateFlow()
    init { load() }
    fun load() {
        viewModelScope.launch {
            runCatching { jars.returns(listOf("COMPLETED"), 50).first { it.id == returnId } }
                .onSuccess { _state.value = _state.value.copy(loading = false, ret = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage("vendor claim return", strings.get(R.string.vend_err_find_pickup))) }
        }
    }
    fun update(f: ClaimNewUiState.() -> ClaimNewUiState) { _state.value = _state.value.f() }
    fun send() {
        val s = _state.value
        if (!s.valid) return
        _state.value = s.copy(sending = true, error = null)
        viewModelScope.launch {
            runCatching { jars.file(FileClaim(s.ret!!.depositId!!, s.ret.id, s.amountValue!!, s.reason, s.photos)) }
                .onSuccess { _state.value = _state.value.copy(sending = false, filed = it) }
                .onFailure { _state.value = _state.value.copy(sending = false,
                    error = it.userMessage("vendor file claim", strings.get(R.string.vend_err_send_claim))) }
        }
    }
}

@Composable
fun VendorClaimNewScreen(onBack: () -> Unit, onFiled: (claimId: String) -> Unit = {}, vm: VendorClaimNewViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.filed) { s.filed?.let { onFiled(it.id) } }
    BoondhPage(
        title = stringResource(R.string.vend_damage_claim), onBack = onBack, backLabel = stringResource(R.string.vend_pickups),
        subtitle = stringResource(R.string.vend_claim_new_sub),
        footer = if (s.ret == null) null else ({
            s.error?.let { Notice(it, Tone.Bad) }
            PrimaryButton(stringResource(if (s.sending) R.string.vend_sending else R.string.vend_send_claim), vm::send, enabled = s.valid && !s.sending, color = NeerlyColors.VendorPrimary)
        })
    ) {
        val r = s.ret
        if (r == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.vend_picked_up), r.completedAt?.let { "${shortDay(it)} ${timeOf(it)}" } ?: "—")
                InfoRow(stringResource(R.string.vend_jars_label), trailing = "${r.containerCount}")
                InfoRow(stringResource(R.string.vend_evidence), stringResource(if (s.photos.isEmpty()) R.string.vend_no_photo else R.string.vend_photo_attached))
            }
        }
        item { SectionLabel(stringResource(R.string.vend_whats_wrong)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CLAIM_REASONS.take(3).forEach { k -> FilterChip(selected = s.kind == k, onClick = { vm.update { copy(kind = k) } },
                    label = { Text(stringResource(CLAIM_REASON_LABELS.getValue(k))) }, modifier = Modifier.heightIn(min = 44.dp)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CLAIM_REASONS.drop(3).forEach { k -> FilterChip(selected = s.kind == k, onClick = { vm.update { copy(kind = k) } },
                    label = { Text(stringResource(CLAIM_REASON_LABELS.getValue(k))) }, modifier = Modifier.heightIn(min = 44.dp)) }
            }
        }
        item {
            OutlinedTextField(s.amount, { v -> vm.update { copy(amount = v.filter { it.isDigit() || it == '.' }.take(8)) } },
                label = { Text(stringResource(R.string.vend_you_ask_for)) }, prefix = { Text("₹") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.vend_rest_back_customer), fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
        item {
            OutlinedTextField(s.note, { v -> vm.update { copy(note = v.take(400)) } }, label = { Text(stringResource(R.string.vend_note_boondh)) },
                minLines = 3, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        }
    }
}

// ---------------- V42 detail ----------------

data class ClaimDetailUiState(val loading: Boolean = true, val claim: DamageClaim? = null, val confirming: Boolean = false,
                              val working: Boolean = false, val error: String? = null)

@HiltViewModel
class VendorClaimDetailViewModel @Inject constructor(
    private val jars: VendorJarsApi,
    saved: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val claimId: String = saved["claimId"] ?: error("claimId required")
    private val _state = MutableStateFlow(ClaimDetailUiState())
    val state = _state.asStateFlow()
    init { load() }
    fun load() {
        viewModelScope.launch {
            runCatching { jars.claim(claimId) }
                .onSuccess { _state.value = _state.value.copy(loading = false, claim = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage("vendor claim", strings.get(R.string.vend_err_load_claim))) }
        }
    }
    fun askWithdraw(on: Boolean) { _state.value = _state.value.copy(confirming = on) }
    fun withdraw() {
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { jars.withdraw(claimId) }
                .onSuccess { _state.value = _state.value.copy(working = false, confirming = false, claim = it) }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage("vendor withdraw claim", strings.get(R.string.vend_err_withdraw_claim))) }
        }
    }
}

@Composable
fun VendorClaimDetailScreen(onBack: () -> Unit, onSupport: () -> Unit = {}, vm: VendorClaimDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val c = s.claim
    BoondhPage(title = c?.let { claimPill(it).first } ?: stringResource(R.string.vend_damage_claim), onBack = onBack, backLabel = stringResource(R.string.vend_claims),
        subtitle = c?.let { stringResource(R.string.vend_claim_detail_sub, it.claimAmount.rupees(), it.reason) }) {
        if (c == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.vend_claim_sent), "${shortDay(c.filedAt)} ${timeOf(c.filedAt)} · " +
                    pluralStringResource(R.plurals.vend_n_photos, c.evidencePhotos.size, c.evidencePhotos.size))
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_boondh_reviews), if (c.open) stringResource(R.string.vend_within_2_days) else c.resolvedAt?.let { stringResource(R.string.vend_decided_day, shortDay(it)) })
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_decision), when (c.status) {
                    "APPROVED" -> stringResource(R.string.vend_dec_approved, c.awardedAmount?.rupees() ?: "")
                    "REJECTED" -> stringResource(R.string.vend_dec_rejected)
                    "WITHDRAWN" -> stringResource(R.string.vend_dec_withdrawn)
                    else -> stringResource(R.string.vend_dec_pending)
                })
                c.adminNote?.let { Text(stringResource(R.string.vend_boondh_says, it), fontSize = 14.sp, color = NeerlyColors.Ink700) }
            }
        }
        item { BoondhCard { InfoRow(stringResource(R.string.vend_you_ask_for), trailing = c.claimAmount.rupees()); c.awardedAmount?.let { InfoRow(stringResource(R.string.vend_boondh_awarded), trailing = it.rupees()) } } }
        s.error?.let { item { Notice(it, Tone.Bad) } }
        if (c.open) item {
            if (s.confirming) {
                Notice(stringResource(R.string.vend_withdraw_confirm), Tone.Warn)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(stringResource(R.string.vend_keep_claim), { vm.askWithdraw(false) }, modifier = Modifier.weight(1f), color = NeerlyColors.VendorPrimary)
                    PrimaryButton(stringResource(if (s.working) R.string.vend_withdrawing else R.string.vend_withdraw), vm::withdraw, enabled = !s.working,
                        modifier = Modifier.weight(1f), color = NeerlyColors.Err)
                }
            } else SecondaryButton(stringResource(R.string.vend_withdraw_claim), { vm.askWithdraw(true) }, color = NeerlyColors.Err)
        }
        item { TextButton(onClick = onSupport, modifier = Modifier.heightIn(min = 44.dp)) {
            Text(stringResource(R.string.vend_talk_boondh), color = NeerlyColors.VendorPrimary, fontWeight = FontWeight.SemiBold) } }
    }
}
