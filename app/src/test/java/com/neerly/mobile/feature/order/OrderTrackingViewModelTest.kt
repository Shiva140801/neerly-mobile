package com.neerly.mobile.feature.order

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.repo.CustomerRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class OrderTrackingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private fun order(status: String) = OrderResponse(
        id = "o1", orderNumber = "NEE-26115-0001", customerId = "c1",
        vendorId = "v1", driverId = null, status = status,
        subtotal = BigDecimal("100"), deliveryFee = BigDecimal("30"),
        surgeAmount = null, taxAmount = BigDecimal.ZERO,
        discount = null, depositAmount = null, totalAmount = BigDecimal("130"),
        placedAt = "2026-04-25T09:00:00Z"
    )

    private fun vm(savedOrderId: String = "o1") =
        OrderTrackingViewModel(repo, SavedStateHandle(mapOf("orderId" to savedOrderId)))

    @Test
    fun init_firstFetch_populatesOrder() = runTest(dispatcher) {
        coEvery { repo.order("o1") } returns order("PREPARING")
        val tracking = vm()
        advanceUntilIdle()
        // Coroutine is in the delay; cancel to stop the polling loop.
        assertEquals("PREPARING", tracking.state.value.order?.status)
    }

    @Test
    fun terminal_DELIVERED_stopsPolling() = runTest(dispatcher) {
        coEvery { repo.order("o1") } returns order("DELIVERED")
        val tracking = vm()
        advanceUntilIdle()
        // Advance well past the next would-be poll — the loop should've ended.
        advanceTimeBy(60_000L)
        assertEquals("DELIVERED", tracking.state.value.order?.status)
    }

    @Test
    fun init_fetchFailure_setsError() = runTest(dispatcher) {
        coEvery { repo.order(any()) } throws RuntimeException("down")
        val tracking = vm()
        advanceUntilIdle()
        assertEquals("down", tracking.state.value.error)
    }
}
