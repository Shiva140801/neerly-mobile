package com.neerly.mobile.feature.customer

import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.repo.CustomerRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerHomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()

    private val primary = AddressResponse(
        id = "a1", label = "Home", flatNo = "301", buildingName = "Tulip",
        streetArea = "Madhapur", landmark = null, city = "Hyderabad",
        pincode = "500081", lat = 17.44, lng = 78.39,
        deliveryInstructions = null, liftAvailable = true, floorNumber = 3,
        securityContactName = null, securityContactPhone = null,
        isPrimary = true, createdAt = "2026-04-25T10:00:00Z"
    )

    private val vendor = VendorCardResponse(
        id = "v1", businessName = "Sri Ganesh Water Supply", tier = "TIER_1",
        status = "ACTIVE", businessCity = "Hyderabad", businessPincode = "500081",
        avgRating = BigDecimal("4.7"), totalOrders = 1240, fssaiNumber = "12345678901234"
    )

    private val activeOrder = OrderResponse(
        id = "o1", orderNumber = "NEE-26115-0001", customerId = "c1",
        vendorId = "v1", driverId = null, status = "OUT_FOR_DELIVERY",
        subtotal = BigDecimal("190.00"), deliveryFee = BigDecimal("30.00"),
        surgeAmount = null, taxAmount = BigDecimal("0.00"), discount = null,
        depositAmount = null, totalAmount = BigDecimal("220.00"),
        placedAt = "2026-04-25T09:00:00Z"
    )

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun load_happyPath_populatesVendorsAndPrimaryAddress() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { repo.vendors("500081") } returns listOf(vendor)
        coEvery { repo.activeOrders() } returns emptyList()
        coEvery { repo.wallet() } returns WalletResponse(
            userId = "c1", balance = BigDecimal("340.00"),
            heldAmount = BigDecimal.ZERO, availableAmount = BigDecimal("340.00")
        )

        val vm = CustomerHomeViewModel(repo)
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(false, s.loading)
        assertNull(s.error)
        assertEquals("Sri Ganesh Water Supply", s.vendors.single().businessName)
        assertEquals("500081", s.primaryAddress?.pincode)
        assertEquals(BigDecimal("340.00"), s.wallet?.availableAmount)
    }

    @Test
    fun load_noAddresses_fallsBackToDefaultPincode() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns emptyList()
        coEvery { repo.vendors("500032") } returns listOf(vendor)
        coEvery { repo.activeOrders() } returns emptyList()
        coEvery { repo.wallet() } returns WalletResponse(
            userId = "c1", balance = BigDecimal.ZERO,
            heldAmount = BigDecimal.ZERO, availableAmount = BigDecimal.ZERO
        )

        val vm = CustomerHomeViewModel(repo)
        advanceUntilIdle()

        assertNull(vm.state.value.primaryAddress)
        assertEquals(1, vm.state.value.vendors.size)
    }

    @Test
    fun load_activeOrdersFailureIsTolerated_homeStillLoads() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { repo.vendors(any()) } returns listOf(vendor)
        coEvery { repo.activeOrders() } throws RuntimeException("orders service down")
        coEvery { repo.wallet() } returns WalletResponse("c1", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)

        val vm = CustomerHomeViewModel(repo)
        advanceUntilIdle()

        assertEquals(false, vm.state.value.loading)
        assertTrue(vm.state.value.activeOrders.isEmpty())
        assertNotNull(vm.state.value.vendors.firstOrNull())
    }

    @Test
    fun load_addressesFailure_surfacesError() = runTest(dispatcher) {
        coEvery { repo.addresses() } throws RuntimeException("offline")

        val vm = CustomerHomeViewModel(repo)
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(false, s.loading)
        assertEquals("offline", s.error)
    }

    @Test
    fun activeOrder_included_whenNonTerminal() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { repo.vendors(any()) } returns emptyList()
        coEvery { repo.activeOrders() } returns listOf(activeOrder)
        coEvery { repo.wallet() } returns WalletResponse("c1", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)

        val vm = CustomerHomeViewModel(repo)
        advanceUntilIdle()

        assertEquals("OUT_FOR_DELIVERY", vm.state.value.activeOrders.single().status)
    }
}
