package com.neerly.mobile.feature.vendor

import com.neerly.mobile.data.dto.VendorOrderResponse
import com.neerly.mobile.data.dto.VendorTodaySummary
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.vendor.dashboard.VendorTodayViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class VendorTodayViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: VendorRepository = mockk()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private fun summary() = VendorTodaySummary(
        ordersToday = 12, deliveredToday = 9, activeOrders = 2, pendingAccept = 1,
        grossToday = BigDecimal("1140"), earningToday = BigDecimal("1026"),
        avgRating = BigDecimal("4.7"), strikesLast30d = 0
    )

    private fun order(status: String = "PLACED") = VendorOrderResponse(
        id = "o1", orderNumber = "NEE-26115-0042", customerId = "c1",
        customerFirstName = "Priya", customerPhoneMask = "**12",
        deliveryAddress = "Tulip 301", pincode = "500081",
        distanceKm = BigDecimal("1.4"),
        items = emptyList(),
        orderValue = BigDecimal("190"), yourEarning = BigDecimal("171"),
        paymentStatus = "UPI_PAID", slotRequested = "NOW", notes = null,
        status = status, placedAt = "2026-04-25T09:00:00Z",
        acceptDeadlineAt = "2026-04-25T09:03:00Z"
    )

    @Test
    fun load_groupsByStatus() = runTest(dispatcher) {
        coEvery { repo.today() } returns summary()
        coEvery { repo.pendingOrders() } returns listOf(order("PLACED"))
        coEvery { repo.activeOrders() } returns listOf(order("PREPARING"))
        coEvery { repo.completedToday() } returns listOf(order("DELIVERED"))

        val vm = VendorTodayViewModel(repo)
        advanceUntilIdle()

        assertNotNull(vm.state.value.summary)
        assertEquals(1, vm.state.value.pending.size)
        assertEquals(1, vm.state.value.active.size)
        assertEquals(1, vm.state.value.completed.size)
    }

    @Test
    fun load_partialFailure_ignoresMissingSegments() = runTest(dispatcher) {
        coEvery { repo.today() } returns summary()
        coEvery { repo.pendingOrders() } throws RuntimeException("filter not supported")
        coEvery { repo.activeOrders() } returns emptyList()
        coEvery { repo.completedToday() } returns emptyList()

        val vm = VendorTodayViewModel(repo)
        advanceUntilIdle()

        assertNotNull(vm.state.value.summary)
        assertEquals(0, vm.state.value.pending.size)
    }

    @Test
    fun load_summaryFailure_setsError() = runTest(dispatcher) {
        coEvery { repo.today() } throws RuntimeException("offline")
        coEvery { repo.pendingOrders() } returns emptyList()
        coEvery { repo.activeOrders() } returns emptyList()
        coEvery { repo.completedToday() } returns emptyList()

        val vm = VendorTodayViewModel(repo)
        advanceUntilIdle()

        assertEquals("offline", vm.state.value.error)
    }
}
