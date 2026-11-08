package com.neerly.mobile.feature.cart

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
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
import com.neerly.mobile.core.design.SkeletonBlock
import com.neerly.mobile.core.util.asRupees
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

    val cfg = com.neerly.mobile.data.config.LocalAppConfig.current
    val short = (cfg.minOrderValue - cart.subtotal).takeIf { cfg.minOrderValue.signum() > 0 && it.signum() > 0 }
    // Board 12 (cart) and 43 (below the minimum).
    com.neerly.mobile.core.design.BoondhPage(
        title = if (cart.isEmpty) stringResource(R.string.cust_cart_empty) else stringResource(R.string.cust_cart_title),
        onBack = onBack,
        subtitle = cart.vendorName?.let { stringResource(R.string.cust_cart_from, it) } ?: stringResource(R.string.cust_cart_get_started),
        footer = if (cart.isEmpty) null else ({
            short?.let { com.neerly.mobile.core.design.Notice(stringResource(R.string.cust_cart_below_min, it.asRupees(), cart.vendorName ?: stringResource(R.string.cust_this_shop), cfg.minOrderValue.asRupees()), com.neerly.mobile.core.design.Tone.Warn) }
            if (promo.quoting) SkeletonBlock(height = 22.dp, width = 110.dp)
            com.neerly.mobile.core.design.PrimaryButton(
                if (short != null) stringResource(R.string.cust_cart_add_more) else stringResource(R.string.cust_cart_continue_pay, cart.total.asRupees()),
                onCheckout, enabled = !cart.isEmpty && short == null && !promo.quoting)
        })
    ) {
        if (cart.isEmpty) return@BoondhPage
        items(cart.items, key = { it.productId }) { item ->
            CartLine(
                item = item,
                onInc = { vm.updateQuantity(item.productId, item.quantity + 1) },
                onDec = { vm.updateQuantity(item.productId, item.quantity - 1) },
                onRemove = { vm.removeLine(item.productId) }
            )
        }
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
        item { PricingBreakdown(cart) }
        item { TextButton(onClick = { vm.clearCart() }, modifier = Modifier.heightIn(min = 44.dp)) {
            Text(stringResource(R.string.cust_cart_empty_action), color = NeerlyColors.Err) } }
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
                Text(stringResource(R.string.cust_cart_line, item.unitPrice.asRupees(), item.quantity, item.lineTotal.asRupees()),
                    fontSize = 12.sp, color = NeerlyColors.Ink500)
                if (!item.keepContainer) {
                    Text(stringResource(R.string.cust_cart_swap_tag), fontSize = 11.sp, color = NeerlyColors.VendorDark)
                } else if (item.depositPerContainer != null) {
                    Text(stringResource(R.string.cust_cart_plus_deposit, item.lineDeposit.asRupees()),
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
            PricingRow(stringResource(R.string.cust_subtotal), cart.subtotal)
            if (cart.deliveryFee > BigDecimal.ZERO) PricingRow(stringResource(R.string.cust_delivery), cart.deliveryFee)
            if (cart.surge > BigDecimal.ZERO) PricingRow(stringResource(R.string.cust_surge), cart.surge, surge = true)
            if (cart.deposit > BigDecimal.ZERO) PricingRow(stringResource(R.string.cust_deposit), cart.deposit, sub = stringResource(R.string.cust_refundable))
            if (cart.discount > BigDecimal.ZERO) PricingRow(cart.promoCode?.let { stringResource(R.string.cust_promo_code_line, it) } ?: stringResource(R.string.cust_promo),
                cart.discount.negate())
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = NeerlyColors.Ink100)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cust_total), Modifier.weight(1f),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(cart.total.asRupees(),
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
            (if (amount.signum() < 0) "-" else "") + amount.abs().asRupees(),
            fontSize = 14.sp,
            color = if (amount.signum() < 0) NeerlyColors.Ok else NeerlyColors.Ink900
        )
    }
}
