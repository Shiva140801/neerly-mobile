package com.neerly.mobile.feature.order

import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.repo.CustomerRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class OrderHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private fun order(id: String = "o1", status: String = "DELIVERED") = OrderResponse(
        id = id, orderNumber = "NEE-26115-0001", customerId = "c1",
        vendorId = "v1", driverId = null, status = status,
        subtotal = BigDecimal("100"), deliveryFee = BigDecimal("30"),
        surgeAmount = null, taxAmount = BigDecimal.ZERO,
        discount = null, depositAmount = null, totalAmount = BigDecimal("130"),
        placedAt = "2026-04-25T09:00:00Z"
    )

    @Test
    fun load_success_populatesOrders() = runTest(dispatcher) {
        coEvery { repo.myOrders(0, 50) } returns listOf(order(), order("o2", "PLACED"))
        val vm = OrderHistoryViewModel(repo)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.orders.size)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun load_failure_setsErrorMessage() = runTest(dispatcher) {
        coEvery { repo.myOrders(0, 50) } throws RuntimeException("offline")
        val vm = OrderHistoryViewModel(repo)
        advanceUntilIdle()

        assertEquals("offline", vm.state.value.error)
        assertTrue(vm.state.value.orders.isEmpty())
    }

    @Test
    fun load_manualRefresh_fetchesAgain() = runTest(dispatcher) {
        coEvery { repo.myOrders(0, 50) } returnsMany listOf(emptyList(), listOf(order()))

        val vm = OrderHistoryViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value.orders.isEmpty())

        vm.load()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.orders.size)
    }
}
