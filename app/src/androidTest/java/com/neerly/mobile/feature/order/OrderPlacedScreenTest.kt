package com.neerly.mobile.feature.order

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neerly.mobile.core.design.NeerlyTheme
import org.junit.Rule
import org.junit.Test

class OrderPlacedScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun screen_rendersOrderNumberAndCtas() {
        rule.setContent {
            NeerlyTheme {
                OrderPlacedScreen(
                    orderId = "abc12345",
                    orderNumber = "NEE-26115-0001",
                    onTrack = {},
                    onHome = {}
                )
            }
        }
        rule.onNodeWithText("Order placed").assertIsDisplayed()
        rule.onNodeWithText("Order #NEE-26115-0001").assertIsDisplayed()
        rule.onNodeWithText("Track order").assertIsDisplayed()
        rule.onNodeWithText("Back to home").assertIsDisplayed()
    }

    @Test
    fun trackButton_invokesCallback() {
        var tracked = false
        rule.setContent {
            NeerlyTheme {
                OrderPlacedScreen(
                    orderId = "abc",
                    orderNumber = "NEE-26115-0001",
                    onTrack = { tracked = true },
                    onHome = {}
                )
            }
        }
        rule.onNodeWithText("Track order").performClick()
        rule.waitForIdle()
        assert(tracked) { "expected onTrack to fire" }
    }

    @Test
    fun homeButton_invokesCallback() {
        var home = false
        rule.setContent {
            NeerlyTheme {
                OrderPlacedScreen(
                    orderId = "abc",
                    orderNumber = "NEE-26115-0001",
                    onTrack = {},
                    onHome = { home = true }
                )
            }
        }
        rule.onNodeWithText("Back to home").performClick()
        rule.waitForIdle()
        assert(home) { "expected onHome to fire" }
    }
}
