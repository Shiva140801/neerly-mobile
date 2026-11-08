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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()
    private val extras: SubscriptionExtrasApi = mockk()

    private fun sub(id: String = "s1", status: String = "ACTIVE") = SubscriptionResponse(
        id = id, customerId = "c1", vendorId = "v1", vendorName = "Sri Ganesh",
        productId = "p1", productName = "20L Cool", frequency = "DAILY",
        daysOfWeek = emptyList(), quantityPerDelivery = 1, slotStart = "07:00:00", slotEnd = "09:00:00",
        startDate = "2026-04-25", paymentMethod = "UPI_AUTOPAY",
        addressId = "a1", unitPrice = BigDecimal("95"), status = status,
        pausedUntil = null, nextDeliveryDate = "2026-04-26"
    )

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun load_groupsByStatus() = runTest(dispatcher) {
        coEvery { repo.subscriptions() } returns listOf(
            sub("s1", "ACTIVE"), sub("s2", "PAUSED"),
            sub("s3", "PENDING_MANDATE"), sub("s4", "CANCELLED")
        )
        val vm = SubscriptionListViewModel(repo, extras, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.active.size)
        assertEquals(1, vm.state.value.paused.size)
        assertEquals(1, vm.state.value.pending.size)
        assertEquals(1, vm.state.value.ended.size)
    }

    @Test
    fun load_failure_setsError() = runTest(dispatcher) {
        coEvery { repo.subscriptions() } throws RuntimeException("offline")
        val vm = SubscriptionListViewModel(repo, extras, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        // Customer-facing copy, not `Throwable.message`.
        assertEquals("Couldn't load your subscriptions. Please try again.", vm.state.value.error)
        assertFalse(vm.state.value.error!!.contains("offline"))
    }

    @Test
    fun resume_swapsInTheResumedPlan() = runTest(dispatcher) {
        coEvery { repo.subscriptions() } returns listOf(sub("s2", "PAUSED"))
        coEvery { extras.resume("s2") } returns sub("s2", "ACTIVE")
        val vm = SubscriptionListViewModel(repo, extras, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        vm.resume("s2")
        advanceUntilIdle()
        assertEquals(1, vm.state.value.active.size)
        assertEquals(null, vm.state.value.resuming)
    }
}
