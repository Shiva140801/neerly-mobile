package com.neerly.mobile.feature.address

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.SubscriptionResponse
import com.neerly.mobile.data.repo.CustomerRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** WP-2: drag-the-map pin, address search and "N suppliers deliver here". */
@OptIn(ExperimentalCoroutinesApi::class)
class AddressPinPickerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk(relaxed = true)
    private val mapApi: AddressMapApi = mockk()
    private var reverses = 0
    private var searches = 0
    private var found: List<PlaceMatch> = emptyList()

    private val locator = object : AddressLocator {
        override suspend fun currentFix(): Pair<Double, Double>? = null
        override suspend fun reverse(lat: Double, lng: Double): FoundPlace {
            reverses++
            return FoundPlace("Mindspace Rd, Raidurg", "500032", "Hyderabad", "Mindspace Junction, Raidurg")
        }
    }
    private val places = object : PlaceSearch {
        override suspend fun search(query: String): List<PlaceMatch> { searches++; return found }
    }

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { mapApi.serviceability("500032") } returns Serviceability("500032", 2, listOf(ServiceableShop("v1", "Pure Drops"), ServiceableShop("v3", "Ganga")))
        coEvery { mapApi.serviceability("500099") } returns Serviceability("500099", 0)
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm(addressId: String? = null) = AddressFormViewModel(repo, locator, places, mapApi,
        SavedStateHandle(if (addressId == null) emptyMap() else mapOf("addressId" to addressId)), com.neerly.mobile.support.TestStrings)

    private fun match(title: String, lat: Double, lng: Double, pincode: String) =
        PlaceMatch(title, "Area $pincode", lat, lng, "$title Rd", pincode, "Hyderabad")

    @Test
    fun pinMoved_countsAtOnce_andReverseGeocodesOncePerBurst() = runTest(dispatcher) {
        val vm = vm()
        vm.pinMoved(17.4400, 78.3800)
        vm.pinMoved(17.4401, 78.3801)
        vm.pinMoved(17.4402, 78.3802)
        assertTrue(vm.state.value.locationCaptured)
        assertEquals(17.4402, vm.state.value.latitude, 0.0)
        advanceTimeBy(399)
        assertEquals(0, reverses)
        advanceUntilIdle()
        assertEquals(1, reverses)
        val s = vm.state.value
        assertEquals("500032", s.pincode)
        assertEquals("Mindspace Junction, Raidurg", s.foundAt)
        assertEquals(2L, s.serviceCount)
        coVerify(exactly = 1) { mapApi.serviceability("500032") }
    }

    @Test
    fun pinMoved_outsideIndia_isRefused() = runTest(dispatcher) {
        val vm = vm()
        vm.pinMoved(37.42, -122.08)
        advanceUntilIdle()
        assertFalse(vm.state.value.locationCaptured)
        assertNotNull(vm.state.value.locationError)
    }

    @Test
    fun search_isDebounced_andMarksSavedAndWhoDelivers() = runTest(dispatcher) {
        val work = mockk<AddressResponse>(relaxed = true)
        every { work.id } returns "w1"; every { work.label } returns "Work"
        every { work.latitude } returns 17.4483; every { work.longitude } returns 78.3741
        coEvery { repo.addresses() } returns listOf(work)
        found = listOf(
            match("Tower B, Mindspace", 17.4484, 78.3742, "500081"),   // ~15 m from Work
            match("Mindspace Junction", 17.4300, 78.3800, "500032"),
            match("Mindspace Residency", 17.4000, 78.3300, "500099")
        )
        val vm = vm()
        vm.onQuery("Mind")
        vm.onQuery("Mindspace")
        advanceUntilIdle()
        assertEquals(1, searches)
        val rows = vm.state.value.matches
        assertEquals("Work", rows[0].savedAs)
        assertNull(rows[0].serviceCount)
        assertEquals(2L, rows[1].serviceCount)
        assertEquals(0L, rows[2].serviceCount)
    }

    @Test
    fun shortQuery_doesNotSearch() = runTest(dispatcher) {
        val vm = vm()
        vm.onQuery("Mi")
        advanceUntilIdle()
        assertEquals(0, searches)
        assertTrue(vm.state.value.matches.isEmpty())
    }

    @Test
    fun pickMatch_movesThePinThereAndFillsTheForm() = runTest(dispatcher) {
        found = listOf(match("Mindspace Junction", 17.4300, 78.3800, "500032"))
        val vm = vm()
        vm.onQuery("Mindspace")
        advanceUntilIdle()
        vm.pickMatch(vm.state.value.matches.first())
        advanceUntilIdle()
        val s = vm.state.value
        assertTrue(s.locationCaptured)
        assertEquals(17.4300, s.latitude, 0.0)
        assertEquals("500032", s.pincode)
        assertEquals("Mindspace Junction Rd", s.street)
        assertTrue(s.matches.isEmpty())
        assertEquals(2L, s.serviceCount)
    }

    @Test
    fun editPin_awayFromASubscribedShop_asksFirst_andKeepOldPinRestoresIt() = runTest(dispatcher) {
        val home = mockk<AddressResponse>(relaxed = true)
        every { home.id } returns "a1"; every { home.label } returns "Parents' place"
        every { home.latitude } returns 17.4375; every { home.longitude } returns 78.4482
        every { home.flatNumber } returns "12-3-45"; every { home.buildingName } returns "Srinivasa"
        every { home.street } returns "Srinivasa Nagar"; every { home.pincode } returns "500038"
        coEvery { repo.addresses() } returns listOf(home)
        coEvery { mapApi.serviceability("500038") } returns Serviceability("500038", 1, listOf(ServiceableShop("v2", "Sri Balaji")))
        val sub = mockk<SubscriptionResponse>(relaxed = true)
        every { sub.addressId } returns "a1"; every { sub.status } returns "ACTIVE"
        every { sub.vendorId } returns "v2"; every { sub.vendorName } returns "Sri Balaji"
        coEvery { repo.subscriptions() } returns listOf(sub)

        val vm = vm("a1")
        advanceUntilIdle()
        // Typed street/pincode stay as typed; clear the pincode so the pin's pincode is used.
        vm.update { copy(pincode = "") }
        vm.pinMoved(17.4300, 78.3800)               // over to 500032, where Sri Balaji doesn't go
        advanceUntilIdle()
        assertEquals(listOf("Sri Balaji"), vm.state.value.lostShops)

        vm.save {}
        assertTrue(vm.state.value.confirmMove)
        coVerify(exactly = 0) { repo.updateAddress(any(), any()) }

        vm.keepOldPin()
        assertEquals(17.4375, vm.state.value.latitude, 0.0)
        assertFalse(vm.state.value.pinChanged)

        vm.pinMoved(17.4300, 78.3800)
        advanceUntilIdle()
        var done = false
        vm.confirmMove { done = true }
        advanceUntilIdle()
        assertTrue(done)
        coVerify { repo.updateAddress("a1", match { it.latitude == 17.4300 }) }
    }

    @Test
    fun locationRefused_thenTypeInstead_showsTheTypedPathWithoutAnError() = runTest(dispatcher) {
        val vm = vm()
        vm.locationDenied()
        assertNotNull(vm.state.value.locationError)
        vm.typeInstead()
        assertTrue(vm.state.value.typing)
        assertNull(vm.state.value.locationError)
    }
}
