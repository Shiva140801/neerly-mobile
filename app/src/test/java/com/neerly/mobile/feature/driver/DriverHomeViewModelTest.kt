package com.neerly.mobile.feature.driver

import com.neerly.mobile.data.dto.DriverAssignment
import com.neerly.mobile.data.dto.DriverShiftResponse
import com.neerly.mobile.data.repo.DriverRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class DriverHomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: DriverRepository = mockk()
    private val ticker: DriverLocationTicker = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private fun activeShift() = DriverShiftResponse(
        id = "shift-1", driverId = "drv-1", vendorId = "v-1",
        startedAt = "2026-04-25T07:00:00Z", endedAt = null,
        deliveriesCount = 3, codCollected = BigDecimal("450")
    )

    private fun assignment(status: String) = DriverAssignment(
        orderId = "o1", orderNumber = "NEE-26115-0001",
        customerFirstName = "Priya", customerPhoneMask = "**12",
        deliveryAddress = "Tulip 301", deliveryLat = 17.44, deliveryLng = 78.39,
        product = "20L Cool × 2", paymentMethod = "UPI",
        codAmount = null, status = status, deliveryOtp = null, notes = null
    )

    @Test
    fun load_offDuty_emptyAssignments() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns null

        val vm = DriverHomeViewModel(repo, ticker)
        advanceUntilIdle()

        assertFalse(vm.state.value.isOnDuty)
        assertEquals(0, vm.state.value.assignments.size)
        assertNull(vm.state.value.activeAssignment)
    }

    @Test
    fun load_onDuty_pullsAssignments() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(assignment("DISPATCHED"))

        val vm = DriverHomeViewModel(repo, ticker)
        advanceUntilIdle()

        assertTrue(vm.state.value.isOnDuty)
        assertEquals(1, vm.state.value.assignments.size)
        assertNotNull(vm.state.value.activeAssignment)
    }

    @Test
    fun activeAssignment_onlyNonTerminalStates() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(
            assignment("DELIVERED"), assignment("ARRIVED")
        )

        val vm = DriverHomeViewModel(repo, ticker)
        advanceUntilIdle()

        assertEquals("ARRIVED", vm.state.value.activeAssignment?.status)
    }

    @Test
    fun locationLoop_pingsWithActiveOrderWhilePermitted_thenStopsOnRevoke() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(assignment("DISPATCHED"))
        val fix = mockk<android.location.Location>(relaxed = true)
        every { fix.latitude } returns 17.44
        every { fix.longitude } returns 78.39
        every { fix.hasBearing() } returns false
        every { fix.hasSpeed() } returns false
        every { fix.hasAccuracy() } returns false
        every { ticker.freshFix() } returns fix
        every { ticker.isParked() } returns false
        every { ticker.batteryPct() } returns 80
        coEvery { repo.ping(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        val vm = DriverHomeViewModel(repo, ticker)
        advanceUntilIdle()
        vm.onLocationPermission(true)
        // Run the first loop iteration only — advanceUntilIdle would never return
        // against an endless ping loop.
        advanceTimeBy(100)

        coVerify(atLeast = 1) {
            repo.ping(17.44, 78.39, null, null, null, 80, "o1")
        }

        vm.onLocationPermission(false)
        advanceUntilIdle()
        verify { ticker.stop() }
    }
}
