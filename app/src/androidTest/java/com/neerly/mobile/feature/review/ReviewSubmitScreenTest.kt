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
    fun reviewScreen_rendersHeadingAndVendorName() {
        rule.setContent {
            NeerlyTheme {
                ReviewSubmitScreen(
                    orderId = "abc12345",
                    vendorName = "Sri Ganesh Water Supply",
                    onSubmit = { _, _ -> },
                    onBack = {}
                )
            }
        }
        rule.onNodeWithText("Sri Ganesh Water Supply").assertIsDisplayed()
        rule.onNodeWithText("How was it?").assertIsDisplayed()
        rule.onNodeWithText("Submit review").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun reviewScreen_selectingStar_enablesSubmit() {
        rule.setContent {
            NeerlyTheme {
                ReviewSubmitScreen(
                    orderId = "abc12345",
                    vendorName = "Sri Ganesh",
                    onSubmit = { _, _ -> },
                    onBack = {}
                )
            }
        }
        // There are 5 '★' buttons — tap the first one to register rating=1.
        rule.onAllNodesWithText("★")[0].performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Submit review").assertIsEnabled()
    }

    @Test
    fun reviewScreen_submitFiresCallback() {
        var captured: Pair<Int, String?>? = null
        rule.setContent {
            NeerlyTheme {
                ReviewSubmitScreen(
                    orderId = "o",
                    vendorName = "V",
                    onSubmit = { r, t -> captured = r to t },
                    onBack = {}
                )
            }
        }
        rule.onAllNodesWithText("★")[4].performClick()  // 5 stars
        rule.waitForIdle()
        rule.onNodeWithText("Submit review").performClick()
        rule.waitForIdle()
        assert(captured?.first == 5) { "expected rating 5, got $captured" }
    }
}
