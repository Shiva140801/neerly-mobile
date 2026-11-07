package com.neerly.mobile.feature.wallet

import com.neerly.mobile.data.dto.InitiatePaymentResult
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.dto.WalletTopupRequest
import com.neerly.mobile.data.dto.WalletTransaction
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
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()
    private val gateway: com.neerly.mobile.feature.checkout.GatewayPayments = mockk()
    private val activity: android.app.Activity = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun load_populatesBalanceAndTransactions() = runTest(dispatcher) {
        coEvery { repo.wallet() } returns WalletResponse("u1",
            BigDecimal("500"), BigDecimal("0"), BigDecimal("500"))
        coEvery { repo.walletTransactions() } returns listOf(
            WalletTransaction("t1", "TOPUP", BigDecimal("500"),
                BigDecimal("500"), "UPI", "2026-04-25T09:00:00Z")
        )
        val vm = WalletViewModel(repo, gateway)
        advanceUntilIdle()

        assertEquals(BigDecimal("500"), vm.state.value.balance?.availableAmount)
        assertEquals(1, vm.state.value.transactions.size)
    }

    @Test
    fun topup_invalidAmount_setsError() = runTest(dispatcher) {
        coEvery { repo.wallet() } returns WalletResponse("u1",
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        coEvery { repo.walletTransactions() } returns emptyList()

        val vm = WalletViewModel(repo, gateway)
        advanceUntilIdle()
        vm.topup(activity, BigDecimal.ZERO)
        advanceUntilIdle()

        assertEquals("Enter a valid amount", vm.state.value.error)
    }

    @Test
    fun topup_captured_saysSo_andReloadsBalance() = runTest(dispatcher) {
        coEvery { repo.wallet() } returns WalletResponse("u1",
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO) andThen WalletResponse("u1",
            BigDecimal("500"), BigDecimal.ZERO, BigDecimal("500"))
        coEvery { repo.walletTransactions() } returns emptyList()
        val init = InitiatePaymentResult("pay1", "rzp_order_1", 50000L, "rzp_test_key")
        coEvery { repo.walletTopup(any<WalletTopupRequest>()) } returns init
        coEvery { gateway.pay(activity, init, any(), any(), any()) } returns
            com.neerly.mobile.feature.checkout.PaymentOutcome.Captured

        val vm = WalletViewModel(repo, gateway)
        advanceUntilIdle()
        vm.topup(activity, BigDecimal("500"))
        advanceUntilIdle()

        assertEquals(BigDecimal("500"), vm.state.value.balance?.availableAmount)
    }

    @Test
    fun topup_declined_showsWhy() = runTest(dispatcher) {
        coEvery { repo.wallet() } returns WalletResponse("u1",
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        coEvery { repo.walletTransactions() } returns emptyList()
        coEvery { repo.walletTopup(any<WalletTopupRequest>()) } returns
            InitiatePaymentResult("pay1", "rzp_order_1", 50000L, "rzp_test_key")
        coEvery { gateway.pay(any(), any(), any(), any(), any()) } returns
            com.neerly.mobile.feature.checkout.PaymentOutcome.Declined("Payment cancelled. Nothing was taken.")

        val vm = WalletViewModel(repo, gateway)
        advanceUntilIdle()
        vm.topup(activity, BigDecimal("500"))
        advanceUntilIdle()

        assertEquals("Payment cancelled. Nothing was taken.", vm.state.value.error)
    }

    @Test
    fun load_failure_leavesBalanceNullAndUsesPlainCopy() = runTest(dispatcher) {
        coEvery { repo.wallet() } throws
            RuntimeException("Unable to create converter for WalletResponse")

        val vm = WalletViewModel(repo, gateway)
        advanceUntilIdle()

        // The screen keys "did the load succeed?" off `balance`, so a failed load
        // must never leave a zero balance behind for the UI to render as fact.
        assertNull(vm.state.value.balance)
        assertEquals("Couldn't load your balance. Please try again.", vm.state.value.error)
    }

    private fun fail(msg: String): Nothing = throw AssertionError(msg)
}
