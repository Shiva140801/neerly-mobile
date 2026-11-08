package com.neerly.mobile.feature.vendor.settings

import com.neerly.mobile.data.dto.VendorHoursRow
import com.neerly.mobile.data.repo.VendorRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class VendorSettingsTest {
    private val repo: VendorRepository = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun hoursSummary_groupsMatchingDays() {
        val rows = (1..6).map { VendorHoursRow(it, "06:00:00", "21:00:00") } + VendorHoursRow(7, "06:00:00", "13:00:00")
        assertEquals("Mon–Sat 6 AM–9 PM · Sun 6 AM–1 PM", hoursSummary(rows))
        assertEquals("Hours not set", hoursSummary(emptyList()))
    }

    @Test
    fun closure_addsEachDayAndSaysSubscribersAreTold() {
        coEvery { repo.holidays() } returns emptyList()
        val vm = VendorBusinessConfigViewModel(repo, com.neerly.mobile.support.TestStrings)
        vm.addClosure(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 10), "Diwali")
        coVerify { repo.addHoliday("2026-11-09", "Diwali") }
        coVerify { repo.addHoliday("2026-11-10", "Diwali") }
        assertTrue(vm.state.value.saved!!.contains("2 days"))
        assertTrue(vm.state.value.saved!!.contains("Wed 11 Nov"))
    }

    @Test
    fun removingADay_isHonestAboutSkippedJars() {
        coEvery { repo.holidays() } returns emptyList()
        val vm = VendorBusinessConfigViewModel(repo, com.neerly.mobile.support.TestStrings)
        vm.removeHoliday("2026-11-09")
        assertTrue(vm.state.value.saved!!.contains("stay skipped"))
    }

    @Test
    fun hours_closingBeforeOpening_isRefused() {
        coEvery { repo.holidays() } returns emptyList()
        val vm = VendorBusinessConfigViewModel(repo, com.neerly.mobile.support.TestStrings)
        vm.saveHours(listOf(VendorHoursRow(1, "21:00:00", "06:00:00")))
        assertNotNull(vm.state.value.error)
        coVerify(exactly = 0) { repo.replaceHours(any()) }
        assertFalse(validSpan("09:00", "08:00"))
        assertEquals("06:30:00", padTime("6:3"))
    }
}
