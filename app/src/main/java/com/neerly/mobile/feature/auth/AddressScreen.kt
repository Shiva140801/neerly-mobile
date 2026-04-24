package com.neerly.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * S-CUST-REG-07A First address.
 * Real Google Maps Compose panel is added once maps key is wired;
 * placeholder shows the map region.
 */
@Composable
fun AddressScreen(onSaved: () -> Unit) {
    var flat by remember { mutableStateOf("") }
    var building by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    val valid = flat.isNotBlank() && building.isNotBlank() && pincode.matches(Regex("^[1-9]\\d{5}\$"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NeerlySpacing.x5, vertical = NeerlySpacing.x4)
    ) {
        Text("Where should we deliver?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        Spacer(Modifier.height(NeerlySpacing.x4))

        // Map placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(NeerlyRadius.md))
                .background(NeerlyColors.Ink100)
                .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(NeerlyRadius.md)),
            contentAlignment = Alignment.Center
        ) {
            Text("📍 Map with draggable pin\n(Google Maps Compose wires here)",
                fontSize = 12.sp, color = NeerlyColors.Ink500)
        }

        Spacer(Modifier.height(NeerlySpacing.x4))

        FormField("Flat / House No.", flat, onChange = { flat = it.take(50) })
        FormField("Building / Apartment name", building, onChange = { building = it.take(200) })
        FormField("Street / Area", street, onChange = { street = it.take(200) })
        FormField("Landmark (optional)", landmark, onChange = { landmark = it.take(200) })
        FormField("Pincode", pincode, onChange = { pincode = it.take(6).filter(Char::isDigit) },
            keyboardType = KeyboardType.Number)

        Spacer(Modifier.height(NeerlySpacing.x5))

        Button(
            onClick = onSaved,
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) {
            Text("Save address", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(NeerlySpacing.x5))
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink600)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(NeerlyRadius.md)
        )
    }
}
