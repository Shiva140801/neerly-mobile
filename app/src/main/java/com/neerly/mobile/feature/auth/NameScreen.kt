package com.neerly.mobile.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.AuthPageScaffold
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * S-CUST-REG-04 Name entry.
 *
 * The typed name is PATCHed to `/customer/profile` before we move on, so the
 * account stops being called "User" — sign-up creates it with that placeholder
 * because the name is only asked for after OTP verification.
 */
@Composable
fun NameScreen(
    onContinue: (String) -> Unit,
    vm: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    val valid = name.trim().length in 2..50
    val state by vm.state.collectAsState()

    AuthPageScaffold {
        Text("What should we call you?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text("This helps vendors recognise you.", fontSize = 14.sp, color = NeerlyColors.Ink600)

        Spacer(Modifier.height(NeerlySpacing.x8))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(50) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("First name") },
            singleLine = true,
            shape = RoundedCornerShape(NeerlyRadius.md)
        )
        Spacer(Modifier.weight(1f))

        if (state.error != null) {
            Text(
                state.error!!,
                fontSize = 13.sp,
                color = NeerlyColors.Err,
                modifier = Modifier.padding(bottom = NeerlySpacing.x2)
            )
        }

        Button(
            onClick = { vm.saveDisplayName(name) { onContinue(name.trim()) } },
            enabled = valid && !state.savingName,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeerlyColors.CustomerPrimary,
                disabledContainerColor = NeerlyColors.CustomerPrimary.copy(alpha = 0.45f)
            )
        ) {
            Text(
                if (state.savingName) "Saving…" else "Continue",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}
