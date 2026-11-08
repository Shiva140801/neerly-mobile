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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class DriverHomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: DriverRepository = mockk()
    private val ticker: DriverLocationTicker = mockk(relaxed = true)
    private val ops: DriverOpsApi = mockk(relaxed = true)
    private val photos: PhotoUploader = mockk()
    private val platform: DriverPlatformApi = mockk(relaxed = true)
    private val jobs = object : DriverJobStore {
        val loaded = mutableSetOf<String>()
        override fun isLoaded(orderId: String) = orderId in loaded
        override fun markLoaded(orderId: String) { loaded += orderId }
        override fun keepOnly(orderIds: Set<String>) { loaded.retainAll(orderIds) }
    }
    private fun vm() = DriverHomeViewModel(repo, ticker, ops, photos, jobs, platform, com.neerly.mobile.support.TestStrings)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { ops.vendors() } returns emptyList()
        coEvery { ops.today() } returns DriverToday()
    }
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

        val vm = vm()
        advanceUntilIdle()

        assertFalse(vm.state.value.isOnDuty)
        assertEquals(0, vm.state.value.assignments.size)
        assertNull(vm.state.value.activeAssignment)
    }

    @Test
    fun load_onDuty_pullsAssignments() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(assignment("DISPATCHED"))

        val vm = vm()
        advanceUntilIdle()

        assertTrue(vm.state.value.isOnDuty)
        assertEquals(1, vm.state.value.assignments.size)
        assertNotNull(vm.state.value.activeAssignment)
    }

    @Test
    fun activeAssignment_onlyNonTerminalStates() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(
            assignment("DELIVERED"), assignment("ARRIVING")
        )

        val vm = vm()
        advanceUntilIdle()

        assertEquals("ARRIVING", vm.state.value.activeAssignment?.status)
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

        val vm = vm()
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

    private fun cashJob(status: String) = assignment(status).copy(paymentMethod = "COD", codAmount = BigDecimal("195"), emptiesExpected = 2)

    @Test
    fun step_followsTheDrop_loadThenRideThenHandOver() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(assignment("DISPATCHED"))
        coEvery { repo.startDelivery("o1") } returns assignment("DISPATCHED")
        val vm = vm(); advanceUntilIdle()
        assertEquals(DropStep.LOAD, vm.state.value.step)
        vm.loaded("o1"); advanceUntilIdle()
        assertEquals(DropStep.RIDE, vm.state.value.step)
        coVerify { repo.startDelivery("o1") }
        coEvery { repo.assignments() } returns listOf(assignment("ARRIVING"))
        vm.refresh(); advanceUntilIdle()
        assertEquals(DropStep.HAND_OVER, vm.state.value.step)
    }

    @Test
    fun deliver_withoutAnUploadedPhoto_isNotSent() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(assignment("ARRIVING"))
        val vm = vm(); advanceUntilIdle()
        vm.deliver("659082", 0, null, null); advanceUntilIdle()
        assertNotNull(vm.state.value.deliverError)
        coVerify(exactly = 0) { platform.deliver(any(), any()) }
    }

    @Test
    fun deliver_sendsTheRealPhotoKeyAndAValidCashSettlement_neverThePlaceholder() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(cashJob("ARRIVING"))
        val file = java.io.File.createTempFile("door", ".jpg")
        coEvery { photos.upload(file) } returns PhotoUploadResult.Uploaded("order-proof/abc.jpg")
        coEvery { platform.deliver(any(), any()) } returns retrofit2.Response.success(Unit)
        val vm = vm(); advanceUntilIdle()
        vm.photoTaken(file, ok = true); advanceUntilIdle()
        vm.deliver("659082", 2, "SOMETHING_ODD", BigDecimal("195")); advanceUntilIdle()
        coVerify { platform.deliver("o1", match { it.otp == "659082" && it.photoS3Key == "order-proof/abc.jpg" &&
            it.codCollected == BigDecimal("195") && it.emptiesCollected == 2 && it.codSettlement == "CASH" }) }
        coVerify(exactly = 0) { platform.deliver(any(), match { it.photoS3Key == "delivery-photo-pending" }) }
        assertNotNull(vm.state.value.done)
    }

    @Test
    fun deliver_wrongLengthCode_isCaughtBeforeTheServer() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(assignment("ARRIVING"))
        val file = java.io.File.createTempFile("door", ".jpg")
        coEvery { photos.upload(file) } returns PhotoUploadResult.Uploaded("k")
        val vm = vm(); advanceUntilIdle()
        vm.photoTaken(file, true); advanceUntilIdle()
        vm.deliver("3947", 0, null, null); advanceUntilIdle()
        assertNotNull(vm.state.value.deliverError)
        coVerify(exactly = 0) { platform.deliver(any(), any()) }
    }

    @Test
    fun photo_storageDown_isItsOwnState() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns listOf(assignment("ARRIVING"))
        val file = java.io.File.createTempFile("door", ".jpg")
        coEvery { photos.upload(file) } returns PhotoUploadResult.StorageDown
        val vm = vm(); advanceUntilIdle()
        vm.photoTaken(file, true); advanceUntilIdle()
        assertEquals(DoorPhoto.StorageDown, vm.state.value.photo)
    }

    @Test
    fun startShift_severalShops_asksWhichThenStartsThatOne() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns null
        coEvery { ops.vendors() } returns listOf(DriverShop(id = "v1", name = "Sri Balaji"), DriverShop(id = "v2", name = "Pure Drops"))
        coEvery { ops.startShiftFor("v2", any()) } returns activeShift()
        val vm = vm(); advanceUntilIdle()
        vm.startShift(); advanceUntilIdle()
        assertTrue(vm.state.value.choosingShop)
        vm.startShift("v2"); advanceUntilIdle()
        coVerify { ops.startShiftFor("v2", any()) }
        coVerify(exactly = 0) { repo.startShift() }
    }

    @Test
    fun startShift_noShopListYet_fallsBackToThePlainStart() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns null
        coEvery { repo.startShift() } returns activeShift()
        val vm = vm(); advanceUntilIdle()
        vm.startShift(); advanceUntilIdle()
        coVerify { repo.startShift() }
    }

    @Test
    fun endShift_showsDropsStillOut_thenHandsThemBack() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns emptyList()
        coEvery { platform.endCheck() } returns EndCheck(listOf(PendingDrop("o1", "NEE-1", "DISPATCHED")), BigDecimal("195"), false)
        coEvery { platform.finish(any()) } returns FinishShiftResult(handedBack = 1)
        val vm = vm(); advanceUntilIdle()
        vm.askEndShift(); advanceUntilIdle()
        assertEquals(1, vm.state.value.endCheck?.pendingDrops?.size)
        vm.finishShift(handBack = true); advanceUntilIdle()
        coVerify { platform.finish(match { it.handBack }) }
        assertNull(vm.state.value.endCheck)
    }

    @Test
    fun onBreak_stopsOfferPolling() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns emptyList()
        coEvery { platform.shiftState() } returns DriverShiftState("shift-1", state = "ON_BREAK")
        val vm = vm(); vm.setVisible(true); advanceUntilIdle()
        assertTrue(vm.state.value.onBreak)
        coVerify(exactly = 0) { repo.pendingOffer() }
    }

    @Test
    fun wrongCode_showsTriesLeft_andALockShowsUntilWhen() {
        val vm = DriverHomeViewModel(repo, ticker, ops, photos, jobs, platform, com.neerly.mobile.support.TestStrings)
        val wrong = retrofit2.HttpException(retrofit2.Response.error<Unit>(400,
            """{"error":{"code":"ORDER_OTP_MISMATCH","message":"x","details":{"attemptsLeft":2}}}""".toResponseBody(null)))
        val s1 = vm.deliverFailed(DriverHomeUiState(), wrong)
        assertEquals(2, s1.codeAttemptsLeft)
        assertTrue(s1.deliverError!!.contains("2 tries left"))
        val lockedUntil = java.time.Instant.now().plusSeconds(600).toString()
        val locked = retrofit2.HttpException(retrofit2.Response.error<Unit>(423,
            """{"error":{"code":"ORDER_OTP_LOCKED","message":"x","details":{"attemptsLeft":0,"lockedUntil":"$lockedUntil"}}}""".toResponseBody(null)))
        val s2 = vm.deliverFailed(DriverHomeUiState(), locked)
        assertTrue(s2.codeLocked)
    }

    @Test
    fun notHomeTooSoon_saysHowMuchLonger() {
        val vm = DriverHomeViewModel(repo, ticker, ops, photos, jobs, platform, com.neerly.mobile.support.TestStrings)
        val err = ServerError.parse(422, """{"error":{"code":"DRIVER_NOT_HOME_TOO_SOON","message":"x",
            "details":{"callsNeeded":2,"callsMade":1,"waitSecondsNeeded":300,"waitedSeconds":120}}}""")
        assertEquals("Not yet — try calling 1 more time and wait 3 more minutes before you mark them not home.", vm.notHomeProblem(err))
    }

    @Test
    fun offer_thatVanishesUnanswered_isMissed_butAnAnsweredOneIsNot() = runTest(dispatcher) {
        coEvery { repo.currentShift() } returns activeShift()
        coEvery { repo.assignments() } returns emptyList()
        val vm = vm(); advanceUntilIdle()
        val offer = com.neerly.mobile.data.dto.DriverOffer("o9", BigDecimal("35"), "2026-11-07T11:53:13Z", "2026-11-07T11:53:58Z", 45)
        vm.onOffer(offer); vm.onOffer(null)
        assertEquals(BigDecimal("35"), vm.state.value.missed?.fee)
        vm.dismissMissed()
        coEvery { repo.passOffer("o9") } returns Unit
        vm.onOffer(offer); vm.passOffer(); vm.onOffer(null); advanceUntilIdle()
        assertNull(vm.state.value.missed)
    }

    @Test
    fun kmBetween_isAboutRight() {
        val d = kmBetween(17.4401 to 78.3489, 17.4405 to 78.3492)
        assertTrue(d in 0.03..0.08)
    }
}
