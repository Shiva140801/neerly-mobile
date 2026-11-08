package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
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
        Text(stringResource(R.string.cust_name_step), fontSize = 13.sp, color = NeerlyColors.Ink600)
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(stringResource(R.string.cust_name_title), fontSize = 32.sp, lineHeight = 38.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, color = NeerlyColors.Ink900)
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(stringResource(R.string.cust_name_body), fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink600)

        Spacer(Modifier.height(NeerlySpacing.x8))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(50) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.cust_name_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(NeerlyRadius.md)
        )
        val short = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.let { p ->
            if (p.isEmpty()) "" else p[0] + (p.getOrNull(1)?.let { " ${it.first()}." } ?: "") }
        if (short.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.cust_name_shown_as, short), fontSize = 14.sp, color = NeerlyColors.Ink600)
        }
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
                when { state.savingName -> stringResource(R.string.cust_saving); !valid -> stringResource(R.string.cust_name_type_to_continue); else -> stringResource(R.string.cust_continue) },
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}
