package com.neerly.mobile.feature.driver

import com.neerly.mobile.data.dto.DriverAssignment
import com.neerly.mobile.data.dto.DriverShiftResponse
import com.neerly.mobile.data.repo.DriverRepository
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

        val vm = DriverHomeViewModel(repo)
        advanceUntilIdle()

        assertFalse(vm.state.value.isOnDuty)
        assertEquals(0, vm.state.value.assignments.size)
        assertNull(vm.state.value.activeAssignment)
    }

    @Test
    fun load_onDuty_pullsAssignments() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(assignment("DISPATCHED"))

        val vm = DriverHomeViewModel(repo)
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

        val vm = DriverHomeViewModel(repo)
        advanceUntilIdle()

        assertEquals("ARRIVED", vm.state.value.activeAssignment?.status)
    }
}
