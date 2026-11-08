package com.neerly.mobile.feature.customer

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.core.design.OfflineBanner
import com.neerly.mobile.core.design.VendorDetailSkeleton
import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.data.cart.AddOutcome
import com.neerly.mobile.data.dto.ProductResponse
import kotlinx.coroutines.launch

/**
 * Vendor detail + product catalogue.
 *
 * Tapping a product no longer adds it silently — it opens
 * `S-CUST-VEN-PROD-01`, where the customer chooses keep-container vs
 * transfer-and-return and sees what that choice costs today. The
 * "Switch vendor?" dialog still guards a cart that already belongs to someone
 * else.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDetailScreen(
    vendorId: String,
    onBack: () -> Unit,
    onAddToCart: (productId: String) -> Unit,
    onViewCart: () -> Unit = {},
    vm: VendorDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val online by vm.isOnline.collectAsState()
    var pendingSwitch by remember { mutableStateOf<ProductSheetSelection?>(null) }
    var sheetProduct by remember { mutableStateOf<ProductResponse?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { sheetProduct = null }
    }

    val cartNow by vm.cartState.collectAsState()
    val reviews by vm.reviews.collectAsState()
    var tab by remember { mutableStateOf(0) }
    val cfg = com.neerly.mobile.data.config.LocalAppConfig.current
    Column(Modifier.fillMaxSize().background(NeerlyColors.Canvas)) {
        if (!online) OfflineBanner(onRetry = vm::refresh)
        when {
            state.loading -> VendorDetailSkeleton()
            state.error != null -> VendorDetailError(message = state.error!!, onRetry = vm::refresh, onBack = onBack)
            else -> {
                val v = state.vendor
                val name = v?.businessName ?: stringResource(R.string.cust_shop)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    // Board 10 header
                    Column(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.cust_back_arrow), color = NeerlyColors.Paper, fontSize = 15.sp) }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(52.dp).background(com.neerly.mobile.core.design.Boondh.Amber, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Text(name.split(' ').take(2).joinToString("") { it.take(1) }.uppercase(), fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, fontSize = 18.sp)
                            }
                            Text(name, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 26.sp, color = NeerlyColors.Paper)
                        }
                        v?.let { Text(listOfNotNull(it.avgRating?.let { r -> stringResource(R.string.cust_rating_stars, r.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()) },
                            androidx.compose.ui.res.pluralStringResource(R.plurals.cust_orders_count, it.totalOrders.toInt(), it.totalOrders.toInt()), it.businessCity).joinToString(" · "), fontSize = 14.sp, color = com.neerly.mobile.core.design.Boondh.MutedOnNavy) }
                        v?.fssaiNumber?.let { Text(stringResource(R.string.cust_shop_verified_fssai, it), fontSize = 13.sp, color = com.neerly.mobile.core.design.Boondh.MutedOnNavy) }
                    }
                    TabRow(selectedTabIndex = tab, containerColor = NeerlyColors.Paper, contentColor = NeerlyColors.CustomerPrimary) {
                        listOf(stringResource(R.string.cust_shop_tab_water), stringResource(R.string.cust_shop_tab_reviews), stringResource(R.string.cust_shop_tab_about)).forEachIndexed { i, label ->
                            Tab(selected = tab == i, onClick = { tab = i; if (i == 1) vm.loadReviews() },
                                text = { Text(label, fontWeight = FontWeight.SemiBold) }, modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (tab) {
                            0 -> if (state.products.isEmpty()) {
                                com.neerly.mobile.core.design.LoadState(false, null, vm::refresh, stringResource(R.string.cust_shop_nothing_listed),
                                    stringResource(R.string.cust_shop_nothing_listed_body))
                            } else state.products.forEach { p ->
                                val soldOut = p.status != "AVAILABLE"
                                com.neerly.mobile.core.design.BoondhCard(onClick = if (soldOut) null else ({ sheetProduct = p })) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(listOfNotNull(p.brand, p.name).joinToString(" "), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                                color = if (soldOut) NeerlyColors.Ink600 else NeerlyColors.Ink900)
                                            Text(when {
                                                soldOut -> stringResource(R.string.cust_shop_sold_out)
                                                p.allowTransferAndReturn -> stringResource(R.string.cust_shop_swap)
                                                p.depositAmount != null -> stringResource(R.string.cust_shop_deposit, p.depositAmount.asRupees())
                                                else -> p.description ?: ""
                                            }, fontSize = 13.sp, color = if (soldOut) NeerlyColors.Warn else NeerlyColors.Ink600)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(p.price.asRupees(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                                            val inCart = vm.quantityInCart(p.id)
                                            if (!soldOut) TextButton(onClick = { sheetProduct = p }, modifier = Modifier.heightIn(min = 44.dp)) {
                                                Text(if (inCart > 0) stringResource(R.string.cust_shop_in_cart, inCart) else stringResource(R.string.cust_add), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                            1 -> when {
                                reviews == null -> com.neerly.mobile.core.design.LoadState(true, null, {})
                                reviews!!.isEmpty() -> com.neerly.mobile.core.design.LoadState(false, null, {}, stringResource(R.string.cust_shop_no_reviews), stringResource(R.string.cust_shop_no_reviews_body))
                                else -> reviews!!.forEach { r ->
                                    val starsCd = stringResource(R.string.cust_stars_cd, r.rating)
                                    com.neerly.mobile.core.design.BoondhCard {
                                        Text("★".repeat(r.rating) + "☆".repeat(5 - r.rating), color = com.neerly.mobile.core.design.Boondh.Amber, fontSize = 16.sp,
                                            modifier = Modifier.semantics { contentDescription = starsCd })
                                        r.text?.let { Text(it, fontSize = 15.sp, lineHeight = 21.sp, color = NeerlyColors.Ink900) }
                                        Text(com.neerly.mobile.core.design.shortDay(r.createdAt), fontSize = 12.sp, color = NeerlyColors.Ink600)
                                        r.vendorResponse?.let { Text(stringResource(R.string.cust_shop_replied, name, it.text), fontSize = 14.sp, color = NeerlyColors.Ink700) }
                                    }
                                }
                            }
                            else -> com.neerly.mobile.core.design.BoondhCard {
                                com.neerly.mobile.core.design.InfoRow(stringResource(R.string.cust_shop_where), "${v?.businessCity ?: ""} ${v?.businessPincode ?: ""}".trim())
                                v?.fssaiNumber?.let { com.neerly.mobile.core.design.InfoRow(stringResource(R.string.cust_shop_fssai), it) }
                                com.neerly.mobile.core.design.InfoRow(stringResource(R.string.cust_shop_delivery), stringResource(R.string.cust_shop_delivery_line, cfg.deliveryFeeNear.asRupees(), cfg.freeDeliveryAbove.asRupees()))
                                com.neerly.mobile.core.design.InfoRow(stringResource(R.string.cust_shop_jars), stringResource(R.string.cust_shop_jars_line))
                            }
                        }
                    }
                }
                if (!cartNow.isEmpty && cartNow.vendorId == vendorId) {
                    Column(Modifier.fillMaxWidth().background(NeerlyColors.Paper).padding(horizontal = 20.dp, vertical = 12.dp)) {
                        com.neerly.mobile.core.design.PrimaryButton(stringResource(R.string.cust_shop_view_cart, cartNow.totalQuantity, cartNow.subtotal.asRupees()), onViewCart)
                    }
                }
            }
        }
    }

    sheetProduct?.let { product ->
        ModalBottomSheet(
            onDismissRequest = { sheetProduct = null },
            sheetState = sheetState,
            containerColor = NeerlyColors.Paper,
            shape = RoundedCornerShape(topStart = NeerlyRadius.xl, topEnd = NeerlyRadius.xl)
        ) {
            ProductSheetContent(
                product = product,
                vendorName = state.vendor?.businessName,
                initialQuantity = vm.quantityInCart(product.id),
                alreadyInCart = vm.quantityInCart(product.id) > 0,
                onConfirm = { selection ->
                    when (vm.setCartLine(selection)) {
                        is AddOutcome.Added -> {
                            closeSheet()
                            onAddToCart(selection.product.id)
                        }
                        is AddOutcome.VendorMismatch -> {
                            pendingSwitch = selection
                            closeSheet()
                        }
                    }
                }
            )
        }
    }

    pendingSwitch?.let { selection ->
        AlertDialog(
            onDismissRequest = { pendingSwitch = null },
            title = { Text(stringResource(R.string.cust_shop_switch_title)) },
            text = {
                Text(
                    stringResource(R.string.cust_shop_switch_body, state.vendor?.businessName ?: stringResource(R.string.cust_this_shop))
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.confirmReplaceWithNewVendor(selection)
                    pendingSwitch = null
                    onAddToCart(selection.product.id)
                }) {
                    Text(stringResource(R.string.cust_replace), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwitch = null }) { Text(stringResource(R.string.cust_cancel)) }
            }
        )
    }
}

@Composable
private fun VendorDetailError(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(NeerlySpacing.x6),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.cust_shop_load_failed_title),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(message, fontSize = 13.sp, color = NeerlyColors.Ink500)
        Spacer(Modifier.height(NeerlySpacing.x5))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) { Text(stringResource(R.string.cust_try_again)) }
        TextButton(onClick = onBack) { Text(stringResource(R.string.cust_go_back), color = NeerlyColors.Ink500) }
    }
}
