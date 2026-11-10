package com.neerly.mobile.feature.vendor.orders

import org.junit.Assert.assertEquals
import org.junit.Test

/** VNewOrder "12th order with you". */
class OrderOrdinalTest {
    @Test fun englishOrdinal_suffixes() {
        assertEquals(listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "101st", "111th"),
            listOf(1, 2, 3, 4, 11, 12, 13, 21, 22, 101, 111).map(::englishOrdinal))
    }
}
