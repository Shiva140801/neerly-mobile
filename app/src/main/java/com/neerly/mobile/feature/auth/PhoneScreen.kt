package com.neerly.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/** S-CUST-REG-02 — Phone entry. Sends OTP via the dev backend (no Firebase). */
@Composable
fun PhoneScreen(
    onOtpSent: (phone: String) -> Unit,
    vm: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var phone by remember { mutableStateOf("") }
    val valid = phone.length == 10 && phone.first() in "6789"
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .padding(horizontal = NeerlySpacing.x6, vertical = NeerlySpacing.x5)
    ) {
        Text(
            "Enter your mobile number",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900,
            lineHeight = 34.sp
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text("We'll send you a one-time password.", fontSize = 14.sp, color = NeerlyColors.Ink600)

        Spacer(Modifier.height(NeerlySpacing.x8))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .width(78.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(NeerlyRadius.md))
                    .background(NeerlyColors.Ink100),
                contentAlignment = Alignment.Center
            ) {
                Text("+91", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
            }
            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) phone = it },
                modifier = Modifier.weight(1f).height(52.dp),
                placeholder = { Text("98765 43210") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = NeerlyColors.Ink50,
                    focusedBorderColor = NeerlyColors.CustomerPrimary,
                    unfocusedBorderColor = NeerlyColors.Ink200
                ),
                shape = RoundedCornerShape(NeerlyRadius.md)
            )
        }

        Spacer(Modifier.weight(1f))

        if (state.error != null) {
            Text(state.error!!, fontSize = 13.sp, color = NeerlyColors.Err,
                modifier = Modifier.padding(vertical = 4.dp))
        }

        Button(
            onClick = { vm.sendOtp(phone) { phoneE164, _ -> onOtpSent(phoneE164) } },
            enabled = valid && !state.sending,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeerlyColors.CustomerPrimary,
                disabledContainerColor = NeerlyColors.CustomerPrimary.copy(alpha = 0.45f)
            )
        ) {
            Text(
                if (state.sending) "Sending…" else "Send OTP",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(NeerlySpacing.x3))

        Text(
            "Dev mode: default OTP 123456 always works (or check backend logs).",
            fontSize = 12.sp,
            color = NeerlyColors.Ink500,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
