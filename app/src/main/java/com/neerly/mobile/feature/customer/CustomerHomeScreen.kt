package com.neerly.mobile.feature.customer

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
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
import com.neerly.mobile.core.design.CustomerBottomBar
import com.neerly.mobile.core.design.CustomerTab
import com.neerly.mobile.core.design.HomeSkeleton
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.core.design.OfflineBanner
import com.neerly.mobile.core.design.OfflineScreen
import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.VendorCardResponse

/**
 * Customer home — live data from `GET /customer/vendors?pincode=…`.
 *
 * Layout (top → bottom):
 *   header  : greeting + primary address pill + wallet chip + notification bell slot
 *   errors  : error banner with Retry if the initial load failed
 *   active  : horizontal strip of in-flight orders (empty section suppressed)
 *   vendors : vertical list of vendor cards
 */
@Composable
fun CustomerHomeScreen(
    onVendorClick: (String) -> Unit,
    onOrderClick: (String) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onSelectTab: (CustomerTab) -> Unit = {},
    onOpenCart: () -> Unit = {},
    onSubscription: (String) -> Unit = {},
    onAddAddress: () -> Unit = {},
    vm: CustomerHomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val online by vm.isOnline.collectAsState()
    // Back from adding the first address: reload so shops for that pincode show up.
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME && !vm.state.value.loading && vm.state.value.primaryAddress == null) vm.refresh()
        }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Cold start with nothing to show and no connection: the full-screen
    // offline state, not a spinner that will never resolve.
    if (!online && state.vendors.isEmpty() && !state.loading) {
        Scaffold(
            containerColor = NeerlyColors.Canvas,
            bottomBar = { CustomerBottomBar(current = CustomerTab.Home, onSelect = onSelectTab) }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                OfflineScreen(onRetry = vm::refresh)
            }
        }
        return
    }

    val hour = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Kolkata")).hour
    val first = state.name?.substringBefore(' ')
    val greeting = when {
        hour < 12 -> if (first != null) stringResource(R.string.cust_home_morning_name, first) else stringResource(R.string.cust_home_morning)
        hour < 17 -> if (first != null) stringResource(R.string.cust_home_afternoon_name, first) else stringResource(R.string.cust_home_afternoon)
        else -> if (first != null) stringResource(R.string.cust_home_evening_name, first) else stringResource(R.string.cust_home_evening)
    }
    Column(Modifier.fillMaxSize().background(NeerlyColors.Canvas)) {
        if (!online) OfflineBanner(onRetry = vm::refresh)
        com.neerly.mobile.core.design.BoondhPage(
            title = greeting,
            subtitle = state.primaryAddress?.let { stringResource(R.string.cust_home_delivering_to, com.neerly.mobile.feature.address.addressLabel(it.label), it.street) } ?: stringResource(R.string.cust_home_set_address),
            bottomBar = { CustomerBottomBar(current = CustomerTab.Home, onSelect = onSelectTab) }
        ) {
            if (state.loading) { item { HomeSkeleton() }; return@BoondhPage }
            if (state.error != null) { item { com.neerly.mobile.core.design.LoadState(false, state.error, vm::refresh) }; return@BoondhPage }
            if (state.primaryAddress == null) item {
                com.neerly.mobile.core.design.BoondhCard {
                    Text(stringResource(R.string.cust_home_where_deliver), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text(stringResource(R.string.cust_home_add_address_body), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    com.neerly.mobile.core.design.PrimaryButton(stringResource(R.string.cust_add_address), onAddAddress)
                }
            }
            state.activeOrders.forEach { o -> item {
                com.neerly.mobile.core.design.BoondhCard(onClick = { onOrderClick(o.id) }) {
                    com.neerly.mobile.core.design.Pill(when (o.status) {
                        "DISPATCHED", "ARRIVING" -> stringResource(R.string.cust_home_on_the_way); "VENDOR_ACCEPTED", "PREPARING" -> stringResource(R.string.cust_home_being_packed); else -> stringResource(R.string.cust_home_finding_shop)
                    }, com.neerly.mobile.core.design.Tone.Info)
                    com.neerly.mobile.core.design.InfoRow(o.vendorName ?: o.orderNumber, stringResource(R.string.cust_home_tap_track, o.totalAmount.asRupees()))
                }
            } }
            state.usual?.let { u -> item {
                Column(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(16.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cust_home_usual), fontSize = 13.sp, color = com.neerly.mobile.core.design.Boondh.MutedOnNavy)
                    Text(u.items.map { stringResource(R.string.cust_qty_item, it.quantity, it.productName) }.joinToString(", "), fontSize = 20.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, color = NeerlyColors.Paper)
                    Text(stringResource(R.string.cust_home_last_ordered, u.vendorName ?: stringResource(R.string.cust_your_shop), com.neerly.mobile.core.design.shortDay(u.placedAt)),
                        fontSize = 13.sp, color = com.neerly.mobile.core.design.Boondh.MutedOnNavy)
                    com.neerly.mobile.core.design.PrimaryButton(stringResource(R.string.cust_home_order_again, u.subtotal.asRupees()),
                        { if (vm.orderUsual()) onOpenCart() }, color = com.neerly.mobile.core.design.Boondh.Amber, contentColor = NeerlyColors.Ink900)
                }
            } }
            state.nextSubscription?.let { sub -> item {
                com.neerly.mobile.core.design.BoondhCard(onClick = { onSubscription(sub.id) }) {
                    com.neerly.mobile.core.design.InfoRow(stringResource(R.string.cust_home_sub_when, com.neerly.mobile.core.design.shortDay(sub.nextDeliveryDate),
                        com.neerly.mobile.core.design.timeWindow(sub.slotStart, sub.slotEnd)),
                        stringResource(R.string.cust_home_sub_line, stringResource(R.string.cust_qty_item, sub.quantity, sub.productName ?: stringResource(R.string.cust_jar))))
                }
            } }
            item {
                OutlinedButton(onClick = onOpenSearch, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ink200),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = NeerlyColors.Paper)) {
                    Text(stringResource(R.string.cust_home_search), Modifier.weight(1f), color = NeerlyColors.Ink600, fontSize = 15.sp)
                }
            }
            item { com.neerly.mobile.core.design.SectionLabel(androidx.compose.ui.res.pluralStringResource(R.plurals.cust_home_suppliers_open, state.vendors.size, state.vendors.size)) }
            if (state.vendors.isEmpty()) item {
                com.neerly.mobile.core.design.LoadState(false, null, vm::refresh, stringResource(R.string.cust_home_no_suppliers),
                    stringResource(R.string.cust_home_no_suppliers_body))
            }
            items(state.vendors, key = { it.id }) { v ->
                com.neerly.mobile.core.design.BoondhCard(onClick = { onVendorClick(v.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).background(NeerlyColors.CustomerSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(v.businessName.split(' ').take(2).joinToString("") { it.take(1) }.uppercase(),
                                fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(v.businessName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                            Text(listOfNotNull(v.avgRating?.let { stringResource(R.string.cust_rating_stars, it.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()) },
                                androidx.compose.ui.res.pluralStringResource(R.plurals.cust_orders_count, v.totalOrders.toInt(), v.totalOrders.toInt()),
                                if (v.fssaiNumber != null) stringResource(R.string.cust_fssai_verified) else null).joinToString(" · "),
                                fontSize = 13.sp, color = NeerlyColors.Ink600)
                        }
                    }
                }
            }
        }
    }
}
