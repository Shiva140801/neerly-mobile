package com.neerly.mobile.feature.complaint

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.neerly.mobile.core.design.NeerlyTheme
import org.junit.Rule
import org.junit.Test

class ComplaintFileScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun complaintScreen_showsCategoryChipsAndDisabledSubmit() {
        rule.setContent {
            NeerlyTheme {
                ComplaintFileScreen(
                    orderId = "abc12345",
                    onSubmit = { _, _, _ -> },
                    onBack = {}
                )
            }
        }
        rule.onNodeWithText("What happened?").assertIsDisplayed()
        rule.onNodeWithText("Late delivery").assertIsDisplayed()
        rule.onNodeWithText("Water quality").assertIsDisplayed()
        rule.onNodeWithText("File complaint").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun complaintScreen_showsSlaCopy() {
        rule.setContent {
            NeerlyTheme {
                ComplaintFileScreen(orderId = null, onSubmit = { _, _, _ -> }, onBack = {})
            }
        }
        rule.onNodeWithText("24 hours", substring = true).assertIsDisplayed()
    }
}
