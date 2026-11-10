package com.neerly.mobile.feature.order

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.api.DiscoveryApi
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.AlternativeLineDto
import com.neerly.mobile.data.dto.AlternativeShopDto
import com.neerly.mobile.data.dto.OrderAlternativesResponse
import com.neerly.mobile.data.dto.OrderItemResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.RerouteRequest
import com.neerly.mobile.data.repo.CustomerRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/** Board NoVendor: same order, other shops. */
@OptIn(ExperimentalCoroutinesApi::class)
class NoVendorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val api: DiscoveryApi = mockk()
    private val repo: CustomerRepository = mockk()
    private val cart = CartStore()

    private val pureDrops = AlternativeShopDto(
        vendorId = "pd", businessName = "Pure Drops", etaMinutesMin = 25, etaMinutesMax = 35,
        lines = listOf(AlternativeLineDto("pd-jar", "20L Jar", 2, BigDecimal("55"), BigDecimal("110"), "KEEP")),
        subtotal = BigDecimal("110"), deliveryFee = BigDecimal("25"), depositTotal = BigDecimal.ZERO, totalAmount = BigDecimal("135"))

    private fun order(id: String) = OrderResponse(
        id = id, orderNumber = "NEE-1", customerId = "c1", vendorId = "sb", driverId = null, status = "FAILED",
        subtotal = BigDecimal("120"), deliveryFee = BigDecimal("25"), surgeSurcharge = null, taxes = null, discount = null,
        depositTotal = null, totalAmount = BigDecimal("145"), placedAt = "2026-11-07T09:00:00Z",
        items = listOf(OrderItemResponse(productId = "sb-jar", productName = "20L Jar", quantity = 2,
            unitPrice = BigDecimal("60"), subtotal = BigDecimal("120"), containerMode = "TRANSFER_AND_RETURN")))

    private fun vm(): NoVendorViewModel {
        coEvery { api.alternatives("o1") } returns OrderAlternativesResponse("o1", "NEE-1", "sb", "Sri Balaji", listOf(pureDrops))
        return NoVendorViewModel(api, repo, cart, com.neerly.mobile.support.TestStrings, SavedStateHandle(mapOf("orderId" to "o1")))
    }

    @Test fun sendTo_reroutesAndHandsBackTheNewOrder() = runTest(dispatcher) {
        coEvery { api.reroute("o1", RerouteRequest("pd")) } returns order("o2")
        val vm = vm()
        advanceUntilIdle()
        assertEquals(listOf("Pure Drops"), vm.state.value.data!!.alternatives.map { it.businessName })
        vm.sendTo(pureDrops)
        advanceUntilIdle()
        assertEquals("o2", vm.state.value.rerouted)
        coVerify(exactly = 1) { api.reroute("o1", RerouteRequest("pd")) }
    }

    @Test fun sendTo_failureSaysSo_andCanBeRetried() = runTest(dispatcher) {
        coEvery { api.reroute("o1", any()) } throws RuntimeException("409")
        val vm = vm()
        advanceUntilIdle()
        vm.sendTo(pureDrops)
        advanceUntilIdle()
        assertNull(vm.state.value.rerouted)
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.sending)
    }

    @Test fun waitForTheShop_putsTheSameLinesBackInItsCart() = runTest(dispatcher) {
        coEvery { repo.order("o1") } returns order("o1")
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.refillCartForOriginalShop())
        val c = cart.snapshot
        assertEquals("sb", c.vendorId)
        assertEquals("Sri Balaji", c.vendorName)
        assertEquals(2, c.items.single().quantity)
        assertEquals(false, c.items.single().keepContainer)   // swap at the door, as ordered
    }
}
