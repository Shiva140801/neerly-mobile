package com.neerly.mobile.feature.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewTextTest {
    private val ORDER = listOf("On time", "Polite", "Jar was clean", "Seal intact", "Carried it upstairs", "Swapped quickly")

    @Test
    fun reviewText_tagsInBoardOrderThenNote() {
        assertEquals("On time · Seal intact\nLeft it at the gate", reviewText(setOf("Seal intact", "On time"), "  Left it at the gate ", ORDER))
    }

    @Test
    fun reviewText_nothingPicked_isNull() {
        assertNull(reviewText(emptySet(), "   ", ORDER))
    }

    @Test
    fun reviewText_tagsOnly() {
        assertEquals("Polite", reviewText(setOf("Polite"), "", ORDER))
    }
}
