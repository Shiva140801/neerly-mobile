package com.neerly.mobile.feature.auth

import com.neerly.mobile.data.auth.AuthRepository
import com.neerly.mobile.data.dto.DevSendOtpResponse
import com.neerly.mobile.support.TestStrings
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class OtpResendTest {
    private val dispatcher = StandardTestDispatcher()
    private val sender: OtpSender = mockk()
    private val repo: AuthRepository = mockk(relaxed = true)
    private var clock = 1_000_000L

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = AuthViewModel(repo, TestStrings, sender).also { it.now = { clock } }

    @Test fun policy_countsDownFromTheLastSend_roundingUp() {
        val p = ResendPolicy()
        assertEquals(ResendPolicy.Gate.Cooldown(30), p.gate(1_000, lastSentAtMs = 1_000, resendsMs = emptyList()))
        assertEquals(ResendPolicy.Gate.Cooldown(23), p.gate(8_000, 1_000, emptyList()))
        assertEquals(ResendPolicy.Gate.Cooldown(1), p.gate(30_500, 1_000, emptyList()))
        assertEquals(ResendPolicy.Gate.Open, p.gate(31_000, 1_000, emptyList()))
        assertEquals(ResendPolicy.Gate.Open, p.gate(0, null, emptyList()))
    }

    @Test fun policy_capsResendsPerRollingHour() {
        val p = ResendPolicy()
        val sends = listOf(0L, 60_000L, 120_000L)
        assertEquals(ResendPolicy.Gate.Limited(3_600_000L), p.gate(200_000L, 120_000L, sends))
        // An hour after the first resend, one slot frees up.
        assertEquals(ResendPolicy.Gate.Open, p.gate(3_600_000L, 120_000L, sends))
    }

    @Test fun clock_formatsLikeTheBoard() {
        assertEquals("0:23", resendClock(23))
        assertEquals("1:05", resendClock(65))
        assertEquals("0:00", resendClock(-4))
    }

    @Test fun smsParsing_takesTheStandaloneSixDigitCode() {
        assertEquals("574681", otpFromSms("574681 is your Boondh code. Don't share it. -Boondh"))
        assertEquals("120034", otpFromSms("<#> Your code: 120034\nFA+9qCX9VSu"))
        assertNull(otpFromSms("Call +919876543210 for help"))   // longer digit runs never match
        assertNull(otpFromSms("Pay ₹110 for 2 jars"))
        assertNull(otpFromSms(null))
    }

    @Test fun resend_isBlockedDuringCooldown_thenSendsAndRestartsTheClock() = runTest(dispatcher) {
        coEvery { sender.send(any(), any()) } returns Result.success(DevSendOtpResponse("+919876500123", 300, "hint"))
        val vm = vm()
        vm.otpScreenShown()
        assertEquals(ResendPolicy.Gate.Cooldown(30), vm.resendGate())

        vm.resendOtp("+919876500123", "CUSTOMER")
        advanceUntilIdle()
        coVerify(exactly = 0) { sender.send(any(), any()) }

        clock += 30_000
        vm.resendOtp("+919876500123", "CUSTOMER")
        advanceUntilIdle()
        coVerify(exactly = 1) { sender.send("+919876500123", "CUSTOMER") }
        assertEquals("We sent a new code.", vm.state.value.resendNotice)
        assertEquals(ResendPolicy.Gate.Cooldown(30), vm.resendGate())
    }

    @Test fun resend_failureShowsAMessage_andKeepsTheGateOpen() = runTest(dispatcher) {
        coEvery { sender.send(any(), any()) } returns Result.failure(IOException("no network"))
        val vm = vm()
        vm.otpScreenShown()
        clock += 31_000
        vm.resendOtp("+919876500123", "CUSTOMER")
        advanceUntilIdle()
        assertEquals(false, vm.state.value.resending)
        assertEquals(true, vm.state.value.error?.isNotBlank())
        assertEquals(ResendPolicy.Gate.Open, vm.resendGate())
    }

    @Test fun resend_stopsAfterThreeInAnHour() = runTest(dispatcher) {
        coEvery { sender.send(any(), any()) } returns Result.success(DevSendOtpResponse("+919876500123", 300, "hint"))
        val vm = vm()
        vm.otpScreenShown()
        repeat(4) {
            clock += 31_000
            vm.resendOtp("+919876500123", "CUSTOMER")
            advanceUntilIdle()
        }
        coVerify(exactly = 3) { sender.send(any(), any()) }
        assertEquals(true, vm.resendGate() is ResendPolicy.Gate.Limited)
    }
}
