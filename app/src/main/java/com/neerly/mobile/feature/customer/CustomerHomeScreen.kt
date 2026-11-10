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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    onNotifications: () -> Unit = {},
    vm: CustomerHomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val online by vm.isOnline.collectAsState()
    // Back from adding the first address: reload so shops for that pincode show up.
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (!vm.state.value.loading && vm.state.value.primaryAddress == null) vm.refresh()
                vm.refreshUnread()
            }
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
    val addressLine = state.primaryAddress?.let { "${com.neerly.mobile.feature.address.addressLabel(it.label)} · ${it.street}" }
        ?: stringResource(R.string.cust_home_set_address)
    val bellLabel = if (state.unread > 0) androidx.compose.ui.res.pluralStringResource(R.plurals.cust_home_bell_unread, state.unread, state.unread)
        else stringResource(R.string.cust_home_bell)
    Scaffold(containerColor = NeerlyColors.Canvas,
        bottomBar = { CustomerBottomBar(current = CustomerTab.Home, onSelect = onSelectTab) }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
        if (!online) OfflineBanner(onRetry = vm::refresh)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Board Home: address switcher + bell, then a two-line serif greeting.
            item {
                com.neerly.mobile.core.design.AddressHeader(stringResource(R.string.cust_home_delivering_caps), addressLine,
                    onAddress = { if (state.primaryAddress == null) onAddAddress() else onChangeAddress() },
                    bellLabel = bellLabel, unread = state.unread, onBell = onNotifications)
            }
            item {
                Column {
                    com.neerly.mobile.core.design.PageTitle(greeting, size = 32)
                    if (state.usual != null) Text(stringResource(R.string.cust_home_running_low), fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 36.sp, color = NeerlyColors.Ink600)
                }
            }
            if (state.loading) { item { HomeSkeleton() }; return@LazyColumn }
            if (state.error != null) { item { com.neerly.mobile.core.design.LoadState(false, state.error, vm::refresh) }; return@LazyColumn }
            if (state.primaryAddress == null) item {
                com.neerly.mobile.core.design.BoondhCard {
                    Text(stringResource(R.string.cust_home_where_deliver), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text(stringResource(R.string.cust_home_add_address_body), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    com.neerly.mobile.core.design.PrimaryButton(stringResource(R.string.cust_add_address), onAddAddress)
                }
            }
            // Live orders: one quiet strip each, straight to tracking.
            state.activeOrders.forEach { o -> item {
                com.neerly.mobile.core.design.StripCard(tint = NeerlyColors.Water50, onClick = { onOrderClick(o.id) },
                    leading = { Icon(androidx.compose.material.icons.Icons.Filled.LocalShipping, null, tint = NeerlyColors.CustomerPrimary, modifier = Modifier.size(22.dp)) },
                    trailing = { Text(stringResource(R.string.cust_home_track), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) }) {
                    Column {
                        Text(when (o.status) {
                            "DISPATCHED", "ARRIVING" -> stringResource(R.string.cust_home_on_the_way); "VENDOR_ACCEPTED", "PREPARING" -> stringResource(R.string.cust_home_being_packed)
                            else -> stringResource(R.string.cust_home_finding_shop)
                        }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(listOfNotNull(o.vendorName, o.totalAmount.asRupees()).joinToString(" · "), fontSize = 13.sp, color = NeerlyColors.Ink600)
                    }
                }
            } }
            state.usual?.let { u -> item { UsualCard(u, state, onOrderAgain = { if (vm.orderUsual()) onOpenCart() }) } }
            item { com.neerly.mobile.core.design.SearchField(stringResource(R.string.cust_home_search), onOpenSearch) }
            state.nextSubscription?.let { sub -> item {
                com.neerly.mobile.core.design.StripCard(onClick = { onSubscription(sub.id) },
                    leading = { Icon(androidx.compose.material.icons.Icons.Filled.Autorenew, null, tint = NeerlyColors.Ink900, modifier = Modifier.size(22.dp)) },
                    trailing = {
                        TextButton(onClick = vm::skipNextDelivery, enabled = !state.skipping, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.cust_skip), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }) {
                    val whenText = stringResource(R.string.cust_home_sub_when, com.neerly.mobile.core.design.shortDay(sub.nextDeliveryDate),
                        com.neerly.mobile.core.design.timeWindow(sub.slotStart, sub.slotEnd))
                    val rest = stringResource(R.string.cust_home_sub_strip, androidx.compose.ui.res.pluralStringResource(R.plurals.cust_jars_n, sub.quantity, sub.quantity))
                    Text(androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(whenText) }
                        append(" · "); append(rest)
                    }, fontSize = 15.sp, lineHeight = 21.sp, color = NeerlyColors.Ink900)
                }
            } }
            state.message?.let { m -> item { com.neerly.mobile.core.design.Notice(m, com.neerly.mobile.core.design.Tone.Info) } }
            val open = state.openVendors
            val closed = state.closedVendors
            if (state.vendors.isEmpty()) {
                item { NoSuppliers(state, vm::joinWaitlist, onChangeAddress, onJoinAsShop) }
                return@LazyColumn
            }
            val openCount = state.vendors.count { it.takingOrders && !it.closedForNow }
            item { com.neerly.mobile.core.design.SectionHeader(stringResource(R.string.cust_home_suppliers_title),
                androidx.compose.ui.res.pluralStringResource(R.plurals.cust_home_open_now_n, openCount, openCount)) }
            item { FilterChips(state.filter, vm::toggleFilter) }
            if (open.isEmpty() && closed.isEmpty()) item {
                Text(stringResource(R.string.cust_home_no_match), fontSize = 14.sp, color = NeerlyColors.Ink600)
            }
            items(open, key = { it.id }) { v -> ShopRow(v, onClick = { onVendorClick(v.id) }) }
            if (closed.isNotEmpty()) {
                item { com.neerly.mobile.core.design.SectionLabel(stringResource(R.string.cust_home_closed_section)) }
                items(closed, key = { "closed-" + it.id }) { v -> ShopRow(v, onClick = { onVendorClick(v.id) }) }
            }
        }
    }
    }
}

/** Board Home "Your usual": navy card, jar art, amber Order again with the shop's ETA beside it. */
@Composable
private fun UsualCard(u: OrderResponse, state: HomeUiState, onOrderAgain: () -> Unit) {
    val jars = u.items.sumOf { it.quantity }.coerceIn(1, 3)
    val shop = state.vendors.firstOrNull { it.id == u.vendorId }
    val onItsWay = state.activeOrders.any { it.vendorId == u.vendorId }
    Column(Modifier.fillMaxWidth().background(NeerlyColors.CustomerDark, RoundedCornerShape(NeerlyRadius.lg)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.cust_home_usual_caps).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp, color = com.neerly.mobile.core.design.Boondh.Amber)
                Text(u.items.map { stringResource(R.string.cust_qty_item, it.quantity, it.productName) }.joinToString(", "), fontSize = 24.sp,
                    lineHeight = 28.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Medium, color = NeerlyColors.Paper)
                Text(if (onItsWay) stringResource(R.string.cust_home_usual_on_way, u.vendorName ?: stringResource(R.string.cust_your_shop))
                    else stringResource(R.string.cust_home_last_ordered, u.vendorName ?: stringResource(R.string.cust_your_shop), com.neerly.mobile.core.design.shortDay(u.placedAt)),
                    fontSize = 14.sp, color = com.neerly.mobile.core.design.Boondh.MutedOnNavy)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clearAndSetSemantics { }) {
                repeat(jars) { com.neerly.mobile.core.design.JarGlyph() }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1f)) {
                com.neerly.mobile.core.design.PrimaryButton(stringResource(R.string.cust_home_order_again, u.subtotal.asRupees()),
                    onOrderAgain, color = com.neerly.mobile.core.design.Boondh.Amber, contentColor = NeerlyColors.Ink900)
            }
            shop?.etaMinutesMax?.let { Text(stringResource(R.string.cust_home_eta_approx, it), fontSize = 15.sp, color = com.neerly.mobile.core.design.Boondh.MutedOnNavy) }
        }
    }
}

/** Board Home chips: Open now / Top rated / Chilled / Tanker. Tap again to clear. */
@Composable
private fun FilterChips(selected: HomeFilter?, onToggle: (HomeFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(HomeFilter.entries) { f ->
            com.neerly.mobile.core.design.BoondhChip(when (f) {
                HomeFilter.OPEN_NOW -> stringResource(R.string.cust_home_chip_open)
                HomeFilter.TOP_RATED -> stringResource(R.string.cust_home_chip_top)
                HomeFilter.CHILLED -> stringResource(R.string.cust_home_chip_chilled)
                HomeFilter.TANKER -> stringResource(R.string.cust_home_chip_tanker)
            }, selected == f) { onToggle(f) }
        }
    }
}

/** A shop row (board Home): initials tile, name + verified/NEW, rating · ETA · km, "20L from ₹X" on the right. */
@Composable
private fun ShopRow(v: VendorCardResponse, onClick: () -> Unit) {
    val closedNow = v.closedForNow || !v.takingOrders
    com.neerly.mobile.core.design.ListRow(onClick = onClick) {
        com.neerly.mobile.core.design.InitialsTile(v.businessName.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(),
            bg = if (closedNow) NeerlyColors.Ink100 else NeerlyColors.CustomerSoft, fg = if (closedNow) NeerlyColors.Ink600 else NeerlyColors.CustomerPrimary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(v.businessName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (closedNow) NeerlyColors.Ink600 else NeerlyColors.Ink900,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (v.fssaiNumber != null) Icon(androidx.compose.material.icons.Icons.Filled.Verified, stringResource(R.string.cust_fssai_verified),
                    tint = NeerlyColors.Ok, modifier = Modifier.size(18.dp))
                if ((v.reviewCount ?: 0) == 0 && v.totalOrders < 20) com.neerly.mobile.core.design.InkTag(stringResource(R.string.cust_home_new_tag))
            }
            val rating = v.avgRating?.let { r ->
                stringResource(R.string.cust_rating_stars, r.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()) +
                    (v.reviewCount?.takeIf { it > 0 }?.let { " ($it)" } ?: "")
            }
            val eta = if (v.etaMinutesMin != null && v.etaMinutesMax != null && !closedNow)
                stringResource(R.string.cust_home_eta, v.etaMinutesMin, v.etaMinutesMax) else null
            val km = v.distanceKm?.let { stringResource(R.string.cust_home_km, String.format(java.util.Locale.ROOT, "%.1f", it)) }
            Text(listOfNotNull(rating, eta, km).joinToString(" · "), fontSize = 14.sp, color = NeerlyColors.Ink600, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (closedNow) {
                val back = v.backOn?.let(::backLabel)
                Text(when {
                    v.closedLabel != null && back != null -> stringResource(R.string.cust_home_closed_label_back, v.closedLabel, back)
                    back != null -> stringResource(R.string.cust_home_closed_back, back)
                    else -> stringResource(R.string.cust_home_closed_now)
                }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Warn)
            }
        }
        if (!closedNow) v.jarFromPrice?.let {
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.cust_home_jar_from_caption), fontSize = 13.sp, color = NeerlyColors.Ink600)
                Text(it.asRupees(), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
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
