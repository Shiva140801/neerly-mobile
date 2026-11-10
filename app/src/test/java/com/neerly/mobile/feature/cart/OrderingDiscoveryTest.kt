package com.neerly.mobile.feature.cart

import com.neerly.mobile.data.api.DiscoveryApi
import com.neerly.mobile.data.cart.Cart
import com.neerly.mobile.data.cart.CartItem
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.cart.DeliverySlot
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.ProductTemplate
import com.neerly.mobile.data.dto.SlotCalendarResponse
import com.neerly.mobile.data.dto.SlotDayDto
import com.neerly.mobile.data.dto.SlotOptionDto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/** WP-3: the slot picker, the cart's "When?" and quick adds. */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderingDiscoveryTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun slot(start: String, end: String, state: String) = SlotOptionDto(start, end, state)
    private val today = SlotDayDto("2026-11-07", true, opensAt = "06:00", closesAt = "21:00", slots = listOf(
        slot("06:00", "09:00", "PASSED"), slot("09:00", "12:00", "PASSED"), slot("12:00", "15:00", "OPEN"), slot("17:00", "20:00", "FULL")))
    private val sunday = SlotDayDto("2026-11-08", true, opensAt = "06:00", closesAt = "13:00", slots = listOf(
        slot("06:00", "09:00", "OPEN"), slot("09:00", "12:00", "FILLING"), slot("12:00", "15:00", "CLOSED"), slot("17:00", "20:00", "CLOSED")))
    private val diwali = SlotDayDto("2026-11-09", false, closedReason = "HOLIDAY", label = "Diwali", slots = listOf(
        slot("06:00", "09:00", "CLOSED")))

    // --- pure rules --------------------------------------------------------------

    @Test fun deliverySlot_isSentAsAnIstInstant() {
        val s = DeliverySlot(LocalDate.of(2026, 11, 8), LocalTime.of(6, 0), LocalTime.of(9, 0))
        assertEquals("2026-11-08T00:30:00Z", s.startIso)
        assertEquals("2026-11-08T03:30:00Z", s.endIso)
    }

    @Test fun initialDay_isThePickedDay_elseTheFirstBookableOne() {
        val all = listOf(diwali, today, sunday)
        assertEquals(1, SlotPickerLogic.initialDay(all, null))       // Diwali has nothing bookable
        assertEquals(2, SlotPickerLogic.initialDay(all, DeliverySlot(LocalDate.of(2026, 11, 8), LocalTime.of(6, 0), LocalTime.of(9, 0))))
    }

    @Test fun noteOf_saysWhyAWindowIsShut() {
        assertEquals(SlotPickerLogic.Note.Shuts(LocalTime.of(13, 0)), SlotPickerLogic.noteOf(sunday, sunday.slots[2]))
        assertEquals(SlotPickerLogic.Note.ShopClosed("Diwali"), SlotPickerLogic.noteOf(diwali, diwali.slots[0]))
        assertEquals(SlotPickerLogic.Note.Filling, SlotPickerLogic.noteOf(sunday, sunday.slots[1]))
        assertEquals(SlotPickerLogic.Note.Passed, SlotPickerLogic.noteOf(today, today.slots[0]))
        assertEquals(SlotPickerLogic.Note.Full, SlotPickerLogic.noteOf(today, today.slots[3]))
    }

    @Test fun shortDayClose_onlyForADayThatShutsBeforeTheEvening() {
        assertEquals(LocalTime.of(13, 0), SlotPickerLogic.shortDayClose(sunday))
        assertNull(SlotPickerLogic.shortDayClose(today))
        assertNull(SlotPickerLogic.shortDayClose(diwali))
    }

    @Test fun partOf_namesTheTimeOfDay() {
        assertEquals(SlotPickerLogic.Part.EARLY, SlotPickerLogic.partOf(LocalTime.of(6, 0)))
        assertEquals(SlotPickerLogic.Part.MORNING, SlotPickerLogic.partOf(LocalTime.of(9, 0)))
        assertEquals(SlotPickerLogic.Part.AFTERNOON, SlotPickerLogic.partOf(LocalTime.of(12, 0)))
        assertEquals(SlotPickerLogic.Part.EVENING, SlotPickerLogic.partOf(LocalTime.of(17, 0)))
    }

    // --- view model --------------------------------------------------------------

    private val api: DiscoveryApi = mockk()
    private val store = CartStore().also {
        it.addItem("v1", "Sri Balaji", CartItem("p1", "20L Jar", BigDecimal("60"), 2))
    }

    @Test fun slotPicker_pickingAClosedWindowDoesNothing_aBookableOneGoesOnTheCart() = runTest(dispatcher) {
        coEvery { api.slotDays("v1", 5) } returns SlotCalendarResponse("v1", listOf(today, sunday, diwali))
        val vm = SlotPickerViewModel(api, store, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        assertEquals(0, vm.state.value.dayIndex)
        vm.pickSlot(today.slots[0])                 // passed
        assertNull(vm.state.value.picked)
        assertFalse(vm.confirm())
        vm.pickDay(1)
        vm.pickSlot(sunday.slots[2])                // after the shop shuts
        assertNull(vm.state.value.picked)
        vm.pickSlot(sunday.slots[0])
        assertTrue(vm.confirm())
        assertEquals(DeliverySlot(LocalDate.of(2026, 11, 8), LocalTime.of(6, 0), LocalTime.of(9, 0)), store.snapshot.slot)
    }

    @Test fun slotPicker_reopensOnTheSlotAlreadyPicked() = runTest(dispatcher) {
        store.setSlot(DeliverySlot(LocalDate.of(2026, 11, 8), LocalTime.of(9, 0), LocalTime.of(12, 0)))
        coEvery { api.slotDays("v1", 5) } returns SlotCalendarResponse("v1", listOf(today, sunday))
        val vm = SlotPickerViewModel(api, store, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.dayIndex)
        assertEquals("09:00", vm.state.value.picked?.start)
    }

    @Test fun slotPicker_nothingBookable_isSaid() = runTest(dispatcher) {
        coEvery { api.slotDays("v1", 5) } returns SlotCalendarResponse("v1", listOf(diwali))
        val vm = SlotPickerViewModel(api, store, com.neerly.mobile.support.TestStrings)
        advanceUntilIdle()
        assertTrue(vm.state.value.nothingBookable)
    }

    @Test fun cart_slotIsClearedWhenTheShopChanges() {
        store.setSlot(DeliverySlot(LocalDate.of(2026, 11, 8), LocalTime.of(6, 0), LocalTime.of(9, 0)))
        store.replaceWithNewVendor("v2", "Pure Drops", CartItem("p9", "20L Jar", BigDecimal("55"), 1))
        assertNull(store.snapshot.slot)
    }

    // --- quick adds ----------------------------------------------------------------

    private fun product(id: String, price: String, keep: Boolean = false, swap: Boolean = false, deposit: String? = null, status: String = "AVAILABLE") =
        ProductResponse(id = id, vendorId = "v1", template = ProductTemplate(id = "t-$id", categoryCode = "X", displayName = "Item $id"),
            brand = null, price = BigDecimal(price), status = status, allowKeepContainer = keep, allowTransferAndReturn = swap,
            depositAmount = deposit?.let(::BigDecimal))

    @Test fun quickAdds_oneMoreOfTheLine_andTheCheapestThingWithoutADepositChoice() {
        val cart = Cart(vendorId = "v1", items = listOf(CartItem("p1", "20L Jar", BigDecimal("60"), 1, keepContainer = false)))
        val shelf = listOf(
            product("p1", "60", swap = true),
            product("jar-keep", "20", keep = true, deposit = "150"),   // would need a deposit decision
            product("can", "45"),                                     // plain, no container question
            product("gone", "10", status = "OUT_OF_STOCK"),
            product("bottle", "50")
        )
        val adds = QuickAdds.pick(cart, shelf)
        assertEquals(listOf("p1", "can"), adds.map { it.productId })
        assertTrue(adds[0].oneMore)
        assertFalse(adds[0].keepContainer)                // same choice as the line
        assertNull(adds[1].deposit)
    }

    @Test fun quickAdds_swapOnlyProduct_comesBackAtTheDoor() {
        val cart = Cart(vendorId = "v1", items = listOf(CartItem("p1", "20L Jar", BigDecimal("60"), 10)))
        val adds = QuickAdds.pick(cart, listOf(product("swap", "30", swap = true)))
        assertEquals(listOf("swap"), adds.map { it.productId })   // the 10-jar line can't take one more
        assertFalse(adds.single().keepContainer)
    }
}
