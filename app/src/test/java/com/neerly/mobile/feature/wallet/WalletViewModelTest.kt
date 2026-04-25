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
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()

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
        val vm = WalletViewModel(repo)
        advanceUntilIdle()

        assertEquals(BigDecimal("500"), vm.state.value.balance?.availableAmount)
        assertEquals(1, vm.state.value.transactions.size)
    }

    @Test
    fun topup_invalidAmount_setsError() = runTest(dispatcher) {
        coEvery { repo.wallet() } returns WalletResponse("u1",
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        coEvery { repo.walletTransactions() } returns emptyList()

        val vm = WalletViewModel(repo)
        advanceUntilIdle()
        vm.topup(BigDecimal.ZERO) { fail("should not call") }
        advanceUntilIdle()

        assertEquals("Enter a valid amount", vm.state.value.error)
    }

    @Test
    fun topup_success_invokesCallbackWithResult() = runTest(dispatcher) {
        coEvery { repo.wallet() } returns WalletResponse("u1",
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        coEvery { repo.walletTransactions() } returns emptyList()
        coEvery { repo.walletTopup(any<WalletTopupRequest>()) } returns
            InitiatePaymentResult("pay1", "rzp_order_1", 50000L, "rzp_test_key")

        val vm = WalletViewModel(repo)
        advanceUntilIdle()
        var captured: InitiatePaymentResult? = null
        vm.topup(BigDecimal("500")) { captured = it }
        advanceUntilIdle()

        assertEquals("rzp_order_1", captured?.razorpayOrderId)
    }

    private fun fail(msg: String): Nothing = throw AssertionError(msg)
}
