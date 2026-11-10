package com.neerly.mobile.feature.vendor.jars

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.feature.vendor.catalog.ShopProduct
import com.neerly.mobile.feature.vendor.catalog.ShopTemplate
import com.neerly.mobile.feature.vendor.catalog.VendorCatalogApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class VendorJarsViewModelsTest {
    private val jars: VendorJarsApi = mockk()
    private val catalog: VendorCatalogApi = mockk()
    private val api: NeerlyApi = mockk()
    private val stock = JarStock("s1", "p1", totalInStock = 30, outWithCustomers = 12, damagedCount = 1, lostCount = 0, totalOwned = 43)
    private val product = ShopProduct("p1", ShopTemplate("t1", "JAR_20L_NORMAL", "20L Jar (Normal)"), price = BigDecimal("85"), dailyCapacity = 50, status = "AVAILABLE")

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { jars.stock() } returns listOf(stock)
        coEvery { catalog.products() } returns listOf(product)
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun overview_namesJarsAndCountsOpenWork() {
        coEvery { jars.returns(null, 50) } returns listOf(ret("r1", "REQUESTED"))
        coEvery { jars.claims(50) } returns ClaimPage(listOf(claim("c1", "FILED"), claim("c2", "APPROVED")))
        val vm = VendorJarsViewModel(jars, catalog, com.neerly.mobile.support.TestStrings)
        val s = vm.state.value
        assertEquals("20L Jar (Normal)", s.lines.single().name)
        assertEquals(43, s.owned); assertEquals(12, s.atHomes)
        assertEquals(1, s.openReturns); assertEquals(1, s.openClaims)
    }

    @Test
    fun summary_showsToWash_andMarkWashedClearsIt() {
        coEvery { jars.returns(null, 50) } returns emptyList()
        coEvery { jars.claims(50) } returns ClaimPage(emptyList())
        coEvery { jars.summary() } returnsMany listOf(
            JarsSummary(fullInShop = 28, emptyToWash = 2, atHomesOnTime = 10, late = 2,
                lateCustomers = listOf(LateJarCustomer("c1", "Sneha G.", 1, 4, pickupRequested = false))),
            JarsSummary(fullInShop = 30, emptyToWash = 0, atHomesOnTime = 10, late = 2))
        coEvery { jars.washed(MarkWashed()) } returns mapOf("washed" to 2)
        val vm = VendorJarsViewModel(jars, catalog, com.neerly.mobile.support.TestStrings)
        assertEquals(2, vm.state.value.summary!!.emptyToWash)
        assertEquals("Sneha G.", vm.state.value.summary!!.lateCustomers.single().name)
        vm.markWashed()
        io.mockk.coVerify(exactly = 1) { jars.washed(MarkWashed()) }
        assertEquals(0, vm.state.value.summary!!.emptyToWash)
    }

    @Test
    fun stockAdjust_damagedIsCappedAtWhatsInTheShop_andNeedsAReason() {
        coEvery { jars.adjust(any()) } returns stock.copy(totalInStock = 28, damagedCount = 3)
        val vm = VendorStockAdjustViewModel(jars, catalog, com.neerly.mobile.support.TestStrings)
        vm.reason(StockReason.DAMAGED)
        repeat(100) { vm.step(1) }
        assertEquals(30, vm.state.value.value)
        assertFalse("note required", vm.state.value.valid)
        repeat(28) { vm.step(-1) }
        vm.note("cracked in the wash")
        vm.save()
        coVerify { jars.adjust(AdjustStock("p1", 2, "MARK_DAMAGED", "cracked in the wash")) }
        assertEquals(28, vm.state.value.lines.single().stock.totalInStock)
    }

    @Test
    fun stockAdjust_recountToTheSameNumber_isNotAChange() {
        val vm = VendorStockAdjustViewModel(jars, catalog, com.neerly.mobile.support.TestStrings)
        assertEquals(30, vm.state.value.value)
        vm.note("Sunday count")
        assertFalse(vm.state.value.valid)
        vm.step(-1)
        assertTrue(vm.state.value.valid)
    }

    @Test
    fun pickups_eveningHidesToday_andBooksInIndiaTime() {
        coEvery { jars.returns(null, 50) } returns listOf(ret("r1", "REQUESTED"))
        coEvery { jars.returns(listOf("COMPLETED"), 20) } returns emptyList()
        coEvery { api.vendorTeam() } returns emptyList()
        coEvery { jars.schedule(any(), any()) } returns ret("r1", "SCHEDULED")
        val vm = VendorJarReturnsViewModel(jars, api, com.neerly.mobile.support.TestStrings)
        vm.clock = Clock.fixed(ZonedDateTime.of(2026, 11, 7, 18, 0, 0, 0, ZoneId.of("Asia/Kolkata")).toInstant(), ZoneId.of("Asia/Kolkata"))
        assertEquals(LocalDate.of(2026, 11, 8), vm.days().first())
        vm.startPlanning(vm.state.value.open.single())
        vm.edit { copy(slotHour = 8) }
        vm.schedule()
        coVerify { jars.schedule("r1", ScheduleReturn("2026-11-08T02:30:00Z", null)) }
        assertEquals("SCHEDULED", vm.state.value.open.single().status)
    }

    @Test
    fun pickups_markCollected_movesItToCollected() {
        coEvery { jars.returns(null, 50) } returns listOf(ret("r1", "SCHEDULED"))
        coEvery { jars.returns(listOf("COMPLETED"), 20) } returns emptyList()
        coEvery { api.vendorTeam() } returns emptyList()
        coEvery { jars.complete("r1", any()) } returns ret("r1", "COMPLETED")
        val vm = VendorJarReturnsViewModel(jars, api, com.neerly.mobile.support.TestStrings)
        vm.collected(vm.state.value.open.single())
        assertTrue(vm.state.value.open.isEmpty())
        assertEquals("r1", vm.state.value.collected.single().id)
    }

    @Test
    fun claimNew_needsThePickupPhoto_andSendsItAsEvidence() {
        coEvery { jars.returns(listOf("COMPLETED"), 50) } returns listOf(ret("r1", "COMPLETED").copy(depositId = "d1", pickupPhotoUrl = "pickups/r1.jpg"))
        coEvery { jars.file(any()) } returns claim("c9", "FILED")
        val vm = VendorClaimNewViewModel(jars, SavedStateHandle(mapOf("returnId" to "r1")), com.neerly.mobile.support.TestStrings)
        vm.update { copy(kind = "Cracked", amount = "200", note = "near the handle") }
        assertTrue(vm.state.value.valid)
        vm.send()
        coVerify { jars.file(FileClaim("d1", "r1", BigDecimal("200"), "Cracked — near the handle", listOf("pickups/r1.jpg"))) }
        assertEquals("c9", vm.state.value.filed?.id)
    }

    @Test
    fun claimNew_withoutAPhoto_cannotBeSent() {
        coEvery { jars.returns(listOf("COMPLETED"), 50) } returns listOf(ret("r1", "COMPLETED").copy(depositId = "d1"))
        val vm = VendorClaimNewViewModel(jars, SavedStateHandle(mapOf("returnId" to "r1")), com.neerly.mobile.support.TestStrings)
        vm.update { copy(kind = "Cracked", amount = "200") }
        assertFalse(vm.state.value.valid)
    }

    @Test
    fun claimDetail_withdraw_showsWithdrawn() {
        coEvery { jars.claim("c1") } returns claim("c1", "FILED")
        coEvery { jars.withdraw("c1", any()) } returns claim("c1", "WITHDRAWN")
        val vm = VendorClaimDetailViewModel(jars, SavedStateHandle(mapOf("claimId" to "c1")), com.neerly.mobile.support.TestStrings)
        vm.askWithdraw(true)
        vm.withdraw()
        assertEquals("WITHDRAWN", vm.state.value.claim?.status)
        assertFalse(vm.state.value.confirming)
    }

    private fun ret(id: String, status: String) = JarReturn(id, "cust", returnMode = "SCHEDULED_PICKUP", containerCount = 1, status = status)
    private fun claim(id: String, status: String) = DamageClaim(id, "cust", "d1", claimAmount = BigDecimal("200"), reason = "Cracked",
        status = status, filedAt = "2026-11-07T05:00:00Z")
}
