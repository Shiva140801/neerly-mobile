package com.neerly.mobile.feature.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neerly.mobile.core.design.NeerlyTheme
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun welcomeScreen_rendersHeadlineAndCta() {
        rule.setContent { NeerlyTheme { WelcomeScreen(onGetStarted = {}) } }

        rule.onNodeWithText("Water,", substring = true).assertIsDisplayed()
        rule.onNodeWithText("sorted.", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Order clean water from trusted suppliers near you.").assertIsDisplayed()
        rule.onNodeWithText("Get started").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun welcomeScreen_tapCta_invokesCallback() {
        var clicked = false
        rule.setContent { NeerlyTheme { WelcomeScreen(onGetStarted = { clicked = true }) } }

        rule.onNodeWithText("Get started").performClick()
        rule.waitForIdle()

        assert(clicked) { "Expected onGetStarted callback to fire" }
    }

    @Test
    fun welcomeScreen_showsVendorLinkAndTerms() {
        rule.setContent { NeerlyTheme { WelcomeScreen(onGetStarted = {}) } }

        rule.onNodeWithText("Register as vendor", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Terms", substring = true).assertIsDisplayed()
    }
}
