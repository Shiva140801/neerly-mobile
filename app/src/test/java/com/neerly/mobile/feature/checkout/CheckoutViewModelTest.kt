package com.neerly.mobile.feature.checkout

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.cart.CartItem
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.InitiatePaymentResult
import com.neerly.mobile.data.dto.OrderResponse
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()
    private val api: NeerlyApi = mockk()
    private lateinit var cart: CartStore

    private val primary = AddressResponse(
        id = "a1", label = "Home", flatNo = "301", buildingName = "Tulip",
        streetArea = "Madhapur", landmark = null, city = "Hyderabad",
        pincode = "500081", lat = 17.44, lng = 78.39,
        deliveryInstructions = null, liftAvailable = true, floorNumber = 3,
        securityContactName = null, securityContactPhone = null,
        isPrimary = true, createdAt = "2026-04-25T10:00:00Z"
    )

    private val sampleOrder = OrderResponse(
        id = "o1", orderNumber = "NEE-26115-0001", customerId = "c1",
        vendorId = "v1", driverId = null, status = "PLACED",
        subtotal = BigDecimal("190.00"), deliveryFee = BigDecimal("30.00"),
        surgeAmount = null, taxAmount = BigDecimal("0.00"),
        discount = null, depositAmount = null,
        totalAmount = BigDecimal("220.00"), placedAt = "2026-04-25T09:00:00Z"
    )

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        cart = CartStore()
        cart.addItem("v1", "Sri Ganesh",
            CartItem("p1", "20L Cool", BigDecimal("95"), 2))
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun init_loadsAddresses_selectsPrimary() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)

        val vm = CheckoutViewModel(repo, api, cart)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.addresses.size)
        assertEquals("a1", vm.state.value.selectedAddress?.id)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun placeOrder_noAddress_surfacesError() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns emptyList()

        val vm = CheckoutViewModel(repo, api, cart)
        advanceUntilIdle()

        var captured: OrderResponse? = null
        vm.placeOrder { captured = it }
        advanceUntilIdle()

        assertNull(captured)
        assertEquals("Please pick a delivery address", vm.state.value.error)
    }

    @Test
    fun placeOrder_emptyCart_surfacesError() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        cart.clear()

        val vm = CheckoutViewModel(repo, api, cart)
        advanceUntilIdle()

        vm.placeOrder {}
        advanceUntilIdle()

        assertEquals("Cart is empty", vm.state.value.error)
    }

    @Test
    fun placeOrder_happyPath_invokesCallbackWithOrder() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { api.placeOrder(any()) } returns sampleOrder

        val vm = CheckoutViewModel(repo, api, cart)
        advanceUntilIdle()

        var captured: OrderResponse? = null
        vm.placeOrder { captured = it }
        advanceUntilIdle()

        assertEquals("o1", captured?.id)
        assertEquals(sampleOrder, vm.state.value.placedOrder)
    }

    @Test
    fun placeOrder_apiFailure_surfacesMessage() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { api.placeOrder(any()) } throws RuntimeException("network 500")

        val vm = CheckoutViewModel(repo, api, cart)
        advanceUntilIdle()
        vm.placeOrder {}
        advanceUntilIdle()

        assertEquals("network 500", vm.state.value.error)
    }

    @Test
    fun initiatePayment_cod_clearsCart() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { api.initiatePayment(any()) } returns InitiatePaymentResult(
            paymentId = "pay1", razorpayOrderId = null,
            amountPaise = 22000L, keyId = null
        )

        val vm = CheckoutViewModel(repo, api, cart)
        advanceUntilIdle()
        vm.selectPaymentMethod("COD")

        var razorpayOrderIdReceived: String? = "nonnull-seed"
        vm.initiatePayment(sampleOrder) { _, id -> razorpayOrderIdReceived = id }
        advanceUntilIdle()

        assertNull(razorpayOrderIdReceived)
        assertTrue(cart.snapshot.isEmpty)
    }

    @Test
    fun initiatePayment_upi_keepsCartUntilSuccessCallback() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { api.initiatePayment(any()) } returns InitiatePaymentResult(
            paymentId = "pay1", razorpayOrderId = "order_rzp_1",
            amountPaise = 22000L, keyId = "rzp_test_xxx"
        )

        val vm = CheckoutViewModel(repo, api, cart)
        advanceUntilIdle()
        vm.selectPaymentMethod("UPI")

        var razorpayOrderId: String? = null
        vm.initiatePayment(sampleOrder) { _, id -> razorpayOrderId = id }
        advanceUntilIdle()

        assertEquals("order_rzp_1", razorpayOrderId)
        // Cart preserved until onPaymentCaptured fires from Razorpay callback
        assertFalse(cart.snapshot.isEmpty)
    }

    @Test
    fun onPaymentCaptured_clearsCartAndMarksCaptured() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)

        val vm = CheckoutViewModel(repo, api, cart)
        advanceUntilIdle()
        vm.onPaymentCaptured()

        assertTrue(cart.snapshot.isEmpty)
        assertTrue(vm.state.value.paymentCaptured)
    }
}
