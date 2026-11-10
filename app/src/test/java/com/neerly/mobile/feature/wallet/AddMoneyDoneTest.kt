package com.neerly.mobile.feature.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/** Board AddMoneyDone: "₹730 covers the next 6". */
class AddMoneyDoneTest {
    private fun done(balance: String?, per: String?) = TopUpDone(BigDecimal("500"), balance?.let(::BigDecimal), Instant.EPOCH,
        per?.let { WalletCoverage("Sri Balaji", BigDecimal(it)) })

    @Test fun coverage_countsWholeDeliveries() = assertEquals(6, done("730", "120").deliveriesCovered)
    @Test fun coverage_unknownUntilTheBalanceIsRead() = assertNull(done(null, "120").deliveriesCovered)
    @Test fun coverage_noneWithoutAWalletSubscription() = assertNull(done("730", null).deliveriesCovered)
}
