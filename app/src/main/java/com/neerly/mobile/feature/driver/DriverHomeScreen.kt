package com.neerly.mobile.feature.driver

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.data.dto.DriverOffer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.DriverAssignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    onCodReconcile: () -> Unit,
    onSwitchRole: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    vm: DriverHomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e ->
            when (e) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> vm.setVisible(true)
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> vm.setVisible(false)
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); vm.setVisible(false) }
    }
    var deliverFor by remember { mutableStateOf<DriverAssignment?>(null) }
    var otpDraft by remember { mutableStateOf("") }

    // Live tracking needs a fix stream the moment the shift starts, so ask
    // when the driver goes on duty rather than burying it in onboarding.
    val context = LocalContext.current
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> vm.onLocationPermission(granted) }
    LaunchedEffect(state.isOnDuty) {
        if (!state.isOnDuty) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) vm.onLocationPermission(true)
        else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

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
                    } else {
                        // Off duty there was no way out of the driver app at all. Leaving
                        // mid-shift stays behind "End shift" so cash is reconciled first.
                        onSwitchRole?.let {
                            TextButton(onClick = it) { Text("Switch", color = NeerlyColors.DriverPrimary) }
                        }
                        TextButton(onClick = onLogout) { Text("Log out", color = NeerlyColors.Ink700) }
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
            else -> LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                state.error?.let { item { Text(it, color = NeerlyColors.Err, fontSize = 13.sp) } }
                state.lastDelivered?.let {
                    item {
                        Surface(color = NeerlyColors.OkSoft, shape = RoundedCornerShape(NeerlyRadius.md),
                            modifier = Modifier.fillMaxWidth()) {
                            Text("✓ #$it delivered", Modifier.padding(NeerlySpacing.x4),
                                color = NeerlyColors.Ok, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                val active = state.activeAssignment
                when {
                    active != null -> item {
                        AssignmentCard(
                            active,
                            onArrived = { vm.markArrived(active.orderId) },
                            onDeliver = { deliverFor = active; otpDraft = "" }
                        )
                    }
                    state.offer != null -> item {
                        OfferCard(state.offer!!, busy = state.offerBusy, onAccept = vm::acceptOffer, onPass = vm::passOffer)
                    }
                    else -> item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(color = NeerlyColors.OkSoft, shape = RoundedCornerShape(NeerlyRadius.pill)) {
                                Text("AVAILABLE",
                                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = NeerlyColors.Ok, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(20.dp))
                            Text("Waiting for your next run",
                                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                            Text("Runs your vendor offers appear here.",
                                fontSize = 13.sp, color = NeerlyColors.Ink500)
                        }
                    }
                }
            }
        }
    }

    deliverFor?.let { a ->
        var empties by remember(a.orderId) { mutableStateOf(a.emptiesExpected) }
        var settlement by remember(a.orderId) { mutableStateOf("CASH") }
        AlertDialog(
            onDismissRequest = { if (!state.delivering) deliverFor = null },
            title = { Text("Confirm delivery") },
            text = {
                Column {
                    Text("Ask the customer for their 6-digit delivery code:", fontSize = 13.sp, color = NeerlyColors.Ink500)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otpDraft, onValueChange = { otpDraft = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Empties collected", fontSize = 13.sp, color = NeerlyColors.Ink700, modifier = Modifier.weight(1f))
                        TextButton(onClick = { empties = (empties - 1).coerceAtLeast(0) }) { Text("−") }
                        Text("$empties", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { empties = (empties + 1).coerceAtMost(20) }) { Text("+") }
                    }
                    Text(
                        if (a.emptiesExpected == 0) "No empties expected on this drop."
                        else "Expected ${a.emptiesExpected}.",
                        fontSize = 11.sp, color = NeerlyColors.Ink500
                    )
                    if (a.paymentMethod == "COD") {
                        Spacer(Modifier.height(12.dp))
                        Text("Collect ${(a.codAmount ?: java.math.BigDecimal.ZERO).asRupees()}",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Warn)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("CASH" to "Cash", "UPI_AT_DOOR" to "UPI", "NOT_COLLECTED" to "Not paid").forEach { (k, label) ->
                                FilterChip(selected = settlement == k, onClick = { settlement = k }, label = { Text(label) })
                            }
                        }
                    }
                    state.deliverError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = NeerlyColors.Err, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = otpDraft.length == 6 && !state.delivering,
                    onClick = {
                        vm.completeDelivery(
                            orderId = a.orderId,
                            otp = otpDraft,
                            // Camera + S3 upload is an external integration not wired locally.
                            photoS3Key = "delivery-photo-pending",
                            codCollected = if (a.paymentMethod == "COD" && settlement != "NOT_COLLECTED") a.codAmount else null,
                            emptiesCollected = empties,
                            codSettlement = if (a.paymentMethod == "COD") settlement else null
                        ) { ok -> if (ok) deliverFor = null }
                    }
                ) { Text(if (state.delivering) "Checking…" else "Mark delivered", color = NeerlyColors.Ok, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { deliverFor = null }, enabled = !state.delivering) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OfferCard(offer: DriverOffer, busy: Boolean, onAccept: () -> Unit, onPass: () -> Unit) {
    val deadline = remember(offer.expiresAt) { runCatching { java.time.Instant.parse(offer.expiresAt) }.getOrNull() }
    var now by remember { mutableStateOf(java.time.Instant.now()) }
    LaunchedEffect(deadline) {
        while (deadline != null && now.isBefore(deadline)) { kotlinx.coroutines.delay(1_000); now = java.time.Instant.now() }
    }
    val left = deadline?.let { java.time.Duration.between(now, it).seconds.coerceAtLeast(0) } ?: offer.secondsRemaining
    val t = offer.task
    Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md), shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(NeerlySpacing.x4), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NEW RUN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.DriverPrimary,
                    modifier = Modifier.weight(1f))
                Surface(color = if (left <= 10) NeerlyColors.ErrSoft else NeerlyColors.WarnSoft,
                    shape = RoundedCornerShape(NeerlyRadius.pill)) {
                    Text(if (left == 0L) "Expired" else "${left}s to accept",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = if (left <= 10) NeerlyColors.Err else NeerlyColors.Warn)
                }
            }
            Text("You earn ${offer.driverFee.asRupees()}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            if (t != null) {
                Text("PICKUP · ${t.vendorName}", fontSize = 11.sp, color = NeerlyColors.Ink500, fontWeight = FontWeight.SemiBold)
                Text(t.pickupAddress, fontSize = 13.sp, color = NeerlyColors.Ink700)
                Text("DROP" + (t.dropDistanceKm?.let { " · %.1f km".format(it) } ?: "") + " · ${t.customerFirstName}",
                    fontSize = 11.sp, color = NeerlyColors.Ink500, fontWeight = FontWeight.SemiBold)
                Text(t.dropAddress, fontSize = 13.sp, color = NeerlyColors.Ink700)
                Text(
                    listOfNotNull(
                        if (t.jars == 1) "1 jar" else "${t.jars} jars",
                        "${t.loadKg.stripTrailingZeros().toPlainString()} kg",
                        t.emptiesExpected.takeIf { it > 0 }?.let { "collect $it empt${if (it == 1) "y" else "ies"}" },
                        if (t.paymentMethod == "COD") "COD ${(t.codAmount ?: java.math.BigDecimal.ZERO).asRupees()}" else "Prepaid"
                    ).joinToString(" · "),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPass, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Pass") }
                Button(onClick = onAccept, enabled = !busy && left > 0, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.DriverPrimary)
                ) { Text(if (busy) "Accepting…" else "Accept", fontWeight = FontWeight.SemiBold) }
            }
        }
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
                Text("COD ${(a.codAmount ?: java.math.BigDecimal.ZERO).asRupees()}",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Warn)
            }
            if (a.emptiesExpected > 0) {
                Text("Collect ${a.emptiesExpected} empt${if (a.emptiesExpected == 1) "y" else "ies"}",
                    fontSize = 12.sp, color = NeerlyColors.Ink700)
            }
            Spacer(Modifier.height(NeerlySpacing.x3))
            // Real statuses. "Start delivery" acknowledged without moving the order, so the
            // card never left it; EN_ROUTE/ARRIVED were never sent.
            when (a.status) {
                "PREPARING" -> Text("Waiting for the vendor to load your run.",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                "DISPATCHED" -> Button(
                    onClick = onArrived,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.DriverPrimary)
                ) { Text("I've arrived", fontWeight = FontWeight.SemiBold) }
                "ARRIVING" -> Button(
                    onClick = onDeliver,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.Ok)
                ) { Text("Confirm delivery", fontWeight = FontWeight.SemiBold) }
                else -> Text(a.status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp, color = NeerlyColors.Ink500)
            }
        }
    }
}
