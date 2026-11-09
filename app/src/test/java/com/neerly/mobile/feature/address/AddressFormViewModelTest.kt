package com.neerly.mobile.feature.address

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.repo.CustomerRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddressFormViewModelTest {
    private val repo: CustomerRepository = mockk(relaxed = true)
    private val mapApi: AddressMapApi = mockk()
    private var placesFound: List<PlaceMatch> = emptyList()
    private var searches = 0
    private val places = object : PlaceSearch {
        override suspend fun search(query: String): List<PlaceMatch> { searches++; return placesFound }
    }

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { mapApi.serviceability(any()) } answers { Serviceability(firstArg(), 0) }
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun locator(fix: Pair<Double, Double>?, place: FoundPlace? = null) = object : AddressLocator {
        override suspend fun currentFix() = fix
        override suspend fun reverse(lat: Double, lng: Double) = place
    }

    @Test
    fun save_withoutARealPin_isRefusedAndNothingIsSent() {
        val vm = AddressFormViewModel(repo, locator(null), places, mapApi, SavedStateHandle(), com.neerly.mobile.support.TestStrings)
        vm.update { copy(flatNumber = "204", buildingName = "Lakshmi", street = "Road 12", pincode = "500032") }
        vm.save {}
        assertNotNull(vm.state.value.error)
        coVerify(exactly = 0) { repo.createAddress(any()) }
    }

    @Test
    fun useCurrentLocation_usesTheFixAndFillsBlankFieldsOnly() {
        val vm = AddressFormViewModel(repo, locator(17.44 to 78.35, FoundPlace("Road No. 12, Jubilee Hills", "500033", "Hyderabad", "Road No. 12…")), places, mapApi, SavedStateHandle(), com.neerly.mobile.support.TestStrings)
        vm.update { copy(street = "My own street") }
        vm.useCurrentLocation()
        val s = vm.state.value
        assertTrue(s.locationCaptured)
        assertEquals(17.44, s.latitude, 0.0)
        assertEquals("My own street", s.street)
        assertEquals("500033", s.pincode)
    }

    @Test
    fun useCurrentLocation_noFix_saysSoAndStaysUnset() {
        val vm = AddressFormViewModel(repo, locator(null), places, mapApi, SavedStateHandle(), com.neerly.mobile.support.TestStrings)
        vm.useCurrentLocation()
        assertFalse(vm.state.value.locationCaptured)
        assertNotNull(vm.state.value.locationError)
    }

    @Test
    fun save_withPin_createsTheAddressAtThatPin() {
        coEvery { repo.createAddress(any()) } returns mockk(relaxed = true)
        val vm = AddressFormViewModel(repo, locator(17.44 to 78.35), places, mapApi, SavedStateHandle(), com.neerly.mobile.support.TestStrings)
        vm.useCurrentLocation()
        vm.update { copy(flatNumber = "204", buildingName = "Lakshmi", street = "Road 12", pincode = "500032") }
        var done = false
        vm.save { done = true }
        assertTrue(done)
        coVerify { repo.createAddress(match { it.latitude == 17.44 && it.longitude == 78.35 }) }
    }

    @Test
    fun secondFix_replacesWhatTheFirstFilledButKeepsTypedFields() {
        var fix = 17.40 to 78.48
        var place = FoundPlace("Abids", "500001", "Hyderabad", null)
        val loc = object : AddressLocator {
            override suspend fun currentFix() = fix
            override suspend fun reverse(lat: Double, lng: Double) = place
        }
        val vm = AddressFormViewModel(repo, loc, places, mapApi, SavedStateHandle(), com.neerly.mobile.support.TestStrings)
        vm.useCurrentLocation()
        vm.update { copy(street = "Road No. 12") }
        fix = 17.44 to 78.35; place = FoundPlace("Gachibowli", "500032", "Hyderabad", null)
        vm.useCurrentLocation()
        val s = vm.state.value
        assertEquals("Road No. 12", s.street)
        assertEquals("500032", s.pincode)
        assertEquals("Hyderabad", s.city)
    }

    @Test
    fun fixOutsideIndia_isNotAccepted() {
        val vm = AddressFormViewModel(repo, locator(37.42 to -122.08), places, mapApi, SavedStateHandle(), com.neerly.mobile.support.TestStrings)
        vm.useCurrentLocation()
        assertFalse(vm.state.value.locationCaptured)
        assertNotNull(vm.state.value.locationError)
    }
}
