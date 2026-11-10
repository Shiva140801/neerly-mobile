package com.neerly.mobile.feature.checkout

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
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
    onAddAddress: () -> Unit = {},
    vm: CheckoutViewModel = hiltViewModel()
) {
    // Back from adding an address: pick it up and price the order.
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME && vm.state.value.addresses.isEmpty() && !vm.state.value.loading) vm.loadAddresses()
        }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snapshot by cart.state.collectAsState()

    val total = state.quote?.totalAmount
    val walletShort = total != null && state.walletBalance != null && state.walletBalance!! < total
    val cta = when {
        state.placing -> stringResource(R.string.cust_co_placing)
        state.paying -> stringResource(R.string.cust_co_paying)
        state.awaitingPaymentFor != null -> stringResource(R.string.cust_co_retry_payment)
        state.addresses.isEmpty() && !state.loading -> stringResource(R.string.cust_co_add_address)
        total == null -> stringResource(R.string.cust_co_working_total)
        state.paymentMethod == "COD" -> stringResource(R.string.cust_co_cta_cod, total.asRupees())
        state.paymentMethod == "WALLET" -> stringResource(R.string.cust_co_cta_wallet, total.asRupees())
        state.paymentMethod == "CARD" -> stringResource(R.string.cust_co_cta_card, total.asRupees())
        else -> stringResource(R.string.cust_co_cta_upi, total.asRupees())
    }
    val walletSub = state.walletBalance?.let { b ->
        if (total != null && b < total) stringResource(R.string.cust_co_wallet_short, b.asRupees(), (total - b).asRupees())
        else stringResource(R.string.cust_co_wallet_available, b.asRupees()) } ?: stringResource(R.string.cust_co_wallet_sub)
    // Board Checkout order: UPI, wallet, card, cash — each with its own glyph tile.
    val methods = listOf(
        Triple("UPI", stringResource(R.string.cust_co_upi), stringResource(R.string.cust_co_upi_sub)),
        Triple("WALLET", stringResource(R.string.cust_co_wallet), walletSub),
        Triple("CARD", stringResource(R.string.cust_co_card), stringResource(R.string.cust_co_card_sub)),
        Triple("COD", stringResource(R.string.cust_co_cash), stringResource(R.string.cust_co_cash_sub))
    )
    val yourShop = stringResource(R.string.cust_your_shop)
    val swapTip = stringResource(R.string.cust_co_swap_tip)
    val addAddressNotice = stringResource(R.string.cust_co_add_address_notice)
    val addAddress = stringResource(R.string.cust_add_address)
    // Boards PaymentPending / PaymentFailed take over the page until the payment settles.
    when (val page = state.paymentPage) {
        is PaymentPage.Pending -> {
            PaymentPendingPage(page, snapshot.vendorName ?: yourShop, state.paying,
                onApproved = vm::checkPaymentNow, onPayAnotherWay = vm::closePaymentPage,
                onCancel = { vm.cancelUnpaid(onBack) })
            return
        }
        is PaymentPage.Failed -> {
            PaymentFailedPage(page, state.paying, state.paymentProblem,
                onOtherUpi = { context.findActivity()?.let { vm.retryWith(it, "UPI") } },
                onCard = { context.findActivity()?.let { vm.retryWith(it, "CARD") } },
                onCash = vm::payCashInstead, onBack = vm::closePaymentPage)
            return
        }
        null -> Unit
    }
    // Board Checkout: chevron + title, navy "Paying <shop> ₹X", the four ways to pay with
    // the radio on the right, the jar-swap note. Address and bill now live on the cart.
    com.neerly.mobile.core.design.BoondhPage(
        title = stringResource(R.string.cust_co_title), onBack = onBack, backLabel = stringResource(R.string.cust_co_back_cart),
        inlineTitle = true,
        footer = {
            state.error?.let { com.neerly.mobile.core.design.Notice(it, com.neerly.mobile.core.design.Tone.Bad) }
            state.paymentProblem?.let { com.neerly.mobile.core.design.Notice(it, com.neerly.mobile.core.design.Tone.Bad) }
            if (state.addresses.isEmpty() && !state.loading) com.neerly.mobile.core.design.PrimaryButton(cta, onAddAddress)
            else com.neerly.mobile.core.design.PrimaryButton(cta, {
                val activity = context.findActivity() ?: return@PrimaryButton
                vm.placeAndPay(activity) { order -> onOrderPlaced(order) }
            }, enabled = !state.placing && !state.paying && total != null && state.selectedAddress != null &&
                !snapshot.isEmpty && !(state.paymentMethod == "WALLET" && walletShort))
            // Razorpay only handles UPI/card; on cash or wallet the line would be untrue.
            if (state.paymentMethod in setOf("UPI", "CARD")) Text(stringResource(R.string.cust_co_secured), fontSize = 13.sp, color = NeerlyColors.Ink600,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    ) {
        if (state.loading && state.addresses.isEmpty()) { item { CartSkeleton() }; return@BoondhPage }
        item {
            Row(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(16.dp)).padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cust_co_paying_to, snapshot.vendorName ?: yourShop), Modifier.weight(1f),
                    fontSize = 15.sp, color = com.neerly.mobile.core.design.Boondh.MutedOnNavy)
                if (total != null) Text(total.asRupees(), fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontSize = 30.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                else if (state.quoting) Text(stringResource(R.string.cust_co_working_total), fontSize = 13.sp,
                    color = com.neerly.mobile.core.design.Boondh.MutedOnNavy)
            }
        }
        state.selectedAddress?.let { a -> item {
            Text(stringResource(R.string.cust_co_to_address, com.neerly.mobile.feature.address.addressLabel(a.label),
                listOf(a.flatNumber, a.buildingName).filter { it.isNotBlank() }.joinToString(", ").ifBlank { a.street }),
                fontSize = 14.sp, color = NeerlyColors.Ink600)
        } }
        if (state.quote == null && !state.quoting && !state.loading && state.selectedAddress != null) item {
            BillCard(null, false, onRetry = vm::refreshQuote)
        }
        methods.forEach { (code, title, sub) ->
            item {
                val on = state.paymentMethod == code
                Row(Modifier.fillMaxWidth()
                    .background(NeerlyColors.Paper, RoundedCornerShape(16.dp))
                    .border(if (on) 2.dp else 1.dp, if (on) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200, RoundedCornerShape(16.dp))
                    .clickable(role = androidx.compose.ui.semantics.Role.RadioButton) { vm.selectPaymentMethod(code) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).background(NeerlyColors.Ink100, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(when (code) {
                            "UPI" -> androidx.compose.material.icons.Icons.Outlined.PhoneAndroid
                            "WALLET" -> androidx.compose.material.icons.Icons.Outlined.AccountBalanceWallet
                            "CARD" -> androidx.compose.material.icons.Icons.Outlined.CreditCard
                            else -> androidx.compose.material.icons.Icons.Outlined.Payments
                        }, contentDescription = null, tint = NeerlyColors.Ink900)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(sub, fontSize = 14.sp, color = if (code == "WALLET" && walletShort) NeerlyColors.Warn else NeerlyColors.Ink600)
                    }
                    RadioButton(selected = on, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = NeerlyColors.CustomerPrimary))
                }
            }
        }
        if (state.addresses.isEmpty() && !state.loading) {
            item { com.neerly.mobile.core.design.Notice(addAddressNotice, com.neerly.mobile.core.design.Tone.Warn) }
            item { com.neerly.mobile.core.design.SecondaryButton(addAddress, onAddAddress) }
        }
        if (state.paymentMethod != "WALLET" && snapshot.items.any { !it.keepContainer }) item {
            com.neerly.mobile.core.design.Notice(swapTip, com.neerly.mobile.core.design.Tone.Warn)
        }
    }
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
                Text(stringResource(R.string.cust_co_addr_line, addr.flatNumber, addr.street, addr.pincode),
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
                    Text(stringResource(R.string.cust_co_working_total), fontSize = 13.sp, color = NeerlyColors.Ink500)
                quote == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cust_co_price_failed), fontSize = 13.sp, color = NeerlyColors.Err,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.cust_retry), color = NeerlyColors.CustomerPrimary) }
                }
                else -> {
                    BillLine(stringResource(R.string.cust_water), quote.subtotal.asRupees())
                    BillLine(
                        stringResource(R.string.cust_delivery),
                        if (quote.deliveryFee.signum() == 0) stringResource(R.string.cust_free) else quote.deliveryFee.asRupees()
                    )
                    if (quote.surgeSurcharge.signum() > 0) BillLine(stringResource(R.string.cust_surge), quote.surgeSurcharge.asRupees())
                    if (quote.taxes.signum() > 0) BillLine(stringResource(R.string.cust_taxes), quote.taxes.asRupees())
                    if (quote.discount.signum() > 0) BillLine(stringResource(R.string.cust_promo), stringResource(R.string.cust_minus_amount, quote.discount.asRupees()), NeerlyColors.Ok)
                    quote.promoMessage?.let { Text(it, fontSize = 12.sp, color = NeerlyColors.Warn) }
                    if (quote.depositTotal.signum() > 0) {
                        BillLine(stringResource(R.string.cust_ps_refundable_deposit), quote.depositTotal.asRupees())
                        Text(
                            stringResource(R.string.cust_co_deposit_back),
                            fontSize = 11.sp, color = NeerlyColors.Ink500
                        )
                    }
                    HorizontalDivider(color = NeerlyColors.Ink100)
                    BillLine(stringResource(R.string.cust_total), quote.totalAmount.asRupees(), bold = true)
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
