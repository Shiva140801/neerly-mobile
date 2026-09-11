package com.neerly.mobile.feature.customer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * The arithmetic behind `S-CUST-VEN-PROD-01`. This is what the customer is
 * agreeing to pay, so the awkward cases — deposit larger than the water,
 * transfer-and-return charging nothing — are pinned here rather than left to a
 * screenshot.
 */
class ProductSheetTest {

    @Test
    fun keepContainer_chargesWaterPlusDeposit() {
        val b = payTodayFor(
            price = BigDecimal("85"),
            depositAmount = BigDecimal("300"),
            quantity = 1,
            mode = ContainerMode.KEEP
        )
        assertEquals(BigDecimal("85.00"), b.water)
        assertEquals(BigDecimal("300.00"), b.deposit)
        assertEquals(BigDecimal("385.00"), b.total)
    }

    @Test
    fun keepContainer_depositCanExceedTheWaterPrice() {
        val b = payTodayFor(BigDecimal("80"), BigDecimal("300"), 1, ContainerMode.KEEP)
        // The whole reason the deposit gets display type on the sheet.
        assertEquals(1, b.deposit.compareTo(b.water))
        assertEquals(BigDecimal("380.00"), b.total)
    }

    @Test
    fun transferAndReturn_chargesNoDeposit_evenWhenProductHasOne() {
        val b = payTodayFor(BigDecimal("60"), BigDecimal("300"), 2, ContainerMode.TRANSFER_AND_RETURN)
        assertEquals(BigDecimal("120.00"), b.water)
        assertEquals(BigDecimal("0.00"), b.deposit)
        assertEquals(BigDecimal("120.00"), b.total)
    }

    @Test
    fun deposit_multipliesWithQuantity() {
        val b = payTodayFor(BigDecimal("85"), BigDecimal("300"), 3, ContainerMode.KEEP)
        assertEquals(BigDecimal("255.00"), b.water)
        assertEquals(BigDecimal("900.00"), b.deposit)
        assertEquals(BigDecimal("1155.00"), b.total)
    }

    @Test
    fun nullDeposit_isTreatedAsZero_notAsACrash() {
        val b = payTodayFor(BigDecimal("45"), null, 2, ContainerMode.KEEP)
        assertEquals(BigDecimal("0.00"), b.deposit)
        assertEquals(BigDecimal("90.00"), b.total)
    }

    @Test
    fun money_keepsTwoDecimalPlaces_halfUp() {
        val b = payTodayFor(BigDecimal("33.335"), BigDecimal.ZERO, 1, ContainerMode.KEEP)
        assertEquals(BigDecimal("33.34"), b.water)
    }

    @Test
    fun defaultMode_transferOnlyProduct_doesNotQuoteADeposit() {
        assertEquals(
            ContainerMode.TRANSFER_AND_RETURN,
            defaultModeFor(allowKeep = false, allowTransfer = true)
        )
    }

    @Test
    fun defaultMode_keepOnlyProduct_isKeep() {
        assertEquals(ContainerMode.KEEP, defaultModeFor(allowKeep = true, allowTransfer = false))
    }

    @Test
    fun defaultMode_bothAllowed_opensOnKeep() {
        assertEquals(ContainerMode.KEEP, defaultModeFor(allowKeep = true, allowTransfer = true))
    }

    @Test
    fun defaultMode_neitherFlagSet_fallsBackToKeep_matchingTheBackend() {
        assertEquals(ContainerMode.KEEP, defaultModeFor(allowKeep = false, allowTransfer = false))
    }
}
