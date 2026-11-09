package com.neerly.mobile.feature.address

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.focusRequester
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
 * With a Maps key the top card is a map with a fixed pin the customer drags onto the
 * gate (boards Address, AddressPin, AddressPinEdit), plus search for an address they
 * aren't standing at (AddressAdd, AddressType). Without a key it falls back to GPS +
 * typed fields.
 */
/** Boards 34–37 — find the door first (real GPS), then the details a driver needs. */
@Composable
fun AddressFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    /** Opens the LocationDenied screen (board LocationDenied) after the permission is refused. */
    onLocationDenied: () -> Unit = {},
    /** That screen's answer: "granted" (try GPS again) or "typed" (type the address instead). */
    locationChoice: kotlinx.coroutines.flow.StateFlow<String>? = null,
    onLocationChoiceHandled: () -> Unit = {},
    vm: AddressFormViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) vm.useCurrentLocation() else { vm.locationDenied(); onLocationDenied() }
    }
    val locate = { ask.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)) }
    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val choice = locationChoice?.collectAsState()?.value.orEmpty()
    LaunchedEffect(choice) {
        when (choice) {
            "granted" -> locate()
            "typed" -> { vm.typeInstead(); runCatching { searchFocus.requestFocus() } }
            else -> return@LaunchedEffect
        }
        onLocationChoiceHandled()
    }
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
                if (com.neerly.mobile.core.maps.pinPickerAvailable) {
                    com.neerly.mobile.core.maps.PinPicker(
                        target = com.google.android.gms.maps.model.LatLng(s.latitude, s.longitude),
                        onSettled = vm::pinMoved,
                        description = stringResource(R.string.cust_addr_map_desc),
                        pinColor = NeerlyColors.CustomerPrimary
                    )
                    Text(s.foundAt ?: if (s.locationCaptured) stringResource(R.string.cust_addr_pin_at, "%.5f".format(s.latitude), "%.5f".format(s.longitude))
                        else stringResource(R.string.cust_addr_find_building), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text(stringResource(if (s.editing) R.string.cust_addr_drag_hint_edit else R.string.cust_addr_drag_hint), fontSize = 13.sp, color = NeerlyColors.Ink600)
                    ServiceLine(s)
                    SecondaryButton(if (s.locating) stringResource(R.string.cust_addr_finding) else stringResource(R.string.cust_addr_use_location), locate)
                } else if (s.locationCaptured) {
                    Pill(stringResource(R.string.cust_addr_location_set), Tone.Ok)
                    Text(s.foundAt ?: stringResource(R.string.cust_addr_pin_at, "%.5f".format(s.latitude), "%.5f".format(s.longitude)), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    ServiceLine(s)
                    SecondaryButton(if (s.locating) stringResource(R.string.cust_addr_finding) else stringResource(R.string.cust_addr_use_location_again), locate)
                } else {
                    Text(stringResource(R.string.cust_addr_find_building), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text(stringResource(R.string.cust_addr_find_building_body), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    PrimaryButton(if (s.locating) stringResource(R.string.cust_addr_finding) else stringResource(R.string.cust_addr_use_location), locate, enabled = !s.locating)
                }
                s.locationError?.let { Notice(it, Tone.Warn) }
            }
        }
        if (s.confirmMove) item {
            BoondhCard {
                Notice(pluralStringResource(R.plurals.cust_addr_move_loses, s.lostShops.size, s.lostShops.joinToString(", ")), Tone.Warn)
                PrimaryButton(stringResource(R.string.cust_addr_use_spot), { vm.confirmMove(onSaved) })
                SecondaryButton(stringResource(R.string.cust_addr_keep_old_pin), vm::keepOldPin)
            }
        }
        item { SectionLabel(stringResource(R.string.cust_addr_search_title)) }
        item {
            Column {
                if (s.locationError != null || s.typing) Text(stringResource(R.string.cust_addr_type_instead), fontSize = 14.sp, color = NeerlyColors.Ink600,
                    modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = s.query, onValueChange = vm::onQuery, singleLine = true,
                    placeholder = { Text(stringResource(R.string.cust_addr_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocus), shape = RoundedCornerShape(NeerlyRadius.md)
                )
                when {
                    s.searching -> Text(stringResource(R.string.cust_addr_searching), fontSize = 13.sp, color = NeerlyColors.Ink600, modifier = Modifier.padding(top = 8.dp))
                    s.matches.isNotEmpty() -> {
                        SectionLabel(stringResource(R.string.cust_addr_matches))
                        s.matches.forEach { row -> MatchItem(row) { vm.pickMatch(row) } }
                        Text(stringResource(R.string.cust_addr_after_pick), fontSize = 13.sp, color = NeerlyColors.Ink600, modifier = Modifier.padding(top = 4.dp))
                    }
                    s.searchedOnce -> Text(stringResource(R.string.cust_addr_no_matches), fontSize = 13.sp, color = NeerlyColors.Ink600, modifier = Modifier.padding(top = 8.dp))
                }
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

/** "2 suppliers deliver here" / "Nobody on Boondh delivers here yet" under the pin. */
@Composable
private fun ServiceLine(s: AddressFormUiState) {
    when {
        s.serviceChecking -> Text(stringResource(R.string.cust_addr_service_checking), fontSize = 13.sp, color = NeerlyColors.Ink600)
        s.serviceCount == 0L -> Text(stringResource(R.string.cust_addr_service_none), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Warn)
        s.serviceCount != null -> Text(pluralStringResource(R.plurals.cust_addr_suppliers_here, s.serviceCount.toInt(), s.serviceCount.toInt()),
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ok)
    }
}

/** One search match (board AddressAdd): name, area + pincode, and either "already saved as" or who delivers. */
@Composable
private fun MatchItem(row: MatchRow, onPick: () -> Unit) {
    val m = row.match
    val tail = when {
        row.savedAs != null -> stringResource(R.string.cust_addr_saved_as, addressLabel(row.savedAs))
        row.serviceCount == 0L -> stringResource(R.string.cust_addr_service_none)
        row.serviceCount != null -> pluralStringResource(R.plurals.cust_addr_suppliers_here, row.serviceCount.toInt(), row.serviceCount.toInt())
        else -> null
    }
    Column(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(NeerlyRadius.md))
            .clickable(onClick = onPick).padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(m.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
        Text(listOfNotNull(m.subtitle.ifBlank { null }, tail).joinToString(" · "), fontSize = 13.sp,
            color = if (row.serviceCount == 0L && row.savedAs == null) NeerlyColors.Warn else NeerlyColors.Ink600)
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
