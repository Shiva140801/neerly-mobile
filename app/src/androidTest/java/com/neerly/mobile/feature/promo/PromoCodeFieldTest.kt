package com.neerly.mobile.feature.promo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.neerly.mobile.core.design.NeerlyTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class PromoCodeFieldTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun field_rendersApplyWhenNoDiscount() {
        rule.setContent {
            NeerlyTheme {
                PromoCodeField(
                    orderSubtotal = BigDecimal("200.00"),
                    isFirstOrder = true,
                    currentDiscount = BigDecimal.ZERO,
                    currentReason = null,
                    onQuote = {},
                    onRemove = {}
                )
            }
        }
        rule.onNodeWithText("Apply").assertIsDisplayed()
        rule.onNodeWithText("first-order", substring = true).assertIsDisplayed()
    }

    @Test
    fun field_showsRejectionReason() {
        rule.setContent {
            NeerlyTheme {
                PromoCodeField(
                    orderSubtotal = BigDecimal("50.00"),
                    isFirstOrder = false,
                    currentDiscount = BigDecimal.ZERO,
                    currentReason = "Min order value is 100.00",
                    onQuote = {},
                    onRemove = {}
                )
            }
        }
        rule.onNodeWithText("Min order value is 100.00").assertIsDisplayed()
    }

    @Test
    fun field_showsAppliedDiscount() {
        rule.setContent {
            NeerlyTheme {
                PromoCodeField(
                    orderSubtotal = BigDecimal("500.00"),
                    isFirstOrder = false,
                    currentDiscount = BigDecimal("100.00"),
                    currentReason = null,
                    onQuote = {},
                    onRemove = {}
                )
            }
        }
        rule.onNodeWithText("₹100.00 off").assertIsDisplayed()
        rule.onNodeWithText("Remove").assertIsDisplayed()
    }
}
