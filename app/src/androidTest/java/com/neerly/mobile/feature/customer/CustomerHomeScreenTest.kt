package com.neerly.mobile.feature.customer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neerly.mobile.core.design.NeerlyTheme
import org.junit.Rule
import org.junit.Test

class CustomerHomeScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun customerHome_rendersGreetingAndVendorList() {
        rule.setContent { NeerlyTheme { CustomerHomeScreen(onVendorClick = {}) } }

        rule.onNodeWithText("Hi, Priya", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Vendors near you").assertIsDisplayed()
        rule.onNodeWithText("Sri Ganesh Water Supply").assertIsDisplayed()
        rule.onNodeWithText("Pure Drops Madhapur").assertIsDisplayed()
    }

    @Test
    fun customerHome_tapVendorCard_invokesCallbackWithId() {
        var picked: String? = null
        rule.setContent { NeerlyTheme { CustomerHomeScreen(onVendorClick = { picked = it }) } }

        rule.onNodeWithText("Sri Ganesh Water Supply").performClick()
        rule.waitForIdle()

        assert(picked == "1") { "Expected vendorId '1', got $picked" }
    }
}
