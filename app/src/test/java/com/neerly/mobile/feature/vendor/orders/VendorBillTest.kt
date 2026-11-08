package com.neerly.mobile.feature.vendor.orders

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class VendorBillTest {
    // The order seen on the emulator: 2 × ₹85 jars + ₹25 delivery + ₹600 jar deposit, shop earns ₹171.99.
    private val order = VendorOrderView(
        id = "o", orderNumber = "NEE-1", status = "VENDOR_ACCEPTED", placedAt = "2026-11-07T12:42:00Z",
        items = listOf(VendorOrderLine(productId = "p1", productName = "20L Jar", quantity = 2, unitPrice = BigDecimal("85"))),
        orderValue = BigDecimal("795"), depositTotal = BigDecimal("600"), yourEarning = BigDecimal("171.99")
    )

    @Test fun depositIsNotTheShopsSaleNorBoondhsFee() {
        val (sale, delivery, cut) = vendorBill(order)
        assertEquals(BigDecimal("195"), sale)
        assertEquals(BigDecimal("25"), delivery)
        assertEquals(BigDecimal("23.01"), cut)
    }
}

class LiveOffersTest {
    private fun offer(id: String, deadline: String?) = VendorOrderView(id = id, orderNumber = id, status = "VENDOR_ASSIGNED",
        placedAt = "2026-11-07T11:00:00Z", acceptDeadlineAt = deadline)
    private val now = java.time.Instant.parse("2026-11-07T11:40:00Z")

    @org.junit.Test fun expiredOffersNeitherRingNorList() {
        val live = com.neerly.mobile.feature.vendor.dashboard.liveOffers(listOf(
            offer("gone", "2026-11-07T11:39:00Z"), offer("late", "2026-11-07T11:42:00Z"), offer("soon", "2026-11-07T11:41:00Z")), now)
        org.junit.Assert.assertEquals(listOf("soon", "late"), live.map { it.id })
    }
}
