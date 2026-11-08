package com.neerly.mobile.core.design

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class RupeesTest {
    @Test fun wholeAmounts_haveNoPaise() = assertEquals("₹120", BigDecimal("120.00").rupees())
    @Test fun paiseShowOnlyWhenPresent() = assertEquals("₹6,002.01", BigDecimal("6002.01").rupees())
    @Test fun indianGrouping() = assertEquals("₹1,25,000", BigDecimal("125000").rupees())
    @Test fun zero() = assertEquals("₹0", BigDecimal.ZERO.rupees())
    @Test fun negative() = assertEquals("−₹25", BigDecimal("-25").rupees())
    @Test fun wholeForTiles() = assertEquals("₹6,002", BigDecimal("6002.01").rupeesWhole())
}

class TimeWindowTest {
    @org.junit.Test fun samePeriod_once() = org.junit.Assert.assertEquals("6–8 AM", timeWindow("06:00:00", "08:00:00"))
    @org.junit.Test fun acrossNoon_both() = org.junit.Assert.assertEquals("11 AM–1 PM", timeWindow("11:00", "13:00"))
    @org.junit.Test fun evening() = org.junit.Assert.assertEquals("5–7 PM", timeWindow("17:00", "19:00"))
}
