package com.neerly.mobile.feature.vendor.earnings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.EarningsBucket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorEarningsScreen(
    onBack: () -> Unit,
    vm: VendorEarningsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Earnings", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.VendorPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Loading…", color = NeerlyColors.Ink500)
            }
            state.summary == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(state.error ?: "No data", color = NeerlyColors.Err)
            }
            else -> {
                val s = state.summary!!
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(NeerlySpacing.x4),
                    verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
                ) {
                    NextPayoutCard(s.nextPayoutAt, s.nextPayoutAmount?.toPlainString())
                    BucketCard("Today", s.today)
                    BucketCard("This week", s.week)
                    BucketCard("This month", s.month)
                }
            }
        }
    }
}

@Composable
private fun NextPayoutCard(at: String?, amount: String?) {
    Surface(color = NeerlyColors.VendorPrimary, shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(NeerlySpacing.x5)) {
            Text("Next payout", fontSize = 12.sp,
                color = NeerlyColors.Paper.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            Text("₹${amount ?: "—"}", fontSize = 24.sp,
                fontWeight = FontWeight.Bold, color = NeerlyColors.Paper)
            Text(at?.let { "On ${it.take(10)} via NEFT" } ?: "Schedule pending",
                fontSize = 12.sp, color = NeerlyColors.Paper.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun BucketCard(label: String, bucket: EarningsBucket) {
    Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Text(label, fontSize = 12.sp, color = NeerlyColors.Ink500, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("₹${bucket.net}", fontSize = 22.sp,
                fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text("${bucket.orders} orders · gross ₹${bucket.gross}",
                fontSize = 12.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(NeerlySpacing.x2))
            Row(Modifier.fillMaxWidth()) {
                LineCol("Commission", "−₹${bucket.commission}", Modifier.weight(1f))
                LineCol("Gateway", "−₹${bucket.gatewayFee}", Modifier.weight(1f))
                LineCol("TCS", "−₹${bucket.tcs}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LineCol(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = NeerlyColors.Ink500, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 12.sp, color = NeerlyColors.Err)
    }
}
