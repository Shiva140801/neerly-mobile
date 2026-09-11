package com.neerly.mobile.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Rupee formatting for display.
 *
 * The backend sends money as JSON numbers, so `BigDecimal("85.0").toString()`
 * renders "85.0" — which is how "₹85" ended up on screen as "₹85.0". Prices in
 * this catalogue are whole rupees in practice, so paise are shown only when
 * they're actually non-zero.
 *
 * Grouping uses the Indian convention (₹1,00,000) since every customer-facing
 * amount in the app is INR.
 */
private val indianGrouping: DecimalFormat
    get() = DecimalFormat("#,##,##0", DecimalFormatSymbols(Locale.ENGLISH))

private val indianGroupingWithPaise: DecimalFormat
    get() = DecimalFormat("#,##,##0.00", DecimalFormatSymbols(Locale.ENGLISH))

/** "₹85", "₹1,250", "₹99.50". */
fun BigDecimal.asRupees(): String {
    val scaled = setScale(2, RoundingMode.HALF_UP)
    val body = if (scaled.stripTrailingZeros().scale() <= 0) {
        indianGrouping.format(scaled)
    } else {
        indianGroupingWithPaise.format(scaled)
    }
    return "₹$body"
}

/** Same, tolerating a missing amount. */
fun BigDecimal?.asRupeesOrZero(): String = (this ?: BigDecimal.ZERO).asRupees()
