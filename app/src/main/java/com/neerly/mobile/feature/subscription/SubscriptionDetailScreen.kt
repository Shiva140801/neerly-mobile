package com.neerly.mobile.feature.subscription

import androidx.compose.foundation.layout.*
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
import java.time.LocalDate

/**
 * Subscription detail — shows next delivery, full history, and the action buttons:
 *   - Skip next (opens a date picker; default = next delivery date)
 *   - Pause for X days
 *   - End subscription
 *
 * For the MVP UI we use simple text dialogs for date input. A proper date picker
 * lives in V1.1 once we add the Compose Material3 date picker dependency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    onBack: () -> Unit,
    vm: SubscriptionDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var pauseOpen by remember { mutableStateOf(false) }
    var skipOpen by remember { mutableStateOf(false) }
    var cancelOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Subscription", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
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
            state.subscription == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(state.error ?: "Subscription not found", color = NeerlyColors.Err)
            }
            else -> {
                val s = state.subscription!!
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(NeerlySpacing.x4),
                    verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
                ) {
                    Surface(
                        color = NeerlyColors.Paper,
                        shape = RoundedCornerShape(NeerlyRadius.md),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(NeerlySpacing.x4)) {
                            Text(s.productName,
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                            Text(s.vendorName, fontSize = 13.sp, color = NeerlyColors.Ink500)
                            Spacer(Modifier.height(NeerlySpacing.x2))
                            Text("${s.frequency} · ${s.quantity} qty · ${s.deliverySlot}",
                                fontSize = 13.sp, color = NeerlyColors.Ink700)
                            s.nextDeliveryDate?.let {
                                Spacer(Modifier.height(NeerlySpacing.x2))
                                Text("Next delivery: ${it.take(10)}",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = NeerlyColors.CustomerDark)
                            }
                            Spacer(Modifier.height(NeerlySpacing.x2))
                            Text("Status: ${s.status}",
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (s.status == "ACTIVE") NeerlyColors.Ok else NeerlyColors.Ink500)
                        }
                    }

                    if (s.status in setOf("ACTIVE", "PAUSED")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { skipOpen = true },
                                modifier = Modifier.weight(1f)
                            ) { Text("Skip next") }
                            OutlinedButton(
                                onClick = { pauseOpen = true },
                                modifier = Modifier.weight(1f)
                            ) { Text("Pause") }
                        }
                        Button(
                            onClick = { cancelOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.ErrSoft, contentColor = NeerlyColors.Err)
                        ) { Text("End subscription") }
                    }

                    state.error?.let {
                        Text(it, color = NeerlyColors.Err, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (skipOpen) {
        // V1: skip the next delivery (today + 1 day). Power users can pick via V1.1.
        AlertDialog(
            onDismissRequest = { skipOpen = false },
            title = { Text("Skip next delivery?") },
            text = { Text("We'll skip your next scheduled delivery. You won't be charged for it.") },
            confirmButton = {
                TextButton(onClick = {
                    val d = state.subscription?.nextDeliveryDate?.take(10) ?: LocalDate.now().toString()
                    vm.skip(d); skipOpen = false
                }) { Text("Skip", color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { skipOpen = false }) { Text("Cancel") } }
        )
    }

    if (pauseOpen) {
        AlertDialog(
            onDismissRequest = { pauseOpen = false },
            title = { Text("Pause subscription?") },
            text = { Text("We'll pause for 7 days. You can resume anytime from this screen. Up to 30 consecutive days allowed.") },
            confirmButton = {
                TextButton(onClick = {
                    val from = java.time.Instant.now().toString()
                    val until = java.time.Instant.now().plusSeconds(7L * 24 * 3600).toString()
                    vm.pause(from, until, reason = null); pauseOpen = false
                }) { Text("Pause 7 days", color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { pauseOpen = false }) { Text("Cancel") } }
        )
    }

    if (cancelOpen) {
        AlertDialog(
            onDismissRequest = { cancelOpen = false },
            title = { Text("End subscription?") },
            text = { Text("You can always start a new one. Any pending containers must still be returned.") },
            confirmButton = {
                TextButton(onClick = { vm.cancel(reason = null); cancelOpen = false }) {
                    Text("End", color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { cancelOpen = false }) { Text("Keep") } }
        )
    }
}
