package com.neerly.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/** S-CUST-REG-04 Name entry */
@Composable
fun NameScreen(onContinue: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val valid = name.trim().length in 2..50

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .padding(horizontal = NeerlySpacing.x6, vertical = NeerlySpacing.x5)
    ) {
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
        Button(
            onClick = { onContinue(name.trim()) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) {
            Text("Continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
