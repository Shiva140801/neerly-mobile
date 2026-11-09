package com.neerly.mobile.feature.notification

import com.neerly.mobile.data.dto.NotificationPrefDto
import com.neerly.mobile.data.repo.CustomerRepository
import com.neerly.mobile.feature.notification.NotificationPrefsViewModel.Topic
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPrefsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = NotificationPrefsViewModel(repo, com.neerly.mobile.support.TestStrings)

    @Test fun defaults_matchTheBoard_offersOff_restOn() = runTest(dispatcher) {
        coEvery { repo.notificationPrefs() } returns emptyList()
        val vm = vm(); advanceUntilIdle()
        assertTrue(vm.isOn(Topic.ORDER))
        assertTrue(vm.isOn(Topic.SUBS))
        assertTrue(vm.isOn(Topic.QUIET))
        assertTrue(vm.isOn(Topic.SMS))
        assertFalse(vm.isOn(Topic.OFFERS))
    }

    @Test fun orderUpdates_areLocked_onEvenIfTheServerSaysOff() = runTest(dispatcher) {
        coEvery { repo.notificationPrefs() } returns listOf(NotificationPrefDto("PUSH", "ORDER_UPDATES", false))
        val vm = vm(); advanceUntilIdle()
        assertTrue(vm.isOn(Topic.ORDER))
        vm.toggle(Topic.ORDER, false); advanceUntilIdle()
        coVerify(exactly = 0) { repo.setNotificationPref(any(), any(), any()) }
    }

    @Test fun toggle_savesTheRow_andRollsBackOnFailure() = runTest(dispatcher) {
        coEvery { repo.notificationPrefs() } returns emptyList()
        val vm = vm(); advanceUntilIdle()
        vm.toggle(Topic.OFFERS, true); advanceUntilIdle()
        coVerify { repo.setNotificationPref("PUSH", "OFFERS", true) }
        assertTrue(vm.isOn(Topic.OFFERS))

        coEvery { repo.setNotificationPref("SMS", "ORDER_UPDATES", false) } throws RuntimeException("down")
        vm.toggle(Topic.SMS, false); advanceUntilIdle()
        assertTrue(vm.isOn(Topic.SMS))
        assertEquals(true, vm.state.value.error != null)
    }
}
