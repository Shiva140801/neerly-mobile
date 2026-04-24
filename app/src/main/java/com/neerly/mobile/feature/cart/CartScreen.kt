package com.neerly.mobile.feature.cart

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.neerly.mobile.data.cart.Cart
import com.neerly.mobile.data.cart.CartItem
import com.neerly.mobile.feature.promo.PromoCodeField
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    vm: CartViewModel = hiltViewModel()
) {
    val cart by vm.cart.collectAsState()
    val promo by vm.promo.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Your cart", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        cart.vendorName?.let {
                            Text(it, fontSize = 12.sp, color = NeerlyColors.Ink500)
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                actions = {
                    if (!cart.isEmpty) {
                        TextButton(onClick = { vm.clearCart() }) {
                            Text("Clear", color = NeerlyColors.Err)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        },
        bottomBar = {
            if (!cart.isEmpty) {
                Surface(color = NeerlyColors.Paper, shadowElevation = 4.dp) {
                    Column(Modifier.padding(NeerlySpacing.x4)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Total", fontSize = 12.sp, color = NeerlyColors.Ink500)
                                Text("₹${cart.total}", fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                            }
                            Button(
                                onClick = onCheckout,
                                enabled = !cart.isEmpty,
                                modifier = Modifier.height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
                            ) {
                                Text("Checkout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (cart.isEmpty) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your cart is empty",
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                    Spacer(Modifier.height(6.dp))
                    Text("Add water from a vendor to get started.",
                        fontSize = 13.sp, color = NeerlyColors.Ink500)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(cart.items, key = { it.productId }) { item ->
                    CartLine(
                        item = item,
                        onInc = { vm.updateQuantity(item.productId, item.quantity + 1) },
                        onDec = { vm.updateQuantity(item.productId, item.quantity - 1) },
                        onRemove = { vm.removeLine(item.productId) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    PromoCodeField(
                        orderSubtotal = cart.subtotal,
                        isFirstOrder = promo.isFirstOrder,
                        currentDiscount = cart.discount,
                        currentReason = promo.error,
                        onQuote = { vm.quotePromo(it, isFirstOrder = false) },
                        onRemove = { vm.removePromo() }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                item { PricingBreakdown(cart) }
            }
        }
    }
}

@Composable
private fun CartLine(
    item: CartItem,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeerlyColors.CustomerSofter),
                contentAlignment = Alignment.Center
            ) { Text("💧", fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.productName, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text("₹${item.unitPrice} × ${item.quantity} = ₹${item.lineTotal}",
                    fontSize = 12.sp, color = NeerlyColors.Ink500)
                if (!item.keepContainer) {
                    Text("Transfer & return", fontSize = 11.sp, color = NeerlyColors.VendorDark)
                } else if (item.depositPerContainer != null) {
                    Text("+ ₹${item.lineDeposit} deposit",
                        fontSize = 11.sp, color = NeerlyColors.Ink500)
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                QtyButton("−", onDec)
                Text(item.quantity.toString(), fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp))
                QtyButton("+", onInc)
            }
        }
    }
}

@Composable
private fun QtyButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NeerlyColors.CustomerSofter)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 18.sp, color = NeerlyColors.CustomerDark, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun PricingBreakdown(cart: Cart) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            PricingRow("Subtotal", cart.subtotal)
            if (cart.deliveryFee > BigDecimal.ZERO) PricingRow("Delivery", cart.deliveryFee)
            if (cart.surge > BigDecimal.ZERO) PricingRow("Surge", cart.surge, surge = true)
            if (cart.deposit > BigDecimal.ZERO) PricingRow("Deposit", cart.deposit, sub = "refundable")
            if (cart.discount > BigDecimal.ZERO) PricingRow("Promo${cart.promoCode?.let { " · $it" } ?: ""}",
                cart.discount.negate())
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = NeerlyColors.Ink100)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total", Modifier.weight(1f),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text("₹${cart.total}",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            }
        }
    }
}

@Composable
private fun PricingRow(label: String, amount: BigDecimal, surge: Boolean = false, sub: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp,
                color = if (surge) NeerlyColors.Warn else NeerlyColors.Ink700)
            sub?.let { Text(it, fontSize = 11.sp, color = NeerlyColors.Ink500) }
        }
        Text(
            (if (amount.signum() < 0) "-₹" else "₹") + amount.abs(),
            fontSize = 14.sp,
            color = if (amount.signum() < 0) NeerlyColors.Ok else NeerlyColors.Ink900
        )
    }
}
