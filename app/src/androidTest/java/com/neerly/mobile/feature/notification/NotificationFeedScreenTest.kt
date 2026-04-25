package com.neerly.mobile.feature.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neerly.mobile.core.design.NeerlyTheme
import org.junit.Rule
import org.junit.Test

class NotificationFeedScreenTest {

    @get:Rule val rule = createComposeRule()

    private val sample = listOf(
        FeedItem("n1", "ORDER_PLACED", "Order confirmed", "₹299 from Sri Ganesh", false, "Just now"),
        FeedItem("n2", "COMPLAINT_UPDATE", null, "Admin replied on your complaint", true, "2h ago")
    )

    @Test
    fun feed_showsEmptyStateWhenListIsEmpty() {
        rule.setContent {
            NeerlyTheme {
                NotificationFeedScreen(items = emptyList(), loading = false, onOpenItem = {}, onBack = {})
            }
        }
        rule.onNodeWithText("All caught up").assertIsDisplayed()
    }

    @Test
    fun feed_rendersRowsAndInvokesOpenCallback() {
        var opened: String? = null
        rule.setContent {
            NeerlyTheme {
                NotificationFeedScreen(
                    items = sample,
                    loading = false,
                    onOpenItem = { opened = it },
                    onBack = {}
                )
            }
        }
        rule.onNodeWithText("Order confirmed").assertIsDisplayed()
        rule.onNodeWithText("₹299 from Sri Ganesh").assertIsDisplayed().performClick()
        rule.waitForIdle()
        assert(opened == "n1") { "expected onOpenItem('n1'), got $opened" }
    }
}
