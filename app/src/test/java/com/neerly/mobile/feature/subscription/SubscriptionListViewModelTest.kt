package com.neerly.mobile.feature.subscription

import com.neerly.mobile.data.dto.SubscriptionResponse
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
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()

    private fun sub(id: String = "s1", status: String = "ACTIVE") = SubscriptionResponse(
        id = id, customerId = "c1", vendorId = "v1", vendorName = "Sri Ganesh",
        productId = "p1", productName = "20L Cool", frequency = "DAILY",
        daysOfWeek = emptyList(), quantity = 1, deliverySlot = "7-9AM",
        addressId = "a1", unitPrice = BigDecimal("95"), status = status,
        pausedFrom = null, pausedUntil = null, nextDeliveryDate = "2026-04-26",
        createdAt = "2026-04-25T09:00:00Z"
    )

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun load_groupsByStatus() = runTest(dispatcher) {
        coEvery { repo.subscriptions() } returns listOf(
            sub("s1", "ACTIVE"), sub("s2", "PAUSED"),
            sub("s3", "PENDING_MANDATE"), sub("s4", "CANCELLED")
        )
        val vm = SubscriptionListViewModel(repo)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.active.size)
        assertEquals(1, vm.state.value.paused.size)
        assertEquals(1, vm.state.value.pending.size)
        assertEquals(1, vm.state.value.ended.size)
    }

    @Test
    fun load_failure_setsError() = runTest(dispatcher) {
        coEvery { repo.subscriptions() } throws RuntimeException("offline")
        val vm = SubscriptionListViewModel(repo)
        advanceUntilIdle()
        assertEquals("offline", vm.state.value.error)
    }
}
