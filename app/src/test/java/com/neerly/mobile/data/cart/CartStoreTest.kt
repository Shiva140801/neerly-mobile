package com.neerly.mobile.data.cart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CartStoreTest {

    private lateinit var store: CartStore

    @Before fun setUp() { store = CartStore() }

    private fun item(id: String = "p1", price: String = "95", qty: Int = 1,
                     keep: Boolean = true, deposit: String? = null) =
        CartItem(
            productId = id,
            productName = "20L Cool",
            unitPrice = BigDecimal(price),
            quantity = qty,
            keepContainer = keep,
            depositPerContainer = deposit?.let { BigDecimal(it) }
        )

    @Test
    fun addItem_emptyCart_populatesVendorAndItem() {
        val outcome = store.addItem("v1", "Sri Ganesh", item())
        assertTrue(outcome is AddOutcome.Added)
        assertEquals("v1", store.snapshot.vendorId)
        assertEquals("Sri Ganesh", store.snapshot.vendorName)
        assertEquals(1, store.snapshot.items.size)
    }

    @Test
    fun addItem_sameVendor_differentProducts_appendsRow() {
        store.addItem("v1", "Sri Ganesh", item("p1"))
        val outcome = store.addItem("v1", "Sri Ganesh", item("p2", "20"))
        assertTrue(outcome is AddOutcome.Added)
        assertEquals(2, store.snapshot.items.size)
    }

    @Test
    fun addItem_sameVendor_sameProduct_incrementsQuantity() {
        store.addItem("v1", "Sri Ganesh", item("p1", qty = 2))
        store.addItem("v1", "Sri Ganesh", item("p1", qty = 3))
        assertEquals(5, store.snapshot.items.single().quantity)
    }

    @Test
    fun addItem_differentVendor_returnsMismatchWithoutMutating() {
        store.addItem("v1", "Sri Ganesh", item("p1"))
        val outcome = store.addItem("v2", "Pure Drops", item("p2", "20"))
        assertTrue(outcome is AddOutcome.VendorMismatch)
        assertEquals("v1", store.snapshot.vendorId)
        assertEquals(1, store.snapshot.items.size)
    }

    @Test
    fun replaceWithNewVendor_clearsPreviousAndAddsFresh() {
        store.addItem("v1", "Sri Ganesh", item("p1"))
        store.replaceWithNewVendor("v2", "Pure Drops", item("p3", "100"))
        assertEquals("v2", store.snapshot.vendorId)
        assertEquals(1, store.snapshot.items.size)
        assertEquals("p3", store.snapshot.items.single().productId)
    }

    @Test
    fun updateQuantity_toZero_removesLine() {
        store.addItem("v1", "Sri Ganesh", item("p1"))
        store.addItem("v1", "Sri Ganesh", item("p2", "20"))
        store.updateQuantity("p1", 0)
        assertEquals(1, store.snapshot.items.size)
        assertEquals("p2", store.snapshot.items.single().productId)
    }

    @Test
    fun updateQuantity_removingLastLine_clearsVendor() {
        store.addItem("v1", "Sri Ganesh", item("p1"))
        store.updateQuantity("p1", 0)
        assertNull(store.snapshot.vendorId)
        assertTrue(store.snapshot.isEmpty)
    }

    @Test
    fun updateQuantity_above10_clampsTo10() {
        store.addItem("v1", "Sri Ganesh", item("p1"))
        store.updateQuantity("p1", 50)
        assertEquals(10, store.snapshot.items.single().quantity)
    }

    @Test
    fun subtotal_sumsLines() {
        store.addItem("v1", "Sri Ganesh", item("p1", "95", qty = 2))
        store.addItem("v1", "Sri Ganesh", item("p2", "20", qty = 3))
        assertEquals(BigDecimal("250.00"), store.snapshot.subtotal)
    }

    @Test
    fun total_appliesDeliveryAndSurgeAndDepositLessDiscount_clampedToZero() {
        store.addItem("v1", "Sri Ganesh", item("p1", "100", qty = 1))
        store.setPricing(BigDecimal("30"), BigDecimal("20"), BigDecimal("500"))
        store.applyPromoQuote("NEERLY50", BigDecimal("100"))
        // 100 + 30 + 20 + 500 − 100 = 550
        assertEquals(BigDecimal("550.00"), store.snapshot.total)
    }

    @Test
    fun total_neverNegative() {
        store.addItem("v1", "Sri Ganesh", item("p1", "50", qty = 1))
        store.applyPromoQuote("BIG", BigDecimal("10000"))
        assertEquals(BigDecimal("0.00"), store.snapshot.total)
    }

    @Test
    fun clearPromo_removesCodeAndDiscount() {
        store.addItem("v1", "Sri Ganesh", item("p1"))
        store.applyPromoQuote("X", BigDecimal("20"))
        store.clearPromo()
        assertNull(store.snapshot.promoCode)
        assertEquals(BigDecimal.ZERO, store.snapshot.discount)
    }

    @Test
    fun lineDeposit_keepContainer_returnsDeposit() {
        val it = item("p1", "95", qty = 2, keep = true, deposit = "500")
        assertEquals(BigDecimal("1000.00"), it.lineDeposit)
    }

    @Test
    fun lineDeposit_transferAndReturn_zero() {
        val it = item("p1", "95", qty = 2, keep = false, deposit = "500")
        assertEquals(BigDecimal.ZERO, it.lineDeposit)
    }
}
