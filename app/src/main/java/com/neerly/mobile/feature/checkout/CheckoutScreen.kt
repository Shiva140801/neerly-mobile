package com.neerly.mobile.feature.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.neerly.mobile.core.design.CartSkeleton
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.OrderQuoteResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.core.util.asRupees

/**
 * Checkout review — address selector, payment method, slot, total.
 * Place-order button is the single CTA; it triggers the VM to:
 *   1. create the order server-side
 *   2. initiate payment
 *   3. launch the Razorpay overlay (or for COD, skip to confirmation)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cart: CartStore,
    onBack: () -> Unit,
    onOrderPlaced: (OrderResponse) -> Unit,
    vm: CheckoutViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snapshot by cart.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        },
        bottomBar = {
            Surface(color = NeerlyColors.Paper, shadowElevation = 4.dp) {
                Column(Modifier.padding(NeerlySpacing.x4)) {
                    if (state.error != null) {
                        Text(state.error!!, color = NeerlyColors.Err, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                    state.paymentProblem?.let {
                        Text(it, color = NeerlyColors.Err, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pay", fontSize = 12.sp, color = NeerlyColors.Ink500)
                            // Only ever the server's figure — the cart subtotal left out
                            // delivery and the refundable deposit.
                            Text(state.quote?.totalAmount?.asRupees() ?: "—", fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        }
                        Button(
                            onClick = {
                                val activity = context.findActivity() ?: return@Button
                                vm.placeAndPay(activity) { order -> onOrderPlaced(order) }
                            },
                            enabled = !state.placing && !state.paying && state.quote != null &&
                                state.selectedAddress != null && !snapshot.isEmpty,
                            modifier = Modifier.height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
                        ) {
                            Text(
                                when {
                                    state.placing -> "Placing…"
                                    state.paying -> "Paying…"
                                    state.awaitingPaymentFor != null -> "Try payment again"
                                    else -> "Place order"
                                },
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        // `SKEL-CART` — the artboard's note is "CTA bar stays anchored", which
        // is exactly this screen: addresses load over the network while the
        // Place-order bar holds its position.
        if (state.loading && state.addresses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) { CartSkeleton() }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(NeerlySpacing.x4),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item { SectionHeader("Deliver to") }
            if (state.addresses.isEmpty() && !state.loading) {
                item {
                    Surface(color = NeerlyColors.WarnSoft, shape = RoundedCornerShape(NeerlyRadius.md),
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Add a delivery address to continue.",
                            Modifier.padding(NeerlySpacing.x4),
                            color = NeerlyColors.Warn, fontSize = 14.sp)
                    }
                }
            } else {
                items(state.addresses.size) { idx ->
                    val a = state.addresses[idx]
                    AddressChip(
                        addr = a,
                        selected = state.selectedAddress?.id == a.id,
                        onClick = { vm.selectAddress(a.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
            item { SectionHeader("Payment method") }
            item {
                PaymentMethodRow(state.paymentMethod) { vm.selectPaymentMethod(it) }
            }

            item { Spacer(Modifier.height(4.dp)) }
            item { SectionHeader("Bill") }
            item { BillCard(state.quote, state.quoting, onRetry = vm::refreshQuote) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
}

@Composable
private fun AddressChip(addr: AddressResponse, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) NeerlyColors.CustomerSofter else NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.CustomerPrimary)
                 else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(addr.label, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text("${addr.flatNumber}, ${addr.street} · ${addr.pincode}",
                    fontSize = 12.sp, color = NeerlyColors.Ink500)
            }
            if (selected) {
                Text("✓", fontSize = 18.sp,
                    color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(selected: String, onPick: (String) -> Unit) {
    val methods = listOf(
        "UPI" to "UPI",
        "Card" to "CARD",
        "Wallet" to "WALLET",
        "COD" to "COD"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        methods.forEach { (label, code) ->
            val isOn = selected == code
            Surface(
                color = if (isOn) NeerlyColors.CustomerPrimary else NeerlyColors.Paper,
                shape = RoundedCornerShape(NeerlyRadius.pill),
                shadowElevation = 1.dp,
                modifier = Modifier.clickable { onPick(code) }
            ) {
                Text(
                    label,
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isOn) NeerlyColors.Paper else NeerlyColors.Ink700,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun BillCard(quote: OrderQuoteResponse?, loading: Boolean, onRetry: () -> Unit) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                quote == null && loading ->
                    Text("Working out your total…", fontSize = 13.sp, color = NeerlyColors.Ink500)
                quote == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Couldn't price this order.", fontSize = 13.sp, color = NeerlyColors.Err,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetry) { Text("Retry", color = NeerlyColors.CustomerPrimary) }
                }
                else -> {
                    BillLine("Water", quote.subtotal.asRupees())
                    BillLine(
                        "Delivery",
                        if (quote.deliveryFee.signum() == 0) "Free" else quote.deliveryFee.asRupees()
                    )
                    if (quote.surgeSurcharge.signum() > 0) BillLine("Busy-hour surcharge", quote.surgeSurcharge.asRupees())
                    if (quote.taxes.signum() > 0) BillLine("Taxes", quote.taxes.asRupees())
                    if (quote.discount.signum() > 0) BillLine("Promo", "−" + quote.discount.asRupees(), NeerlyColors.Ok)
                    quote.promoMessage?.let { Text(it, fontSize = 12.sp, color = NeerlyColors.Warn) }
                    if (quote.depositTotal.signum() > 0) {
                        BillLine("Refundable deposit", quote.depositTotal.asRupees())
                        Text(
                            "Comes back to your wallet when you return the jar.",
                            fontSize = 11.sp, color = NeerlyColors.Ink500
                        )
                    }
                    HorizontalDivider(color = NeerlyColors.Ink100)
                    BillLine("Total", quote.totalAmount.asRupees(), bold = true)
                }
            }
        }
    }
}

@Composable
private fun BillLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = NeerlyColors.Ink900,
    bold: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = NeerlyColors.Ink700, modifier = Modifier.weight(1f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = 14.sp, color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}
