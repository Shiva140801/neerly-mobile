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
    onChangeAddress: () -> Unit = {},
    onJoinAsShop: () -> Unit = {},
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            com.neerly.mobile.core.design.InfoRow(stringResource(R.string.cust_home_sub_when, com.neerly.mobile.core.design.shortDay(sub.nextDeliveryDate),
                                com.neerly.mobile.core.design.timeWindow(sub.slotStart, sub.slotEnd)),
                                stringResource(R.string.cust_home_sub_line, stringResource(R.string.cust_qty_item, sub.quantity, sub.productName ?: stringResource(R.string.cust_jar))))
                        }
                        // Board Home: skipping tomorrow is one tap, not a trip into the plan.
                        OutlinedButton(onClick = vm::skipNextDelivery, enabled = !state.skipping, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.cust_skip), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } }
            state.message?.let { m -> item { com.neerly.mobile.core.design.Notice(m, com.neerly.mobile.core.design.Tone.Info) } }
            item {
                OutlinedButton(onClick = onOpenSearch, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ink200),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = NeerlyColors.Paper)) {
                    Text(stringResource(R.string.cust_home_search), Modifier.weight(1f), color = NeerlyColors.Ink600, fontSize = 15.sp)
                }
            }
            val open = state.openVendors
            val closed = state.closedVendors
            if (state.vendors.isEmpty()) {
                item { NoSuppliers(state, vm::joinWaitlist, onChangeAddress, onJoinAsShop) }
                return@BoondhPage
            }
            item { com.neerly.mobile.core.design.SectionLabel(androidx.compose.ui.res.pluralStringResource(R.plurals.cust_home_suppliers_open,
                state.vendors.count { it.takingOrders }, state.vendors.count { it.takingOrders })) }
            item { FilterChips(state.filter, vm::toggleFilter) }
            if (open.isEmpty() && closed.isEmpty()) item {
                Text(stringResource(R.string.cust_home_no_match), fontSize = 14.sp, color = NeerlyColors.Ink600)
            }
            items(open, key = { it.id }) { v -> ShopCard(v, onClick = { onVendorClick(v.id) }) }
            if (closed.isNotEmpty()) {
                item { com.neerly.mobile.core.design.SectionLabel(stringResource(R.string.cust_home_closed_section)) }
                items(closed, key = { "closed-" + it.id }) { v -> ShopCard(v, onClick = { onVendorClick(v.id) }) }
            }
        }
    }
}

/** Board Home chips: Open now / Top rated / Chilled / Tanker. Tap again to clear. */
@Composable
private fun FilterChips(selected: HomeFilter?, onToggle: (HomeFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(HomeFilter.entries) { f ->
            FilterChip(
                selected = selected == f,
                onClick = { onToggle(f) },
                label = { Text(when (f) {
                    HomeFilter.OPEN_NOW -> stringResource(R.string.cust_home_chip_open)
                    HomeFilter.TOP_RATED -> stringResource(R.string.cust_home_chip_top)
                    HomeFilter.CHILLED -> stringResource(R.string.cust_home_chip_chilled)
                    HomeFilter.TANKER -> stringResource(R.string.cust_home_chip_tanker)
                }) },
                modifier = Modifier.heightIn(min = 48.dp)
            )
        }
    }
}

/** A shop row (board Home). A closed shop is greyed and says when it's back (boards Home, DarkHome). */
@Composable
private fun ShopCard(v: VendorCardResponse, onClick: () -> Unit) {
    val closedNow = v.closedForNow || !v.takingOrders
    com.neerly.mobile.core.design.BoondhCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).background(if (closedNow) NeerlyColors.Ink100 else NeerlyColors.CustomerSoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Text(v.businessName.split(' ').take(2).joinToString("") { it.take(1) }.uppercase(),
                    fontWeight = FontWeight.Bold, color = if (closedNow) NeerlyColors.Ink600 else NeerlyColors.CustomerPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(v.businessName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (closedNow) NeerlyColors.Ink600 else NeerlyColors.Ink900)
                val rating = v.avgRating?.let { r ->
                    stringResource(R.string.cust_rating_stars, r.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()) +
                        (v.reviewCount?.takeIf { it > 0 }?.let { " ($it)" } ?: "")
                }
                val eta = if (v.etaMinutesMin != null && v.etaMinutesMax != null && v.takingOrders)
                    stringResource(R.string.cust_home_eta, v.etaMinutesMin, v.etaMinutesMax) else null
                Text(listOfNotNull(rating, eta,
                    if (v.reviewCount == null) androidx.compose.ui.res.pluralStringResource(R.plurals.cust_orders_count, v.totalOrders.toInt(), v.totalOrders.toInt()) else null,
                    if (v.fssaiNumber != null) stringResource(R.string.cust_fssai_verified) else null).joinToString(" · "),
                    fontSize = 13.sp, color = NeerlyColors.Ink600)
                if (closedNow) {
                    val back = v.backOn?.let(::backLabel)
                    Text(when {
                        v.closedLabel != null && back != null -> stringResource(R.string.cust_home_closed_label_back, v.closedLabel, back)
                        back != null -> stringResource(R.string.cust_home_closed_back, back)
                        else -> stringResource(R.string.cust_home_closed_now)
                    }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Warn)
                } else v.jarFromPrice?.let {
                    Text(stringResource(R.string.cust_home_jar_from, it.asRupees()), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                }
            }
        }
    }
}

/** Board NoSuppliers: sign up to be told, try another address, or join as a shop. */
@Composable
private fun NoSuppliers(state: HomeUiState, onJoin: () -> Unit, onChangeAddress: () -> Unit, onJoinAsShop: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.cust_home_no_suppliers_area), fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            fontSize = 24.sp, color = NeerlyColors.Ink900)
        Text(stringResource(R.string.cust_home_no_suppliers_area_body), fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink600)
        if (state.onWaitlist) com.neerly.mobile.core.design.Notice(stringResource(R.string.cust_home_waitlist_done), com.neerly.mobile.core.design.Tone.Ok)
        else com.neerly.mobile.core.design.PrimaryButton(stringResource(R.string.cust_home_waitlist_cta), onJoin, enabled = !state.joiningWaitlist)
        com.neerly.mobile.core.design.SecondaryButton(stringResource(R.string.cust_home_try_address), onChangeAddress)
        TextButton(onClick = onJoinAsShop, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.cust_home_join_shop), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** "6 AM" today, "Wed 6 AM" this week, "11 Nov, 6 AM" later — IST, in the app's language. */
internal fun backLabel(iso: String): String = runCatching {
    val zone = java.time.ZoneId.of("Asia/Kolkata")
    val at = java.time.Instant.parse(iso).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val loc = com.neerly.mobile.core.util.AppLocale.locale
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, at.toLocalDate())
    val pattern = when { days == 0L -> "h a"; days in 1..6 -> "EEE h a"; else -> "d MMM, h a" }
    at.format(java.time.format.DateTimeFormatter.ofPattern(pattern, loc))
}.getOrDefault("")
