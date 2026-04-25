package com.neerly.mobile.feature.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neerly.mobile.core.design.NeerlyTheme
import org.junit.Rule
import org.junit.Test

class ReviewSubmitScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun reviewScreen_rendersHeadingAndTwoAxes() {
        rule.setContent {
            NeerlyTheme {
                ReviewSubmitScreen(
                    orderId = "abc12345",
                    vendorName = "Sri Ganesh Water Supply",
                    onSubmit = { _, _, _, _ -> },
                    onBack = {}
                )
            }
        }
        rule.onNodeWithText("Sri Ganesh Water Supply").assertIsDisplayed()
        rule.onNodeWithText("Vendor experience").assertIsDisplayed()
        rule.onNodeWithText("Water quality").assertIsDisplayed()
        rule.onNodeWithText("Submit review").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun reviewScreen_bothAxesRated_enablesSubmit() {
        rule.setContent {
            NeerlyTheme {
                ReviewSubmitScreen(
                    orderId = "abc12345",
                    vendorName = "Sri Ganesh",
                    onSubmit = { _, _, _, _ -> },
                    onBack = {}
                )
            }
        }
        // 10 star buttons in total (5 vendor + 5 water). Tap the 5th and 10th.
        val stars = rule.onAllNodesWithText("★")
        stars[4].performClick()   // 5-star vendor
        stars[9].performClick()   // 5-star water
        rule.waitForIdle()
        rule.onNodeWithText("Submit review").assertIsEnabled()
    }

    @Test
    fun reviewScreen_submitFiresCallbackWithBothAxes() {
        var captured: Quadruple<Int, Int, Int, String?>? = null
        rule.setContent {
            NeerlyTheme {
                ReviewSubmitScreen(
                    orderId = "o",
                    vendorName = "V",
                    onSubmit = { overall, vendor, water, text ->
                        captured = Quadruple(overall, vendor, water, text)
                    },
                    onBack = {}
                )
            }
        }
        val stars = rule.onAllNodesWithText("★")
        stars[4].performClick()   // 5-star vendor
        stars[9].performClick()   // 5-star water
        rule.waitForIdle()
        rule.onNodeWithText("Submit review").performClick()
        rule.waitForIdle()
        assert(captured?.b == 5 && captured?.c == 5) { "expected 5/5, got $captured" }
    }
}

private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
