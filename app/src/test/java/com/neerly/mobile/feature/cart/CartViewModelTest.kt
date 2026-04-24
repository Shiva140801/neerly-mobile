package com.neerly.mobile.feature.cart

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.cart.CartItem
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.PromoQuoteResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api: NeerlyApi = mockk()
    private lateinit var store: CartStore
    private lateinit var vm: CartViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = CartStore()
        vm = CartViewModel(store, api)
        store.addItem("v1", "Sri Ganesh",
            CartItem("p1", "20L Cool", BigDecimal("100"), 2, keepContainer = true))
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun updateQuantity_delegatesToStore() {
        vm.updateQuantity("p1", 5)
        assertEquals(5, store.snapshot.items.single().quantity)
    }

    @Test
    fun removeLine_clearsPromo() {
        store.applyPromoQuote("X", BigDecimal("10"))
        vm.removeLine("p1")
        assertTrue(store.snapshot.isEmpty)
        assertNull(store.snapshot.promoCode)
    }

    @Test
    fun quotePromo_eligible_applies() = runTest(dispatcher) {
        coEvery { api.quotePromo(any()) } returns PromoQuoteResponse(
            code = "NEERLY50", eligible = true, reason = null,
            discount = BigDecimal("50"), finalTotal = BigDecimal("150")
        )
        vm.quotePromo("NEERLY50", isFirstOrder = true)
        advanceUntilIdle()

        assertTrue(vm.promo.value.applied)
        assertEquals("NEERLY50", store.snapshot.promoCode)
        assertEquals(BigDecimal("50.00"), store.snapshot.discount)
    }

    @Test
    fun quotePromo_ineligible_surfacesReasonAndClearsPromo() = runTest(dispatcher) {
        coEvery { api.quotePromo(any()) } returns PromoQuoteResponse(
            code = "FIRST", eligible = false, reason = "First order only",
            discount = BigDecimal.ZERO, finalTotal = BigDecimal("200")
        )
        vm.quotePromo("FIRST", isFirstOrder = false)
        advanceUntilIdle()

        assertFalse(vm.promo.value.applied)
        assertEquals("First order only", vm.promo.value.error)
        assertNull(store.snapshot.promoCode)
    }

    @Test
    fun quotePromo_emptyCart_rejectsImmediately() = runTest(dispatcher) {
        store.clear()
        vm.quotePromo("X", isFirstOrder = false)
        advanceUntilIdle()
        assertEquals("Add items before applying a code", vm.promo.value.error)
    }

    @Test
    fun removePromo_clearsDiscount() {
        store.applyPromoQuote("X", BigDecimal("20"))
        vm.removePromo()
        assertNull(store.snapshot.promoCode)
        assertEquals(BigDecimal.ZERO, store.snapshot.discount)
    }
}
