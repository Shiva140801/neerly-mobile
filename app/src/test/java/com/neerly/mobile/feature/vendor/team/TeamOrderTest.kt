package com.neerly.mobile.feature.vendor.team

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class TeamOrderTest {
    private fun d(id: String, status: String = "OFF", drops: Int = 0, name: String = id) =
        TeamDriver(membershipId = "m$id", driverId = id, name = name, liveStatus = status, dropsToday = drops)
    private fun c(id: String, drops: Int, cash: String, counted: Boolean = false) = CashToday(driverId = id, day = "2026-11-10",
        drops = drops, cashExpected = BigDecimal(cash), emptiesExpected = 0,
        confirmation = if (counted) CashConfirmation("c", "2026-11-10", BigDecimal(cash), BigDecimal(cash), BigDecimal.ZERO, 0, 0, 0, false, null, "2026-11-10T15:00:00Z") else null)

    @Test fun cashToCount_comesFirst_thenOut_thenBusiest() {
        val members = listOf(d("idle"), d("busy", drops = 5), d("onShift", "ON_SHIFT"), d("ravi", drops = 2), d("counted", drops = 3))
        val cash = listOf(c("ravi", 2, "220"), c("counted", 3, "300", counted = true), c("busy", 5, "0"))
        assertEquals(listOf("ravi", "onShift", "busy", "counted", "idle"), teamOrder(members, cash).map { it.driverId })
    }
}
