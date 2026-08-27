package com.neerly.mobile.feature.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.VendorCardResponse

/**
 * 5-step wizard for creating a new subscription. Matches Customer Flows §SUB-NEW-01.
 * The state machine lives in [SubscriptionCreateViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionCreateScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    vm: SubscriptionCreateViewModel = hiltViewModel()
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
                            1 -> "Pick a product"
                            2 -> "How often?"
                            3 -> "Deliver to"
                            else -> "Confirm subscription"
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
        Column(
            Modifier.padding(padding).fillMaxSize().padding(NeerlySpacing.x4)
        ) {
            StepDots(current = s.step, total = 5)
            Spacer(Modifier.height(NeerlySpacing.x3))

            when {
                s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Loading…", color = NeerlyColors.Ink500)
                }
                s.error != null && s.step == 0 -> Text(s.error!!, color = NeerlyColors.Err)
                else -> when (s.step) {
                    0 -> StepVendor(s.vendors, vm::pickVendor)
                    1 -> StepProduct(s.products, s.loadingProducts, vm::pickProduct)
                    2 -> StepFrequency(s, vm)
                    3 -> StepAddress(s.addresses, vm::pickAddress)
                    else -> StepConfirm(s, vm) { onCreated(it) }
                }
            }
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .height(6.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i <= current) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200)
            )
        }
    }
}

@Composable
private fun StepVendor(vendors: List<VendorCardResponse>, onPick: (VendorCardResponse) -> Unit) {
    if (vendors.isEmpty()) {
        EmptyMsg("No active vendors deliver to your area yet.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(vendors, key = { it.id }) { v ->
            Card(
                onClick = { onPick(v) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NeerlyRadius.md),
                colors = CardDefaults.cardColors(containerColor = NeerlyColors.Paper)
            ) {
                Column(Modifier.padding(NeerlySpacing.x4)) {
                    Text(v.businessName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink900)
                    Text(
                        "${v.businessCity} · ${v.totalOrders} orders" +
                            (v.avgRating?.let { " · ★ $it" } ?: ""),
                        fontSize = 12.sp, color = NeerlyColors.Ink500
                    )
                }
            }
        }
    }
}

@Composable
private fun StepProduct(items: List<ProductResponse>, loading: Boolean, onPick: (ProductResponse) -> Unit) {
    if (loading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Loading…", color = NeerlyColors.Ink500) }
        return
    }
    if (items.isEmpty()) {
        EmptyMsg("This vendor has nothing available right now.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.id }) { p ->
            Card(
                onClick = { onPick(p) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NeerlyRadius.md),
                colors = CardDefaults.cardColors(containerColor = NeerlyColors.Paper)
            ) {
                Row(Modifier.padding(NeerlySpacing.x4), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = NeerlyColors.Ink900)
                        Text(p.brand ?: p.categoryCode,
                            fontSize = 12.sp, color = NeerlyColors.Ink500)
                    }
                    Text("₹${p.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = NeerlyColors.CustomerPrimary)
                }
            }
        }
    }
}

@Composable
private fun StepFrequency(s: SubscriptionCreateUiState, vm: SubscriptionCreateViewModel) {
    val freqs = listOf("DAILY", "ALT_DAY", "TWICE_WEEK", "WEEKLY", "CUSTOM")
    val slots = listOf("6-8AM", "7-9AM", "8-10AM", "5-7PM", "7-9PM")
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    Column(verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)) {
        SectionHeader("Frequency")
        ChipRow(freqs, selected = s.frequency, onPick = vm::setFrequency)

        if (s.frequency == "CUSTOM") {
            SectionHeader("Pick days")
            ChipRow(days, selected = null, multiSelected = s.daysOfWeek, onPick = vm::toggleDay)
        }

        SectionHeader("Delivery slot")
        ChipRow(slots, selected = s.slot, onPick = vm::setSlot)

        SectionHeader("Quantity")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { vm.setQuantity(s.quantity - 1) }) { Text("−") }
            Text("  ${s.quantity}  ", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp))
            OutlinedButton(onClick = { vm.setQuantity(s.quantity + 1) }) { Text("+") }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = vm::confirmStep2,
            enabled = s.frequency.isNotBlank() && s.slot.isNotBlank() &&
                (s.frequency != "CUSTOM" || s.daysOfWeek.isNotEmpty()),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) { Text("Continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun StepAddress(addresses: List<AddressResponse>, onPick: (AddressResponse) -> Unit) {
    if (addresses.isEmpty()) {
        EmptyMsg("Add a delivery address from your profile first.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(addresses, key = { it.id }) { a ->
            Card(
                onClick = { onPick(a) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NeerlyRadius.md),
                colors = CardDefaults.cardColors(containerColor = NeerlyColors.Paper)
            ) {
                Column(Modifier.padding(NeerlySpacing.x4)) {
                    Row {
                        Text(a.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = NeerlyColors.Ink900)
                        if (a.isPrimary) {
                            Spacer(Modifier.width(8.dp))
                            Text("PRIMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = NeerlyColors.CustomerDark)
                        }
                    }
                    Text("${a.flatNo}, ${a.streetArea}", fontSize = 13.sp, color = NeerlyColors.Ink800)
                    Text("${a.city} · ${a.pincode}", fontSize = 12.sp, color = NeerlyColors.Ink500)
                }
            }
        }
    }
}

@Composable
private fun StepConfirm(
    s: SubscriptionCreateUiState,
    vm: SubscriptionCreateViewModel,
    onCreated: (String) -> Unit
) {
    val product = s.selectedProduct
    val vendor = s.selectedVendor
    val addr = s.selectedAddress
    Column(verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)) {
        Surface(
            color = NeerlyColors.Paper,
            shape = RoundedCornerShape(NeerlyRadius.md),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(NeerlySpacing.x4)) {
                Text("${product?.name} × ${s.quantity}",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = NeerlyColors.Ink900)
                Text("from ${vendor?.businessName}", fontSize = 13.sp, color = NeerlyColors.Ink500)
                Spacer(Modifier.height(8.dp))
                Text("• ${s.frequency}${if (s.daysOfWeek.isNotEmpty()) " (${s.daysOfWeek.joinToString()})" else ""}",
                    fontSize = 13.sp, color = NeerlyColors.Ink800)
                Text("• Slot ${s.slot}", fontSize = 13.sp, color = NeerlyColors.Ink800)
                Text("• Per delivery: ₹${product?.price?.times(java.math.BigDecimal(s.quantity)) ?: "—"}",
                    fontSize = 13.sp, color = NeerlyColors.Ink800)
                Spacer(Modifier.height(6.dp))
                Text(addr?.let { "to ${it.label} · ${it.flatNo}, ${it.streetArea}" } ?: "—",
                    fontSize = 12.sp, color = NeerlyColors.Ink500)
            }
        }

        SectionHeader("Pay via")
        ChipRow(listOf("UPI_AUTOPAY", "WALLET"),
            selected = s.paymentMode, onPick = vm::setPaymentMode)
        Text(
            when (s.paymentMode) {
                "UPI_AUTOPAY" -> "We'll create a UPI mandate. Charged ₹0 today; per-delivery debits start automatically."
                "WALLET" -> "Each delivery debits your wallet. Top up to keep deliveries flowing."
                else -> ""
            },
            fontSize = 12.sp, color = NeerlyColors.Ink500
        )

        if (s.error != null) Text(s.error!!, color = NeerlyColors.Err, fontSize = 13.sp)

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { vm.submit(onCreated) },
            enabled = s.canSubmit && !s.submitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) {
            Text(if (s.submitting) "Submitting…" else "Start subscription",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = NeerlyColors.Ink600,
        modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String?,
    multiSelected: List<String> = emptyList(),
    onPick: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { opt ->
            val active = (selected == opt) || (opt in multiSelected)
            Surface(
                shape = RoundedCornerShape(NeerlyRadius.pill),
                color = if (active) NeerlyColors.CustomerSofter else NeerlyColors.Paper,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (active) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200
                ),
                modifier = Modifier.clickable { onPick(opt) }
            ) {
                Text(
                    opt.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) NeerlyColors.CustomerDark else NeerlyColors.Ink700
                )
            }
        }
    }
}

@Composable
private fun EmptyMsg(text: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(text, fontSize = 14.sp, color = NeerlyColors.Ink500)
    }
}
