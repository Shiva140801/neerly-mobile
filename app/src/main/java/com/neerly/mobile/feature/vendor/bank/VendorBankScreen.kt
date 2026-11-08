package com.neerly.mobile.feature.vendor.bank

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.dto.AddVendorBankAccountRequest
import com.neerly.mobile.data.dto.VendorBankAccountResponse

/** Boards V04 (first account) and V33 (change account, history). */
@Composable
fun VendorBankScreen(
    onBack: () -> Unit,
    vm: VendorBankViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val cur = s.current
    if (s.editing && cur != null) BackHandler { vm.edit(false) }
    BoondhPage(
        title = stringResource(if (cur == null) R.string.vend_bank_first_title else R.string.vend_payout_account),
        onBack = if (s.editing && cur != null) ({ vm.edit(false) }) else onBack,
        subtitle = stringResource(R.string.vend_bank_sub)
    ) {
        if (s.loading && cur == null) { item { LoadState(true, null, vm::refresh) }; return@BoondhPage }
        if (s.saved) item { Notice(stringResource(R.string.vend_bank_saved), Tone.Ok) }
        if (cur != null && !s.editing) {
            item { CurrentAccount(cur) }
            item { SecondaryButton(stringResource(R.string.vend_change_details), { vm.edit(true) }) }
            if (s.history.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.vend_earlier_accounts)) }
                s.history.forEach { h -> item(key = h.id) {
                    InfoRow("${h.bankName ?: stringResource(R.string.vend_bank)} ${h.accountNumberMasked}",
                        listOfNotNull(h.accountHolderName, stringResource(R.string.vend_date_range, shortDay(h.addedAt), h.deactivatedAt?.let(::shortDay) ?: stringResource(R.string.vend_now))).joinToString(" · "),
                        stringResource(R.string.vend_replaced), NeerlyColors.Ink600)
                } }
            }
            s.error?.let { item { Notice(it, Tone.Bad) } }
        } else {
            item { AccountForm(s.submitting, s.error, replacing = cur != null) { req, confirm -> vm.add(req, confirm) } }
        }
    }
}

@Composable
private fun CurrentAccount(a: VendorBankAccountResponse) {
    BoondhCard {
        if (VendorBankViewModel.checked(a)) Pill(stringResource(R.string.vend_checked), Tone.Ok) else Pill(stringResource(R.string.vend_saved), Tone.Neutral)
        Text("${a.bankName ?: stringResource(R.string.vend_bank_account)} ${a.accountNumberMasked}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        InfoRow(stringResource(R.string.vend_holder), trailing = a.accountHolderName)
        InfoRow(stringResource(R.string.vend_ifsc), trailing = listOfNotNull(a.ifsc, a.branchName).joinToString(" · "))
        InfoRow(stringResource(R.string.vend_type), trailing = a.accountType.lowercase().replaceFirstChar { it.uppercase() })
        InfoRow(stringResource(R.string.vend_added), trailing = shortDay(a.addedAt))
        Text(stringResource(if (VendorBankViewModel.checked(a)) R.string.vend_bank_checked_note else R.string.vend_bank_unchecked_note),
            fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
    }
}

@Composable
private fun AccountForm(submitting: Boolean, error: String?, replacing: Boolean, onSave: (AddVendorBankAccountRequest, String) -> Unit) {
    var holder by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var ifsc by rememberSaveable { mutableStateOf("") }
    var bankName by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Field(stringResource(R.string.vend_holder_name), holder, stringResource(R.string.vend_holder_hint)) { holder = it.take(200) }
        Field(stringResource(R.string.vend_account_number), account, null, KeyboardType.NumberPassword) { account = it.filter(Char::isDigit).take(18) }
        Field(stringResource(R.string.vend_type_again), confirm, if (confirm.isNotEmpty() && confirm != account) stringResource(R.string.vend_not_match_yet) else null,
            KeyboardType.Number) { confirm = it.filter(Char::isDigit).take(18) }
        Field(stringResource(R.string.vend_ifsc), ifsc, null, KeyboardType.Ascii, KeyboardCapitalization.Characters) { ifsc = it.uppercase().filter(Char::isLetterOrDigit).take(11) }
        Field(stringResource(R.string.vend_bank_name_opt), bankName, null) { bankName = it.take(200) }
        if (replacing) Notice(stringResource(R.string.vend_replacing_note), Tone.Warn)
        error?.let { Notice(it, Tone.Bad) }
        PrimaryButton(stringResource(if (submitting) R.string.vend_saving else if (replacing) R.string.vend_save_new_account else R.string.vend_save_account), {
            onSave(AddVendorBankAccountRequest(accountHolderName = holder.trim(), accountNumber = account, ifsc = ifsc,
                bankName = bankName.trim().ifBlank { null }), confirm)
        }, enabled = !submitting, color = NeerlyColors.VendorPrimary)
    }
}

@Composable
private fun Field(label: String, value: String, hint: String?, type: KeyboardType = KeyboardType.Text,
                  caps: KeyboardCapitalization = KeyboardCapitalization.Words, onChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value, onChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = type, capitalization = caps), shape = RoundedCornerShape(12.dp))
        hint?.let { Text(it, fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}
