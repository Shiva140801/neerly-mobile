package com.neerly.mobile.feature.order

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.neerly.mobile.core.design.OfflineCapabilityCard
import com.neerly.mobile.core.design.OrderTrackingSkeleton
import com.neerly.mobile.core.design.StaleDataStamp
import com.neerly.mobile.core.util.asRupees

/**
 * Live tracking — vertical timeline of status events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    onBack: () -> Unit,
    onRateOrder: (orderId: String, orderNumber: String) -> Unit,
    onFileComplaint: (String) -> Unit,
    onCancel: (String) -> Unit = {},
    /** Board NoVendor: no shop took it — same order, other shops. */
    onOtherShops: (orderId: String) -> Unit = {},
    vm: OrderTrackingViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val online by vm.isOnline.collectAsState()
    val order = state.order
    val t = state.tracking
    // Board 15: the heading is the one thing the customer wants to know right now.
    val cantShow = stringResource(R.string.cust_tr_cant_show)
    val headline = when (order?.status) {
        null -> stringResource(R.string.cust_tr_your_order)
        "PLACED", "VENDOR_ASSIGNED", "VENDOR_REJECTED" -> stringResource(R.string.cust_home_finding_shop)
        "VENDOR_ACCEPTED", "PREPARING" -> stringResource(R.string.cust_tr_getting_ready)
        "DISPATCHED", "ARRIVING" -> t?.driverName?.let { stringResource(R.string.cust_tr_driver_on_way, it.substringBefore(' ')) } ?: stringResource(R.string.cust_home_on_the_way)
        "DELIVERED" -> stringResource(R.string.cust_tr_delivered)
        "CANCELLED" -> stringResource(R.string.cust_tr_cancelled)
        else -> stringResource(R.string.cust_tr_failed)
    }
    Column(Modifier.fillMaxSize().background(NeerlyColors.Canvas)) {
        if (!online) OfflineBanner(onRetry = vm::refresh)
        com.neerly.mobile.core.design.BoondhPage(
            title = headline, onBack = onBack, backLabel = stringResource(R.string.cust_tr_back_orders),
            subtitle = order?.let { stringResource(R.string.cust_tr_subtitle, it.orderNumber, it.totalAmount.asRupees()) }
        ) {
            if (state.loading && order == null) { item { OrderTrackingSkeleton() }; return@BoondhPage }
            if (order == null) { item { com.neerly.mobile.core.design.LoadState(false, state.error ?: cantShow, vm::refresh) }; return@BoondhPage }
            if (!online) state.lastUpdatedLabel?.let { l -> item { StaleDataStamp(l) } }
            // Board Offline: reassure first — the order doesn't stop because the phone lost signal.
            if (!online && order.status !in OrderTrackingViewModel.TERMINAL) item {
                com.neerly.mobile.core.design.Notice(
                    t?.driverName?.takeIf { order.status in OrderTrackingViewModel.IN_FLIGHT }
                        ?.let { stringResource(R.string.cust_tr_offline_still_coming_driver, it.substringBefore(' ')) }
                        ?: stringResource(R.string.cust_tr_offline_still_coming),
                    com.neerly.mobile.core.design.Tone.Info)
            }
            if (order.status !in OrderTrackingViewModel.TERMINAL) item {
                DeliveryCodeCard(state.deliveryCode, state.codeRequesting, state.codeError, vm::requestNewDeliveryCode)
            }
            if (order.status in OrderTrackingViewModel.IN_FLIGHT && t != null && com.neerly.mobile.BuildConfig.MAPS_ENABLED) item { OrderLiveMap(t) }
            item { StatusSteps(order.status) }
            if (t?.driverName != null) item {
                com.neerly.mobile.core.design.BoondhCard {
                    com.neerly.mobile.core.design.InfoRow(t.driverName!!, listOfNotNull(
                        t.deliveriesWithCustomer?.takeIf { it > 0 }?.let { n -> androidx.compose.ui.res.pluralStringResource(R.plurals.cust_tr_delivered_times, n, n) },
                        t.vehicle, t.driverPhoneMask).joinToString(" · "))
                }
            }
            if (t != null && (t.driverPhone != null || t.shopPhone != null)) item {
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    com.neerly.mobile.core.design.CallButton(t.driverPhone, stringResource(R.string.cust_call_driver))
                    com.neerly.mobile.core.design.CallButton(t.shopPhone, stringResource(R.string.cust_call_shop))
                }
            }
            when (order.status) {
                "DELIVERED" -> item {
                    if (state.reviewed) Text(stringResource(R.string.cust_tr_rated), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    else com.neerly.mobile.core.design.PrimaryButton(stringResource(R.string.cust_tr_rate), { onRateOrder(order.id, order.orderNumber) })
                }
                "FAILED", "CANCELLED" -> item {
                    EndedCard(order.status, order.paymentMethod)
                    // Failed before any driver had it = no shop said yes (board NoVendor).
                    if (order.status == "FAILED" && order.driverId == null)
                        com.neerly.mobile.core.design.PrimaryButton(stringResource(R.string.cust_nv_see_others), { onOtherShops(order.id) })
                }
            }
            if (order.status !in OrderTrackingViewModel.TERMINAL && order.status != "DELIVERED") item {
                com.neerly.mobile.core.design.SecondaryButton(stringResource(R.string.cust_tr_cancel), { onCancel(order.id) }, color = NeerlyColors.Err)
            }
            if (!online) item { OfflineCapabilityCard(Modifier.fillMaxWidth()) }
            item { TextButton(onClick = { onFileComplaint(order.id) }, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.cust_tr_need_help), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) } }
        }
    }
}

/** Confirmed → Packed → On the way → At your door, as on the board. */
@Composable
private fun StatusSteps(current: String) {
    val steps = listOf(stringResource(R.string.cust_tr_step_confirmed), stringResource(R.string.cust_tr_step_packed), stringResource(R.string.cust_tr_step_on_way), stringResource(R.string.cust_tr_step_door))
    val idx = when (current) {
        "PLACED", "VENDOR_ASSIGNED", "VENDOR_REJECTED" -> -1
        "VENDOR_ACCEPTED" -> 0
        "PREPARING" -> 1
        "DISPATCHED" -> 2
        "ARRIVING", "DELIVERED" -> 3
        else -> -1
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEachIndexed { i, label ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i <= idx) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200))
                Text(label, fontSize = 12.sp, fontWeight = if (i == idx) FontWeight.Bold else FontWeight.Normal,
                    color = if (i <= idx) NeerlyColors.Ink900 else NeerlyColors.Ink600)
            }
        }
    }
}

@Composable
private fun DeliveryCodeCard(code: String?, requesting: Boolean, error: String?, onRequest: () -> Unit) {
    Surface(
        color = NeerlyColors.CustomerSofter,
        shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Text(stringResource(R.string.cust_tr_code_label), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = NeerlyColors.Ink600, letterSpacing = 0.8.sp)
            if (code != null) {
                Text(
                    code.toCharArray().joinToString(" "),
                    fontSize = 36.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                    letterSpacing = 4.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                )
                Text(stringResource(R.string.cust_tr_code_tell),
                    fontSize = 13.sp, color = NeerlyColors.Ink600)
            } else {
                Text(stringResource(R.string.cust_tr_code_missing),
                    fontSize = 13.sp, color = NeerlyColors.Ink700)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onRequest, enabled = !requesting) {
                    Text(if (requesting) stringResource(R.string.cust_tr_code_getting) else stringResource(R.string.cust_tr_code_get),
                        color = NeerlyColors.CustomerPrimary)
                }
            }
            error?.let { Text(it, fontSize = 12.sp, color = NeerlyColors.Err) }
        }
    }
}

@Composable
private fun EndedCard(status: String, paymentMethod: String?) {
    val (title, body) = if (status == "FAILED") {
        stringResource(R.string.cust_tr_failed_title) to
            if (paymentMethod == "COD") stringResource(R.string.cust_tr_failed_cod)
            else stringResource(R.string.cust_tr_failed_paid)
    } else {
        stringResource(R.string.cust_tr_cancelled_title) to
            if (paymentMethod == "COD") stringResource(R.string.cust_tr_cancelled_cod) else stringResource(R.string.cust_tr_cancelled_paid)
    }
    Surface(
        color = NeerlyColors.ErrSoft,
        shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Err)
            Spacer(Modifier.height(4.dp))
            Text(body, fontSize = 13.sp, color = NeerlyColors.Ink700)
        }
    }
}
