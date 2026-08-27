package com.neerly.mobile.feature.event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.VendorCardResponse
import java.math.BigDecimal
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Event booking wizard. Lean V1: customer picks one vendor, fills items + slot,
 * gets a quote (advance vs full), and the booking enters PENDING_VENDOR_CONFIRM.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventBookingScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    vm: EventBookingViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (s.step) {
                            0 -> "Pick a vendor"
                            1 -> "Pick items"
                            2 -> "Event details"
                            3 -> "Where to deliver"
                            else -> "Review booking"
                        },
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = { if (s.step == 0) onBack() else vm.back() }) {
                        Text("Back", color = NeerlyColors.CustomerPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(NeerlySpacing.x4)) {
            ProgressDots(s.step, 5)
            Spacer(Modifier.height(NeerlySpacing.x3))
            when {
                s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Loading…", color = NeerlyColors.Ink500)
                }
                else -> when (s.step) {
                    0 -> StepVendor(s.vendors, vm::pickVendor)
                    1 -> StepItems(s.products, s.loadingProducts, s.itemQty, vm::setQty, vm::goToDetails)
                    2 -> StepDetails(s, vm)
                    3 -> StepAddress(s.addresses, vm::pickAddress)
                    else -> StepReview(s, vm) { onCreated(it) }
                }
            }
            if (s.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(s.error!!, color = NeerlyColors.Err, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.height(6.dp).weight(1f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i <= current) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200)
            )
        }
    }
}

@Composable
private fun StepVendor(vendors: List<VendorCardResponse>, onPick: (VendorCardResponse) -> Unit) {
    if (vendors.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("No active vendors deliver to your area yet.",
                color = NeerlyColors.Ink500, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(vendors, key = { it.id }) { v ->
            Card(
                onClick = { onPick(v) },
                shape = RoundedCornerShape(NeerlyRadius.md),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NeerlyColors.Paper)
            ) {
                Column(Modifier.padding(NeerlySpacing.x4)) {
                    Text(v.businessName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink900)
                    Text("${v.businessCity} · ${v.totalOrders} orders" +
                        (v.avgRating?.let { " · ★ $it" } ?: ""),
                        fontSize = 12.sp, color = NeerlyColors.Ink500)
                }
            }
        }
    }
}

@Composable
private fun StepItems(
    products: List<ProductResponse>,
    loading: Boolean,
    qty: Map<String, Int>,
    onSetQty: (String, Int) -> Unit,
    onContinue: () -> Unit
) {
    if (loading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Loading…", color = NeerlyColors.Ink500) }
        return
    }
    Column {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products, key = { it.id }) { p ->
                Surface(
                    color = NeerlyColors.Paper,
                    shape = RoundedCornerShape(NeerlyRadius.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(NeerlySpacing.x4),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = NeerlyColors.Ink900)
                            Text("₹${p.price}", fontSize = 12.sp, color = NeerlyColors.Ink500)
                        }
                        QtyStepper(
                            value = qty[p.id] ?: 0,
                            onChange = { onSetQty(p.id, it) }
                        )
                    }
                }
            }
        }
        Button(
            onClick = onContinue,
            enabled = qty.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) {
            Text("Continue (${qty.values.sum()} units)",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QtyStepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onChange(value - 1) }, enabled = value > 0) { Text("−") }
        Text(" $value ", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp))
        OutlinedButton(onClick = { onChange(value + 1) }) { Text("+") }
    }
}

@Composable
private fun StepDetails(s: EventBookingUiState, vm: EventBookingViewModel) {
    val fmt = DateTimeFormatter.ofPattern("EEE dd MMM, hh:mm a").withZone(ZoneId.systemDefault())
    Column(verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)) {
        FieldText("Event name (optional)", s.eventName, vm::setEventName)
        FieldText("Expected guests", s.expectedGuests?.toString() ?: "",
            vm::setExpectedGuests, KeyboardType.Number)

        SectionHeader("Slot")
        Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(NeerlySpacing.x4)) {
                Text("Starts: ${fmt.format(s.eventStart)}",
                    fontSize = 13.sp, color = NeerlyColors.Ink800)
                Text("Ends:   ${fmt.format(s.eventEnd)}",
                    fontSize = 13.sp, color = NeerlyColors.Ink800)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        vm.setStart(s.eventStart.plus(1, ChronoUnit.DAYS))
                    }) { Text("+1 day") }
                    OutlinedButton(onClick = {
                        vm.setEnd(s.eventEnd.plus(1, ChronoUnit.HOURS))
                    }) { Text("+1 hr") }
                }
                Text(
                    "Tip: a real picker (DatePicker + TimePicker) wires here in next sprint.",
                    fontSize = 11.sp, color = NeerlyColors.Ink500
                )
            }
        }

        SectionHeader("Chilling lead time")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 4, 6, 12).forEach { h ->
                val active = s.chillingLeadHours == h
                Surface(
                    shape = RoundedCornerShape(NeerlyRadius.pill),
                    color = if (active) NeerlyColors.CustomerSofter else NeerlyColors.Paper,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (active) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200
                    ),
                    modifier = Modifier.clickable { vm.setChillingHours(h) }
                ) {
                    Text(if (h == 0) "None" else "$h h",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (active) NeerlyColors.CustomerDark else NeerlyColors.Ink700)
                }
            }
        }

        FieldText("Notes for vendor (optional)", s.customerNotes, vm::setNotes)

        Button(
            onClick = vm::goToAddress,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) { Text("Continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun StepAddress(addresses: List<AddressResponse>, onPick: (AddressResponse) -> Unit) {
    if (addresses.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Add a delivery address from your profile first.",
                color = NeerlyColors.Ink500, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(addresses, key = { it.id }) { a ->
            Card(
                onClick = { onPick(a) },
                shape = RoundedCornerShape(NeerlyRadius.md),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NeerlyColors.Paper)
            ) {
                Column(Modifier.padding(NeerlySpacing.x4)) {
                    Text(a.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink900)
                    Text("${a.flatNo}, ${a.streetArea}", fontSize = 12.sp, color = NeerlyColors.Ink800)
                    Text("${a.city} · ${a.pincode}", fontSize = 11.sp, color = NeerlyColors.Ink500)
                }
            }
        }
    }
}

@Composable
private fun StepReview(
    s: EventBookingUiState,
    vm: EventBookingViewModel,
    onCreated: (String) -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("EEE dd MMM, hh:mm a").withZone(ZoneId.systemDefault())
    val totalUnits = s.itemQty.values.sum()
    val rough = s.products
        .filter { it.id in s.itemQty }
        .sumOf { it.price * BigDecimal(s.itemQty[it.id] ?: 0) }
    val durationHours = Duration.between(s.eventStart, s.eventEnd).toHours()

    Column(verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)) {
        Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(NeerlySpacing.x4)) {
                Text(s.eventName.ifBlank { "Event booking" },
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text(s.selectedVendor?.businessName ?: "—",
                    fontSize = 13.sp, color = NeerlyColors.Ink500)
                Spacer(Modifier.height(8.dp))
                Text("Slot: ${fmt.format(s.eventStart)} → ${fmt.format(s.eventEnd)} ($durationHours h)",
                    fontSize = 12.sp, color = NeerlyColors.Ink800)
                s.expectedGuests?.let {
                    Text("Guests: $it", fontSize = 12.sp, color = NeerlyColors.Ink800)
                }
                Text("Chilling lead: ${s.chillingLeadHours} h",
                    fontSize = 12.sp, color = NeerlyColors.Ink800)
                Spacer(Modifier.height(6.dp))
                Text("Items ($totalUnits units, est. ₹$rough)",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                s.products.filter { it.id in s.itemQty }.forEach { p ->
                    Text("• ${p.name} × ${s.itemQty[p.id]}",
                        fontSize = 12.sp, color = NeerlyColors.Ink700)
                }
                Spacer(Modifier.height(6.dp))
                Text("To: ${s.selectedAddress?.label} · ${s.selectedAddress?.flatNo}, ${s.selectedAddress?.streetArea}",
                    fontSize = 12.sp, color = NeerlyColors.Ink500)
                if (s.customerNotes.isNotBlank()) {
                    Text("Notes: ${s.customerNotes}", fontSize = 12.sp, color = NeerlyColors.Ink500)
                }
            }
        }

        Text(
            "After you submit, the vendor confirms in their app. " +
                "We'll then ask for the advance and lock the slot.",
            fontSize = 12.sp, color = NeerlyColors.Ink500
        )

        Button(
            onClick = { vm.submit(onCreated) },
            enabled = !s.submitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) {
            Text(if (s.submitting) "Submitting…" else "Request booking",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink600)
}

@Composable
private fun FieldText(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink600)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = label != "Notes for vendor (optional)",
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(NeerlyRadius.md)
        )
    }
}
