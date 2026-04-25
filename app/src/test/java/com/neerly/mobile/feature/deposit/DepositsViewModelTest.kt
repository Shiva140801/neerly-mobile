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
        orderId = "o1", productId = "p1", productName = "20L Cool",
        amount = BigDecimal("500"), status = status,
        heldAt = "2026-04-25T09:00:00Z",
        returnDeadline = "2026-04-27", returnedAt = null, forfeitedAt = null,
        lateFeeAccrued = BigDecimal.ZERO, gracePeriodEndAt = null
    )

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun load_groupsByStatus() = runTest(dispatcher) {
        coEvery { repo.deposits() } returns listOf(
            dep("d1", "HELD"), dep("d2", "RETURNED"), dep("d3", "FORFEITED")
        )
        val vm = DepositsViewModel(repo)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.held.size)
        assertEquals(1, vm.state.value.returned.size)
        assertEquals(1, vm.state.value.forfeited.size)
    }

    @Test
    fun scheduleReturn_postsAndRefreshes() = runTest(dispatcher) {
        coEvery { repo.deposits() } returns listOf(dep())
        coEvery { repo.scheduleReturn(any()) } returns ReturnResponse(
            id = "r1", depositId = "d1", mode = "SCHEDULED_PICKUP",
            status = "SCHEDULED", scheduledSlot = "2026-04-27T10:00:00Z",
            completedAt = null, refundAmount = null, photoS3Keys = emptyList()
        )

        val vm = DepositsViewModel(repo)
        advanceUntilIdle()
        vm.scheduleReturn("d1", "SCHEDULED_PICKUP")
        advanceUntilIdle()

        coVerify { repo.scheduleReturn(any<ReturnRequest>()) }
    }
}
