package com.neerly.mobile.feature.vendor.shop

import com.neerly.mobile.data.dto.VendorHolidayRow
import com.neerly.mobile.data.repo.VendorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class VendorShopViewModelsTest {
    private val api: VendorShopApi = mockk(relaxed = true)
    private val repo: VendorRepository = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun review(id: String, rating: Int, replied: Boolean = false) =
        ShopReview(id, rating = rating, text = "ok", vendorResponse = if (replied) ShopReviewReply("thanks") else null, createdAt = "2026-11-06T10:00:00Z")

    @Test
    fun reviews_filterAndReplyReplacesTheRow() {
        coEvery { api.reviews(any(), any()) } returns listOf(review("a", 5, replied = true), review("b", 2), review("c", 4))
        coEvery { api.reply("b", ReplyBody("Sorry — fixed")) } returns review("b", 2, replied = true)
        val vm = VendorReviewsViewModel(api, com.neerly.mobile.support.TestStrings)
        assertEquals(listOf("b", "c"), vm.state.value.shown.map { it.id })     // to reply first
        vm.setFilter(ReviewFilter.Low)
        assertEquals(listOf("b"), vm.state.value.shown.map { it.id })
        vm.reply("b", "  Sorry — fixed ")
        assertNotNull(vm.state.value.reviews.first { it.id == "b" }.vendorResponse)
    }

    @Test
    fun complaints_sendAppendsToTheThread() {
        val c = ShopComplaint("c1", category = "DAMAGED_CONTAINER", subject = "Jar leaking", description = "Seam drips",
            status = "OPEN", filedAt = "2026-11-06T09:00:00Z")
        coEvery { api.complaints(any(), any()) } returns listOf(c)
        coEvery { api.complaint("c1") } returns c
        coEvery { api.sendMessage("c1", any()) } returns ShopComplaintMessage("m1", "VENDOR_OWNER", "Fresh jar tonight", createdAt = "2026-11-06T09:31:00Z")
        val vm = VendorComplaintsViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.openThread("c1")
        vm.send("Fresh jar tonight")
        assertEquals(1, vm.state.value.open!!.messages.size)
        vm.send("   ")
        coVerify(exactly = 1) { api.sendMessage(any(), any()) }
    }

    @Test
    fun events_declineNeedsAReason_andConfirmUpdates() {
        val e = ShopEvent("e1", "Wedding", 300, "2026-11-22T01:30:00Z", subtotal = BigDecimal("2520"), totalAmount = BigDecimal("2620"),
            status = "PENDING_CONFIRMATION")
        coEvery { api.events(any(), any(), any()) } returns listOf(e)
        coEvery { api.event("e1") } returns e
        coEvery { api.rejectEvent("e1", any()) } returns e.copy(status = "VENDOR_REJECTED")
        val vm = VendorEventsViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.open("e1"); vm.startDecline()
        vm.reject(" ")
        assertNotNull(vm.state.value.actionError)
        coVerify(exactly = 0) { api.rejectEvent(any(), any()) }
        vm.reject("Fully booked that day")
        assertEquals("VENDOR_REJECTED", vm.state.value.open!!.status)
        assertNotNull(vm.state.value.done)
    }

    @Test
    fun shopTab_countsWhatsWaiting() {
        coEvery { api.dashboard() } returns ShopDashboard(businessName = "Sri Balaji Water", pendingEvents = 1, licencesToRenew = 1)
        coEvery { api.reviews(any(), any()) } returns listOf(review("a", 5), review("b", 4, replied = true))
        coEvery { api.complaints(any(), any()) } returns listOf(
            ShopComplaint("c1", category = "OTHER", subject = "x", description = "y", status = "OPEN", filedAt = "2026-11-06T09:00:00Z"),
            ShopComplaint("c2", category = "OTHER", subject = "x", description = "y", status = "RESOLVED", filedAt = "2026-11-01T09:00:00Z"))
        coEvery { repo.holidays() } returns listOf(VendorHolidayRow("2020-01-01", "old"), VendorHolidayRow("2099-11-09", "Diwali"))
        val s = ShopTabViewModel(api, repo, com.neerly.mobile.support.TestStrings).state.value
        assertEquals(1, s.reviewsToReply)
        assertEquals(1, s.openComplaints)
        assertEquals("2099-11-09", s.nextHoliday?.date)
        assertNull(s.error)
    }
}
