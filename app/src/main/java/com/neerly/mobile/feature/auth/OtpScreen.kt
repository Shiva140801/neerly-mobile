package com.neerly.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * S-CUST-REG-03 — OTP entry. Posts to /auth/dev/verify-otp via the AuthViewModel.
 * On success the access+refresh tokens are persisted in TokenStore and we
 * advance via [onVerified]. Default OTP `123456` always works in dev.
 */
@Composable
fun OtpScreen(
    phone: String,
    onVerified: () -> Unit,
    vm: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var code by remember { mutableStateOf("") }
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .padding(horizontal = NeerlySpacing.x6, vertical = NeerlySpacing.x5)
    ) {
        Text(
            "Enter OTP",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text("Sent to $phone · Change", fontSize = 14.sp, color = NeerlyColors.Ink600)

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
            placeholder = { Text("Tap keypad or auto-detect") },
            singleLine = true
        )

        Spacer(Modifier.height(NeerlySpacing.x3))

        Text(
            "Dev mode: 123456 always works (or check the backend log).",
            fontSize = 12.sp,
            color = NeerlyColors.Ink500,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

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
            Text(if (state.verifying) "Verifying…" else "Verify",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
