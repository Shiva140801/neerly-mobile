package com.neerly.mobile.feature.wallet

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.dto.WalletTransaction
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    onTopupReady: (paymentId: String, razorpayOrderId: String?) -> Unit,
    vm: WalletViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Wallet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        when {
            state.loading && state.balance == null -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text("Loading…", color = NeerlyColors.Ink500)
                }
            }
            // Balance never loaded. Showing "₹0" and "No transactions yet" here
            // would tell a customer with money that they have none, so we render
            // nothing but the failure and a retry.
            state.balance == null -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(NeerlySpacing.x4)) {
                    BalanceUnavailable(
                        message = state.error ?: "Couldn't load your balance. Please try again.",
                        onRetry = vm::refresh
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(NeerlySpacing.x4),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    item { BalanceCard(state.balance!!) }
                    item {
                        Text("Add money", fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                    }
                    item { TopupChips(disabled = state.toppingUp, onPick = { amount ->
                        vm.topup(amount) { result ->
                            onTopupReady(result.paymentId, result.razorpayOrderId)
                        }
                    }) }
                    if (state.error != null) {
                        item { Text(state.error!!, color = NeerlyColors.Err, fontSize = 13.sp) }
                    }
                    item { Spacer(Modifier.height(NeerlySpacing.x3)) }
                    item {
                        Text("Recent transactions", fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                    }
                    if (state.transactions.isEmpty()) {
                        item {
                            Text("No transactions yet.",
                                fontSize = 13.sp, color = NeerlyColors.Ink500)
                        }
                    } else {
                        items(state.transactions, key = { it.id }) { TxnRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceUnavailable(message: String, onRetry: () -> Unit) {
    Surface(
        color = NeerlyColors.ErrSoft,
        shape = RoundedCornerShape(NeerlyRadius.lg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x6)) {
            Text("Couldn't load your balance",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Err)
            Spacer(Modifier.height(NeerlySpacing.x2))
            Text(message, fontSize = 13.sp, color = NeerlyColors.Ink700)
            Spacer(Modifier.height(NeerlySpacing.x4))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(NeerlyRadius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
            ) { Text("Retry", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Only ever called with a balance the backend actually returned. */
@Composable
private fun BalanceCard(balance: WalletResponse) {
    Surface(
        color = NeerlyColors.CustomerPrimary,
        shape = RoundedCornerShape(NeerlyRadius.lg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x6)) {
            Text("Available balance",
                fontSize = 12.sp, color = NeerlyColors.Paper.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "₹${balance.availableAmount}",
                fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Paper
            )
            balance.heldAmount.takeIf { it.signum() > 0 }?.let {
                Spacer(Modifier.height(6.dp))
                Text("₹$it held for active orders",
                    fontSize = 12.sp, color = NeerlyColors.Paper.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun TopupChips(disabled: Boolean, onPick: (BigDecimal) -> Unit) {
    val amounts = listOf("200", "500", "1000", "2000")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        amounts.forEach { a ->
            Surface(
                color = NeerlyColors.Paper,
                shape = RoundedCornerShape(NeerlyRadius.pill),
                shadowElevation = 1.dp,
                modifier = Modifier.clickable(enabled = !disabled) { onPick(BigDecimal(a)) }
            ) {
                Text("+₹$a",
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = NeerlyColors.CustomerDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TxnRow(txn: WalletTransaction) {
    val signed = if (txn.amount.signum() < 0) "−₹${txn.amount.abs()}" else "+₹${txn.amount}"
    val color = if (txn.amount.signum() < 0) NeerlyColors.Err else NeerlyColors.Ok

    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(txn.type, fontSize = 11.sp, color = NeerlyColors.Ink500,
                    fontWeight = FontWeight.SemiBold)
                Text(txn.description ?: txn.type, fontSize = 14.sp,
                    color = NeerlyColors.Ink900, fontWeight = FontWeight.SemiBold)
                Text(txn.occurredAt.take(10), fontSize = 11.sp, color = NeerlyColors.Ink500)
            }
            Text(signed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}
