package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
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
import com.neerly.mobile.BuildConfig
import com.neerly.mobile.core.design.AuthPageScaffold
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/** S-CUST-REG-02 — Phone entry. Sends OTP via the dev backend (no Firebase). */
@Composable
fun PhoneScreen(
    onOtpSent: (phone: String) -> Unit,
    role: String = "CUSTOMER",
    vm: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var phone by remember { mutableStateOf("") }
    val valid = phone.length == 10 && phone.first() in "6789"
    val state by vm.state.collectAsState()

    AuthPageScaffold {
        Text(
            stringResource(R.string.cust_phone_title),
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            color = NeerlyColors.Ink900,
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(stringResource(R.string.cust_phone_body), fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink600)

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
            onClick = { vm.sendOtp(phone, role) { phoneE164, _ -> onOtpSent(phoneE164) } },
            enabled = valid && !state.sending,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeerlyColors.CustomerPrimary,
                disabledContainerColor = NeerlyColors.CustomerPrimary.copy(alpha = 0.45f)
            )
        ) {
            Text(
                if (state.sending) stringResource(R.string.cust_phone_sending) else stringResource(R.string.cust_phone_send_code),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }

        // Debug-only crutch for the dev-OTP backend; never ships to a customer.
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(NeerlySpacing.x3))
            Text(
                stringResource(R.string.cust_phone_dev_hint),
                fontSize = 12.sp,
                color = NeerlyColors.Ink500,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
