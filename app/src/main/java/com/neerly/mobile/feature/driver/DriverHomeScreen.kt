package com.neerly.mobile.feature.driver

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
import com.neerly.mobile.data.dto.DriverAssignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    onCodReconcile: () -> Unit,
    vm: DriverHomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var deliverFor by remember { mutableStateOf<DriverAssignment?>(null) }
    var otpDraft by remember { mutableStateOf("") }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Driver", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                actions = {
                    if (state.isOnDuty) {
                        TextButton(onClick = onCodReconcile) {
                            Text("COD", color = NeerlyColors.DriverPrimary)
                        }
                        TextButton(onClick = { vm.endShift(codHandedOver = null) }) {
                            Text("End shift", color = NeerlyColors.Err)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Loading…", color = NeerlyColors.Ink500)
            }
            !state.isOnDuty -> OffDutyHero(onStart = vm::startShift, error = state.error)
            state.assignments.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = NeerlyColors.OkSoft, shape = RoundedCornerShape(NeerlyRadius.pill)) {
                        Text("AVAILABLE",
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            color = NeerlyColors.Ok, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Waiting for next assignment",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(state.assignments, key = { it.orderId }) { a ->
                    AssignmentCard(
                        a,
                        onStart = { vm.startDelivery(a.orderId) },
                        onArrived = { vm.markArrived(a.orderId) },
                        onDeliver = { deliverFor = a; otpDraft = "" }
                    )
                }
            }
        }
    }

    deliverFor?.let { a ->
        AlertDialog(
            onDismissRequest = { deliverFor = null },
            title = { Text("Confirm delivery") },
            text = {
                Column {
                    Text("Customer's 4-digit OTP:", fontSize = 13.sp, color = NeerlyColors.Ink500)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otpDraft, onValueChange = { otpDraft = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (a.paymentMethod == "COD") {
                        Spacer(Modifier.height(12.dp))
                        Text("COD to collect: ₹${a.codAmount ?: 0}",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Warn)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.completeDelivery(
                        orderId = a.orderId,
                        otp = otpDraft,
                        photoS3Key = "delivery-photo-pending",  // upload wired in V1.1
                        codCollected = a.codAmount
                    )
                    deliverFor = null
                }) { Text("Mark delivered", color = NeerlyColors.Ok, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { deliverFor = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OffDutyHero(onStart: () -> Unit, error: String?) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(NeerlySpacing.x6)) {
            Text("Off-duty", fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
            Spacer(Modifier.height(8.dp))
            Text("Tap below when you're ready to start receiving deliveries.",
                fontSize = 13.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(NeerlySpacing.x6))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(0.85f).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.DriverPrimary)
            ) {
                Text("Start shift", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = NeerlyColors.Err, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AssignmentCard(
    a: DriverAssignment,
    onStart: () -> Unit,
    onArrived: () -> Unit,
    onDeliver: () -> Unit
) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Text("#${a.orderNumber} · ${a.customerFirstName}",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text(a.product, fontSize = 13.sp, color = NeerlyColors.Ink700)
            Text(a.deliveryAddress, fontSize = 12.sp, color = NeerlyColors.Ink500)
            if (a.paymentMethod == "COD") {
                Text("COD ₹${a.codAmount ?: 0}",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Warn)
            }
            Spacer(Modifier.height(NeerlySpacing.x3))
            when (a.status) {
                "DISPATCHED" -> Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.DriverPrimary)
                ) { Text("Start delivery", fontWeight = FontWeight.SemiBold) }
                "EN_ROUTE" -> Button(
                    onClick = onArrived,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.DriverPrimary)
                ) { Text("I've arrived", fontWeight = FontWeight.SemiBold) }
                "ARRIVED" -> Button(
                    onClick = onDeliver,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.Ok)
                ) { Text("Confirm delivery (OTP)", fontWeight = FontWeight.SemiBold) }
                else -> Text("Status: ${a.status}",
                    fontSize = 12.sp, color = NeerlyColors.Ink500)
            }
        }
    }
}
