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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.selected
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
    onOpenVendor: (String) -> Unit = {},
    vm: VendorDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val online by vm.isOnline.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::refresh)
    var pendingSwitch by remember { mutableStateOf<ProductSheetSelection?>(null) }
    var sheetProduct by remember { mutableStateOf<ProductResponse?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { sheetProduct = null }
    }

    val cartNow by vm.cartState.collectAsState()
    val reviews by vm.reviews.collectAsState()
    val soldOut by vm.soldOut.collectAsState()
    var tab by remember { mutableStateOf(0) }
    val cfg = com.neerly.mobile.data.config.LocalAppConfig.current
    Column(Modifier.fillMaxSize().background(NeerlyColors.Canvas)) {
        if (!online) OfflineBanner(onRetry = vm::refresh)
        when {
            state.loading && state.vendor == null -> VendorDetailSkeleton()
            state.error != null -> VendorDetailError(message = state.error!!, onRetry = vm::refresh, onBack = onBack)
            else -> {
                val v = state.vendor
                val name = v?.businessName ?: stringResource(R.string.cust_shop)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    // Board Vendor: light header — chevron, share; initials tile + serif name;
                    // three stats split by hairlines; verified line; left text tabs.
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val shareText = stringResource(R.string.cust_shop2_share_text, name)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            com.neerly.mobile.core.design.BackChevron(onBack, stringResource(R.string.core_back))
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                context.startActivity(android.content.Intent.createChooser(
                                    android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                                        .putExtra(android.content.Intent.EXTRA_TEXT, shareText), null))
                            }, modifier = Modifier.size(48.dp)) {
                                Icon(androidx.compose.material.icons.Icons.Outlined.Share, contentDescription = stringResource(R.string.cust_shop2_share), tint = NeerlyColors.Ink900)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            com.neerly.mobile.core.design.InitialsTile(name.split(' ').take(2).joinToString("") { it.take(1) }.uppercase(), size = 72.dp)
                            Column(Modifier.weight(1f)) {
                                Text(name, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Medium,
                                    fontSize = 28.sp, lineHeight = 32.sp, color = NeerlyColors.Ink900)
                                v?.let { Text(listOfNotNull(it.businessCity.ifBlank { null }, it.businessPincode.ifBlank { null }).joinToString(" · "),
                                    fontSize = 15.sp, color = NeerlyColors.Ink600) }
                            }
                        }
                        if (v != null) Column {
                            HorizontalDivider(color = NeerlyColors.Ink200)
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                val rating = v.avgRating?.setScale(1, java.math.RoundingMode.HALF_UP)?.toPlainString()
                                ShopStat(if (rating != null) "$rating ★" else "—",
                                    v.reviewCount?.let { androidx.compose.ui.res.pluralStringResource(R.plurals.cust_shop2_reviews, it, it) }
                                        ?: androidx.compose.ui.res.pluralStringResource(R.plurals.cust_orders_count, v.totalOrders.toInt(), v.totalOrders.toInt()),
                                    Modifier.weight(1f))
                                VerticalDivider(color = NeerlyColors.Ink200)
                                ShopStat(if (v.etaMinutesMin != null && v.etaMinutesMax != null) "${v.etaMinutesMin}–${v.etaMinutesMax}" else "—",
                                    stringResource(R.string.cust_shop2_min_to_you), Modifier.weight(1f).padding(start = 14.dp))
                                VerticalDivider(color = NeerlyColors.Ink200)
                                ShopStat(if (cfg.minOrderValue.signum() > 0) cfg.minOrderValue.asRupees() else stringResource(R.string.cust_shop2_no_min),
                                    stringResource(R.string.cust_shop2_min_order), Modifier.weight(1f).padding(start = 14.dp))
                            }
                            HorizontalDivider(color = NeerlyColors.Ink200)
                        }
                        if (v?.fssaiNumber != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Verified, contentDescription = null, tint = NeerlyColors.Ok, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.cust_shop2_verified), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ok)
                            Text("· " + stringResource(R.string.cust_shop_fssai) + " " + v.fssaiNumber, fontSize = 14.sp, color = NeerlyColors.Ink600)
                        }
                        if (state.closed && v != null) ClosedPanel(v, state, vm::pingMeWhenOpen)
                        // Outside today's hours: say so (Home already does), but a slot can still be booked.
                        else if (v != null && v.openState == "AFTER_HOURS") {
                            val back = v.backOn?.let { iso -> runCatching { java.time.Instant.parse(iso).atZone(java.time.ZoneId.of("Asia/Kolkata"))
                                .format(java.time.format.DateTimeFormatter.ofPattern("EEE h:mm a", com.neerly.mobile.core.util.AppLocale.locale)) }.getOrNull() }
                            com.neerly.mobile.core.design.Notice(
                                if (back != null) stringResource(R.string.cust_shop2_after_hours, back) else stringResource(R.string.cust_shop2_after_hours_plain),
                                com.neerly.mobile.core.design.Tone.Warn)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            listOf(stringResource(R.string.cust_shop_tab_water), stringResource(R.string.cust_shop_tab_reviews), stringResource(R.string.cust_shop_tab_about)).forEachIndexed { i, label ->
                                Column(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Tab) { tab = i; if (i == 1) vm.loadReviews() }
                                    .semantics { selected = tab == i }.heightIn(min = 48.dp), verticalArrangement = Arrangement.Bottom) {
                                    Text(label, fontSize = 16.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                                        color = if (tab == i) NeerlyColors.Ink900 else NeerlyColors.Ink600, modifier = Modifier.padding(bottom = 10.dp))
                                    Box(Modifier.height(3.dp).width(if (tab == i) 44.dp else 0.dp).background(com.neerly.mobile.core.design.Boondh.Amber))
                                }
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = NeerlyColors.Ink200)
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (tab) {
                            0 -> if (state.products.isEmpty()) {
                                com.neerly.mobile.core.design.LoadState(false, null, vm::refresh, stringResource(R.string.cust_shop_nothing_listed),
                                    stringResource(R.string.cust_shop_nothing_listed_body))
                            } else state.products.forEachIndexed { i, p ->
                                val soldOut = p.status != "AVAILABLE"
                                val inCart = vm.quantityInCart(p.id)
                                // Board Vendor rows: name, what it is, price; Add or the outlined stepper.
                                // Closed shop: prices readable, nothing added. Sold out: muted + other options.
                                com.neerly.mobile.core.design.ListRow(divider = i < state.products.lastIndex, onClick = when {
                                    state.closed -> null
                                    soldOut -> ({ vm.openSoldOut(p.id) })
                                    else -> ({ sheetProduct = p })
                                }) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(listOfNotNull(p.brand, p.name).joinToString(" · "), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                            color = if (soldOut) NeerlyColors.Ink600 else NeerlyColors.Ink900)
                                        Text(when {
                                            soldOut -> stringResource(R.string.cust_shop_sold_out)
                                            !p.description.isNullOrBlank() -> p.description
                                            p.allowTransferAndReturn -> stringResource(R.string.cust_shop_swap)
                                            p.depositAmount != null -> stringResource(R.string.cust_shop_deposit, p.depositAmount.asRupees())
                                            else -> ""
                                        }, fontSize = 14.sp, color = if (soldOut) NeerlyColors.Err else NeerlyColors.Ink600,
                                            fontWeight = if (soldOut) FontWeight.SemiBold else FontWeight.Normal)
                                        if (!soldOut) Text(p.price.asRupees(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                                    }
                                    when {
                                        state.closed -> Unit
                                        soldOut -> Text(stringResource(R.string.cust_shop2_other_options), color = NeerlyColors.CustomerPrimary,
                                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        inCart > 0 -> Row(Modifier.border(1.5.dp, NeerlyColors.CustomerPrimary, RoundedCornerShape(12.dp)),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            StepButton("−", stringResource(R.string.cust_cart2_less, p.name)) { vm.stepQuantity(p.id, inCart - 1) }
                                            Text("$inCart", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary,
                                                modifier = Modifier.widthIn(min = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            StepButton("+", stringResource(R.string.cust_cart2_more, p.name)) { vm.stepQuantity(p.id, inCart + 1) }
                                        }
                                        else -> OutlinedButton(onClick = { sheetProduct = p }, shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeerlyColors.Ink200),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = NeerlyColors.Paper),
                                            modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 96.dp)) {
                                            Text(stringResource(R.string.cust_add), color = NeerlyColors.CustomerPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        if (tab == 0 && state.closed && state.nearbyOpen.isNotEmpty()) {
                            com.neerly.mobile.core.design.SectionLabel(stringResource(R.string.cust_shop_open_nearby))
                            state.nearbyOpen.forEach { n ->
                                com.neerly.mobile.core.design.BoondhCard(onClick = { onOpenVendor(n.id) }) {
                                    val eta = if (n.etaMinutesMin != null && n.etaMinutesMax != null) stringResource(R.string.cust_home_eta, n.etaMinutesMin, n.etaMinutesMax) else null
                                    com.neerly.mobile.core.design.InfoRow(n.businessName,
                                        when {
                                            eta != null && n.jarFromPrice != null -> stringResource(R.string.cust_shop_eta_from, eta, n.jarFromPrice.asRupees())
                                            else -> eta ?: n.jarFromPrice?.asRupees()
                                        })
                                }
                            }
                        }
                    }
                }
                if (!cartNow.isEmpty && cartNow.vendorId == vendorId) {
                    // Board Vendor cart bar: what's in it on the left, View cart on the right.
                    HorizontalDivider(color = NeerlyColors.Ink200)
                    Row(Modifier.fillMaxWidth().background(NeerlyColors.Canvas).padding(horizontal = 20.dp, vertical = 12.dp)
                        .background(NeerlyColors.CustomerPrimary, RoundedCornerShape(16.dp))
                        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onViewCart)
                        .padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(androidx.compose.ui.res.pluralStringResource(R.plurals.cust_shop2_items, cartNow.totalQuantity, cartNow.totalQuantity),
                                fontSize = 13.sp, color = com.neerly.mobile.core.design.Boondh.MutedOnNavy)
                            Text(cartNow.subtotal.asRupees(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        }
                        Text(stringResource(R.string.cust_shop2_view_cart), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
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
                onClose = { closeSheet() },
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

    soldOut?.let { alt ->
        ModalBottomSheet(onDismissRequest = vm::closeSoldOut, containerColor = NeerlyColors.Paper,
            shape = RoundedCornerShape(topStart = NeerlyRadius.xl, topEnd = NeerlyRadius.xl)) {
            SoldOutSheet(alt,
                onOpenShop = { id -> vm.closeSoldOut(); onOpenVendor(id) },
                onAddHere = { id -> vm.closeSoldOut(); state.products.firstOrNull { it.id == id }?.let { sheetProduct = it } })
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

/** Board VendorClosed: why it's shut, when it's back, and "ping me when they open". */
@Composable
private fun ClosedPanel(v: com.neerly.mobile.data.dto.VendorCardResponse, state: VendorDetailUiState, onPing: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(NeerlyColors.WarnSoft).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        com.neerly.mobile.core.design.Pill(when {
            v.openState == "PAUSED" -> stringResource(R.string.cust_shop_paused)
            v.closedLabel != null -> stringResource(R.string.cust_shop_closed_for, v.closedLabel)
            else -> stringResource(R.string.cust_shop_closed)
        }, com.neerly.mobile.core.design.Tone.Warn)
        v.backOn?.let { iso ->
            val back = runCatching {
                java.time.Instant.parse(iso).atZone(java.time.ZoneId.of("Asia/Kolkata"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM, h a", com.neerly.mobile.core.util.AppLocale.locale))
            }.getOrNull()
            back?.let { Text(stringResource(R.string.cust_shop_back_on, it), fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 22.sp, color = NeerlyColors.Ink900) }
        }
        Text(stringResource(R.string.cust_shop_closed_body), fontSize = 14.sp, lineHeight = 20.sp, color = NeerlyColors.Ink700)
        if (state.pingOn) com.neerly.mobile.core.design.Notice(stringResource(R.string.cust_shop_ping_on), com.neerly.mobile.core.design.Tone.Ok)
        else com.neerly.mobile.core.design.SecondaryButton(stringResource(R.string.cust_shop_ping_me), onPing, enabled = !state.pinging)
        state.message?.let { com.neerly.mobile.core.design.Notice(it, com.neerly.mobile.core.design.Tone.Bad) }
    }
}

/** Board OutOfStock: when it's back, and other ways to get one sooner. */
@Composable
private fun SoldOutSheet(
    alt: com.neerly.mobile.data.dto.ProductAlternativesResponse,
    onOpenShop: (String) -> Unit,
    onAddHere: (String) -> Unit
) {
    val zone = java.time.ZoneId.of("Asia/Kolkata")
    val loc = com.neerly.mobile.core.util.AppLocale.locale
    val since = alt.soldOutSince?.let { iso -> runCatching {
        val at = java.time.Instant.parse(iso).atZone(zone)
        if (at.toLocalDate() == java.time.LocalDate.now(zone)) at.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", loc))
        else com.neerly.mobile.core.design.shortDay(iso)
    }.getOrNull() }
    // Board OutOfStock: one grey pill "SOLD OUT SINCE 9:18 AM · BACK TOMORROW", serif name,
    // then "Want one sooner?" as hairline rows with an outlined Add.
    val pill = listOfNotNull(
        since?.let { stringResource(R.string.cust_oos_since, it) } ?: stringResource(R.string.cust_oos_sold_out),
        alt.backOn?.let { stringResource(R.string.cust_oos_back, backDay(it)) }
    ).joinToString(" · ")
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(pill.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = NeerlyColors.Ink700,
            modifier = Modifier.background(NeerlyColors.Ink100, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
        Text(alt.name, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 26.sp, lineHeight = 30.sp, color = NeerlyColors.Ink900)
        com.neerly.mobile.core.design.SectionLabel(stringResource(R.string.cust_oos_sooner), Modifier.padding(top = 6.dp))
        if (alt.otherShops.isEmpty() && alt.sameShop.isEmpty()) Text(stringResource(R.string.cust_oos_none), fontSize = 14.sp, color = NeerlyColors.Ink600)
        Column {
            alt.otherShops.forEach { o ->
                AltRow(title = listOfNotNull(o.name, o.vendorName).joinToString(" · "), detail = stringResource(R.string.cust_oos_other_shop), price = o.price,
                    action = stringResource(R.string.cust_oos_open)) { onOpenShop(o.vendorId) }
            }
            alt.sameShop.forEach { o ->
                AltRow(title = o.name, detail = stringResource(R.string.cust_oos_same_shop), price = o.price,
                    action = stringResource(R.string.cust_add)) { onAddHere(o.productId) }
            }
        }
    }
}

/** "tomorrow" / "Wed" for the pill's "back …". */
@Composable
private fun backDay(iso: String): String {
    val zone = java.time.ZoneId.of("Asia/Kolkata")
    val d = runCatching { java.time.LocalDate.parse(iso.take(10)) }.getOrNull() ?: return com.neerly.mobile.core.design.shortDay(iso)
    return if (d == java.time.LocalDate.now(zone).plusDays(1)) stringResource(R.string.cust_oos_tomorrow) else com.neerly.mobile.core.design.shortDay(iso)
}

@Composable
private fun AltRow(title: String, detail: String, price: java.math.BigDecimal, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text(detail, fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
        Text(price.asRupees(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        Spacer(Modifier.width(10.dp))
        OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 44.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ink300),
            contentPadding = PaddingValues(horizontal = 14.dp)) {
            Text(action, color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(color = NeerlyColors.Ink200)
}

@Composable
private fun ShopStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 14.dp)) {
        Text(value, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = NeerlyColors.Ink900)
        Text(label, fontSize = 14.sp, color = NeerlyColors.Ink600)
    }
}

@Composable
private fun StepButton(label: String, description: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.CustomerPrimary)
    }
}
