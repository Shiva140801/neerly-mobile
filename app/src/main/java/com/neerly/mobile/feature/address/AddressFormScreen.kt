package com.neerly.mobile.feature.address

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * Create / edit a saved address. Backed by [AddressFormViewModel] which
 * decides POST vs PATCH based on whether the SavedStateHandle has
 * `addressId`.
 *
 * Uses a stub map placeholder + "Use my location" button. Once Maps SDK is
 * wired the placeholder swaps for a draggable pin and the lat/lng comes from
 * the camera position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    vm: AddressFormViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (s.editing) "Edit address" else "New address",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NeerlySpacing.x5, vertical = NeerlySpacing.x4)
        ) {
            // Map placeholder — picker stub.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(NeerlyRadius.md))
                    .background(NeerlyColors.Ink100)
                    .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(NeerlyRadius.md)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📍", fontSize = 28.sp)
                    Text(
                        if (s.locationCaptured)
                            "Pinned: %.4f, %.4f".format(s.lat, s.lng)
                        else "Tap to drop a pin (Maps SDK wires here)",
                        fontSize = 12.sp, color = NeerlyColors.Ink500
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = vm::useCurrentLocation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NeerlyRadius.pill)
            ) { Text("Use my current location") }

            Spacer(Modifier.height(NeerlySpacing.x4))

            LabeledField("Label (Home, Work, …)", s.label) { v -> vm.update { copy(label = v.take(40)) } }
            LabeledField("Flat / House No.", s.flatNo) { v -> vm.update { copy(flatNo = v.take(50)) } }
            LabeledField("Building / Apartment name", s.buildingName) { v ->
                vm.update { copy(buildingName = v.take(200)) }
            }
            LabeledField("Street / Area", s.streetArea) { v ->
                vm.update { copy(streetArea = v.take(200)) }
            }
            LabeledField("Landmark (optional)", s.landmark) { v ->
                vm.update { copy(landmark = v.take(200)) }
            }
            LabeledField("City", s.city) { v -> vm.update { copy(city = v.take(100)) } }
            LabeledField(
                "Pincode", s.pincode, keyboardType = KeyboardType.Number
            ) { v -> vm.update { copy(pincode = v.take(6).filter(Char::isDigit)) } }
            LabeledField(
                "Floor (optional)", s.floorNumber, keyboardType = KeyboardType.Number
            ) { v -> vm.update { copy(floorNumber = v.take(3).filter(Char::isDigit)) } }

            // Driver instructions + lift toggle + security contact
            LabeledField(
                "Delivery instructions for driver",
                s.deliveryInstructions
            ) { v -> vm.update { copy(deliveryInstructions = v.take(500)) } }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lift available", Modifier.weight(1f),
                    fontSize = 14.sp, color = NeerlyColors.Ink800)
                Switch(checked = s.liftAvailable, onCheckedChange = { v ->
                    vm.update { copy(liftAvailable = v) }
                })
            }

            LabeledField(
                "Security/concierge name (optional)",
                s.securityContactName
            ) { v -> vm.update { copy(securityContactName = v.take(120)) } }
            LabeledField(
                "Security/concierge phone (optional)",
                s.securityContactPhone,
                keyboardType = KeyboardType.Phone
            ) { v -> vm.update { copy(securityContactPhone = v.take(15)) } }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set as primary address", Modifier.weight(1f),
                    fontSize = 14.sp, color = NeerlyColors.Ink800)
                Switch(checked = s.setAsPrimary, onCheckedChange = { v ->
                    vm.update { copy(setAsPrimary = v) }
                })
            }

            if (s.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(s.error!!, color = NeerlyColors.Err, fontSize = 13.sp)
            }

            Spacer(Modifier.height(NeerlySpacing.x4))

            Button(
                onClick = { vm.save(onSaved) },
                enabled = s.isValid() && !s.saving && !s.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(NeerlyRadius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
            ) {
                Text(
                    when {
                        s.saving -> "Saving…"
                        s.editing -> "Update address"
                        else -> "Save address"
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(NeerlySpacing.x5))
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink600)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = label != "Delivery instructions for driver",
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(NeerlyRadius.md)
        )
    }
}
