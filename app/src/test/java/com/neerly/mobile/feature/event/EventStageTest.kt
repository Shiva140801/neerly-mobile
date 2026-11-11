package com.neerly.mobile.feature.event

import org.junit.Assert.assertEquals
import org.junit.Test

/** Board EventRequestSent: every server status lands on one of the screen's states. */
class EventStageTest {
    @Test fun pendingIsWaiting() = assertEquals(EventStage.WAITING, eventStage("PENDING_CONFIRMATION"))
    @Test fun confirmedAndPaidAreBooked() {
        listOf("VENDOR_CONFIRMED", "ADVANCE_PAID", "IN_PROGRESS", "DELIVERED").forEach { assertEquals(EventStage.BOOKED, eventStage(it)) }
    }
    @Test fun collectedIsDone() { listOf("PICKED_UP", "COMPLETED").forEach { assertEquals(EventStage.DONE, eventStage(it)) } }
    @Test fun rejectedIsSaidNo() = assertEquals(EventStage.SAID_NO, eventStage("VENDOR_REJECTED"))
    @Test fun expiredIsExpired() = assertEquals(EventStage.EXPIRED, eventStage("EXPIRED"))
    @Test fun cancelledIsWithdrawn() { listOf("CANCELLED", "FAILED").forEach { assertEquals(EventStage.WITHDRAWN, eventStage(it)) } }
}
