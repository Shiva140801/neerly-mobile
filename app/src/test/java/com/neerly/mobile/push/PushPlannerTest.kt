package com.neerly.mobile.push

import androidx.core.app.NotificationCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PushPlannerTest {
    private val now = Instant.parse("2026-11-10T04:35:00Z").toEpochMilli()

    private fun env(role: PushRole? = PushRole.SELLER, fg: Boolean = false, speed: Float? = null, onDuty: Boolean? = true) =
        PushPlanner.Env(activeRole = role, appForeground = fg, nowMs = now, speedMps = speed, onDuty = onDuty)

    private fun p(type: PushType, role: String = roleOf(type), data: Map<String, String> = mapOf("orderId" to "o1"),
                  expiresAt: String? = null, op: PushPayload.Op = PushPayload.Op.POST, actions: String? = null, v: Int = 1) =
        PushPayload(v = v, type = type.name, role = role, title = "t", body = "b", expiresAt = expiresAt, op = op, actions = actions, data = data)

    private fun roleOf(t: PushType) = when (t.role) { PushRole.CUSTOMER -> "CUSTOMER"; PushRole.SELLER -> "VENDOR_OWNER"; PushRole.DRIVER -> "DRIVER" }

    private fun show(d: PushPlanner.Decision) = (d as PushPlanner.Decision.Show).n

    @Test fun everyCatalogRow_hasAChannelInItsOwnRoleGroup() {
        PushType.entries.forEach { t -> assertEquals(t.name, t.role, t.channel.role) }
        assertEquals(18, PushType.entries.count { it.role == PushRole.CUSTOMER })
        assertEquals(18, PushType.entries.count { it.role == PushRole.SELLER })   // V1–V17 + A1 claim decided
        assertEquals(11, PushType.entries.count { it.role == PushRole.DRIVER })   // D1–D12 minus D5 (local card)
    }

    @Test fun onlySos_bypassesDoNotDisturb() {
        assertEquals(setOf(PushChannel.SELLER_SOS, PushChannel.DRIVER_SOS), PushChannel.entries.filter { it.bypassDnd }.toSet())
        assertTrue(PushChannel.entries.filter { it.loudness == Loudness.CRITICAL }.all { it.bypassDnd })
    }

    @Test fun channelIds_areVersioned_andUnique() {
        assertTrue(PushChannel.entries.all { it.id.endsWith("_v1") })
        assertEquals(PushChannel.entries.size, PushChannel.entries.map { it.id }.toSet().size)
    }

    @Test fun sellerRing_appClosed_ringsFullScreen_onRingChannel() {
        val n = show(PushPlanner.decide(p(PushType.SELLER_NEW_ORDER, expiresAt = "2026-11-10T04:38:00Z"), env()))
        assertTrue(n.ring)
        assertEquals(PushChannel.SELLER_RING, n.channel)
        assertEquals("offer:o1", n.tag)
        assertEquals(listOf(PushAction.DECLINE, PushAction.ACCEPT), n.actions)
        assertEquals(Instant.parse("2026-11-10T04:38:00Z").toEpochMilli(), n.expiresAtMs)
    }

    @Test fun sellerRing_appOpen_showsInAppCard_noRing() {
        val d = PushPlanner.decide(p(PushType.SELLER_NEW_ORDER), env(fg = true))
        assertEquals(PushPlanner.Decision.InApp("vendor/new-order/o1", PushType.SELLER_NEW_ORDER), d)
    }

    @Test fun ring_pastItsWindow_isDropped() {
        val d = PushPlanner.decide(p(PushType.SELLER_NEW_ORDER, expiresAt = "2026-11-10T04:34:59Z"), env())
        assertTrue(d is PushPlanner.Decision.Ignore)
    }

    @Test fun ring_forAnotherRole_isDropped_butOtherRowsPostWithSwitchHint() {
        assertTrue(PushPlanner.decide(p(PushType.SELLER_NEW_ORDER), env(role = PushRole.CUSTOMER)) is PushPlanner.Decision.Ignore)
        val n = show(PushPlanner.decide(p(PushType.SELLER_PAYOUT_SENT), env(role = PushRole.CUSTOMER)))
        assertTrue(n.otherRole)
        assertEquals(PushChannel.SELLER_MONEY, n.channel)
    }

    @Test fun driverRing_onlyOnDuty() {
        assertTrue(PushPlanner.decide(p(PushType.DRIVER_NEW_TASK), env(role = PushRole.DRIVER, onDuty = false)) is PushPlanner.Decision.Ignore)
        assertTrue(show(PushPlanner.decide(p(PushType.DRIVER_NEW_TASK), env(role = PushRole.DRIVER, onDuty = true))).ring)
        // Unknown duty (app just started) doesn't swallow a ring — the server already checked.
        assertTrue(show(PushPlanner.decide(p(PushType.DRIVER_NEW_TASK), env(role = PushRole.DRIVER, onDuty = null))).ring)
    }

    @Test fun missedRows_replaceTheRing_sameTag() {
        val ring = show(PushPlanner.decide(p(PushType.SELLER_NEW_ORDER), env()))
        val missed = show(PushPlanner.decide(p(PushType.SELLER_MISSED_ORDER), env()))
        assertEquals(ring.tag, missed.tag)
        assertEquals(ring.id, missed.id)
        val task = show(PushPlanner.decide(p(PushType.DRIVER_NEW_TASK), env(role = PushRole.DRIVER)))
        val missedTask = show(PushPlanner.decide(p(PushType.DRIVER_MISSED_TASK), env(role = PushRole.DRIVER)))
        assertEquals(task.tag, missedTask.tag)
        assertEquals(NotificationCompat.VISIBILITY_SECRET, missedTask.visibility)  // never on the lock screen (NPDShade)
    }

    @Test fun orderThread_oneCardPerOrder() {
        val accepted = show(PushPlanner.decide(p(PushType.CUST_ORDER_ACCEPTED), env(role = PushRole.CUSTOMER)))
        val gate = show(PushPlanner.decide(p(PushType.CUST_AT_GATE), env(role = PushRole.CUSTOMER)))
        val delivered = show(PushPlanner.decide(p(PushType.CUST_DELIVERED), env(role = PushRole.CUSTOMER)))
        assertEquals(setOf("order:o1"), setOf(accepted.tag, gate.tag, delivered.tag))
        val complaint = show(PushPlanner.decide(p(PushType.SELLER_COMPLAINT, data = mapOf("complaintId" to "c9")), env()))
        val reminder = show(PushPlanner.decide(p(PushType.SELLER_COMPLAINT_REMINDER, data = mapOf("complaintId" to "c9")), env()))
        assertEquals("complaint:c9", complaint.tag)
        assertEquals(complaint.tag, reminder.tag)
    }

    @Test fun serverThread_winsOverDerivedTag() {
        val n = show(PushPlanner.decide(p(PushType.CUST_DELIVERED).copy(thread = "order:custom"), env(role = PushRole.CUSTOMER)))
        assertEquals("order:custom", n.tag)
        assertEquals(PushPlanner.idFor("order:custom"), n.id)
    }

    @Test fun cancel_removesTheThread() {
        val d = PushPlanner.decide(p(PushType.SELLER_NEW_ORDER, op = PushPayload.Op.CANCEL), env())
        assertEquals(PushPlanner.Decision.Cancel("offer:o1", PushPlanner.idFor("offer:o1")), d)
    }

    @Test fun idFor_isStableAndPositive() {
        assertEquals(PushPlanner.idFor("order:abc"), PushPlanner.idFor("order:abc"))
        assertTrue(listOf("", "order:x", "offer:" + "z".repeat(200)).all { PushPlanner.idFor(it) >= 0 })
    }

    @Test fun moneyAndComplaints_arePrivateOnLockScreen_withPublicVersion() {
        listOf(PushType.SELLER_PAYOUT_SENT, PushType.SELLER_PAYOUT_HELD, PushType.SELLER_COMPLAINT, PushType.SELLER_CLAIM_DECIDED).forEach { t ->
            val n = show(PushPlanner.decide(p(t), env()))
            assertEquals(t.name, NotificationCompat.VISIBILITY_PRIVATE, n.visibility ?: n.channel.lockscreen)
            assertTrue(t.name, t.publicTitle != null && t.publicBody != null)
        }
    }

    @Test fun deliveryCode_neverOnLockScreen() {   // decision A6
        listOf(PushType.CUST_ORDER_ACCEPTED, PushType.CUST_AT_GATE).forEach {
            assertEquals(NotificationCompat.VISIBILITY_PRIVATE, it.visibility)
        }
    }

    @Test fun noteChange_movingHidesButtons_stoppedShowsCall() {
        val type = PushType.DRIVER_NOTE_CHANGED
        val moving = show(PushPlanner.decide(p(type), env(role = PushRole.DRIVER, speed = 5f)))  // 18 km/h
        assertTrue(moving.actions.isEmpty())
        assertTrue(moving.speak)
        assertEquals(6_000L, moving.timeoutMs)
        val stopped = show(PushPlanner.decide(p(type), env(role = PushRole.DRIVER, speed = 1f)))
        assertEquals(listOf(PushAction.CALL), stopped.actions)
        val unknown = show(PushPlanner.decide(p(type), env(role = PushRole.DRIVER, speed = null)))
        assertEquals(listOf(PushAction.CALL), unknown.actions)
        assertEquals(10f / 3.6f, PushPlanner.MOVING_MPS, 0.001f)
    }

    @Test fun serverActions_overrideDefaults_unknownIdsIgnored() {
        val n = show(PushPlanner.decide(p(PushType.CUST_DELIVERED, actions = "RATE,BOGUS"), env(role = PushRole.CUSTOMER)))
        assertEquals(listOf(PushAction.RATE), n.actions)
    }

    @Test fun newerPayloadVersion_orUnknownType_showsPlainCardOnDefaultChannel() {
        val v2 = show(PushPlanner.decide(p(PushType.CUST_DELIVERED, v = 2), env(role = PushRole.CUSTOMER)))
        assertEquals(PushChannel.CUST_ORDERS, v2.channel)
        assertTrue(v2.actions.isEmpty())
        assertEquals(null, v2.type)
        val unknown = PushPayload(v = 1, type = "SOMETHING_NEW", role = "DRIVER", title = "Hello", body = "x")
        assertEquals(PushChannel.DRIVER_INFO, show(PushPlanner.decide(unknown, env(role = PushRole.DRIVER))).channel)
        val noText = PushPayload(v = 1, type = "SOMETHING_NEW", role = "DRIVER")
        assertTrue(PushPlanner.decide(noText, env(role = PushRole.DRIVER)) is PushPlanner.Decision.Ignore)
    }

    @Test fun signedOut_ignoresEverything() {
        assertTrue(PushPlanner.decide(p(PushType.CUST_DELIVERED), env(role = null)) is PushPlanner.Decision.Ignore)
    }

    @Test fun staffAndManager_getTheSellerFace() {
        assertEquals(PushRole.SELLER, PushRole.of("VENDOR_STAFF"))
        assertEquals(PushRole.SELLER, PushRole.of("vendor_manager"))
        assertEquals(null, PushRole.of("ADMIN_L1"))
    }

    @Test fun liveCard_updatesSilently_andStaysOngoing() {
        val n = show(PushPlanner.decide(p(PushType.CUST_OUT_FOR_DELIVERY), env(role = PushRole.CUSTOMER)))
        assertTrue(n.ongoing)
        assertTrue(n.onlyAlertOnce)
        assertEquals(PushChannel.CUST_LIVE, n.channel)
        val dayEnd = show(PushPlanner.decide(p(PushType.DRIVER_DAY_END), env(role = PushRole.DRIVER)))
        assertTrue(dayEnd.ongoing)     // stays until the shop confirms the cash
        assertFalse(show(PushPlanner.decide(p(PushType.DRIVER_WEEK_EARNINGS), env(role = PushRole.DRIVER))).ongoing)
    }
}
