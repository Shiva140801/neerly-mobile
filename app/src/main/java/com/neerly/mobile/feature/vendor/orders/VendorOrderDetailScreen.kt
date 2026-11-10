package com.neerly.mobile.feature.vendor.orders

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.feature.vendor.dashboard.statusWord

/** Boards V11 (order), V43 (packed, needs a driver) and V21 (cancelled by the customer). */
@Composable
fun VendorOrderDetailScreen(onBack: () -> Unit, vm: VendorOrderDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val o = s.order
    BoondhPage(
        title = o?.orderNumber ?: stringResource(R.string.vend_order),
        onBack = onBack, backLabel = stringResource(R.string.vend_tab_orders),
        subtitle = o?.let { listOf(stringResource(R.string.vend_placed_at, runCatching { istClock(it.placedAt) }.getOrDefault("")),
            stringResource(if (it.paidOnline) R.string.vend_paid_online else R.string.vend_cash_door)).joinToString(" · ") },
        footer = if (s.needsDriver) ({
            s.error?.let { Notice(it, Tone.Bad) }
            PrimaryButton(when {
                s.working -> stringResource(R.string.vend_sending)
                s.pickedDriver == null -> stringResource(R.string.vend_pick_driver)
                else -> stringResource(R.string.vend_send_with, s.team.firstOrNull { it.driverId == s.pickedDriver }?.name?.substringBefore(' ') ?: stringResource(R.string.vend_driver))
            }, vm::send, enabled = s.pickedDriver != null && !s.working)
        }) else null
    ) {
        if (o == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        item {
            Pill(when {
                o.status == "CANCELLED" -> stringResource(R.string.vend_status_cancelled_customer)
                s.needsDriver && o.status == "PREPARING" -> stringResource(R.string.vend_status_packed_needs_driver)
                else -> statusWord(o.status)
            }, when (o.status) { "DELIVERED" -> Tone.Ok; "CANCELLED", "FAILED" -> Tone.Neutral; else -> if (s.needsDriver) Tone.Bad else Tone.Info })
        }
        item {
            BoondhCard {
                Text(o.customerFirstName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                val km = o.distanceKm?.let { stringResource(R.string.vend_km, it.stripTrailingZeros().toPlainString()) }
                Text(listOfNotNull(o.deliveryAddress.ifBlank { null }, km)
                    .joinToString(" · "), fontSize = 14.sp, color = NeerlyColors.Ink600)
                if (o.customerPhoneMask.isNotBlank()) Text(o.customerPhoneMask, fontSize = 13.sp, color = NeerlyColors.Ink600)
                if (o.customerPhone != null || o.driverPhone != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CallButton(o.customerPhone, stringResource(R.string.vend_call_customer), color = NeerlyColors.VendorPrimary)
                    CallButton(o.driverPhone, stringResource(R.string.vend_call_driver), color = NeerlyColors.VendorPrimary)
                }
                o.notes?.let { Text(stringResource(R.string.vend_note, it), fontSize = 14.sp, color = NeerlyColors.Ink700) }
            }
        }
        if (o.status == "CANCELLED") item { CancelledCard(o) } else item { BillCard(o, s.earning) }
        if (o.timeline.isNotEmpty()) item { TimelineCard(o.timeline) }
        if (o.status == "DELIVERED" && s.earning != null) item {
            Text(stringResource(if (s.earning!!.paidOut) R.string.vend_paid_out_bank else R.string.vend_next_payout), fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
        if (s.needsDriver) {
            if (o.status == "VENDOR_ACCEPTED") item {
                SecondaryButton(stringResource(if (s.working) R.string.vend_saving else R.string.vend_mark_packed_later), vm::markReady, enabled = !s.working)
            }
            item { SectionLabel(stringResource(R.string.vend_whos_taking)) }
            if (s.team.isEmpty()) item { Notice(stringResource(R.string.vend_no_drivers_team), Tone.Warn) }
            s.driversByReadiness.forEach { d -> item(key = d.driverId) {
                val on = s.pickedDriver == d.driverId
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .selectable(selected = on, role = Role.RadioButton, enabled = d.liveStatus != "ON_DROP") { vm.pickDriver(d.driverId) },
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = on, onClick = null, enabled = d.liveStatus != "ON_DROP")
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                        Text(listOfNotNull(stringResource(when (d.liveStatus) {
                            "ON_SHIFT" -> R.string.vend_drv_free
                            "ON_DROP" -> R.string.vend_drv_on_drop
                            else -> R.string.vend_drv_off
                        }), stringResource(R.string.vend_drops_today, d.dropsToday),
                            d.phoneMasked.takeIf { it.isNotBlank() }?.let { stringResource(R.string.vend_phone_tail, it.takeLast(4)) }).joinToString(" · "),
                            fontSize = 13.sp, color = NeerlyColors.Ink600)
                    }
                }
            } }
            item { TextButton(onClick = vm::offerDriver, enabled = !s.working, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.vend_offer_whoever), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) } }
            s.note?.let { item { Notice(it, Tone.Info) } }
        } else {
            s.error?.let { item { Notice(it, Tone.Bad) } }
            s.note?.let { item { Notice(it, Tone.Ok) } }
            when (o.status) {
                "DISPATCHED" -> item { Notice(o.driverName?.let { stringResource(R.string.vend_out_for_delivery_with, it) } ?: stringResource(R.string.vend_out_for_delivery), Tone.Info) }
                "ARRIVING" -> {
                    item { Notice(o.driverName?.let { stringResource(R.string.vend_at_door_named, it) } ?: stringResource(R.string.vend_at_door), Tone.Info) }
                    // Only a locked code can be overridden — the server refuses otherwise.
                    if (o.codeLocked) item { CodeLockedOverride(s.working, o.codeLockedUntil, vm::confirmWithoutCode) }
                }
                "VENDOR_ASSIGNED" -> item { Notice(stringResource(R.string.vend_waiting_accept), Tone.Warn) }
                else -> Unit
            }
        }
    }
}

/** Seconds until a locked code reopens, ticking once a second while shown. */
@androidx.compose.runtime.Composable
private fun lockedSecondsLeft(until: String?): Long {
    val end = until?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() } ?: return 0
    var now by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(java.time.Instant.now()) }
    androidx.compose.runtime.LaunchedEffect(end) { while (now.isBefore(end)) { kotlinx.coroutines.delay(1000); now = java.time.Instant.now() } }
    return java.time.Duration.between(now, end).seconds.coerceAtLeast(0)
}

/** The order's steps as the server recorded them. */
@Composable
private fun TimelineCard(steps: List<VendorOrderStep>) {
    BoondhCard {
        SectionLabel(stringResource(R.string.vend_what_happened))
        steps.forEach { st ->
            InfoRow(stepLabelRes(st.status)?.let { stringResource(it) } ?: stepFallback(st.status), trailing = runCatching {
                java.time.Instant.parse(st.at).atZone(java.time.ZoneId.of("Asia/Kolkata"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMM, h:mm a"))
            }.getOrDefault(""))
        }
    }
}

/** The words for a timeline step; null for a status this app doesn't know yet. */
@androidx.annotation.StringRes
internal fun stepLabelRes(status: String): Int? = when (status) {
    "PLACED" -> R.string.vend_step_placed
    "VENDOR_ASSIGNED" -> R.string.vend_step_offered
    "VENDOR_ACCEPTED" -> R.string.vend_step_accepted
    "PREPARING" -> R.string.vend_step_packed
    "DISPATCHED" -> R.string.vend_step_left
    "ARRIVING" -> R.string.vend_step_door
    "DELIVERED" -> R.string.vend_step_delivered
    "CANCELLED" -> R.string.vend_step_cancelled
    "FAILED" -> R.string.vend_step_failed
    "VENDOR_REJECTED" -> R.string.vend_step_declined
    else -> null
}

/** A status newer than this app: shown as the server's code, tidied. */
internal fun stepFallback(status: String) = status.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

/** V21 — what happened to the money when the customer cancelled. */
@Composable
private fun CancelledCard(o: VendorOrderView) {
    BoondhCard {
        SectionLabel(stringResource(R.string.vend_stock_back_itself))
        o.items.forEach { InfoRow(stringResource(R.string.vend_bill_line, it.quantity, it.productName), stringResource(R.string.vend_back_on_shelf)) }
        HorizontalDivider(color = NeerlyColors.Ink200)
        InfoRow(stringResource(R.string.vend_customer_paid), trailing = if (o.paidOnline) o.orderValue.rupees() else stringResource(R.string.vend_nothing_cash))
        if (o.cancellationFee.signum() > 0) InfoRow(stringResource(R.string.vend_late_cancel_fee), stringResource(R.string.vend_late_cancel_fee_sub), o.cancellationFee.rupees())
        // VCustCancelled: what goes back to the customer (what they paid, less the trip fee kept).
        if (o.paidOnline) InfoRow(stringResource(R.string.vend_refunded_customer), stringResource(R.string.vend_refunded_customer_sub),
            (o.orderValue - o.cancellationFee).max(java.math.BigDecimal.ZERO).rupees())
        InfoRow(stringResource(R.string.vend_bill_paid), stringResource(if (o.tripPay.signum() > 0) R.string.vend_for_trip else R.string.vend_nothing_delivered), o.tripPay.rupees(), NeerlyColors.Ok)
    }
}


/** Driver board D26: the code locked after wrong tries — the owner confirms after calling the customer. */
@androidx.compose.runtime.Composable
private fun CodeLockedOverride(working: Boolean, lockedUntil: String?, onConfirm: (String) -> Unit) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var reason by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val left = lockedSecondsLeft(lockedUntil)
    if (!open) {
        Notice(if (left > 0) stringResource(R.string.vend_code_locked_left, "${left / 60}:${"%02d".format(left % 60)}")
            else stringResource(R.string.vend_code_locked), Tone.Warn)
        androidx.compose.material3.TextButton(onClick = { open = true }, modifier = androidx.compose.ui.Modifier.heightIn(min = 48.dp)) {
            androidx.compose.material3.Text(stringResource(R.string.vend_confirm_no_code), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)
        }
        return
    }
    BoondhCard {
        SectionLabel(stringResource(R.string.vend_confirm_no_code))
        androidx.compose.material3.Text(stringResource(R.string.vend_confirm_no_code_body),
            fontSize = 14.sp, color = NeerlyColors.Ink600)
        androidx.compose.material3.OutlinedTextField(value = reason, onValueChange = { reason = it.take(300) },
            label = { androidx.compose.material3.Text(stringResource(R.string.vend_how_confirmed)) },
            modifier = androidx.compose.ui.Modifier.fillMaxWidth())
        PrimaryButton(stringResource(if (working) R.string.vend_confirming else R.string.vend_mark_delivered), { onConfirm(reason) }, enabled = !working && reason.isNotBlank(),
            color = NeerlyColors.Err)
    }
}
