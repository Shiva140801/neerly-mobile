package com.neerly.mobile.feature.event

import com.neerly.mobile.data.repo.CustomerRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class EventBookingViewModelTest {
    private val repo: CustomerRepository = mockk(relaxed = true)

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { repo.addresses() } returns emptyList()
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun applyMinAdvance_pushesTheDefaultSlotPastTheServersNotice() {
        val vm = EventBookingViewModel(repo, com.neerly.mobile.support.TestStrings)
        val now = Instant.parse("2026-11-07T10:20:00Z")
        vm.applyMinAdvance(72, now)
        val s = vm.state.value
        assertFalse(s.eventStart.isBefore(now.plus(72, ChronoUnit.HOURS)))
        assertTrue(s.eventEnd.isAfter(s.eventStart))
    }

    @Test
    fun setStart_tooSoon_isRefusedWithAReason() {
        val vm = EventBookingViewModel(repo, com.neerly.mobile.support.TestStrings)
        vm.applyMinAdvance(72)
        val before = vm.state.value.eventStart
        vm.setStart(Instant.now().plus(1, ChronoUnit.HOURS))
        assertEquals(before, vm.state.value.eventStart)
        assertNotNull(vm.state.value.error)
    }
}
