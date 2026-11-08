package com.neerly.mobile.feature.help

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.api.NeerlyApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelTest {
    private val api: NeerlyApi = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun send_filesAComplaintOnTheOrderWithTheRightCategory() {
        coEvery { api.fileComplaint(any()) } returns mockk(relaxed = true)
        val vm = HelpViewModel(api, SavedStateHandle(mapOf("orderId" to "o1")), com.neerly.mobile.support.TestStrings)
        vm.pick(HelpReason.CHARGED)
        vm.details("UPI took it twice")
        vm.send()
        coVerify { api.fileComplaint(match { it.orderId == "o1" && it.category == "OVERCHARGED" && it.description == "UPI took it twice" }) }
        assertNotNull(vm.state.value.sent)
    }

    @Test
    fun somethingElse_needsAFewWords() {
        val vm = HelpViewModel(api, SavedStateHandle(), com.neerly.mobile.support.TestStrings)
        vm.pick(HelpReason.OTHER)
        assertFalse(vm.state.value.canSend)
        vm.details("The jar is the wrong brand")
        assertTrue(vm.state.value.canSend)
    }

    @Test
    fun send_failure_keepsTheFormAndSaysWhy() {
        coEvery { api.fileComplaint(any()) } throws RuntimeException("down")
        val vm = HelpViewModel(api, SavedStateHandle(), com.neerly.mobile.support.TestStrings)
        vm.pick(HelpReason.LATE)
        vm.send()
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.canSend)
    }

    @org.junit.Test
    fun reasons_followWhereTheOrderIs() {
        org.junit.Assert.assertTrue(HelpReason.LATE in HelpReason.forOrder("DISPATCHED"))
        org.junit.Assert.assertFalse(HelpReason.WATER in HelpReason.forOrder("DISPATCHED"))
        val after = HelpReason.forOrder("DELIVERED")
        org.junit.Assert.assertTrue(HelpReason.WATER in after && HelpReason.SHORT in after)
        org.junit.Assert.assertFalse(HelpReason.LATE in after)
    }
}
