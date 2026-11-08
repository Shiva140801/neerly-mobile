package com.neerly.mobile.feature.driver

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class DriverMoreViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val api: DriverPlatformApi = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val me = DriverMe(driverId = "d1", name = "Suresh W.", verification = "ACTION_NEEDED")

    @Test
    fun badUpi_isRefusedBeforeCallingTheServer() = runTest(dispatcher) {
        coEvery { api.me() } returns me
        val vm = DriverMeViewModel(api, com.neerly.mobile.support.TestStrings); advanceUntilIdle()
        vm.saveUpi("not a upi"); advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        coVerify(exactly = 0) { api.setPayoutUpi(any()) }
        coEvery { api.setPayoutUpi(PayoutUpiBody("suresh@ybl")) } returns me.copy(payoutUpi = "suresh@ybl")
        vm.saveUpi(" suresh@ybl "); advanceUntilIdle()
        assertEquals("suresh@ybl", vm.state.value.me?.payoutUpi)
        assertTrue(vm.state.value.upiSaved)
    }

    @Test
    fun logoutWithCashOrDropsOut_isHeldBack() = runTest(dispatcher) {
        coEvery { api.me() } returns me
        coEvery { api.logoutCheck() } returns LogoutCheck(cashHeld = BigDecimal("195"), pendingDrops = 1, onShift = true, canLogout = false)
        val vm = DriverMeViewModel(api, com.neerly.mobile.support.TestStrings); advanceUntilIdle()
        vm.checkLogout(); advanceUntilIdle()
        assertFalse(vm.state.value.logoutCheck!!.canLogout)
    }

    @Test
    fun vehicleWithoutPlate_isRefusedUnlessItsACycle() = runTest(dispatcher) {
        coEvery { api.vehicle() } returns DriverVehicle()
        coEvery { api.saveVehicle(any()) } answers { firstArg() }
        val vm = DriverVehicleViewModel(api, com.neerly.mobile.support.TestStrings); advanceUntilIdle()
        vm.save("BIKE", "", ""); advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        vm.save("CYCLE", "", ""); advanceUntilIdle()
        coVerify { api.saveVehicle(DriverVehicle("CYCLE", null, null)) }
    }

    @Test
    fun sos_sendsWithTheFix_sharesLocation_andResolves() = runTest(dispatcher) {
        val ticker: DriverLocationTicker = mockk(relaxed = true)
        val fix: android.location.Location = mockk { every { latitude } returns 17.44; every { longitude } returns 78.35 }
        every { ticker.freshFix() } returns fix
        coEvery { api.sos(any()) } returns DriverSos("s1", notified = 2)
        val vm = DriverSosViewModel(api, ticker, com.neerly.mobile.support.TestStrings)
        vm.send(); runCurrentAndAdvance()
        coVerify { api.sos(SosBody(17.44, 78.35, null)) }
        advanceTimeBy(DriverSosViewModel.SHARE_MS + 1)
        coVerify { api.sosLocation("s1", SosLocationBody(17.44, 78.35)) }
        vm.safe(); runCurrentAndAdvance()
        assertTrue(vm.state.value.resolved)
    }

    private fun kotlinx.coroutines.test.TestScope.runCurrentAndAdvance() { testScheduler.runCurrent() }

    @Test
    fun pickup_completesWithThePhotoTakenForIt() = runTest(dispatcher) {
        val photos: PhotoUploader = mockk()
        val f = java.io.File("x.jpg")
        coEvery { api.pickups() } returns listOf(DriverPickup("r1", containerCount = 2, status = "EN_ROUTE"))
        coEvery { photos.upload(f, "ORDER_PROOF", "pickup") } returns PhotoUploadResult.Uploaded("order-proof/p.jpg")
        coEvery { api.completePickup("r1", any()) } returns DriverPickup("r1", containerCount = 2, status = "COMPLETED")
        val vm = DriverPickupsViewModel(api, photos, com.neerly.mobile.support.TestStrings); advanceUntilIdle()
        vm.photoTaken("r1", f, true); advanceUntilIdle()
        vm.complete("r1", "one cap missing"); advanceUntilIdle()
        coVerify { api.completePickup("r1", CompletePickupBody("order-proof/p.jpg", "one cap missing")) }
        assertEquals("COMPLETED", vm.state.value.pickups.single().status)
    }

    @Test
    fun week_cantGoPastThisWeek() = runTest(dispatcher) {
        coEvery { api.week(any()) } returns DriverWeek("2026-11-02", "2026-11-08")
        val vm = DriverWeekViewModel(api, com.neerly.mobile.support.TestStrings); advanceUntilIdle()
        val start = vm.state.value.weekStart
        vm.next(); advanceUntilIdle()
        assertEquals(start, vm.state.value.weekStart)
        vm.previous(); advanceUntilIdle()
        assertEquals(start.minusWeeks(1), vm.state.value.weekStart)
    }
}
