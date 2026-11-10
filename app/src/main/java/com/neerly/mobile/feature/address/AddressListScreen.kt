package com.neerly.mobile.feature.address

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.dto.AddressResponse

/** Board 34 — saved addresses, with what the driver needs to know at each. */
@Composable
fun AddressListScreen(
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (String) -> Unit,
    vm: AddressListViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::refresh)
    var deleting by remember { mutableStateOf<AddressResponse?>(null) }

    // Board Addresses: the default framed in indigo, instructions in quotes, a dashed
    // "+ Add an address" at the end of the list (not a footer button).
    BoondhPage(title = stringResource(R.string.cust_addr_saved_title), onBack = onBack, footer = if (state.addresses.isEmpty()) ({ PrimaryButton(stringResource(R.string.cust_addr_add_an), onAddNew) }) else null) {
        if (state.addresses.isEmpty()) {
            item { LoadState(state.loading, state.error, vm::refresh, stringResource(R.string.cust_addr_none),
                stringResource(R.string.cust_addr_none_body)) }
            return@BoondhPage
        }
        state.error?.let { e -> item { Notice(e, Tone.Bad) } }
        items(state.addresses.size, key = { state.addresses[it].id }) { i ->
            val a = state.addresses[i]
            BoondhCard(if (a.isPrimary) Modifier.border(2.dp, NeerlyColors.CustomerPrimary, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) else Modifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(addressLabel(a.label), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    if (a.isPrimary) { Spacer(Modifier.width(8.dp)); Pill(stringResource(R.string.cust_addr_default), Tone.Info) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onEdit(a.id) }, modifier = Modifier.heightIn(min = 44.dp)) {
                        Text(stringResource(R.string.cust_edit), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold) }
                }
                Text(listOf("${a.flatNumber}, ${a.buildingName}", a.street, "${a.city} ${a.pincode}").filter { it.isNotBlank() }.joinToString(", ") +
                    floorLine(a), fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink700)
                a.deliveryInstructions?.takeIf { it.isNotBlank() }?.let {
                    Text(stringResource(R.string.cust_addr_quote, it), fontSize = 14.sp, color = NeerlyColors.Ink900,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
                a.securityContactName?.takeIf { it.isNotBlank() }?.let {
                    Text(a.securityContactPhone?.let { p -> stringResource(R.string.cust_addr_security_phone_line, it, p) } ?: stringResource(R.string.cust_addr_security, it), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Warn) }
                when (val n = state.suppliersByPincode[a.pincode]) {
                    null -> {}
                    0 -> Notice(stringResource(R.string.cust_addr_no_shops, a.pincode), Tone.Warn)
                    else -> Text(androidx.compose.ui.res.pluralStringResource(R.plurals.cust_addr_suppliers_here, n, n), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ok)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!a.isPrimary) TextButton(onClick = { vm.setPrimary(a.id) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.cust_addr_make_default), color = NeerlyColors.CustomerPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    TextButton(onClick = { deleting = a }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.cust_remove), color = NeerlyColors.Err, fontSize = 14.sp) }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .drawBehind {
                    drawRoundRect(NeerlyColors.Ink500, cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                }
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onAddNew), contentAlignment = Alignment.Center) {
                Text("+  " + stringResource(R.string.cust_addr_add_an), color = NeerlyColors.CustomerPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    deleting?.let { a ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.cust_addr_remove_title, addressLabel(a.label))) },
            text = {
                val plans = state.plansByAddress[a.id].orEmpty()
                Text(if (plans.isEmpty()) stringResource(R.string.cust_addr_remove_body, a.flatNumber, a.buildingName)
                     else stringResource(R.string.cust_addr_remove_body_plan, a.flatNumber, a.buildingName, plans.joinToString(", ")))
            },
            confirmButton = { TextButton(onClick = { vm.delete(a.id); deleting = null }) { Text(stringResource(R.string.cust_remove), color = NeerlyColors.Err) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cust_keep_it)) } }
        )
    }
}

@Composable
private fun floorLine(a: AddressResponse): String = a.floorNumber?.let { f ->
    val floor = if (f == 0) stringResource(R.string.cust_addr_ground) else stringResource(R.string.cust_addr_floor_n, f)
    val lift = if (f > 0) ", " + (if (a.hasLift) stringResource(R.string.cust_addr_with_lift) else stringResource(R.string.cust_addr_no_lift)) else ""
    " · $floor$lift"
} ?: ""
