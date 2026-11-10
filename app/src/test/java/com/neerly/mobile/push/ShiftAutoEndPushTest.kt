package com.neerly.mobile.push

import org.junit.Assert.assertEquals
import org.junit.Test

/** Shift auto-end pushes (D13–D15, V18): who they're for, where they open, what "Stay on" does. */
class ShiftAutoEndPushTest {

    @Test fun newTypes_areKnownByTheirServerNames() {
        for (t in listOf("DRIVER_SHIFT_IDLE_WARN", "DRIVER_SHIFT_AUTO_ENDED", "DRIVER_SHIFT_LIMIT", "SELLER_DRIVER_SHIFT_ENDED")) {
            assertEquals(t, PushType.of(t)?.name)
        }
        assertEquals(PushRole.DRIVER, PushType.DRIVER_SHIFT_AUTO_ENDED.role)
        assertEquals(PushRole.SELLER, PushType.SELLER_DRIVER_SHIFT_ENDED.role)
    }

    @Test fun warning_offersStayOn_asABackgroundAction() {
        assertEquals(listOf(PushAction.STAY_ON), PushType.DRIVER_SHIFT_IDLE_WARN.defaultActions)
        assertEquals(PushAction.Kind.BACKGROUND, PushAction.STAY_ON.kind)
        assertEquals(PushChannel.DRIVER_DAYEND, PushType.DRIVER_SHIFT_IDLE_WARN.channel)
    }

    @Test fun links_openTheDriverHome_andTheShopsTeam() {
        val p = PushPayload(v = 1, type = "DRIVER_SHIFT_AUTO_ENDED", role = "DRIVER", data = mapOf("shiftId" to "s1"))
        assertEquals("driver/home", DeepLinks.route(DeepLinks.default(PushType.DRIVER_SHIFT_AUTO_ENDED, p)))
        assertEquals("driver/home", DeepLinks.route(DeepLinks.default(PushType.DRIVER_SHIFT_IDLE_WARN, p)))
        assertEquals("driver/home", DeepLinks.route(DeepLinks.default(PushType.DRIVER_SHIFT_LIMIT, p)))
        assertEquals(DeepLinks.route("boondh://seller/team"),
            DeepLinks.route(DeepLinks.default(PushType.SELLER_DRIVER_SHIFT_ENDED, p.copy(type = "SELLER_DRIVER_SHIFT_ENDED"))))
    }
}

class ShiftAutoEndNoteTest {
    private val now = java.time.Instant.parse("2026-11-11T08:00:00Z")
    private fun last(endedAt: String, auto: Boolean) =
        com.neerly.mobile.feature.driver.LastShift("s1", "2026-11-11T02:00:00Z", endedAt, if (auto) "AUTO_INACTIVE" else "DRIVER_ENDED", auto, 3)

    @Test fun shownOnlyForAnAutoEnd_inTheLast12Hours() {
        org.junit.Assert.assertTrue(com.neerly.mobile.feature.driver.showAutoEnded(last("2026-11-11T05:00:00Z", true), now))
        org.junit.Assert.assertFalse(com.neerly.mobile.feature.driver.showAutoEnded(last("2026-11-10T19:00:00Z", true), now))
        org.junit.Assert.assertFalse(com.neerly.mobile.feature.driver.showAutoEnded(last("2026-11-11T05:00:00Z", false), now))
        org.junit.Assert.assertFalse(com.neerly.mobile.feature.driver.showAutoEnded(null, now))
    }
}
