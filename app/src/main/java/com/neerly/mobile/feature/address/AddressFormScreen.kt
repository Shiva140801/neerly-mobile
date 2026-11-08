package com.neerly.mobile.feature.address

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
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
import com.neerly.mobile.core.design.*
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
/** Boards 34–37 — find the door first (real GPS), then the details a driver needs. */
@Composable
fun AddressFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    vm: AddressFormViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) vm.useCurrentLocation() else vm.locationDenied()
    }
    val locate = { ask.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)) }
    BoondhPage(
        title = if (s.editing) stringResource(R.string.cust_addr_edit) else stringResource(R.string.cust_home_where_deliver),
        onBack = onBack,
        subtitle = stringResource(R.string.cust_addr_subtitle),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            PrimaryButton(when { s.saving -> stringResource(R.string.cust_saving); s.editing -> stringResource(R.string.cust_addr_save_changes); else -> stringResource(R.string.cust_addr_save) },
                { vm.save(onSaved) }, enabled = !s.saving && !s.loading)
        }
    ) {
        item {
            BoondhCard {
                if (s.locationCaptured) {
                    Pill(stringResource(R.string.cust_addr_location_set), Tone.Ok)
                    Text(s.foundAt ?: stringResource(R.string.cust_addr_pin_at, "%.5f".format(s.latitude), "%.5f".format(s.longitude)), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    SecondaryButton(if (s.locating) stringResource(R.string.cust_addr_finding) else stringResource(R.string.cust_addr_use_location_again), locate)
                } else {
                    Text(stringResource(R.string.cust_addr_find_building), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text(stringResource(R.string.cust_addr_find_building_body), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    PrimaryButton(if (s.locating) stringResource(R.string.cust_addr_finding) else stringResource(R.string.cust_addr_use_location), locate, enabled = !s.locating)
                }
                s.locationError?.let { Notice(it, Tone.Warn) }
            }
        }
        item { SectionLabel(stringResource(R.string.cust_addr_save_as)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // The stored label stays the English key (it's address data); only the chip text is translated.
                listOf("Home", "Work", "Other").forEach { l ->
                    FilterChip(selected = s.label == l, onClick = { vm.update { copy(label = l) } }, label = { Text(addressLabel(l), fontSize = 15.sp) },
                        modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        item { SectionLabel(stringResource(R.string.cust_addr_the_door)) }
        item {
            Column {
                LabeledField(stringResource(R.string.cust_addr_flat), s.flatNumber) { v -> vm.update { copy(flatNumber = v.take(50)) } }
                LabeledField(stringResource(R.string.cust_addr_building), s.buildingName) { v -> vm.update { copy(buildingName = v.take(200)) } }
                LabeledField(stringResource(R.string.cust_addr_street), s.street) { v -> vm.update { copy(street = v.take(300)) } }
                LabeledField(stringResource(R.string.cust_addr_landmark), s.landmark) { v -> vm.update { copy(landmark = v.take(200)) } }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { LabeledField(stringResource(R.string.cust_addr_city), s.city) { v -> vm.update { copy(city = v.take(100)) } } }
                    Box(Modifier.weight(1f)) { LabeledField(stringResource(R.string.cust_addr_pincode), s.pincode, KeyboardType.Number) { v ->
                        vm.update { copy(pincode = v.filter(Char::isDigit).take(6)) } } }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.cust_addr_help_driver)) }
        item {
            Column {
                LabeledField(stringResource(R.string.cust_addr_floor), s.floorNumber, KeyboardType.Number) { v -> vm.update { copy(floorNumber = v.filter(Char::isDigit).take(3)) } }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cust_addr_lift), Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    Switch(checked = s.hasLift, onCheckedChange = { v -> vm.update { copy(hasLift = v) } })
                }
                LabeledField(stringResource(R.string.cust_addr_instructions), s.deliveryInstructions, multiline = true) { v -> vm.update { copy(deliveryInstructions = v.take(500)) } }
                LabeledField(stringResource(R.string.cust_addr_security_name), s.securityContactName) { v -> vm.update { copy(securityContactName = v.take(120)) } }
                LabeledField(stringResource(R.string.cust_addr_security_phone), s.securityContactPhone, KeyboardType.Phone) { v ->
                    vm.update { copy(securityContactPhone = v.take(15)) } }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cust_addr_main), Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    Switch(checked = s.setAsPrimary, onCheckedChange = { v -> vm.update { copy(setAsPrimary = v) } })
                }
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    multiline: Boolean = false,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = !multiline,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(NeerlyRadius.md)
        )
    }
}

/** Home / Work / Other in the app's language; a label the customer typed is shown as typed. */
@Composable
internal fun addressLabel(label: String): String = when (label.trim().lowercase()) {
    "home" -> stringResource(R.string.cust_addr_home)
    "work" -> stringResource(R.string.cust_addr_work)
    "other" -> stringResource(R.string.cust_addr_other)
    else -> label
}
