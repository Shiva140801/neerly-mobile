package com.neerly.mobile.feature.auth

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.ReferralPreview
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralCodeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api: NeerlyApi = mockk()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val priya = ReferralPreview(true, "Priya M.", BigDecimal("50"), BigDecimal("100"))

    @Test
    fun typing_normalisesAndLooksUpTheCode() = runTest(dispatcher) {
        coEvery { api.previewReferral("NR001012") } returns priya
        val vm = ReferralCodeViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.type("nr-001012")
        advanceUntilIdle()
        assertEquals("NR001012", vm.state.value.code)
        assertEquals("Priya M.", vm.state.value.preview?.referrerName)
    }

    @Test
    fun noValidCode_continueSkipsTheClaim() = runTest(dispatcher) {
        val vm = ReferralCodeViewModel(api, com.neerly.mobile.support.TestStrings)
        var done = false
        vm.claim { done = true }
        assertTrue(done)
        coVerify(exactly = 0) { api.submitReferralCode(any()) }
    }

    @Test
    fun unknownCode_showsAProblemAndNoPreview() = runTest(dispatcher) {
        coEvery { api.previewReferral(any()) } throws RuntimeException("boom")
        val vm = ReferralCodeViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.type("NR999999")
        advanceUntilIdle()
        assertNull(vm.state.value.preview)
        assertNotNull(vm.state.value.problem)
    }

    @Test
    fun validCode_isClaimedWithTheBackendFieldName() = runTest(dispatcher) {
        coEvery { api.previewReferral(any()) } returns priya
        coEvery { api.submitReferralCode(any()) } returns priya
        val vm = ReferralCodeViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.type("NR001012"); advanceUntilIdle()
        var done = false
        vm.claim { done = true }; advanceUntilIdle()
        assertTrue(done)
        coVerify { api.submitReferralCode(match { it.code == "NR001012" }) }
    }
}
