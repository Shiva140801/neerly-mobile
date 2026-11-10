package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.BuildConfig
import com.neerly.mobile.core.design.AuthPageScaffold
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * S-CUST-REG-03 — OTP entry. Checks the code through AuthViewModel (Firebase Phone Auth →
 * /auth/exchange, or the dev OTP locally). On success the access+refresh tokens are in
 * TokenStore and we advance via [onVerified].
 */
@Composable
fun OtpScreen(
    phone: String,
    onVerified: (isNew: Boolean, roles: List<String>) -> Unit,
    /** Backend role the first code was requested for (the phone step always asks as CUSTOMER). */
    role: String = "CUSTOMER",
    onEditNumber: () -> Unit = {},
    vm: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var code by remember { mutableStateOf("") }
    val state by vm.state.collectAsState()
    // With Firebase the number may be proven without a typed code; that signs in too.
    LaunchedEffect(Unit) { vm.otpScreenShown(role, onVerified) }

    // Re-evaluate the resend gate every second while it's closed.
    var gate by remember { mutableStateOf(vm.resendGate()) }
    LaunchedEffect(state.codeSentAt, state.resends) {
        while (true) {
            gate = vm.resendGate()
            if (gate == ResendPolicy.Gate.Open) break
            kotlinx.coroutines.delay(1_000)
        }
    }

    // A code read from the SMS (with the customer's one-tap consent) fills in and submits.
    val readingSms = rememberSmsConsent(key = state.codeSentAt) { sms ->
        code = sms
        vm.verifyOtp(phone, sms, onVerified)
    }

    AuthPageScaffold {
        Text(
            stringResource(R.string.cust_otp_title),
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cust_otp_sent_to, phone), fontSize = 14.sp, color = NeerlyColors.Ink600,
                modifier = Modifier.weight(1f, fill = false))
            TextButton(onClick = onEditNumber, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.core_otp_edit_number), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = NeerlyColors.CustomerPrimary)
            }
        }

        Spacer(Modifier.height(NeerlySpacing.x8))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(6) { idx ->
                val char = code.getOrNull(idx)?.toString().orEmpty()
                val active = code.length == idx
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) NeerlyColors.Paper else NeerlyColors.Ink50)
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(char, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                }
            }
        }

        Spacer(Modifier.height(NeerlySpacing.x5))

        // Minimal keypad stub — real Firebase Phone Auth handles input. This demos UI.
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.cust_otp_placeholder)) },
            singleLine = true
        )

        if (readingSms && code.length < 6) {
            Spacer(Modifier.height(NeerlySpacing.x3))
            Text(stringResource(R.string.core_otp_reading_sms), fontSize = 13.sp, color = NeerlyColors.Ink600,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(Modifier.height(NeerlySpacing.x3))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.core_otp_didnt_get), fontSize = 14.sp, color = NeerlyColors.Ink600)
            when (val g = gate) {
                is ResendPolicy.Gate.Cooldown -> Text(" " + stringResource(R.string.core_otp_resend_in, resendClock(g.secondsLeft)),
                    fontSize = 14.sp, color = NeerlyColors.Ink500,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                is ResendPolicy.Gate.Limited -> Text(" " + stringResource(R.string.core_otp_resend_limited,
                    java.time.Instant.ofEpochMilli(g.untilMs).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))),
                    fontSize = 14.sp, color = NeerlyColors.Warn)
                ResendPolicy.Gate.Open -> TextButton(onClick = { code = ""; vm.resendOtp(phone, role) },
                    enabled = !state.resending, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (state.resending) stringResource(R.string.core_otp_resending) else stringResource(R.string.core_otp_resend),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.CustomerPrimary)
                }
            }
        }
        state.resendNotice?.let {
            Text(it, fontSize = 13.sp, color = NeerlyColors.Ok, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        // Only for the local dev-OTP backend (AUTH_MODE=dev); never with real Firebase sign-in.
        if (BuildConfig.DEBUG && vm.isDevMode) {
            Spacer(Modifier.height(NeerlySpacing.x3))
            Text(
                stringResource(R.string.cust_otp_dev_hint),
                fontSize = 12.sp,
                color = NeerlyColors.Ink500,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error!!, fontSize = 13.sp, color = NeerlyColors.Err,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { vm.verifyOtp(phone, code, onVerified) },
            enabled = code.length == 6 && !state.verifying,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) {
            Text(if (state.verifying) stringResource(R.string.cust_verifying) else stringResource(R.string.cust_verify),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
