package com.neerly.mobile.feature.vendor

import com.neerly.mobile.feature.vendor.dashboard.PAUSE_HOURS
import com.neerly.mobile.feature.vendor.dashboard.pauseEndLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PauseLabelTest {
    private val now = Instant.parse("2026-11-10T04:00:00Z") // 9:30 AM IST

    @Test fun sameDay_showsJustTheTime() = assertEquals("4:30 PM", pauseEndLabel("2026-11-10T11:00:00Z", now))

    @Test fun anotherDay_showsTheDayToo() = assertEquals("Wed 11 Nov, 6:00 AM", pauseEndLabel("2026-11-11T00:30:00Z", now))

    @Test fun quickChoices_areWhatTheShopAskedFor() = assertEquals(listOf(1, 2, 3, 5, 10), PAUSE_HOURS)
}
