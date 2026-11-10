package com.neerly.mobile.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushPayloadAndLinksTest {

    @Test fun parse_readsEveryField_andTheDataJson() {
        val p = PushPayload.parse(mapOf(
            "v" to "1", "type" to "SELLER_NEW_ORDER", "role" to "VENDOR_OWNER", "nid" to "n1", "thread" to "offer:o1",
            "op" to "update", "title" to "New order · ₹460", "body" to "Rahul V.", "deepLink" to "boondh://seller/new-order/o1",
            "actions" to "ACCEPT,DECLINE", "expiresAt" to "2026-11-10T04:38:00Z",
            "data" to """{"orderId":"o1","amount":460,"km":2.3,"note":null,"paid":true}"""
        ))
        assertEquals(1, p.v)
        assertEquals("SELLER_NEW_ORDER", p.type)
        assertEquals(PushPayload.Op.UPDATE, p.op)
        assertEquals("o1", p["orderId"])
        assertEquals("460", p["amount"])          // whole numbers stay whole
        assertEquals("2.3", p["km"])
        assertEquals("true", p["paid"])
        assertNull(p["note"])                    // JSON null → absent
        assertEquals(1_794_285_480_000L, p.expiresAtMs)
    }

    @Test fun parse_neverThrows_onJunk() {
        val p = PushPayload.parse(mapOf("v" to "x", "data" to "{not json", "expiresAt" to "soon", "op" to "??"))
        assertEquals(1, p.v)
        assertTrue(p.data.isEmpty())
        assertNull(p.expiresAtMs)
        assertEquals(PushPayload.Op.POST, p.op)
    }

    @Test fun toMap_roundTrips() {
        val p = PushPayload(v = 1, type = "CUST_AT_GATE", role = "CUSTOMER", thread = "order:o1", op = PushPayload.Op.CANCEL,
            title = "Ramesh is at your gate", data = mapOf("orderId" to "o1", "driverPhone" to "+919100027452"))
        assertEquals(p, PushPayload.parse(p.toMap()))
    }

    @Test fun deepLinks_mapToRealRoutes() {
        assertEquals("customer/order/o1", DeepLinks.route("boondh://order/o1"))
        assertEquals("vendor/new-order/o1", DeepLinks.route("boondh://seller/new-order/o1?decline=1"))
        assertEquals("vendor/order/o1", DeepLinks.route("boondh://seller/order/o1"))
        assertEquals("vendor/alert-settings", DeepLinks.route("boondh://seller/alert-settings"))
        assertEquals("driver/home", DeepLinks.route("boondh://driver"))
        assertEquals("driver/sos?send=false", DeepLinks.route("boondh://driver/sos/s1"))
        assertEquals("customer/complaint?orderId=o1", DeepLinks.route("boondh://complaint-new?orderId=o1"))
        // C5 "Pick a new time" opens the re-delivery slot picker; plain order links don't.
        assertEquals("customer/order/o1/redeliver/time", DeepLinks.route("boondh://order/o1/redeliver"))
        assertEquals("customer/order/o1", DeepLinks.route("boondh://order/o1"))
        assertNull(DeepLinks.route("https://evil.example/order/1"))
        assertNull(DeepLinks.route("boondh://nowhere"))
    }

    @Test fun declineLink_isRecognised() {
        assertEquals("o1", DeepLinks.declineOrderId("boondh://seller/new-order/o1?decline=1"))
        assertNull(DeepLinks.declineOrderId("boondh://seller/new-order/o1"))
        assertNull(DeepLinks.declineOrderId("boondh://order/o1?decline=1"))
    }

    @Test fun everyType_hasADefaultLink_thatResolvesInItsOwnFace() {
        val p = PushPayload(v = 1, type = null, role = null, data = mapOf("orderId" to "o1", "complaintId" to "c1",
            "subscriptionId" to "s1", "refundId" to "r1", "vendorId" to "v1", "claimId" to "k1"))
        PushType.entries.forEach { t ->
            val route = DeepLinks.route(DeepLinks.default(t, p))
            assertTrue("${t.name} → $route", route != null)
            val face = DeepLinks.roleOf(route!!)
            assertTrue("${t.name} opens $route", face == null || face == t.role)
        }
    }

    @Test fun everyActionLink_resolves() {
        val p = PushPayload(v = 1, type = null, role = null, data = mapOf("orderId" to "o1"))
        PushType.entries.forEach { t ->
            PushAction.entries.filter { it.kind == PushAction.Kind.OPEN }.forEach { a ->
                assertTrue("${t.name}/${a.name}", DeepLinks.route(DeepLinks.forAction(a, t, p)) != null)
            }
        }
    }
}
