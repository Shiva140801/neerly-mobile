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
    val methods = listOf(
        Triple("UPI", stringResource(R.string.cust_co_upi), stringResource(R.string.cust_co_upi_sub)),
        Triple("CARD", stringResource(R.string.cust_co_card), stringResource(R.string.cust_co_card_sub)),
        Triple("WALLET", stringResource(R.string.cust_co_wallet), walletSub),
        Triple("COD", stringResource(R.string.cust_co_cash), stringResource(R.string.cust_co_cash_sub))
    )
    val yourShop = stringResource(R.string.cust_your_shop)
    val swapTip = stringResource(R.string.cust_co_swap_tip)
    val deliverTo = stringResource(R.string.cust_co_deliver_to)
    val billLabel = stringResource(R.string.cust_co_bill)
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
    // Board 13 — how would you like to pay?
    com.neerly.mobile.core.design.BoondhPage(
        title = stringResource(R.string.cust_co_title), onBack = onBack, backLabel = stringResource(R.string.cust_co_back_cart),
        subtitle = total?.let { stringResource(R.string.cust_co_paying_shop, snapshot.vendorName ?: yourShop, it.asRupees()) },
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
            if (state.paymentMethod in setOf("UPI", "CARD")) Text(stringResource(R.string.cust_co_secured), fontSize = 12.sp, color = NeerlyColors.Ink600,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    ) {
        if (state.loading && state.addresses.isEmpty()) { item { CartSkeleton() }; return@BoondhPage }
        methods.forEach { (code, title, sub) ->
            item {
                val on = state.paymentMethod == code
                com.neerly.mobile.core.design.BoondhCard(
                    Modifier.border(if (on) 2.dp else 0.dp, if (on) NeerlyColors.CustomerPrimary else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(14.dp)),
                    onClick = { vm.selectPaymentMethod(code) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = on, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = NeerlyColors.CustomerPrimary))
                        Spacer(Modifier.width(8.dp))
                        Column { Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                            Text(sub, fontSize = 13.sp, color = if (code == "WALLET" && walletShort) NeerlyColors.Warn else NeerlyColors.Ink600) }
                    }
                }
            }
        }
        item { com.neerly.mobile.core.design.SectionLabel(deliverTo) }
        if (state.addresses.isEmpty() && !state.loading) {
            item { com.neerly.mobile.core.design.Notice(addAddressNotice, com.neerly.mobile.core.design.Tone.Warn) }
            item { com.neerly.mobile.core.design.SecondaryButton(addAddress, onAddAddress) }
        } else {
            items(state.addresses.size) { idx ->
                val a = state.addresses[idx]
                AddressChip(addr = a, selected = state.selectedAddress?.id == a.id, onClick = { vm.selectAddress(a.id) })
            }
        }
        if (state.paymentMethod != "WALLET") item {
            Text(swapTip,
                fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
        }
        item { com.neerly.mobile.core.design.SectionLabel(billLabel) }
        item { BillCard(state.quote, state.quoting, onRetry = vm::refreshQuote) }
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
