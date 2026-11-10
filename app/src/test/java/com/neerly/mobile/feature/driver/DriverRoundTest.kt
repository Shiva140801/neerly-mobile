package com.neerly.mobile.feature.driver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DriverRoundTest {

    @Test
    fun roundEtas_chainFromTheDriver_addingTimeAtEachDoorPassed() {
        val here = 17.4400 to 78.3480
        val a = 17.4490 to 78.3480     // ~1.0 km north
        val b = 17.4580 to 78.3480     // ~1.0 km further
        val etas = roundEtas(here, listOf(a, b), speedKmh = 18)
        assertEquals(1.0, etas[0]!!.km, 0.05)
        assertEquals(3, etas[0]!!.minutes)                         // 1 km at 18 km/h ≈ 3.3 min
        assertEquals(1.0, etas[1]!!.km, 0.05)
        assertEquals(10, etas[1]!!.minutes)                        // 3.3 + 3.3 + 3 at the first door
    }

    @Test
    fun roundEtas_noStartingPoint_orNoPin_givesNoGuess() {
        assertNull(roundEtas(null, listOf(17.44 to 78.34), 18)[0])
        val e = roundEtas(17.44 to 78.34, listOf(null, 17.45 to 78.34), 18)
        assertNull(e[0]); assertTrue(e[1] != null)
    }

    @Test
    fun breakWindow_saysThePeriodOnce_whenBothEndsShareIt() {
        assertEquals("1:15 – 1:45 PM", breakWindow("2026-11-10T07:45:00Z", "2026-11-10T08:15:00Z"))
        assertEquals("11:45 AM – 12:15 PM", breakWindow("2026-11-10T06:15:00Z", "2026-11-10T06:45:00Z"))
        assertNull(breakWindow("2026-11-10T07:45:00Z", null))
    }

    @Test
    fun countdown_neverNegative_andFormatsMinutesSeconds() {
        val now = java.time.Instant.parse("2026-11-10T08:00:00Z").toEpochMilli()
        assertEquals(900L, secondsUntil("2026-11-10T08:15:00Z", now))
        assertEquals(0L, secondsUntil("2026-11-10T07:00:00Z", now))
        assertEquals("14:58", minSec(898))
    }

    @Test
    fun shortExplained_sumsOnlyWhatEachCustomerFellShort() {
        val bills = mapOf("a" to BigDecimal("460"), "b" to BigDecimal("180"))
        assertEquals(BigDecimal("80"), shortExplained(bills, mapOf("a" to BigDecimal("380"))))
        assertEquals(BigDecimal("0"), shortExplained(bills, mapOf("zz" to BigDecimal("10"))))
    }

    @Test
    fun codLines_carryTheReason_andDropUnknownOnes() {
        val lines = codLines(mapOf("o1" to BigDecimal("380"), "o2" to BigDecimal("100")), mapOf("o1" to "PAYS_LATER", "o2" to "BOGUS"))!!
        assertEquals("PAYS_LATER", lines.first { it.orderId == "o1" }.reason)
        assertNull(lines.first { it.orderId == "o2" }.reason)
        assertNull(codLines(emptyMap(), emptyMap()))
    }
}
