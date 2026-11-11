package com.neerly.mobile.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * "Something changed on the server" — app-wide. Pushes (and our own writes) announce it
 * here so whatever screen is showing that order, complaint or list re-reads it now
 * instead of waiting for the next poll or the customer pulling to refresh.
 */
object LiveRefresh {
    /** [type] is the push type (or "LOCAL"); the ids say which thing changed, when known. */
    data class Change(val type: String?, val orderId: String? = null, val complaintId: String? = null,
                      val subscriptionId: String? = null)

    private val changes = MutableSharedFlow<Change>(extraBufferCapacity = 32)
    val events: SharedFlow<Change> = changes.asSharedFlow()

    fun announce(change: Change) { changes.tryEmit(change) }

    /** A push arrived: pull the ids out of its data block (any naming the server uses). */
    fun fromPush(type: String?, data: Map<String, String>) = announce(Change(
        type = type,
        orderId = data["orderId"] ?: data["order_id"],
        complaintId = data["complaintId"] ?: data["complaint_id"],
        subscriptionId = data["subscriptionId"] ?: data["subscription_id"]
    ))
}

/** Runs [action] whenever a change matching [matches] is announced while this screen is shown. */
@Composable
fun OnLiveChange(matches: (LiveRefresh.Change) -> Boolean = { true }, action: () -> Unit) {
    val run by rememberUpdatedState(action)
    val match by rememberUpdatedState(matches)
    LaunchedEffect(Unit) { LiveRefresh.events.collect { if (match(it)) run() } }
}

/**
 * Re-checks every [intervalMs] while [active] — e.g. while an order is still moving or a
 * driver still holds cash. Stops by itself when the screen leaves or [active] turns false.
 */
@Composable
fun LivePoll(active: Boolean, intervalMs: Long, action: () -> Unit) {
    val run by rememberUpdatedState(action)
    LaunchedEffect(active, intervalMs) {
        while (active) { delay(intervalMs); run() }
    }
}
