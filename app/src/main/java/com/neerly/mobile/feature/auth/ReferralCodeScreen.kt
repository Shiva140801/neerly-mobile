package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.ReferralEntryRequest
import com.neerly.mobile.data.dto.ReferralPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReferralUiState(
    val code: String = "",
    val checking: Boolean = false,
    val preview: ReferralPreview? = null,
    val problem: String? = null,
    val claiming: Boolean = false
)

@HiltViewModel
class ReferralCodeViewModel @Inject constructor(
    private val api: NeerlyApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(ReferralUiState())
    val state = _state.asStateFlow()
    private var lookup: Job? = null

    /** Check the code as it's typed (debounced) so the screen can say whose it is. */
    fun type(raw: String) {
        val code = raw.uppercase().filter { it.isLetterOrDigit() }.take(16)
        _state.value = _state.value.copy(code = code, preview = null, problem = null)
        lookup?.cancel()
        if (code.length < 6) return
        lookup = viewModelScope.launch {
            delay(400)
            _state.value = _state.value.copy(checking = true)
            runCatching { api.previewReferral(code) }
                .onSuccess { _state.value = _state.value.copy(checking = false, preview = it) }
                .onFailure { _state.value = _state.value.copy(checking = false,
                    problem = it.userMessage("referral preview", strings.get(R.string.cust_ref_not_found))) }
        }
    }

    fun claim(onDone: () -> Unit) {
        val s = _state.value
        if (s.preview == null) { onDone(); return }
        _state.value = s.copy(claiming = true)
        viewModelScope.launch {
            runCatching { api.submitReferralCode(ReferralEntryRequest(s.code)) }
                .onSuccess { _state.value = _state.value.copy(claiming = false); onDone() }
                .onFailure { _state.value = _state.value.copy(claiming = false,
                    problem = it.userMessage("referral claim", strings.get(R.string.cust_ref_claim_failed))) }
        }
    }
}

/** Board 05 — "Did a friend send you?" Optional; amounts come from the server's settings. */
@Composable
fun ReferralCodeScreen(onDone: () -> Unit, vm: ReferralCodeViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val cfg = com.neerly.mobile.data.config.LocalAppConfig.current
    BoondhPage(
        title = stringResource(R.string.cust_ref_title),
        subtitle = stringResource(R.string.cust_ref_subtitle, cfg.refereeReward.rupees(), cfg.referrerReward.rupees()),
        footer = {
            PrimaryButton(when {
                s.claiming -> stringResource(R.string.cust_ref_applying)
                s.preview != null -> stringResource(R.string.cust_ref_apply_continue)
                else -> stringResource(R.string.cust_ref_continue_without)
            }, { vm.claim(onDone) }, enabled = !s.claiming && !s.checking)
        }
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.cust_ref_step), fontSize = 13.sp, color = NeerlyColors.Ink600, modifier = Modifier.padding(top = 14.dp))
                TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(R.string.cust_skip), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
            }
        }
        item {
            Text(stringResource(R.string.cust_ref_label), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = s.code, onValueChange = vm::type, singleLine = true,
                isError = s.problem != null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            val (msg, color) = when {
                s.checking -> stringResource(R.string.cust_ref_checking) to NeerlyColors.Ink600
                s.preview != null -> stringResource(R.string.cust_ref_valid, s.preview!!.referrerName, s.preview!!.youGet.rupees()) to NeerlyColors.Ok
                s.problem != null -> s.problem!! to NeerlyColors.Err
                else -> stringResource(R.string.cust_ref_hint) to NeerlyColors.Ink600
            }
            Text(msg, fontSize = 14.sp, color = color)
        }
    }
}
