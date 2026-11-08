package com.neerly.mobile.feature.subscription

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.dto.ModifySubscriptionRequest
import com.neerly.mobile.data.dto.SubscriptionInstanceResponse
import com.neerly.mobile.data.dto.SubscriptionResponse
import com.neerly.mobile.data.repo.CustomerRepository
import io.mockk.coEvery
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk(relaxed = true)
    private val extras: SubscriptionExtrasApi = mockk()

    private fun sub(status: String = "ACTIVE", qty: Int = 2) = SubscriptionResponse(
        id = "s1", customerId = "c1", vendorId = "v1", vendorName = "Sri Balaji", productId = "p1", productName = "20L Plain",
        frequency = "ALT_DAY", quantityPerDelivery = qty, slotStart = "06:00:00", slotEnd = "09:00:00", startDate = "2026-02-01",
        paymentMethod = "WALLET", unitPrice = BigDecimal("60"), status = status, nextDeliveryDate = "2026-11-08"
    )
    private fun inst(id: String, date: String, status: String) =
        SubscriptionInstanceResponse(id, "s1", date, status, BigDecimal("120"))

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm(): SubscriptionDetailViewModel {
        coEvery { repo.subscription("s1") } returns sub()
        return SubscriptionDetailViewModel(repo, extras, SavedStateHandle(mapOf("subscriptionId" to "s1")), com.neerly.mobile.support.TestStrings)
    }

    @Test
    fun load_splitsCalendarIntoComingUpAndLately() = runTest(dispatcher) {
        coEvery { extras.instances("s1") } returns listOf(
            inst("a", "2026-11-08", "SCHEDULED"), inst("b", "2026-11-10", "SKIPPED"), inst("c", "2026-11-12", "SCHEDULED"),
            inst("d", "2026-11-06", "DELIVERED"), inst("e", "2026-11-07", "SCHEDULED")
        )
        val vm = vm(); advanceUntilIdle()
        val coming = vm.state.value.comingUp(LocalDate.parse("2026-11-08"))
        // Before the first changeable day isn't offered; skipped days stay visible.
        assertEquals(listOf("a", "b", "c"), coming.map { it.id })
        assertEquals(listOf("d"), vm.state.value.lately.map { it.id })
    }

    @Test
    fun calendarFailure_stillShowsTheSubscription() = runTest(dispatcher) {
        coEvery { extras.instances("s1") } throws RuntimeException("404")
        val vm = vm(); advanceUntilIdle()
        assertNotNull(vm.state.value.subscription)
        assertEquals(null, vm.state.value.error)
    }

    @Test
    fun resume_updatesStatus() = runTest(dispatcher) {
        coEvery { extras.instances("s1") } returns emptyList()
        coEvery { extras.resume("s1") } returns sub("ACTIVE")
        coEvery { repo.subscription("s1") } returns sub("PAUSED")
        val vm = SubscriptionDetailViewModel(repo, extras, SavedStateHandle(mapOf("subscriptionId" to "s1")), com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        vm.resume(); advanceUntilIdle()
        assertEquals("ACTIVE", vm.state.value.subscription!!.status)
    }

    @Test
    fun change_sendsOnlyWhatChanged() = runTest(dispatcher) {
        coEvery { extras.instances("s1") } returns emptyList()
        coEvery { extras.modify("s1", any()) } returns sub(qty = 3)
        val vm = vm(); advanceUntilIdle()
        vm.change(3, null, null); advanceUntilIdle()
        coVerify { extras.modify("s1", ModifySubscriptionRequest(3, null, null)) }
        assertEquals(3, vm.state.value.subscription!!.quantity)
    }

    @Test
    fun pause_success_showsPausedConfirmation_failure_showsServerCopy() = runTest(dispatcher) {
        coEvery { extras.instances("s1") } returns emptyList()
        coEvery { repo.pauseSubscription("s1", any()) } returns sub("PAUSED")
        val vm = vm(); advanceUntilIdle()
        vm.pause("2026-11-12", "2026-11-16", null); advanceUntilIdle()
        assertTrue(vm.state.value.done is Done.Paused)

        coEvery { repo.cancelSubscription("s1", any()) } throws RuntimeException("boom")
        vm.dismissDone(); vm.cancel(null); advanceUntilIdle()
        assertEquals(null, vm.state.value.done)
        assertNotNull(vm.state.value.error)
        assertEquals(false, vm.state.value.working)
    }

    @Test
    fun pauseUntilIResume_sendsNoEndDate() = runTest(dispatcher) {
        coEvery { extras.instances("s1") } returns emptyList()
        coEvery { repo.pauseSubscription("s1", any()) } returns sub("PAUSED")
        val vm = vm(); advanceUntilIdle()
        vm.pause("2026-11-12", null, null); advanceUntilIdle()
        coVerify { repo.pauseSubscription("s1", match { it.pausedUntil == null && it.pausedFrom == "2026-11-12" }) }
        assertEquals(Done.Paused("2026-11-12", null), vm.state.value.done)
    }

    @Test
    fun endingAfterTheCutoff_saysTheLastDeliveryStillComes() = runTest(dispatcher) {
        coEvery { extras.instances("s1") } returns emptyList()
        coEvery { repo.cancelSubscription("s1", any()) } returns sub().copy(finalDeliveryDate = "2026-11-08", cancelledAt = "2026-11-07T16:00:00Z")
        val vm = vm(); advanceUntilIdle()
        vm.cancel(null); advanceUntilIdle()
        assertEquals(Done.Ended("2026-11-08"), vm.state.value.done)
        assertTrue(vm.state.value.subscription!!.ending)
    }

    @Test
    fun pausingAnEndingPlan_getsPlainWords() = runTest(dispatcher) {
        coEvery { extras.instances("s1") } returns emptyList()
        val body = """{"error":{"code":"SUBSCRIPTION_ENDING","message":"Subscription is ending"}}"""
            .toResponseBody("application/json".toMediaType())
        coEvery { repo.pauseSubscription("s1", any()) } throws retrofit2.HttpException(retrofit2.Response.error<Any>(409, body))
        val vm = vm(); advanceUntilIdle()
        vm.pause("2026-11-12", "2026-11-16", null); advanceUntilIdle()
        assertTrue(vm.state.value.error!!.contains("ending after its last delivery"))
    }
}
