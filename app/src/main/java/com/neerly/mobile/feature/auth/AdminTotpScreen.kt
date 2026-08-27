package com.neerly.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/** S-AUTH-TOT-01 — TOTP challenge for Admin role. */
@Composable
fun AdminTotpScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var verifying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .padding(horizontal = NeerlySpacing.x6, vertical = NeerlySpacing.x5)
    ) {
        Text(
            "Verify with authenticator",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text("Enter the 6-digit code from your authenticator app.", fontSize = 14.sp, color = NeerlyColors.Ink600)

        Spacer(Modifier.height(NeerlySpacing.x8))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("000000") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(NeerlyRadius.md)
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                verifying = true
                // In a real app, this would call AuthViewModel.verifyTotp
                // For now, we simulate success
                onVerified()
            },
            enabled = code.length == 6 && !verifying,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.AdminPrimary)
        ) {
            Text(if (verifying) "Verifying…" else "Verify", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Back", color = NeerlyColors.Ink600)
        }
    }
}
