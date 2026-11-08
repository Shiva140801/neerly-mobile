package com.neerly.mobile.feature.wallet

import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R
import com.neerly.mobile.core.design.rupees

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.dto.WithdrawalResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class WithdrawUiState(
    val loading: Boolean = true,
    val wallet: WalletResponse? = null,
    val sending: Boolean = false,
    val done: WithdrawalResponse? = null,
    val error: String? = null
)

@HiltViewModel
class WithdrawViewModel @Inject constructor(private val repo: CustomerRepository,
    private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val _state = MutableStateFlow(WithdrawUiState())
    val state: StateFlow<WithdrawUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repo.wallet() }
                .onSuccess { _state.value = WithdrawUiState(loading = false, wallet = it) }
                .onFailure { _state.value = WithdrawUiState(loading = false, error = it.userMessage("wallet", R.string.acct_withdraw_load_failed, strings)) }
        }
    }

    fun withdraw(amount: BigDecimal, upiId: String) {
        _state.value = _state.value.copy(sending = true, error = null)
        viewModelScope.launch {
            runCatching { repo.withdraw(amount, upiId.trim()) }
                .onSuccess { _state.value = _state.value.copy(sending = false, done = it) }
                .onFailure { _state.value = _state.value.copy(sending = false,
                    error = it.userMessage("withdraw", R.string.acct_withdraw_failed, strings)) }
        }
    }
}

/** Boondh board 74 — move wallet money to the bank. Referral credit stays in the wallet. */
@Composable
fun WithdrawScreen(onBack: () -> Unit, onJars: () -> Unit, vm: WithdrawViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val cfg = LocalAppConfig.current
    val w = state.wallet
    val max = w?.withdrawable ?: BigDecimal.ZERO
    var amount by remember(max) { mutableStateOf(if (max.signum() > 0) max.stripTrailingZeros().toPlainString() else "") }
    var upi by remember { mutableStateOf("") }
    val parsed = amount.toBigDecimalOrNull()
    val valid = parsed != null && parsed >= BigDecimal.TEN && parsed <= max && upi.contains('@')

    Column(Modifier.fillMaxSize().background(NeerlyColors.Canvas).verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹ " + stringResource(R.string.acct_wallet_title), color = NeerlyColors.CustomerPrimary, fontSize = 15.sp) }

        val done = state.done
        if (done != null) {
            Text(stringResource(R.string.acct_withdraw_done_title), fontFamily = FontFamily.Serif, fontSize = 30.sp, color = NeerlyColors.Ink900, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.acct_withdraw_done_body, done.amount.rupees(), done.upiId, cfg.refundDaysMin, cfg.refundDaysMax),
                fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink700)
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)) { Text(stringResource(R.string.acct_withdraw_back), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            return@Column
        }

        Text(stringResource(R.string.acct_withdraw_title), fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 32.sp,
            color = NeerlyColors.Ink900, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.acct_withdraw_sub),
            fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink600)

        if (state.loading) { CircularProgressIndicator(color = NeerlyColors.CustomerPrimary); return@Column }
        if (w == null) { Text(state.error ?: stringResource(R.string.acct_withdraw_load_failed), color = NeerlyColors.Err); return@Column }

        Column(Modifier.fillMaxWidth().background(NeerlyColors.Paper, RoundedCornerShape(14.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Line(stringResource(R.string.acct_withdraw_in_wallet), w.availableAmount.rupees())
            if (w.promoBalance.signum() > 0) Line(stringResource(R.string.acct_withdraw_promo_stays), "−" + w.promoBalance.rupees())
            HorizontalDivider(color = NeerlyColors.Ink200)
            Line(stringResource(R.string.acct_withdraw_can), max.rupees(), bold = true)
        }

        OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text(stringResource(R.string.acct_withdraw_amount)) }, supportingText = { Text(stringResource(R.string.acct_withdraw_amount_hint, max.rupees(), java.math.BigDecimal.TEN.rupees())) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = upi, onValueChange = { upi = it.trim() }, label = { Text(stringResource(R.string.acct_withdraw_upi)) },
            placeholder = { Text(stringResource(R.string.acct_withdraw_upi_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Text(stringResource(R.string.acct_withdraw_timing, cfg.refundDaysMin, cfg.refundDaysMax),
            fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
        TextButton(onClick = onJars, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.acct_withdraw_see_jars), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }

        state.error?.let { Text(it, color = NeerlyColors.Err, fontSize = 14.sp) }
        Button(onClick = { parsed?.let { vm.withdraw(it, upi) } }, enabled = valid && !state.sending,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)) {
            Text(if (state.sending) stringResource(R.string.acct_withdraw_sending) else stringResource(R.string.acct_withdraw_cta, parsed?.rupees() ?: "—"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Line(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp, color = NeerlyColors.Ink700, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = 15.sp, color = NeerlyColors.Ink900, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

