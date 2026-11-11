package com.neerly.mobile.feature.cart

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.LocationOn
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
    onPickSlot: () -> Unit = {},
    onAddAddress: () -> Unit = {},
    vm: CartViewModel = hiltViewModel()
) {
    val cart by vm.cart.collectAsState()
    val promo by vm.promo.collectAsState()
    val quickAdds by vm.quickAdds.collectAsState()
    val addresses by vm.addresses.collectAsState()
    val quote by vm.quote.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var showPromo by remember { mutableStateOf(cart.promoCode != null) }

    val cfg = com.neerly.mobile.data.config.LocalAppConfig.current
    val short = (cfg.minOrderValue - cart.subtotal).takeIf { cfg.minOrderValue.signum() > 0 && it.signum() > 0 }
    androidx.compose.runtime.LaunchedEffect(short != null, cart.vendorId) { if (short != null) vm.loadQuickAdds() }
    val address = addresses.firstOrNull { it.id == cart.addressId }
    // Boards Cart and CartMinimum: chevron + title, hairline lines with an outlined
    // stepper, When? side by side, where it goes, and the bill the customer will pay.
    com.neerly.mobile.core.design.BoondhPage(
        title = if (cart.isEmpty) stringResource(R.string.cust_cart_empty) else stringResource(R.string.cust_cart_title),
        onBack = onBack, inlineTitle = true,
        subtitle = cart.vendorName?.let { stringResource(R.string.cust_cart_from, it) } ?: stringResource(R.string.cust_cart_get_started),
        footer = if (cart.isEmpty) null else ({
            short?.let { com.neerly.mobile.core.design.Notice(stringResource(R.string.cust_cart_below_min, it.asRupees(), cart.vendorName ?: stringResource(R.string.cust_this_shop), cfg.minOrderValue.asRupees()), com.neerly.mobile.core.design.Tone.Warn) }
            com.neerly.mobile.core.design.PrimaryButton(
                when {
                    short != null -> stringResource(R.string.cust_cart_add_more)
                    quote != null -> stringResource(R.string.cust_cart_continue_pay, quote!!.totalAmount.asRupees())
                    else -> stringResource(R.string.cust_cart_continue_checkout)
                },
                onCheckout, enabled = !cart.isEmpty && short == null && !promo.quoting)
        })
    ) {
        if (cart.isEmpty) return@BoondhPage
        item { HorizontalDivider(color = NeerlyColors.Ink200) }
        items(cart.items, key = { it.productId }) { item ->
            CartLine(
                item = item,
                onInc = { vm.updateQuantity(item.productId, item.quantity + 1) },
                onDec = { vm.updateQuantity(item.productId, item.quantity - 1) },
                onRemove = { vm.removeLine(item.productId) }
            )
        }
        if (short != null && quickAdds.isNotEmpty()) item {
            QuickAddsCard(quickAdds, vm::quickAdd, cfg.minOrderValue)
        }
        item {
            val (lo, hi) = cart.etaMin?.let { lo -> lo to (cart.etaMax ?: lo) } ?: cfg.fallbackEta
            WhenCard(cart.slot, stringResource(R.string.cust_cart_now_eta, lo, hi), onNow = vm::deliverNow, onPickSlot = onPickSlot)
        }
        item { AddressCard(address, onChange = { if (addresses.isEmpty()) onAddAddress() else picking = true }) }
        item { Bill(cart, quote) }
        item {
            if (showPromo) PromoCodeField(
                orderSubtotal = cart.subtotal,
                isFirstOrder = promo.isFirstOrder,
                currentDiscount = cart.discount,
                currentReason = promo.error,
                onQuote = { vm.quotePromo(it, isFirstOrder = false) },
                onRemove = { vm.removePromo() }
            ) else TextButton(onClick = { showPromo = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cust_cart2_have_promo), color = NeerlyColors.CustomerPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        item { TextButton(onClick = { vm.clearCart() }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.cust_cart_empty_action), color = NeerlyColors.Err, fontSize = 15.sp) } }
    }
    if (picking) AddressSheet(addresses, cart.addressId,
        onPick = { vm.selectAddress(it); picking = false },
        onAdd = { picking = false; onAddAddress() },
        onDismiss = { picking = false })
}

/** Board Cart "When?": two tiles side by side — right now, or a picked window. */
@Composable
private fun WhenCard(slot: com.neerly.mobile.data.cart.DeliverySlot?, nowEta: String, onNow: () -> Unit, onPickSlot: () -> Unit) {
    Text(stringResource(R.string.cust_cart2_when), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
        WhenOption(stringResource(R.string.cust_cart_now), nowEta, slot == null, onNow, Modifier.weight(1f).fillMaxHeight())
        val picked = slot?.let {
            val day = it.date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", com.neerly.mobile.core.util.AppLocale.locale))
            "$day, " + com.neerly.mobile.core.design.timeWindow(it.start.toString(), it.end.toString())
        }
        WhenOption(stringResource(R.string.cust_cart_pick_slot), picked ?: stringResource(R.string.cust_cart_slot_hint), slot != null, onPickSlot, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun WhenOption(title: String, detail: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.heightIn(min = 64.dp)
            .background(NeerlyColors.Paper, RoundedCornerShape(14.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200, RoundedCornerShape(14.dp))
            .clickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        Text(detail, fontSize = 13.sp, lineHeight = 18.sp, color = NeerlyColors.Ink600)
    }
}

/** Board Cart: where it goes, with Change (a sheet of saved addresses). */
@Composable
private fun AddressCard(a: com.neerly.mobile.data.dto.AddressResponse?, onChange: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(NeerlyColors.Paper, RoundedCornerShape(14.dp))
            .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(14.dp))
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onChange)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(androidx.compose.material.icons.Icons.Outlined.LocationOn, contentDescription = null, tint = NeerlyColors.Ink900)
        Column(Modifier.weight(1f)) {
            if (a == null) Text(stringResource(R.string.cust_cart2_where_to), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            else {
                Text(com.neerly.mobile.feature.address.addressLabel(a.label), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(addressLine(a), fontSize = 14.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
            }
        }
        Text(if (a == null) stringResource(R.string.cust_cart2_add) else stringResource(R.string.cust_cart2_change),
            color = NeerlyColors.CustomerPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun addressLine(a: com.neerly.mobile.data.dto.AddressResponse): String {
    val floor = a.floorNumber?.takeIf { it > 0 }?.let {
        if (a.hasLift) stringResource(R.string.cust_cart2_floor_lift, it) else stringResource(R.string.cust_cart2_floor_no_lift, it)
    }
    return listOfNotNull(listOf(a.flatNumber, a.buildingName).filter { it.isNotBlank() }.joinToString(", ").ifBlank { a.street }, floor)
        .joinToString(" · ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSheet(list: List<com.neerly.mobile.data.dto.AddressResponse>, current: String?,
                         onPick: (String) -> Unit, onAdd: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NeerlyColors.Canvas) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            com.neerly.mobile.core.design.PageTitle(stringResource(R.string.cust_cart2_deliver_where), size = 24)
            Spacer(Modifier.height(8.dp))
            list.forEach { a ->
                com.neerly.mobile.core.design.ListRow(onClick = { onPick(a.id) }) {
                    RadioButton(selected = a.id == current, onClick = null)
                    Column(Modifier.weight(1f)) {
                        Text(com.neerly.mobile.feature.address.addressLabel(a.label), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(addressLine(a), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    }
                }
            }
            TextButton(onClick = onAdd, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cust_cart2_add_new), color = NeerlyColors.CustomerPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Board CartMinimum "Quick adds". */
@Composable
private fun QuickAddsCard(adds: List<QuickAdd>, onAdd: (QuickAdd) -> Unit, minimum: java.math.BigDecimal) {
    com.neerly.mobile.core.design.BoondhCard {
        com.neerly.mobile.core.design.SectionLabel(stringResource(R.string.cust_cart_quick_adds))
        adds.forEach { q ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (q.oneMore) stringResource(R.string.cust_cart_one_more, q.name) else q.name,
                    Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink900)
                Text(q.price.asRupees(), fontSize = 14.sp, color = NeerlyColors.Ink600)
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = { onAdd(q) }, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(R.string.cust_add), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(stringResource(R.string.cust_cart_min_note, minimum.asRupees()), fontSize = 12.sp, color = NeerlyColors.Ink600)
    }
}

@Composable
private fun CartLine(
    item: CartItem,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onRemove: () -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.productName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(when {
                    !item.keepContainer -> stringResource(R.string.cust_cart2_swap_at_door)
                    item.depositPerContainer != null -> stringResource(R.string.cust_cart_plus_deposit, item.lineDeposit.asRupees())
                    else -> stringResource(R.string.cust_cart2_each, item.unitPrice.asRupees())
                }, fontSize = 14.sp, color = NeerlyColors.Ink600)
            }
            Spacer(Modifier.width(10.dp))
            // Outlined stepper pill (board Cart).
            Row(Modifier.border(1.5.dp, NeerlyColors.Ink200, RoundedCornerShape(12.dp)).background(NeerlyColors.Paper, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically) {
                QtyButton("−", stringResource(R.string.cust_cart2_less, item.productName), onDec)
                Text(item.quantity.toString(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                    modifier = Modifier.widthIn(min = 22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                QtyButton("+", stringResource(R.string.cust_cart2_more, item.productName), onInc)
            }
            Spacer(Modifier.width(12.dp))
            Text(item.lineTotal.asRupees(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                modifier = Modifier.widthIn(min = 56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        HorizontalDivider(color = NeerlyColors.Ink200)
    }
}

@Composable
private fun QtyButton(label: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 20.sp, color = NeerlyColors.Ink900, fontWeight = FontWeight.SemiBold) }
}

/** Board Cart bill: dotted leaders, deposit, offer in green, serif "To pay". Server-priced when it can be. */
@Composable
private fun Bill(cart: Cart, quote: com.neerly.mobile.data.dto.OrderQuoteResponse?) {
    Column(Modifier.fillMaxWidth().background(NeerlyColors.Paper, RoundedCornerShape(14.dp))
        .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val cfg = com.neerly.mobile.data.config.LocalAppConfig.current
        BillRow(stringResource(R.string.cust_cart2_water), (quote?.subtotal ?: cart.subtotal).asRupees())
        if (quote != null) BillRow(stringResource(R.string.cust_delivery), quote.deliveryFee.asRupees())
        else BillRow(stringResource(R.string.cust_delivery),
            if (cart.subtotal >= cfg.freeDeliveryAbove) stringResource(R.string.cust_cart_delivery_free)
            else stringResource(R.string.cust_cart_delivery_from, cfg.deliveryFeeNear.asRupees()), muted = true)
        val surge = quote?.surgeSurcharge ?: cart.surge
        if (surge.signum() > 0) BillRow(stringResource(R.string.cust_surge), surge.asRupees())
        val swapping = cart.items.any { !it.keepContainer }
        val deposit = quote?.depositTotal ?: cart.deposit
        if (deposit.signum() > 0 || swapping) BillRow(
            stringResource(R.string.cust_cart2_deposit) + if (deposit.signum() == 0 && swapping) " " + stringResource(R.string.cust_cart2_swapping) else "",
            deposit.asRupees())
        val discount = quote?.discount ?: cart.discount
        if (discount.signum() > 0) BillRow(cart.promoCode?.let { stringResource(R.string.cust_promo_code_line, it) } ?: stringResource(R.string.cust_promo),
            "−" + discount.asRupees(), color = NeerlyColors.Ok)
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = NeerlyColors.Ink200)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (quote != null) stringResource(R.string.cust_cart2_to_pay) else stringResource(R.string.cust_cart_total_before_delivery),
                Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text((quote?.totalAmount ?: cart.total).asRupees(), fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, muted: Boolean = false, color: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.Bottom) {
        Text(label, fontSize = 15.sp, color = color ?: NeerlyColors.Ink900)
        androidx.compose.foundation.Canvas(Modifier.weight(1f).height(10.dp).padding(horizontal = 6.dp)) {
            drawLine(NeerlyColors.Ink200, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height),
                strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 5f)))
        }
        Text(value, fontSize = 15.sp, color = color ?: if (muted) NeerlyColors.Ink600 else NeerlyColors.Ink900)
    }
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
            // The fee depends on the address, which is picked at checkout — say so rather
            // than show a total the customer won't actually pay.
            val cfg = com.neerly.mobile.data.config.LocalAppConfig.current
            val feeKnown = cart.deliveryFee > BigDecimal.ZERO
            val freeDelivery = !feeKnown && cart.subtotal >= cfg.freeDeliveryAbove
            if (feeKnown) PricingRow(stringResource(R.string.cust_delivery), cart.deliveryFee)
            else PricingNote(stringResource(R.string.cust_delivery),
                if (freeDelivery) stringResource(R.string.cust_cart_delivery_free)
                else stringResource(R.string.cust_cart_delivery_from, cfg.deliveryFeeNear.asRupees()))
            if (cart.surge > BigDecimal.ZERO) PricingRow(stringResource(R.string.cust_surge), cart.surge, surge = true)
            if (cart.deposit > BigDecimal.ZERO) PricingRow(stringResource(R.string.cust_deposit), cart.deposit, sub = stringResource(R.string.cust_refundable))
            if (cart.discount > BigDecimal.ZERO) PricingRow(cart.promoCode?.let { stringResource(R.string.cust_promo_code_line, it) } ?: stringResource(R.string.cust_promo),
                cart.discount.negate())
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = NeerlyColors.Ink100)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (feeKnown || freeDelivery) stringResource(R.string.cust_total) else stringResource(R.string.cust_cart_total_before_delivery), Modifier.weight(1f),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(cart.total.asRupees(),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            }
        }
    }
}

@Composable
private fun PricingNote(label: String, note: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp, color = NeerlyColors.Ink600)
        Text(note, fontSize = 14.sp, color = NeerlyColors.Ink600)
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
