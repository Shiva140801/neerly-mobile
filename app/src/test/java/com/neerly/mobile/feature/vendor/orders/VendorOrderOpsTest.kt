package com.neerly.mobile.feature.vendor.orders

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.dto.DriverOfferResult
import com.neerly.mobile.data.repo.VendorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class VendorOrderOpsTest {
    private val dispatcher = StandardTestDispatcher()
    private val ops: VendorOpsApi = mockk()
    private val repo: VendorRepository = mockk(relaxed = true)
    private val prefs: VendorAlertPrefsStore = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun o(status: String, driver: String? = null) = VendorOrderView(id = "o1", orderNumber = "NEE-1", status = status,
        placedAt = "2026-11-07T09:00:00Z", acceptDeadlineAt = "2026-11-07T09:03:00Z", driverId = driver,
        orderValue = BigDecimal("120"), yourEarning = BigDecimal("0"), cancellationFee = BigDecimal("25"), tripPay = BigDecimal("25"))

    @Test fun tabs_followServerStatuses() {
        assertEquals(OrderTab.New, tabOf("VENDOR_ASSIGNED"))
        assertEquals(OrderTab.Preparing, tabOf("PREPARING"))
        assertEquals(OrderTab.Out, tabOf("ARRIVING"))
        assertEquals(OrderTab.Done, tabOf("CANCELLED"))
    }

    @Test fun countdown_neverGoesNegative() {
        val now = Instant.parse("2026-11-07T09:02:30Z")
        assertEquals(30L, secondsLeft("2026-11-07T09:03:00Z", now))
        assertEquals(0L, secondsLeft("2026-11-07T09:03:00Z", now.plusSeconds(120)))
        assertEquals("2:41", mmss(161))
    }

    @Test fun newOrder_customerCancelsWhileRinging_showsWithdrawnNotTimedOut() = runTest(dispatcher) {
        coEvery { ops.order("o1") } returnsMany listOf(o("VENDOR_ASSIGNED"), o("CANCELLED"))
        coEvery { ops.dashboard() } throws RuntimeException("offline")
        val vm = VendorNewOrderViewModel(ops, repo, prefs, SavedStateHandle(mapOf("orderId" to "o1")), com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        assertEquals(OfferStage.Offer, vm.state.value.stage)
        val watch = launch { vm.watchWhileRinging() }
        advanceTimeBy(VendorNewOrderViewModel.RECHECK_MS + 1)
        advanceUntilIdle()
        assertEquals(OfferStage.Withdrawn, vm.state.value.stage)
        watch.cancel()
    }

    @Test fun newOrder_cancelledFoundAfterClockRanOut_isWithdrawn() = runTest(dispatcher) {
        coEvery { ops.order("o1") } returnsMany listOf(o("VENDOR_ASSIGNED"), o("CANCELLED"))
        coEvery { ops.dashboard() } throws RuntimeException("offline")
        val vm = VendorNewOrderViewModel(ops, repo, prefs, SavedStateHandle(mapOf("orderId" to "o1")), com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        vm.expired()
        advanceUntilIdle()
        assertEquals(OfferStage.Withdrawn, vm.state.value.stage)
    }

    @Test fun newOrder_declineNeedsAReason_andSendsTheLabel() = runTest(dispatcher) {
        coEvery { ops.order("o1") } returns o("VENDOR_ASSIGNED")
        val vm = VendorNewOrderViewModel(ops, repo, prefs, SavedStateHandle(mapOf("orderId" to "o1")), com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        vm.startDecline()
        assertFalse(vm.state.value.canDecline)
        vm.pick(DeclineReason.Other)
        assertFalse(vm.state.value.canDecline)  // "Something else" needs words
        vm.pick(DeclineReason.Far)
        assertTrue(vm.state.value.canDecline)
        vm.decline(); advanceUntilIdle()
        coVerify { repo.reject("o1", "Too far for us today") }
        assertEquals(OfferStage.Declined, vm.state.value.stage)
    }

    @Test fun newOrder_alreadyGone_showsTimedOut() = runTest(dispatcher) {
        coEvery { ops.order("o1") } returns o("VENDOR_ASSIGNED")
        val vm = VendorNewOrderViewModel(ops, repo, prefs, SavedStateHandle(mapOf("orderId" to "o1")), com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        coEvery { ops.order("o1") } throws RuntimeException("404")
        vm.expired(); advanceUntilIdle()
        assertEquals(OfferStage.TimedOut, vm.state.value.stage)
    }

    @Test fun detail_packedOrder_sendsWithThePickedDriver() = runTest(dispatcher) {
        coEvery { ops.order("o1") } returns o("VENDOR_ACCEPTED")
        coEvery { ops.team() } returns listOf(VendorTeamLive("d1", "Imran", liveStatus = "ON_SHIFT"), VendorTeamLive("d2", "Suresh", liveStatus = "ON_DROP"))
        val vm = VendorOrderDetailViewModel(repo, ops, SavedStateHandle(mapOf("orderId" to "o1")), io.mockk.mockk(relaxed = true), com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        assertTrue(vm.state.value.needsDriver)
        assertEquals("d1", vm.state.value.driversByReadiness.first().driverId)
        vm.pickDriver("d1"); vm.send(); advanceUntilIdle()
        coVerifyOrder { repo.ready("o1"); repo.dispatch("o1", "d1") }
    }

    @Test fun detail_noDriverOnShift_saysSo() = runTest(dispatcher) {
        coEvery { ops.order("o1") } returns o("PREPARING")
        coEvery { ops.team() } returns emptyList()
        coEvery { repo.offerDriver("o1") } returns DriverOfferResult(offered = false)
        val vm = VendorOrderDetailViewModel(repo, ops, SavedStateHandle(mapOf("orderId" to "o1")), io.mockk.mockk(relaxed = true), com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        vm.offerDriver(); advanceUntilIdle()
        assertTrue(vm.state.value.note!!.startsWith("No driver is on shift"))
    }

    @org.junit.Test
    fun confirmWithoutCode_needsAReason_andExplainsWhenTheCodeIsntLocked() = kotlinx.coroutines.test.runTest(dispatcher) {
        val override: CodeOverrideApi = io.mockk.mockk()
        io.mockk.coEvery { ops.order("o1") } returns io.mockk.mockk(relaxed = true)
        io.mockk.coEvery { override.codeOverride("o1", any()) } returns retrofit2.Response.error(409,
            okhttp3.ResponseBody.Companion.run { """{"error":{"code":"ORDER_CODE_NOT_LOCKED","message":"x"}}""".toResponseBody(null) })
        val vm = VendorOrderDetailViewModel(repo, ops, androidx.lifecycle.SavedStateHandle(mapOf("orderId" to "o1")), override, com.neerly.mobile.support.TestStrings)
        vm.confirmWithoutCode(" ")
        org.junit.Assert.assertNotNull(vm.state.value.error)
        io.mockk.coVerify(exactly = 0) { override.codeOverride(any(), any()) }
        vm.confirmWithoutCode("Called Priya, she has both jars"); testScheduler.advanceUntilIdle()
        org.junit.Assert.assertEquals("The code isn't locked — the driver should ask the customer for it.", vm.state.value.error)
    }
}
