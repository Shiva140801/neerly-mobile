package com.neerly.mobile.feature.address

import com.neerly.mobile.data.dto.AddressResponse
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddressListViewModelTest {
    private val repo: CustomerRepository = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun addr(id: String, pin: String) = AddressResponse(
        id = id, label = "Home", flatNumber = "204", buildingName = "Lakshmi", street = "Road 12",
        city = "Hyderabad", pincode = pin, latitude = 17.44, longitude = 78.35)

    @Test
    fun refresh_countsSuppliersOncePerPincode() {
        coEvery { repo.addresses() } returns listOf(addr("a1", "500032"), addr("a2", "500032"), addr("a3", "500081"))
        coEvery { repo.vendors("500032") } returns listOf(mockk(), mockk())
        coEvery { repo.vendors("500081") } returns emptyList()
        val vm = AddressListViewModel(repo, com.neerly.mobile.support.TestStrings)
        assertEquals(mapOf("500032" to 2, "500081" to 0), vm.state.value.suppliersByPincode)
        coVerify(exactly = 1) { repo.vendors("500032") }
    }

    @Test
    fun delete_failure_isShown() {
        coEvery { repo.addresses() } returns listOf(addr("a1", "500032"))
        coEvery { repo.deleteAddress("a1") } throws RuntimeException("x")
        val vm = AddressListViewModel(repo, com.neerly.mobile.support.TestStrings)
        vm.delete("a1")
        assertNotNull(vm.state.value.error)
        assertEquals(1, vm.state.value.addresses.size)
    }

    @Test
    fun refresh_mapsLivePlansToTheirAddress_forTheDeleteWarning() {
        coEvery { repo.addresses() } returns listOf(addr("a1", "500032"), addr("a2", "500032"))
        coEvery { repo.vendors(any()) } returns emptyList()
        fun plan(addr: String, status: String, shop: String) = mockk<com.neerly.mobile.data.dto.SubscriptionResponse> {
            io.mockk.every { addressId } returns addr; io.mockk.every { this@mockk.status } returns status
            io.mockk.every { vendorName } returns shop; io.mockk.every { productName } returns null
        }
        coEvery { repo.subscriptions() } returns listOf(plan("a1", "ACTIVE", "Pure Drops"), plan("a2", "CANCELLED", "Old Shop"))
        val vm = AddressListViewModel(repo, com.neerly.mobile.support.TestStrings)
        assertEquals(mapOf("a1" to listOf("Pure Drops")), vm.state.value.plansByAddress)
    }
}
