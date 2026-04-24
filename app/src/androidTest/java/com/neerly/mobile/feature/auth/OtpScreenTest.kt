package com.neerly.mobile.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.neerly.mobile.core.design.NeerlyTheme
import org.junit.Rule
import org.junit.Test

class OtpScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun otpScreen_rendersTitleAndMaskedPhone() {
        rule.setContent {
            NeerlyTheme { OtpScreen(phone = "+91 98765 43210", onVerified = {}) }
        }

        rule.onNodeWithText("Enter OTP").assertIsDisplayed()
        rule.onNodeWithText("+91 98765 43210", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Change", substring = true).assertIsDisplayed()
    }

    @Test
    fun otpScreen_verifyDisabledUntilSixDigits() {
        rule.setContent {
            NeerlyTheme { OtpScreen(phone = "+91 98765 43210", onVerified = {}) }
        }

        rule.onNodeWithText("Verify").assertIsNotEnabled()
    }

    @Test
    fun otpScreen_showsResendTimer() {
        rule.setContent {
            NeerlyTheme { OtpScreen(phone = "+91 98765 43210", onVerified = {}) }
        }

        rule.onNodeWithText("Resend", substring = true).assertIsDisplayed()
    }
}
