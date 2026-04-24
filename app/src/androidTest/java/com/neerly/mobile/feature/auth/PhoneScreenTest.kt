package com.neerly.mobile.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.neerly.mobile.core.design.NeerlyTheme
import org.junit.Rule
import org.junit.Test

class PhoneScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun phoneScreen_initialState_sendOtpDisabled() {
        rule.setContent { NeerlyTheme { PhoneScreen(onOtpSent = {}) } }

        rule.onNodeWithText("Enter your mobile number").assertIsDisplayed()
        rule.onNodeWithText("+91").assertIsDisplayed()
        rule.onNodeWithText("Send OTP").assertIsNotEnabled()
    }

    @Test
    fun phoneScreen_validPhone_enablesSendOtp() {
        rule.setContent { NeerlyTheme { PhoneScreen(onOtpSent = {}) } }

        rule.onNodeWithText("98765 43210").performTextReplacement("9876543210")
        rule.waitForIdle()

        rule.onNodeWithText("Send OTP").assertIsEnabled()
    }

    @Test
    fun phoneScreen_invalidPrefix_keepsSendOtpDisabled() {
        rule.setContent { NeerlyTheme { PhoneScreen(onOtpSent = {}) } }

        // Starts with 1 — not valid per Indian mobile rules (must be 6/7/8/9)
        rule.onNodeWithText("98765 43210").performTextReplacement("1234567890")
        rule.waitForIdle()

        rule.onNodeWithText("Send OTP").assertIsNotEnabled()
    }

    @Test
    fun phoneScreen_tapSendOtp_invokesCallbackWithE164() {
        var captured: String? = null
        rule.setContent { NeerlyTheme { PhoneScreen(onOtpSent = { captured = it }) } }

        rule.onNodeWithText("98765 43210").performTextReplacement("9876543210")
        rule.waitForIdle()
        rule.onNodeWithText("Send OTP").performClick()
        rule.waitForIdle()

        assert(captured == "+919876543210") { "Expected E.164 +919876543210, got $captured" }
    }

    @Test
    fun phoneScreen_onlyDigitsAllowed() {
        rule.setContent { NeerlyTheme { PhoneScreen(onOtpSent = {}) } }

        rule.onNodeWithText("98765 43210").performTextInput("abc123!@#456def")
        rule.waitForIdle()

        // Non-digits filtered; only 123456 remains — short, so button still disabled.
        rule.onNodeWithText("Send OTP").assertIsNotEnabled()
    }
}
