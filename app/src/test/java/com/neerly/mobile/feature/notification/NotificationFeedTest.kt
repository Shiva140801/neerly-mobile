package com.neerly.mobile.feature.notification

import com.neerly.mobile.data.dto.NotificationResponse
import com.neerly.mobile.data.repo.TrustRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationFeedTest {
    private val repo: TrustRepository = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun row(id: String, read: Boolean) = NotificationResponse(
        id = id, userId = "u", channel = "IN_APP", category = "ORDER_UPDATES", subject = "s", body = "b",
        status = "SENT", queuedAt = "2026-11-07T04:00:00Z", readAt = if (read) "2026-11-07T05:00:00Z" else null)

    @Test
    fun markAllRead_isOneServerCall_notOnePerRow() {
        coEvery { repo.notifications() } returns listOf(row("a", false), row("b", true), row("c", false))
        coEvery { repo.markAllNotificationsRead() } returns 2
        val vm = NotificationFeedViewModel(repo, com.neerly.mobile.support.TestStrings)
        vm.markAllRead()
        assertTrue(vm.state.value.items.all { it.isRead })
        coVerify(exactly = 1) { repo.markAllNotificationsRead() }
        coVerify(exactly = 0) { repo.markNotificationRead(any()) }
    }

    @Test
    fun markAllRead_failure_putsTheUnreadDotsBack() {
        coEvery { repo.notifications() } returns listOf(row("a", false), row("b", true))
        coEvery { repo.markAllNotificationsRead() } throws RuntimeException("offline")
        val vm = NotificationFeedViewModel(repo, com.neerly.mobile.support.TestStrings)
        vm.markAllRead()
        assertTrue(vm.state.value.items.first { it.id == "a" }.isRead.not())
    }

    @Test
    fun dayHeading_usesIndiaTime() {
        val today = LocalDate.of(2026, 11, 7)
        // 20:00 UTC on the 6th is 01:30 IST on the 7th — today, not yesterday.
        assertEquals("Today", dayHeading("2026-11-06T20:00:00Z", today, com.neerly.mobile.support.TestStrings))
        assertEquals("Yesterday", dayHeading("2026-11-06T10:00:00Z", today, com.neerly.mobile.support.TestStrings))
        assertEquals("Wed 4 Nov", dayHeading("2026-11-04T10:00:00Z", today, com.neerly.mobile.support.TestStrings))
        assertEquals("Earlier", dayHeading("not a date", today, com.neerly.mobile.support.TestStrings))
    }

    @Test
    fun loadFailure_showsFriendlyCopy() {
        coEvery { repo.notifications() } throws java.io.IOException("socket closed")
        val vm = NotificationFeedViewModel(repo, com.neerly.mobile.support.TestStrings)
        assertTrue(vm.state.value.error!!.isNotBlank())
        assertTrue(!vm.state.value.error!!.contains("socket"))
    }
}
