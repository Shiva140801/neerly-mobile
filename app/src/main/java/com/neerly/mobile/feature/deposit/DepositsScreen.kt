package com.neerly.mobile.feature.deposit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.DepositResponse
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositsScreen(
    onBack: () -> Unit,
    vm: DepositsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var pickupTarget by remember { mutableStateOf<DepositResponse?>(null) }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Returns & Deposits", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Loading…", color = NeerlyColors.Ink500)
            }
            state.deposits.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No deposits held",
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                    Spacer(Modifier.height(6.dp))
                    Text("Containers you keep show up here with their return deadline.",
                        fontSize = 13.sp, color = NeerlyColors.Ink500)
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                if (state.held.isNotEmpty()) {
                    item { Section("Active deposits", state.held.sumOf { it.amount }) }
                    items(state.held, key = { it.id }) { d ->
                        Row(d, primary = true, onPickup = { pickupTarget = d })
                    }
                }
                if (state.returned.isNotEmpty()) {
                    item { Section("Returned", state.returned.sumOf { it.amount }) }
                    items(state.returned, key = { it.id }) { Row(it, primary = false) }
                }
                if (state.forfeited.isNotEmpty()) {
                    item { Section("Forfeited", state.forfeited.sumOf { it.amount }) }
                    items(state.forfeited, key = { it.id }) { Row(it, primary = false, forfeited = true) }
                }
            }
        }
    }

    pickupTarget?.let { d ->
        AlertDialog(
            onDismissRequest = { pickupTarget = null },
            title = { Text("Schedule return") },
            text = {
                Text(
                    "How would you like to return ${d.productName ?: "this container"}? " +
                        "Pickup is at your address; drop-off is at the vendor's shop."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.scheduleReturn(d.id, mode = "SCHEDULED_PICKUP")
                    pickupTarget = null
                }) { Text("Schedule pickup", color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.scheduleReturn(d.id, mode = "DROPOFF")
                    pickupTarget = null
                }) { Text("I'll drop off", color = NeerlyColors.Ink700) }
            }
        )
    }
}

@Composable
private fun Section(label: String, total: BigDecimal) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = NeerlyColors.Ink500, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        Text("₹$total", fontSize = 11.sp, color = NeerlyColors.Ink500, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Row(
    deposit: DepositResponse,
    primary: Boolean,
    forfeited: Boolean = false,
    onPickup: (() -> Unit)? = null
) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    deposit.productName ?: "Container",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (forfeited) NeerlyColors.Err else NeerlyColors.Ink900
                )
                Text(deposit.vendorName ?: "—", fontSize = 12.sp, color = NeerlyColors.Ink500)
                deposit.returnDeadline?.let {
                    Text("Return by ${it.take(10)}", fontSize = 11.sp, color = NeerlyColors.CustomerDark)
                }
                if (deposit.lateFeeAccrued.signum() > 0) {
                    Text("Late fee: ₹${deposit.lateFeeAccrued}",
                        fontSize = 11.sp, color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${deposit.amount}",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                if (primary && onPickup != null) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onPickup) { Text("Return", fontSize = 12.sp) }
                }
            }
        }
    }
}
