package com.neerly.mobile.feature.deposit

import com.neerly.mobile.data.dto.DepositResponse
import com.neerly.mobile.data.dto.ReturnRequest
import com.neerly.mobile.data.dto.ReturnResponse
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
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class DepositsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()

    private fun dep(id: String = "d1", status: String = "HELD") = DepositResponse(
        id = id, customerId = "c1", vendorId = "v1", vendorName = "Sri Ganesh",
        orderId = "o1", productName = "20L Cool",
        amount = BigDecimal("500"), status = status,
        heldAt = "2026-04-25T09:00:00Z",
        returnDeadline = "2026-04-27", returnedAt = null,
        lateFeeAccrued = BigDecimal.ZERO, gracePeriodEndAt = null
    )

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun load_groupsByStatus() = runTest(dispatcher) {
        coEvery { repo.deposits() } returns listOf(
            dep("d1", "HELD"), dep("d2", "RETURNED"), dep("d3", "FORFEIT")
        )
        val vm = DepositsViewModel(repo, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.held.size)
        assertEquals(1, vm.state.value.returned.size)
        assertEquals(1, vm.state.value.forfeited.size)
    }

    @Test
    fun scheduleReturn_postsAndRefreshes() = runTest(dispatcher) {
        coEvery { repo.deposits() } returns listOf(dep())
        coEvery { repo.addresses() } returns listOf(mockk<com.neerly.mobile.data.dto.AddressResponse> {
            io.mockk.every { id } returns "a1"; io.mockk.every { isPrimary } returns true
        })
        coEvery { repo.scheduleReturn(any()) } returns ReturnResponse(
            id = "r1", depositId = "d1", returnMode = "SCHEDULED_PICKUP", containerCount = 1,
            status = "REQUESTED"
        )

        val vm = DepositsViewModel(repo, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        vm.scheduleReturn("d1", "SCHEDULED_PICKUP")
        advanceUntilIdle()

        // The backend needs the jar count and, for a pickup, where to collect from.
        coVerify { repo.scheduleReturn(ReturnRequest(
            depositId = "d1", returnMode = "SCHEDULED_PICKUP", containerCount = 1, addressId = "a1")) }
    }

    @Test
    fun scheduleReturn_sendsTheChosenSlotAndMarksTheJarRequested() = runTest(dispatcher) {
        coEvery { repo.deposits() } returns listOf(dep())
        coEvery { repo.addresses() } returns listOf(mockk<com.neerly.mobile.data.dto.AddressResponse> {
            io.mockk.every { id } returns "a1"; io.mockk.every { isPrimary } returns true
        })
        coEvery { repo.scheduleReturn(any()) } returns mockk(relaxed = true)
        coEvery { repo.openReturns() } returns emptyList()
        val vm = DepositsViewModel(repo, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        coEvery { repo.openReturns() } returns listOf(ReturnResponse(id = "r1", depositId = "d1", returnMode = "SCHEDULED_PICKUP",
            containerCount = 1, status = "SCHEDULED"))
        vm.scheduleReturn("d1", "SCHEDULED_PICKUP", "2026-11-09T03:30:00Z")
        advanceUntilIdle()
        coVerify { repo.scheduleReturn(match { it.preferredSlot == "2026-11-09T03:30:00Z" && it.addressId == "a1" }) }
        assertEquals("SCHEDULED_PICKUP", vm.state.value.requested["d1"])
        assertEquals(false, vm.state.value.submitting)
    }

    @Test
    fun scheduleReturn_failure_keepsTheJarAndSaysWhy() = runTest(dispatcher) {
        coEvery { repo.deposits() } returns listOf(dep())
        coEvery { repo.scheduleReturn(any()) } throws RuntimeException("boom")
        val vm = DepositsViewModel(repo, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        vm.scheduleReturn("d1", "DROPOFF")
        advanceUntilIdle()
        org.junit.Assert.assertNotNull(vm.state.value.error)
        org.junit.Assert.assertTrue(vm.state.value.requested.isEmpty())
        assertEquals(1, vm.state.value.held.size)
    }

    @Test
    fun heldTotal_prefersRefundableAmount() = runTest(dispatcher) {
        coEvery { repo.deposits() } returns listOf(dep("d1").copy(refundableAmount = BigDecimal("450")), dep("d2"))
        val vm = DepositsViewModel(repo, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        assertEquals(0, BigDecimal("950").compareTo(vm.state.value.heldTotal))
    }

    @Test
    fun bookedReturn_comesFromTheServer_soItSurvivesARestart() = runTest(dispatcher) {
        coEvery { repo.deposits() } returns listOf(dep("d1"), dep("d2"))
        coEvery { repo.openReturns() } returns listOf(ReturnResponse(id = "r1", depositId = "d2", returnMode = "DROPOFF",
            containerCount = 1, status = "REQUESTED"))
        val vm = DepositsViewModel(repo, com.neerly.mobile.support.TestStrings)   // a fresh view model = a fresh app start
        advanceUntilIdle()
        assertEquals(mapOf("d2" to "DROPOFF"), vm.state.value.requested)
    }

    @Test
    fun refundCopy_followsTheCustomersSetting() = runTest(dispatcher) {
        coEvery { repo.deposits() } returns listOf(dep())
        coEvery { repo.openReturns() } returns emptyList()
        coEvery { repo.me() } returns mockk(relaxed = true) { io.mockk.every { refundDestination } returns "ORIGINAL" }
        val vm = DepositsViewModel(repo, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        org.junit.Assert.assertTrue(com.neerly.mobile.support.TestStrings.get(vm.state.value.refundWhere).startsWith("the way you paid"))
        org.junit.Assert.assertEquals("your Boondh wallet", com.neerly.mobile.support.TestStrings.get(DepositsUiState(refundDestination = "WALLET").refundWhere))
    }
}
