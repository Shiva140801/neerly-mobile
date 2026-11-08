package com.neerly.mobile.feature.gapbatch

import com.neerly.mobile.core.design.Tone
import com.neerly.mobile.data.dto.SearchOffer
import com.neerly.mobile.feature.search.offerDetail
import com.neerly.mobile.feature.vendor.dashboard.VendorTodayUiState
import com.neerly.mobile.feature.vendor.orders.VendorDashboard
import com.neerly.mobile.feature.vendor.orders.VendorOpsApi
import com.neerly.mobile.feature.vendor.orders.VendorOrdersViewModel
import com.neerly.mobile.feature.vendor.orders.stepLabelRes
import com.neerly.mobile.feature.vendor.orders.stepFallback
import com.neerly.mobile.feature.vendor.settings.AreaUiState
import com.neerly.mobile.feature.vendor.settings.DeliveryArea
import com.neerly.mobile.feature.vendor.settings.VendorAreaApi
import com.neerly.mobile.feature.vendor.settings.VendorAreaViewModel
import com.neerly.mobile.feature.vendor.shop.ConfirmEventBody
import com.neerly.mobile.feature.vendor.shop.ShopEvent
import com.neerly.mobile.feature.vendor.shop.VendorEventsViewModel
import com.neerly.mobile.feature.vendor.shop.VendorShopApi
import com.neerly.mobile.feature.vendor.shop.eventPillRes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class GapBatchWiringTest {
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun http(code: Int, errorCode: String) = HttpException(Response.error<Any>(code,
        """{"error":{"code":"$errorCode","message":"x"}}""".toResponseBody(null)))

    // 2 · Today
    @Test fun today_lateJarsComeFromTheDashboard() {
        assertEquals(3, VendorTodayUiState(dashboard = VendorDashboard(jarsOut = 51, jarsLate = 3)).lateJars)
        assertEquals(0, VendorTodayUiState().lateJars)
    }

    // 3 · Orders
    @Test fun orders_doneTabAsksForTodaysCancellations() {
        val ops: VendorOpsApi = mockk(relaxed = true)
        VendorOrdersViewModel(ops, com.neerly.mobile.support.TestStrings)
        coVerify { ops.orders("CANCELLED_TODAY", any()) }
        coVerify(exactly = 0) { ops.orders("CANCELLED", any()) }
    }

    @Test fun timeline_readsLikeTheShopTalks() {
        assertEquals("Left the shop", com.neerly.mobile.support.TestStrings.get(stepLabelRes("DISPATCHED")!!))
        assertEquals("You accepted", com.neerly.mobile.support.TestStrings.get(stepLabelRes("VENDOR_ACCEPTED")!!))
        assertEquals("Something new", stepFallback("SOMETHING_NEW"))
    }

    // 4 · Search offers
    @Test fun offerDetail_showsOnlyWhatTheServerKnows() {
        val o = SearchOffer(vendorId = "v", vendorName = "Sri Ganesh", price = BigDecimal("50"),
            rating = BigDecimal("4.66"), distanceKm = BigDecimal("1.24"))
        assertEquals("4.7 ★ · 1.2 km", offerDetail(o))
        assertEquals("", offerDetail(o.copy(rating = null, distanceKm = null)))
    }

    // 7 · Delivery area
    @Test fun area_addValidatesAndSaveSendsMetres() {
        val api: VendorAreaApi = mockk()
        coEvery { api.area() } returns DeliveryArea(listOf("500032"), 3000)
        coEvery { api.save(any()) } answers { firstArg() }
        val vm = VendorAreaViewModel(api, com.neerly.mobile.support.TestStrings)
        assertEquals(3f, vm.state.value.radiusKm)
        vm.type("50003"); assertFalse(vm.state.value.draftValid)            // 5 digits
        vm.type("500032"); assertFalse(vm.state.value.draftValid)           // already there
        vm.type("500081"); vm.add()
        vm.radius(7.3f)
        vm.save()
        coVerify { api.save(DeliveryArea(listOf("500032", "500081"), 7500)) }
        assertTrue(vm.state.value.saved)
    }

    @Test fun area_serverRefusalShowsPlainWords() {
        val api: VendorAreaApi = mockk()
        coEvery { api.area() } returns DeliveryArea(listOf("500032"), 3000)
        coEvery { api.save(any()) } throws http(400, "VENDOR_AREA_INVALID")
        val vm = VendorAreaViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.save()
        assertFalse(vm.state.value.saved)
        assertTrue(vm.state.value.error != null)
        vm.remove("500032")
        assertFalse(vm.state.value.canSave)                                // at least one pincode
        assertEquals(AreaUiState.MAX_KM, 20f)
    }

    // 8 · Events
    @Test fun expiredEvents_readHonestly() {
        val (res, tone) = eventPillRes("EXPIRED")
        assertEquals("Expired · no answer in time" to Tone.Neutral, com.neerly.mobile.support.TestStrings.get(res!!) to tone)
    }

    @Test fun confirmingAnExpiredRequest_explainsAndReloads() {
        val ev = ShopEvent(id = "e1", eventName = "Wedding", eventStart = "2026-08-28T06:00:00Z",
            subtotal = BigDecimal("850"), totalAmount = BigDecimal("850"), status = "PENDING_CONFIRMATION")
        val api: VendorShopApi = mockk()
        coEvery { api.events(any(), any(), any()) } returns listOf(ev)
        coEvery { api.confirmEvent("e1", any<ConfirmEventBody>()) } throws http(409, "EVENT_EXPIRED")
        val vm = VendorEventsViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.open("e1")
        vm.confirm(null)
        assertTrue(vm.state.value.actionError!!.contains("expired"))
        coVerify(atLeast = 2) { api.events(any(), any(), any()) }
    }
}
