package com.neerly.mobile.feature.subscription

import com.neerly.mobile.data.dto.SubscriptionResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class SubscriptionCopyTest {
    private fun sub(freq: String, days: List<String>? = null, price: BigDecimal? = BigDecimal("60")) = SubscriptionResponse(
        id = "s", customerId = "c", vendorId = "v", productId = "p", productName = "20L Plain", quantityPerDelivery = 2,
        frequency = freq, daysOfWeek = days, slotStart = "06:00:00", slotEnd = "09:00:00", startDate = "2026-11-01",
        paymentMethod = "WALLET", status = "ACTIVE", unitPrice = price
    )

    @Test fun howOften_readsLikeTheBoards() {
        assertEquals("every other day", sub("ALT_DAY").howOften(com.neerly.mobile.support.TestStrings))
        assertEquals("Mon, Wed, Fri", sub("CUSTOM", listOf("MONDAY", "WEDNESDAY", "FRIDAY")).howOften(com.neerly.mobile.support.TestStrings))
    }

    @Test fun eachTime_isPriceTimesJars_orUnknown() {
        assertEquals(BigDecimal("120"), sub("DAILY").eachTime())
        assertEquals(null, sub("DAILY", price = null).eachTime())
        assertEquals("2 × 20L Plain", sub("DAILY").what(com.neerly.mobile.support.TestStrings))
    }

    @Test fun createState_monthlyEstimate() {
        assertEquals(15, SubscriptionCreateUiState(frequency = "ALT_DAY").deliveriesPerMonth)
        assertEquals(12, SubscriptionCreateUiState(frequency = "CUSTOM", daysOfWeek = listOf("MON", "WED", "FRI")).deliveriesPerMonth)
    }
}
