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

/** S-CUST-REG-02 — Phone entry. */
@Composable
fun PhoneScreen(onOtpSent: (phone: String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    val valid = phone.length == 10 && phone.first() in "6789"

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

        Button(
            onClick = { onOtpSent("+91$phone") },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeerlyColors.CustomerPrimary,
                disabledContainerColor = NeerlyColors.CustomerPrimary.copy(alpha = 0.45f)
            )
        ) {
            Text("Send OTP", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(NeerlySpacing.x3))

        Text(
            "Trouble signing in? Contact support",
            fontSize = 12.sp,
            color = NeerlyColors.Ink500,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
