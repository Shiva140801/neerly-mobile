package com.neerly.mobile.feature.checkout

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.cart.CartItem
import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.InitiatePaymentResult
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.repo.CustomerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
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
    private val orderStore: com.neerly.mobile.data.order.OrderLocalStore = mockk(relaxed = true)
    private val gateway: GatewayPayments = mockk()
    private val activity: android.app.Activity = mockk(relaxed = true)
    private val upiInit = InitiatePaymentResult(
        paymentId = "pay1", razorpayOrderId = "order_rzp_1", amountPaise = 22000L, keyId = "rzp_test_xxx"
    )
    private lateinit var cart: CartStore

    private val primary = AddressResponse(
        id = "a1", label = "Home", flatNumber = "301", buildingName = "Tulip",
        street = "Madhapur", landmark = null, city = "Hyderabad",
        pincode = "500081", latitude = 17.44, longitude = 78.39,
        deliveryInstructions = null, hasLift = true, floorNumber = 3,
        securityContactName = null, securityContactPhone = null,
        isPrimary = true, isServiceable = true
    )

    private val sampleOrder = OrderResponse(
        id = "o1", orderNumber = "NEE-26115-0001", customerId = "c1",
        vendorId = "v1", driverId = null, status = "PLACED",
        subtotal = BigDecimal("190.00"), deliveryFee = BigDecimal("30.00"),
        surgeSurcharge = null, taxes = BigDecimal("0.00"),
        discount = null, depositTotal = null,
        totalAmount = BigDecimal("220.00"), placedAt = "2026-04-25T09:00:00Z"
    )

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        // Checkout prices the cart once an address is known; tests that care stub a real quote.
        coEvery { api.quoteOrder(any()) } throws RuntimeException("no quote in this test")
        cart = CartStore()
        cart.addItem("v1", "Sri Ganesh",
            CartItem("p1", "20L Cool", BigDecimal("95"), 2))
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun init_loadsAddresses_selectsPrimary() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.addresses.size)
        assertEquals("a1", vm.state.value.selectedAddress?.id)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun placeOrder_noAddress_surfacesError() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns emptyList()

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
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

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
        advanceUntilIdle()

        vm.placeOrder {}
        advanceUntilIdle()

        assertEquals("Cart is empty", vm.state.value.error)
    }

    @Test
    fun placeOrder_happyPath_invokesCallbackWithOrder() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { api.placeOrder(any(), any()) } returns sampleOrder

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
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
        coEvery { api.placeOrder(any(), any()) } throws RuntimeException("network 500")

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
        advanceUntilIdle()
        vm.placeOrder {}
        advanceUntilIdle()

        // Raw exception text is never shown to a customer.
        assertEquals("Couldn't place your order. Please try again.", vm.state.value.error)
    }

    @Test
    fun placeOrder_4xx_showsTheServersOwnMessage() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        val body = """{"error":{"code":"PROMO_NOT_ELIGIBLE","message":"Promo has expired"}}"""
            .toResponseBody("application/json".toMediaType())
        coEvery { api.placeOrder(any(), any()) } throws retrofit2.HttpException(
            retrofit2.Response.error<Any>(422, body)
        )

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
        advanceUntilIdle()
        vm.placeOrder {}
        advanceUntilIdle()

        assertEquals("Promo has expired", vm.state.value.error)
    }

    @Test
    fun placeOrder_keepsTheDeliveryCode_andReusesTheRequestIdOnRetry() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        val ids = mutableListOf<String>()
        coEvery { api.placeOrder(any(), capture(ids)) } throws java.io.IOException("timeout") andThen
            sampleOrder.copy(otp = "482913")

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
        advanceUntilIdle()
        vm.placeOrder {}
        advanceUntilIdle()
        vm.placeOrder {}
        advanceUntilIdle()

        // Same id both times: a retry after a timeout replays, it does not double-order.
        assertEquals(2, ids.size)
        assertEquals(ids[0], ids[1])
        io.mockk.verify { orderStore.saveDeliveryCode(sampleOrder.id, "482913") }
    }

    @Test
    fun placeAndPay_cod_isDoneOnPlacement_andClearsCart() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { api.placeOrder(any(), any()) } returns sampleOrder

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
        advanceUntilIdle()
        vm.selectPaymentMethod("COD")
        var done: OrderResponse? = null
        vm.placeAndPay(activity) { done = it }
        advanceUntilIdle()

        assertEquals("o1", done?.id)
        assertTrue(cart.snapshot.isEmpty)
        coVerify(exactly = 0) { gateway.pay(any(), any(), any(), any(), any()) }
    }

    @Test
    fun placeAndPay_upiCaptured_clearsCartAndFinishes() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { api.placeOrder(any(), any()) } returns sampleOrder
        coEvery { api.initiatePayment(any()) } returns upiInit
        coEvery { gateway.pay(activity, upiInit, any(), any(), any()) } returns PaymentOutcome.Captured

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
        advanceUntilIdle()
        vm.selectPaymentMethod("UPI")
        var done: OrderResponse? = null
        vm.placeAndPay(activity) { done = it }
        advanceUntilIdle()

        assertEquals("o1", done?.id)
        assertTrue(cart.snapshot.isEmpty)
        assertTrue(vm.state.value.paymentCaptured)
    }

    @Test
    fun placeAndPay_upiDeclined_keepsCart_andRetriesPaymentNotTheOrder() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { api.placeOrder(any(), any()) } returns sampleOrder
        coEvery { api.initiatePayment(any()) } returns upiInit
        coEvery { gateway.pay(any(), any(), any(), any(), any()) } returns
            PaymentOutcome.Declined("Payment cancelled. Nothing was taken.") andThen PaymentOutcome.Captured

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
        advanceUntilIdle()
        vm.selectPaymentMethod("UPI")
        var done: OrderResponse? = null
        vm.placeAndPay(activity) { done = it }
        advanceUntilIdle()

        assertNull(done)
        assertFalse(cart.snapshot.isEmpty)
        assertEquals("o1", vm.state.value.awaitingPaymentFor?.id)
        assertEquals("Payment cancelled. Nothing was taken.", vm.state.value.paymentProblem)

        vm.placeAndPay(activity) { done = it }
        advanceUntilIdle()

        assertEquals("o1", done?.id)
        coVerify(exactly = 1) { api.placeOrder(any(), any()) }   // one order, two payment attempts
    }

    @Test
    fun placeAndPay_upiUnconfirmed_doesNotOfferToPayAgain() = runTest(dispatcher) {
        coEvery { repo.addresses() } returns listOf(primary)
        coEvery { api.placeOrder(any(), any()) } returns sampleOrder
        coEvery { api.initiatePayment(any()) } returns upiInit
        coEvery { gateway.pay(any(), any(), any(), any(), any()) } returns PaymentOutcome.Unconfirmed("confirming")

        val vm = CheckoutViewModel(repo, api, cart, orderStore, gateway)
        advanceUntilIdle()
        vm.selectPaymentMethod("UPI")
        var done: OrderResponse? = null
        vm.placeAndPay(activity) { done = it }
        advanceUntilIdle()

        assertEquals("o1", done?.id)
        assertNull(vm.state.value.awaitingPaymentFor)
    }
}
