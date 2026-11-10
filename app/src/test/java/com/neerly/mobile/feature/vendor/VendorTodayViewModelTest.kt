package com.neerly.mobile.feature.vendor

import com.neerly.mobile.data.dto.VendorBusinessStatus
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.vendor.dashboard.VendorTodayViewModel
import com.neerly.mobile.feature.vendor.orders.VendorAlertPrefsStore
import com.neerly.mobile.feature.vendor.orders.VendorDashboard
import com.neerly.mobile.feature.vendor.orders.VendorOpsApi
import com.neerly.mobile.feature.vendor.orders.VendorOrderView
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class VendorTodayViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: VendorRepository = mockk(relaxed = true)
    private val ops: VendorOpsApi = mockk()
    private val alerts: VendorAlertPrefsStore = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private fun order(id: String, status: String, driver: String? = null) = VendorOrderView(
        id = id, orderNumber = "NEE-1", customerFirstName = "Priya", status = status,
        placedAt = "2026-11-07T09:00:00Z", acceptDeadlineAt = java.time.Instant.now().plusSeconds(170).toString(),  // still live
        orderValue = BigDecimal("190"), yourEarning = BigDecimal("171"), driverId = driver
    )

    private fun stub(pending: List<VendorOrderView>, active: List<VendorOrderView> = emptyList()) {
        coEvery { ops.dashboard() } returns VendorDashboard(businessName = "Sri Balaji", ordersToday = 3, pendingEvents = 1, licencesToRenew = 2)
        coEvery { ops.orders("VENDOR_ASSIGNED", any()) } returns pending
        coEvery { ops.orders("ACTIVE", any()) } returns active
        coEvery { ops.businessStatus() } returns VendorBusinessStatus(true, null, null)
        coEvery { repo.subscriptionsToday() } returns emptyList()
        coEvery { repo.catalog() } returns emptyList()
    }

    @Test
    fun load_findsNewOfferToRing_andPackedOrdersNeedingADriver() = runTest(dispatcher) {
        stub(listOf(order("new1", "VENDOR_ASSIGNED")), listOf(order("p1", "PREPARING"), order("p2", "DISPATCHED", "d1")))
        every { alerts.alerted("new1") } returns false
        val vm = VendorTodayViewModel(repo, ops, alerts, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals("new1", s.alertFor)
        assertEquals(listOf("p1"), s.needDriver.map { it.id })
        assertEquals(1, s.dashboard?.pendingEvents)
        vm.alertShown("new1")
        verify { alerts.markAlerted("new1") }
        assertNull(vm.state.value.alertFor)
    }

    @Test
    fun load_offerAlreadyRung_doesNotRingAgain() = runTest(dispatcher) {
        stub(listOf(order("new1", "VENDOR_ASSIGNED")))
        every { alerts.alerted("new1") } returns true
        val vm = VendorTodayViewModel(repo, ops, alerts, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        assertNull(vm.state.value.alertFor)
    }

    @Test
    fun load_listFailure_isSurfaced_notShownAsQuiet() = runTest(dispatcher) {
        stub(emptyList())
        coEvery { ops.orders("VENDOR_ASSIGNED", any()) } throws RuntimeException("parse failure")
        val vm = VendorTodayViewModel(repo, ops, alerts, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun pause_sendsTheChosenLength_andResumeReopens() = runTest(dispatcher) {
        stub(emptyList())
        val five = com.neerly.mobile.feature.vendor.dashboard.PauseChoice.Hours(5)
        coEvery { repo.pause(five, "stock") } returns VendorBusinessStatus(false, "2026-11-07T15:00:00Z", "stock")
        coEvery { repo.pause(com.neerly.mobile.feature.vendor.dashboard.PauseChoice.RestOfToday, null) } returns
            VendorBusinessStatus(false, "2026-11-08T00:30:00Z", null)
        coEvery { repo.reopen() } returns VendorBusinessStatus(true, null, null)
        val vm = VendorTodayViewModel(repo, ops, alerts, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        vm.pause(five, "stock"); advanceUntilIdle()
        vm.pause(com.neerly.mobile.feature.vendor.dashboard.PauseChoice.RestOfToday, null); advanceUntilIdle()
        vm.resume(); advanceUntilIdle()
        coVerify { repo.pause(five, "stock"); repo.pause(com.neerly.mobile.feature.vendor.dashboard.PauseChoice.RestOfToday, null); repo.reopen() }
    }
}
