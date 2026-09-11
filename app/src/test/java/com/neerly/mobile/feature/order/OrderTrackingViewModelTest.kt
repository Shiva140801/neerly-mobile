package com.neerly.mobile.feature.order

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.core.util.ConnectivityObserver
import com.neerly.mobile.data.repo.CustomerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
    private val connectivity: ConnectivityObserver = mockk<ConnectivityObserver>(relaxed = true).also {
        every { it.isOnline } returns MutableStateFlow(true)
    }

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
        OrderTrackingViewModel(repo, connectivity, SavedStateHandle(mapOf("orderId" to savedOrderId)))

    @Test
    fun init_firstFetch_populatesOrder() = runTest(dispatcher) {
        coEvery { repo.order("o1") } returns order("PREPARING")
        val tracking = vm()
        // runCurrent, not advanceUntilIdle: PREPARING is not terminal, so the poll
        // loop never ends, and draining the scheduler runs it until the JVM dies.
        // That is what was OOMing the whole suite — the class that reported the
        // failure was whichever one shared the poisoned worker.
        runCurrent()
        assertEquals("PREPARING", tracking.state.value.order?.status)
        endPolling()
    }

    /**
     * Steers the next poll to DELIVERED so the loop breaks before the test
     * returns. A VM left polling outlives its test: runTest's teardown drains
     * the scheduler, every drained iteration is another recorded MockK call,
     * and a few leaked loops OOM the shared worker JVM.
     */
    private fun kotlinx.coroutines.test.TestScope.endPolling() {
        coEvery { repo.order("o1") } returns order("DELIVERED")
        advanceTimeBy(OrderTrackingViewModel.POLL_INTERVAL_MS + 1)
        runCurrent()
    }

    @Test
    fun nonTerminal_keepsPolling_onceEachInterval() = runTest(dispatcher) {
        coEvery { repo.order("o1") } returns order("PREPARING")
        val tracking = vm()
        runCurrent()
        advanceTimeBy(OrderTrackingViewModel.POLL_INTERVAL_MS + 1)
        runCurrent()

        // Once on init, once when the interval elapsed — polling continues while the
        // order is live, which is the whole point of the tracking screen.
        coVerify(exactly = 2) { repo.order("o1") }
        assertEquals("PREPARING", tracking.state.value.order?.status)
        endPolling()
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

    private fun tracking(status: String = "DISPATCHED") = com.neerly.mobile.data.dto.OrderTrackingResponse(
        orderId = "o1", orderNumber = "NEE-26115-0001", status = status,
        deliverBy = "2026-04-25T11:00:00Z", driverName = "Ramesh", vehicle = "Tata Ace",
        driverLat = 17.43, driverLng = 78.34, driverHeadingDeg = 118.0,
        driverLocationAt = "2026-04-25T09:30:00Z",
        dropLat = 17.44, dropLng = 78.39
    )

    @Test
    fun inFlight_fetchesTrackingAndPollsAtLiveCadence() = runTest(dispatcher) {
        coEvery { repo.order("o1") } returns order("DISPATCHED")
        coEvery { repo.orderTracking("o1") } returns tracking()
        val t = vm()
        runCurrent()

        assertEquals(17.43, t.state.value.tracking?.driverLat)

        // In flight the poll runs at the 5 s live cadence, not the 15 s idle one.
        advanceTimeBy(OrderTrackingViewModel.LIVE_POLL_INTERVAL_MS + 1)
        runCurrent()
        coVerify(exactly = 2) { repo.orderTracking("o1") }
        endPolling()
    }

    @Test
    fun notInFlight_neverCallsTracking() = runTest(dispatcher) {
        coEvery { repo.order("o1") } returns order("PREPARING")
        val t = vm()
        runCurrent()
        coVerify(exactly = 0) { repo.orderTracking(any()) }
        assertEquals(null, t.state.value.tracking)
        endPolling()
    }

    @Test
    fun init_fetchFailure_setsError() = runTest(dispatcher) {
        coEvery { repo.order(any()) } throws RuntimeException("down")
        val tracking = vm()
        advanceUntilIdle()
        // Customer-facing copy, not `Throwable.message`.
        assertEquals("Couldn't refresh this order. Please try again.", tracking.state.value.error)
        assertFalse(tracking.state.value.error!!.contains("down"))
    }
}
