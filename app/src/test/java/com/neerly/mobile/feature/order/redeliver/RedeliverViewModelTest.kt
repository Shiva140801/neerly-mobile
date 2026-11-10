package com.neerly.mobile.feature.order.redeliver

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.FailedDeliveryView
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.feature.address.AddressMapApi
import com.neerly.mobile.feature.address.Serviceability
import com.neerly.mobile.feature.address.ServiceableShop
import com.neerly.mobile.feature.checkout.GatewayPayments
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class RedeliverViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api: NeerlyApi = mockk()
    private val redelivery: RedeliveryApi = mockk()
    private val maps: AddressMapApi = mockk()
    private val gateway: GatewayPayments = mockk()

    private val failed = OrderResponse(
        id = "o1", orderNumber = "NEE-1", customerId = "c1", vendorId = "v1", driverId = "d1", status = "FAILED",
        subtotal = BigDecimal("240"), deliveryFee = BigDecimal("25"), surgeSurcharge = null, taxes = null, discount = null,
        depositTotal = null, totalAmount = BigDecimal("265"), placedAt = "2026-11-10T03:00:00Z", paymentMethod = "UPI",
        addressId = "home",
        failedDelivery = FailedDeliveryView(reason = "NOT_HOME", state = "AWAITING", keptIfCancelled = BigDecimal("25"),
            refundIfCancelled = BigDecimal("240"), newTripFee = BigDecimal("25")))

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm(mode: String) = RedeliverViewModel(SavedStateHandle(mapOf("orderId" to "o1", "mode" to mode)),
        api, redelivery, maps, gateway, com.neerly.mobile.support.TestStrings)

    @Test
    fun slot_isSentAsTheInstantInHyderabad() = runTest(dispatcher) {
        coEvery { api.order("o1") } returns failed
        coEvery { redelivery.slotDays("v1", any()) } returns SlotCalendar("v1", listOf(
            SlotDayRow("2026-11-09", open = false, closedReason = "HOLIDAY"),
            SlotDayRow("2026-11-10", open = true, slots = listOf(
                SlotOptionRow("09:00:00", "12:00:00", "FULL"), SlotOptionRow("12:00:00", "15:00:00", "OPEN")))))
        coEvery { redelivery.redeliver("o1", any()) } returns failed.copy(id = "o2", status = "PLACED", redeliveryOf = "o1")
        val vm = vm("time")
        advanceUntilIdle()
        assertEquals(1, vm.state.value.dayIndex)              // the first day with an open slot
        vm.pickSlot(vm.state.value.days[1].slots[0])           // full: ignored
        assertNull(vm.state.value.slot)
        vm.pickSlot(vm.state.value.days[1].slots[1])
        vm.book()
        advanceUntilIdle()

        coVerify { redelivery.redeliver("o1", RedeliverRequest(null, "2026-11-10T06:30:00Z", "2026-11-10T09:30:00Z")) }
        assertEquals("o2", vm.state.value.booked?.id)
    }

    @Test
    fun place_onlyAddressesTheShopServes_canBePicked() = runTest(dispatcher) {
        coEvery { api.order("o1") } returns failed
        coEvery { api.addresses() } returns listOf(address("home", "500033"), address("mum", "500099"))
        coEvery { maps.serviceability("500033") } returns Serviceability("500033", 1, listOf(ServiceableShop("v1", "Sri Balaji")))
        coEvery { maps.serviceability("500099") } returns Serviceability("500099", 0)
        val vm = vm("place")
        advanceUntilIdle()
        val (home, mum) = vm.state.value.addresses
        assertTrue(home.served); assertFalse(mum.served)
        vm.pickAddress(mum)
        assertNull(vm.state.value.addressId)
        vm.pickAddress(home)
        assertEquals("home", vm.state.value.addressId)
    }

    @Test
    fun shortWallet_offersATopUpOfTheShortfall() = runTest(dispatcher) {
        coEvery { api.order("o1") } returns failed
        coEvery { api.addresses() } returns listOf(address("home", "500033"))
        coEvery { maps.serviceability(any()) } returns Serviceability("500033", 1, listOf(ServiceableShop("v1", "Sri Balaji")))
        val body = """{"error":{"code":"REDELIVERY_TOP_UP_NEEDED","message":"Add ₹25 to your Boondh wallet","details":{"shortfall":25,"tripFee":25.00}}}"""
        coEvery { redelivery.redeliver("o1", any()) } throws HttpException(Response.error<OrderResponse>(422, body.toResponseBody("application/json".toMediaType())))
        val vm = vm("place")
        advanceUntilIdle()
        vm.pickAddress(vm.state.value.addresses.first())
        vm.book()
        advanceUntilIdle()
        assertEquals(BigDecimal("25"), vm.state.value.topUp)
        assertEquals("Add ₹25 to your Boondh wallet", vm.state.value.error)
    }

    private fun address(id: String, pincode: String) = com.neerly.mobile.data.dto.AddressResponse(
        id = id, label = id, flatNumber = "7", buildingName = "Green View", street = "Road 2", landmark = null,
        city = "Hyderabad", pincode = pincode, latitude = 17.4, longitude = 78.4, deliveryInstructions = null,
        hasLift = false, floorNumber = null, securityContactName = null, securityContactPhone = null,
        isPrimary = id == "home", isServiceable = true)
}
